package org.tmt.m1cs.controlassembly;

import akka.actor.typed.javadsl.ActorContext;
import csw.command.api.javadsl.ICommandService;
import csw.command.client.CommandServiceFactory;
import csw.command.client.messages.TopLevelActorMessage;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.models.AkkaLocation;
import csw.location.models.Connection;
import csw.location.models.LocationUpdated;
import csw.location.models.TrackingEvent;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.CommandResponse;
import csw.params.commands.ControlCommand;
import csw.params.core.models.Prefix;
import csw.time.core.models.UTCTime;

import java.util.HashMap;
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
public class JControlAssemblyHandlers extends JComponentHandlers {

    private final JCswContext cswCtx;
    private final ILogger log;
    private  ActorContext<TopLevelActorMessage> ctx;

    // reference to the template HCD
    private Map<Integer, Optional<ICommandService>> runningHcds;
    private Optional<ICommandService> hcd = Optional.empty(); // NOTE the use of Optional

    JControlAssemblyHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.cswCtx = cswCtx;
        this.ctx = ctx;
        this.log = cswCtx.loggerFactory().getLogger(getClass());
    }


    @Override
    public CompletableFuture<Void> jInitialize() {
    log.info("Initializing segment assembly...");
    return CompletableFuture.runAsync(() -> {
            runningHcds = new HashMap<Integer, Optional<ICommandService>>();
        });
    }

    @Override
    public CompletableFuture<Void> jOnShutdown() {
        return CompletableFuture.runAsync(() -> {

        });
    }


    /* the locationTrackingEvents are how the assembly finds the HCDs
    *  we will create a map of the HCDs based on their segment number
    *  this way we can send commands to all HCDs for testing purposes
    * */

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {
        log.info("in onLocationTrackingEvent()");

        if (trackingEvent instanceof LocationUpdated) {
            // do something for the tracked location when it is updated

            AkkaLocation hcdLocation = (AkkaLocation) ((LocationUpdated) trackingEvent).location();

            hcd = Optional.of(CommandServiceFactory.jMake(hcdLocation, ctx.getSystem()));

            Integer hcdNumber = getSegmentNumber(hcdLocation.prefix());
            log.info("in onLocationTrackingEvent() " + hcd + ", " + hcdNumber);

            runningHcds.put(hcdNumber, hcd);

        }
    }

    private Integer getSegmentNumber(Prefix prefix) {
        String prefixStr = prefix.prefix();
        String numberString = prefixStr.substring(15, prefixStr.length() - 1);
        log.info("localtion Tracking NUMBER- " + numberString);
        return Integer.valueOf(numberString);
    }

    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(ControlCommand controlCommand) {
        return new CommandResponse.Accepted(controlCommand.runId());
    }

    @Override
    public CommandResponse.SubmitResponse onSubmit(ControlCommand controlCommand) {




        return new CommandResponse.Completed(controlCommand.runId());
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

    public void onDiagnosticMode(UTCTime startTime,String hint){

    }


    public void onOperationsMode(){

    }
}