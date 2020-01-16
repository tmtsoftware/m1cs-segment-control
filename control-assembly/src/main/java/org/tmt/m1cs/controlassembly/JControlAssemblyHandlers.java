package org.tmt.m1cs.controlassembly;

import akka.actor.ActorRefFactory;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Adapter;
import akka.stream.Materializer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import csw.command.api.CurrentStateSubscription;
import csw.command.api.javadsl.ICommandService;
import csw.command.client.CommandServiceFactory;
import csw.command.client.messages.TopLevelActorMessage;
import csw.config.api.ConfigData;
import csw.config.api.javadsl.IConfigClientService;
import csw.config.client.internal.ActorRuntime;
import csw.config.client.javadsl.JConfigClientFactory;
import csw.framework.exceptions.FailureStop;

import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.models.*;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.CommandResponse;
import csw.params.commands.ControlCommand;
import csw.params.core.models.Prefix;
import csw.time.core.models.UTCTime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
    private IConfigClientService clientApi;

    private ActorRef<JCommandHandlerActor.CmdMessage> commandHandlerActor;
    private ActorRef<JMonitorActor.MonitorMessage> monitorActor;

    // reference to the template HCD
    private Map<Integer, Optional<ICommandService>> runningHcds;
    private Optional<ICommandService> segmentHcd = Optional.empty(); // NOTE the use of Optional

    // handle to the segmentHcd CurrentState message subscription
    private Optional<CurrentStateSubscription> subscription = Optional.empty();

    JControlAssemblyHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.cswCtx = cswCtx;
        this.ctx = ctx;
        this.log = cswCtx.loggerFactory().getLogger(getClass());

        // create the monitorActor
        monitorActor = ctx.spawnAnonymous(JMonitorActor.behavior(JMonitorActor.AssemblyState.Ready, JMonitorActor.AssemblyMotionState.Halted, cswCtx.loggerFactory()));

        // this is in a try/catch in case the test environment does not have the configuration loaded, so that tests can continue
        try {
            // Handle to the config client service
            clientApi = JConfigClientFactory.clientApi(ctx.getSystem(), cswCtx.locationService());

            // Load the configuration from the configuration service
            Config config = getHcdConfig();

            // log some configuration values as an example of how to extract the values
            logConfig(config);

        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }


    @Override
    public CompletableFuture<Void> jInitialize() {
    log.info("Initializing segment assembly...");
    return CompletableFuture.runAsync(() -> {


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

        System.out.println("in onLocationTrackingEvent()");

        if (trackingEvent instanceof LocationUpdated) {
            // do something for the tracked location when it is updated

            AkkaLocation hcdLocation = (AkkaLocation) ((LocationUpdated) trackingEvent).location();


            segmentHcd = Optional.of(CommandServiceFactory.jMake(hcdLocation, ctx.getSystem()));

            // Actor that handles commands and directs them to worker actors
            commandHandlerActor = ctx.spawnAnonymous(JCommandHandlerActor.behavior(cswCtx.commandResponseManager(), segmentHcd, Boolean.TRUE, cswCtx.loggerFactory()));

            // set up Hcd CurrentState subscription to be handled by the monitor actor
            subscription = Optional.of(segmentHcd.get().subscribeCurrentState(currentState ->
                    monitorActor.tell(new JMonitorActor.CurrentStateEventMessage(currentState))));

            // send message to monitor actor
            monitorActor.tell(new JMonitorActor.LocationEventMessage(segmentHcd));

        } else if (trackingEvent instanceof LocationRemoved) {
            // do something for the tracked location when it is no longer available
            segmentHcd = Optional.empty();
            // FIXME: not sure if this is necessary
            subscription.get().unsubscribe();

            // send message to monitor actor
            monitorActor.tell(new JMonitorActor.LocationEventMessage(segmentHcd));


        }
    }


    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(ControlCommand controlCommand) {
        return new CommandResponse.Accepted(controlCommand.runId());
    }

    @Override
    public CommandResponse.SubmitResponse onSubmit(ControlCommand controlCommand) {


        commandHandlerActor.tell(new JCommandHandlerActor.SubmitCommandMessage(controlCommand));

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

    public void onDiagnosticMode(UTCTime startTime,String hint){

    }


    public void onOperationsMode(){

    }

    public class ConfigNotAvailableException extends FailureStop {

        public ConfigNotAvailableException() {
            super("Configuration not available. Initialization failure.");
        }
    }

    private Config getHcdConfig() {

        try {
            ActorRefFactory actorRefFactory = Adapter.toUntyped(ctx.getSystem());

            ActorRuntime actorRuntime = new ActorRuntime(ctx.getSystem());

            Materializer mat = actorRuntime.mat();

            ConfigData configData = getHcdConfigData();

            return configData.toJConfigObject(mat).get();

        } catch (Exception e) {
            throw new ConfigNotAvailableException();
        }

    }

    private ConfigData getHcdConfigData() throws ExecutionException, InterruptedException {

        log.info("loading assembly configuration");

        // construct the path
        Path filePath = Paths.get("/config/org/tmt/m1cs/control.conf");

        ConfigData activeFile = clientApi.getActive(filePath).get().get();

        return activeFile;
    }

    // examples of how to extract configuration parameters
    private void logConfig(Config config) {

        String IP = config.getString("controlConfig.IP");

        log.info("Configuration parameter read in: IP = " + IP);

        for (ConfigObject configObject : config.getObjectList("controlConfig.variables")) {

            String name = configObject.toConfig().getString("Name");
            String tagName = configObject.toConfig().getString("Tag");
            int tagMemberNumber = configObject.toConfig().getInt("TagMemberNumber");
            String javaTypeName = configObject.toConfig().getString("JavaType");
            boolean isBoolean = configObject.toConfig().getBoolean("IsBoolean");
            int bitPosition = configObject.toConfig().getInt("BitPosition");
            String units = configObject.toConfig().getString("Units");

            log.info("Configuration read into assembly: " +
                    "name = " + name +
                    ", tagName = " + tagName +
                    ", tagMemberNumber = " + tagMemberNumber +
                    ", javaTypeName = " + javaTypeName +
                    ", isBoolean = " + isBoolean +
                    ", bitPosition = " + bitPosition +
                    ", units = " + units
            );
        }
    }
}
