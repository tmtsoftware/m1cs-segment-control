package org.tmt.m1cs.controlassembly;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;


import akka.actor.typed.javadsl.*;
import csw.command.api.javadsl.ICommandService;
import csw.command.client.CommandResponseManager;
import csw.command.client.messages.CommandMessage;
import csw.logging.api.javadsl.ILogger;
import csw.logging.client.javadsl.JLoggerFactory;
import csw.params.commands.ControlCommand;

import java.util.Optional;


public class JCommandHandlerActor extends AbstractBehavior<JCommandHandlerActor.CmdMessage> {


    // add messages here
    interface CmdMessage {}

    public static final class SubmitCommandMessage implements CmdMessage {

        public final ControlCommand controlCommand;


        public SubmitCommandMessage(ControlCommand controlCommand) {
            this.controlCommand = controlCommand;
        }
    }

    public static final class GoOnlineMessage implements CmdMessage { }
    public static final class GoOfflineMessage implements CmdMessage { }



    private ActorContext<CmdMessage> actorContext;
    private JLoggerFactory loggerFactory;
    private ILogger log;
    private Boolean online;
    private CommandResponseManager commandResponseManager;
    private Optional<ICommandService> segmentHcd;

    private JCommandHandlerActor(ActorContext<CmdMessage> actorContext, CommandResponseManager commandResponseManager, Optional<ICommandService> segmentHcd, Boolean online, JLoggerFactory loggerFactory) {
        this.actorContext = actorContext;
        this.loggerFactory = loggerFactory;
        this.log = loggerFactory.getLogger(actorContext, getClass());
        this.online = online;
        this.commandResponseManager = commandResponseManager;
        this.segmentHcd = segmentHcd;
    }

    public static <CmdMessage> Behavior<CmdMessage> behavior(CommandResponseManager commandResponseManager, Optional<ICommandService> segmentHcd, Boolean online, JLoggerFactory loggerFactory) {
        return Behaviors.setup(ctx -> {
            return (AbstractBehavior<CmdMessage>) new JCommandHandlerActor((ActorContext<JCommandHandlerActor.CmdMessage>) ctx, commandResponseManager, segmentHcd, online, loggerFactory);
        });
    }


    @Override
    public Receive<CmdMessage> createReceive() {

        ReceiveBuilder<CmdMessage> builder = newReceiveBuilder()
                .onMessage(SubmitCommandMessage.class,
                        command -> command.controlCommand.commandName().name().equals("setConfigurationParameters"),
                        command -> {
                            log.info("SetConfigurationParameters");
                            handleSetConfigurationParameters(command.controlCommand);
                            return behavior(commandResponseManager, segmentHcd, Boolean.TRUE, loggerFactory);
                        })
                .onMessage(GoOnlineMessage.class,
                        command -> {
                            log.info("GoOnlineMessage Received");
                            // change the behavior to online
                            return behavior(commandResponseManager, segmentHcd, Boolean.TRUE, loggerFactory);
                        })
                .onMessage(GoOfflineMessage.class,
                        command -> {
                            log.info("GoOfflineMessage Received");
                            // change the behavior to online
                            return behavior(commandResponseManager, segmentHcd, Boolean.FALSE, loggerFactory);
                        });

        return builder.build();
    }

    private void handleSetConfigurationParameters(ControlCommand controlCommand) {

        log.info("handleSetConfigurationParameters = " + controlCommand);

        if (online) {

            ActorRef<ControlCommand> setConfigurationParametersActor =
                    actorContext.spawnAnonymous(JSetConfigurationParametersActor.behavior(commandResponseManager, segmentHcd, loggerFactory));

            setConfigurationParametersActor.tell(controlCommand);

            // TODO: when the command is complete, kill the actor
            // ctx.stop(setTargetWavelengthCmdActor)
        }
    }



}

