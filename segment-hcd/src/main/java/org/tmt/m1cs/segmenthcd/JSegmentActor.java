package org.tmt.m1cs.segmenthcd;


import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.util.Timeout;
import csw.command.api.javadsl.ICommandService;
import csw.command.client.CommandResponseManager;
import csw.logging.api.javadsl.ILogger;
import csw.logging.client.javadsl.JLoggerFactory;
import csw.params.commands.CommandName;
import csw.params.commands.CommandResponse;
import csw.params.commands.ControlCommand;
import csw.params.commands.Setup;
import csw.params.core.generics.Parameter;
import csw.params.core.models.Id;
import csw.params.core.models.ObsId;
import csw.params.core.models.Prefix;
import scala.Option;
import scala.concurrent.duration.FiniteDuration;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


public class JSegmentActor extends AbstractBehavior<ControlCommand> {



    private ActorContext<ControlCommand> actorContext;
    private JLoggerFactory loggerFactory;
    private ILogger log;
    private Integer segmentNumber;
    private CommandResponseManager commandResponseManager;


    private JSegmentActor(ActorContext<ControlCommand> actorContext, CommandResponseManager commandResponseManager, Integer segmentNumber, JLoggerFactory loggerFactory) {
        this.actorContext = actorContext;
        this.loggerFactory = loggerFactory;
        this.log = loggerFactory.getLogger(actorContext, getClass());
        this.commandResponseManager = commandResponseManager;
        this.segmentNumber = segmentNumber;

    }

    public static <ControlCommand> Behavior<ControlCommand> behavior(CommandResponseManager commandResponseManager, Integer segmentNumber, JLoggerFactory loggerFactory) {
        return Behaviors.setup(ctx -> {
            return (AbstractBehavior<ControlCommand>) new JSegmentActor((ActorContext<csw.params.commands.ControlCommand>) ctx, commandResponseManager, segmentNumber, loggerFactory);
        });
    }


    @Override
    public Receive<ControlCommand> createReceive() {

        ReceiveBuilder<ControlCommand> builder = newReceiveBuilder()
                .onMessage(ControlCommand.class,
                        command -> command.commandName().name().equals("configure"),
                        command -> {
                            log.info("SetConfigurationParameters");
                            handleSetConfigurationParameters(command);
                            return behavior(commandResponseManager,segmentNumber, loggerFactory);
                        });


        return builder.build();
    }

    private void handleSetConfigurationParameters(ControlCommand message) {

        log.info("handleSetConfigurationParameters = " + message + " for segment number " + segmentNumber);

        // send to HCD, here is where we decide that the architecture is one HCD with 492 segment worker actors

        // NOTE: we use get instead of getOrElse because we assume the command has been validated
        Parameter segmentParam = message.paramSet().find(x -> x.keyName().equals("segment")).get();
        Parameter config1Param = message.paramSet().find(x -> x.keyName().equals("config1")).get();
        Parameter config2Param = message.paramSet().find(x -> x.keyName().equals("config2")).get();

        // create Point and PointDemand messages and send to HCD

        CompletableFuture<CommandResponse.SubmitResponse> setConfigFuture = setConfig(message.maybeObsId(), message.runId(), segmentParam, config1Param, config2Param);

        setConfigFuture.thenAccept((response) -> {

            log.debug("response = " + response);
            log.debug("runId = " + message.runId());

            commandResponseManager.addSubCommand(message.runId(), response.runId());

            commandResponseManager.updateSubCommand(response);

            log.info("config command message handled");

        });

    }


    // do the actual communication with the software running on the segment
    CompletableFuture<CommandResponse.SubmitResponse> setConfig(Option<ObsId> obsId,
                                                                Id runId,
                                                                Parameter segmentParam,
                                                                Parameter config1Param,
                                                                Parameter config2Param) {

        // Run a task asynchronously
        return CompletableFuture.supplyAsync(() -> {
            try {

                // do work here
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return new CommandResponse.Completed(runId);
        });


    }


}

