package org.tmt.m1cs.segmenthcd;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import csw.command.client.messages.TopLevelActorMessage;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.models.TrackingEvent;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.CommandResponse;
import csw.params.commands.ControlCommand;
import csw.params.commands.Setup;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.javadsl.JKeyType;
import csw.time.core.models.UTCTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to SegmentHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw/commons/framework.html
 */
public class JSegmentHcdHandlers extends JComponentHandlers {

    private final JCswContext cswCtx;
    private final ILogger log;
    private final ActorContext<TopLevelActorMessage> ctx;
    private ActorRef<JSegCommandHandlerActor.CmdMessage> segCommandHandlerActor;
    private ActorRef<JStatePublisherActor.StatePublisherMessage> statePublisherActor;


    List<ActorRef<ControlCommand>> segmentActorList;

    JSegmentHcdHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.cswCtx = cswCtx;
        this.ctx = ctx;
        this.log = cswCtx.loggerFactory().getLogger(getClass());
    }

    @Override
    public CompletableFuture<Void> jInitialize() {
    log.info("Initializing segment HCD...");
    return CompletableFuture.runAsync(() -> {


        statePublisherActor =
                ctx.spawnAnonymous(JStatePublisherActor.behavior(cswCtx.currentStatePublisher(), cswCtx.loggerFactory()));


        segmentActorList = new ArrayList<ActorRef<ControlCommand>>();

        // This creates 492 worker actors, which are much more lightweight than individual HCDs
        for (int i=0; i<492; i++) {
            ActorRef<ControlCommand> segmentActor =
                    ctx.spawnAnonymous(JSegmentActor.behavior(cswCtx.commandResponseManager(), i+1, cswCtx.loggerFactory()));
            segmentActorList.add(segmentActor);

            log.info("created segment worker " +(i+1) + ":" + segmentActor);
        }

        segCommandHandlerActor =
                ctx.spawnAnonymous(JSegCommandHandlerActor.behavior(cswCtx.commandResponseManager(), true, segmentActorList, cswCtx.loggerFactory()));

        // uncomment to start the CurrentState publishing
        // statePublisherActor.tell(new JStatePublisherActor.StartMessage());


    });
    }

    @Override
    public CompletableFuture<Void> jOnShutdown() {
        return CompletableFuture.runAsync(() -> {

        });
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {

    }

    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(ControlCommand controlCommand) {
        return new CommandResponse.Accepted(controlCommand.runId());
    }

    @Override
    public CommandResponse.SubmitResponse onSubmit(ControlCommand controlCommand) {

        // this is where the HCD handles commands from the assembly
        switch (controlCommand.commandName().name()) {

            case "configure":
                log.debug("handling config command: " + controlCommand);

                JSegCommandHandlerActor.SubmitCommandMessage message = new JSegCommandHandlerActor.SubmitCommandMessage(controlCommand);

                segCommandHandlerActor.tell(message);

                //cswCtx.commandResponseManager().addOrUpdateCommand(new CommandResponse.Completed(controlCommand.runId()));

                break;

            default:
                log.error("unhandled message in Monitor Actor onMessage: " + controlCommand);
                // maintain actor state

        }

        return new CommandResponse.Started(controlCommand.runId());
    }

    @Override
    public void onOneway(ControlCommand controlCommand) {

    }

    @Override
    public void onGoOffline() {

    }

    @Override
    public void onGoOnline() {

    }


    public void onDiagnosticMode(UTCTime startTime, String hint){

    }


    public void onOperationsMode() {

    }
}
