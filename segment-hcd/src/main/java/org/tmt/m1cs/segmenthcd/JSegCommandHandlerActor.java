package org.tmt.m1cs.segmenthcd;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import csw.command.api.javadsl.ICommandService;
import csw.command.client.CommandResponseManager;
import csw.logging.api.javadsl.ILogger;
import csw.logging.client.javadsl.JLoggerFactory;
import csw.params.commands.ControlCommand;
import csw.params.core.generics.Parameter;

import java.util.List;
import java.util.Optional;


public class JSegCommandHandlerActor extends AbstractBehavior<JSegCommandHandlerActor.CmdMessage> {


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
    private List<ActorRef<ControlCommand>> segmentActorList;

    private JSegCommandHandlerActor(ActorContext<CmdMessage> actorContext, CommandResponseManager commandResponseManager, Boolean online, List<ActorRef<ControlCommand>> segmentActorList, JLoggerFactory loggerFactory) {
        this.actorContext = actorContext;
        this.loggerFactory = loggerFactory;
        this.log = loggerFactory.getLogger(actorContext, getClass());
        this.online = online;
        this.segmentActorList = segmentActorList;
        this.commandResponseManager = commandResponseManager;
    }

    public static <CmdMessage> Behavior<CmdMessage> behavior(CommandResponseManager commandResponseManager, Boolean online, List<ActorRef<ControlCommand>> segmentActorList, JLoggerFactory loggerFactory) {
        return Behaviors.setup(ctx -> {
            return (AbstractBehavior<CmdMessage>) new JSegCommandHandlerActor((ActorContext<JSegCommandHandlerActor.CmdMessage>) ctx, commandResponseManager, online, segmentActorList, loggerFactory);
        });
    }


    @Override
    public Receive<CmdMessage> createReceive() {

        System.out.println("IN CREATE RECEIVE");

        ReceiveBuilder<CmdMessage> builder = newReceiveBuilder()
                .onMessage(SubmitCommandMessage.class,
                        command -> command.controlCommand.commandName().name().equals("configure"),
                        command -> {
                            log.info("configure");

                            handleSetConfigurationParameters(command.controlCommand);
                            return behavior(commandResponseManager, Boolean.TRUE, segmentActorList, loggerFactory);
                        })
                .onMessage(GoOnlineMessage.class,
                        command -> {
                            log.info("GoOnlineMessage Received");
                            // change the behavior to online
                            return behavior(commandResponseManager, Boolean.TRUE, segmentActorList, loggerFactory);
                        })
                .onMessage(GoOfflineMessage.class,
                        command -> {
                            log.info("GoOfflineMessage Received");
                            // change the behavior to online
                            return behavior(commandResponseManager, Boolean.FALSE, segmentActorList, loggerFactory);
                        });

        return builder.build();
    }

    private void handleSetConfigurationParameters(ControlCommand controlCommand) {

        log.info("handleSetConfigurationParameters = " + controlCommand);

        if (online) {

            // look inside the command for the segment number and sent the command to the SegmentActor corresponding to that segment

            Parameter segmentParam = controlCommand.paramSet().find(x -> x.keyName().equals("segment")).get();
            Integer segmentNumber = (Integer)segmentParam.jValues().get(0);
            ActorRef<ControlCommand> segmentActor = segmentActorList.get(segmentNumber-1);

            System.out.println("messaging segment Actor: " + segmentActor + " for segment " + segmentNumber);

            segmentActor.tell(controlCommand);

        }
    }



}

