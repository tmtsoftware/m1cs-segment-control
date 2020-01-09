package org.tmt.m1cs.controlassembly;


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


public class JSetConfigurationParametersActor extends AbstractBehavior<ControlCommand> {



    private ActorContext<ControlCommand> actorContext;
    private JLoggerFactory loggerFactory;
    private ILogger log;
    private CommandResponseManager commandResponseManager;
    private Optional<ICommandService> segmentHcd;

    private JSetConfigurationParametersActor(ActorContext<ControlCommand> actorContext, CommandResponseManager commandResponseManager, Optional<ICommandService> segmentHcd, JLoggerFactory loggerFactory) {
        this.actorContext = actorContext;
        this.loggerFactory = loggerFactory;
        this.log = loggerFactory.getLogger(actorContext, getClass());
        this.commandResponseManager = commandResponseManager;
        this.segmentHcd = segmentHcd;
    }

    public static <ControlCommand> Behavior<ControlCommand> behavior(CommandResponseManager commandResponseManager, Optional<ICommandService> segmentHcd, JLoggerFactory loggerFactory) {
        return Behaviors.setup(ctx -> {
            return (AbstractBehavior<ControlCommand>) new JSetConfigurationParametersActor((ActorContext<csw.params.commands.ControlCommand>) ctx, commandResponseManager, segmentHcd, loggerFactory);
        });
    }


    @Override
    public Receive<ControlCommand> createReceive() {

        ReceiveBuilder<ControlCommand> builder = newReceiveBuilder()
                .onMessage(ControlCommand.class,
                        command -> command.commandName().name().equals("setConfigurationParameters"),
                        command -> {
                            log.info("SetConfigurationParameters");
                            handleSetConfigurationParameters(command);
                            return behavior(commandResponseManager, segmentHcd,loggerFactory);
                        });


        return builder.build();
    }

    private void handleSetConfigurationParameters(ControlCommand message) {

        System.out.println(("handleSetConfigurationParameters"));
        log.info("handleSetConfigurationParameters = " + message);

        // send to HCD, here is where we decide that the architecture is one HCD with 492 segment worker actors

        // NOTE: we use get instead of getOrElse because we assume the command has been validated
        Parameter segmentParam = message.paramSet().find(x -> x.keyName().equals("segment")).get();
        Parameter config1Param = message.paramSet().find(x -> x.keyName().equals("config1")).get();
        Parameter config2Param = message.paramSet().find(x -> x.keyName().equals("config2")).get();

        // create Point and PointDemand messages and send to HCD

        System.out.println(("calling setConfigFuture"));
        CompletableFuture<CommandResponse.SubmitResponse> setConfigFuture = setConfig(message.maybeObsId(), segmentParam, config1Param, config2Param);

        setConfigFuture.thenAccept((response) -> {

            log.debug("response = " + response);
            log.debug("runId = " + message.runId());

            commandResponseManager.addSubCommand(message.runId(), response.runId());

            commandResponseManager.updateSubCommand(response);

            log.info("move command message handled");

        });

    }

    private Prefix assemblyPrefix = new Prefix("m1cs.control-assembly");



    CompletableFuture<CommandResponse.SubmitResponse> setConfig(Option<ObsId> obsId,
                                            Parameter segmentParam,
                                            Parameter config1Param,
                                            Parameter config2Param) {


        if (segmentHcd.isPresent()) {

            System.out.println("Sending command to HCD");

            Setup setupHcd = new Setup(assemblyPrefix, new CommandName("configure"), Optional.empty()).add(segmentParam).add(config1Param).add(config2Param);

            CompletableFuture<CommandResponse.SubmitResponse> commandResponse = segmentHcd.get()
                    .submitAndWait(setupHcd, Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS))
                    );

            return commandResponse;

        } else {

            return CompletableFuture.completedFuture(new CommandResponse.Error(new Id(""), "Can't locate segmentHcd"));
        }

    }



}

