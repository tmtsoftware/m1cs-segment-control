package org.tmt.m1cs.controlassembly;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import csw.command.api.javadsl.ICommandService;
import csw.logging.api.javadsl.ILogger;
import csw.logging.client.javadsl.JLoggerFactory;
import csw.params.core.states.CurrentState;

import java.util.Optional;

public class JMonitorActor extends AbstractBehavior<JMonitorActor.MonitorMessage> {


    public enum AssemblyState {
        Ready, Degraded, Disconnected, Faulted
    }
    public enum AssemblyMotionState {
        Idle, Halted, Tracking
    }

    // add messages here
    interface MonitorMessage {}

    // message to manually change the state
    public static final class AssemblyStateChangeMessage implements MonitorMessage {

        public final AssemblyState assemblyState;

        public AssemblyStateChangeMessage(AssemblyState assemblyState) {
            this.assemblyState = assemblyState;
        }
    }

    // message to manually change the motion state
    public static final class AssemblyMotionStateChangeMessage implements MonitorMessage {

        public final AssemblyMotionState assemblyMotionState;

        public AssemblyMotionStateChangeMessage(AssemblyMotionState assemblyMotionState) {
            this.assemblyMotionState = assemblyMotionState;
        }
    }

    // message to initiate state changes when connection to HCD is gained or lost
    public static final class LocationEventMessage implements MonitorMessage {

        public final Optional<ICommandService> segmentHcd;

        public LocationEventMessage(Optional<ICommandService> segmentHcd) {
            this.segmentHcd = segmentHcd;
        }
    }

    // message containing the current state of the HCD
    public static final class CurrentStateEventMessage implements MonitorMessage {

        public final CurrentState currentState;

        public CurrentStateEventMessage(CurrentState currentState) {
            this.currentState = currentState;
        }
    }


    private ActorContext<MonitorMessage> actorContext;
    private JLoggerFactory loggerFactory;
    private ILogger log;
    private AssemblyState assemblyState;
    private AssemblyMotionState assemblyMotionState;

    private JMonitorActor(ActorContext<MonitorMessage> actorContext, AssemblyState assemblyState, AssemblyMotionState assemblyMotionState, JLoggerFactory loggerFactory) {
        this.actorContext = actorContext;
        this.loggerFactory = loggerFactory;
        this.log = loggerFactory.getLogger(actorContext, getClass());
        this.assemblyState = assemblyState;
        this.assemblyMotionState = assemblyMotionState;
    }

    public static <MonitorMessage> Behavior<MonitorMessage> behavior(AssemblyState assemblyState, AssemblyMotionState assemblyMotionState, JLoggerFactory loggerFactory) {
        return Behaviors.setup(ctx -> {
            return (AbstractBehavior<MonitorMessage>) new JMonitorActor((ActorContext<JMonitorActor.MonitorMessage>) ctx, assemblyState, assemblyMotionState, loggerFactory);
        });
    }


    @Override
    public Receive<MonitorMessage> createReceive() {

        ReceiveBuilder<MonitorMessage> builder = newReceiveBuilder()

                .onMessage(AssemblyStateChangeMessage.class,
                        message -> {
                            log.info("AssemblyStateChangeMessage Received");
                            // change the behavior state
                            return behavior(message.assemblyState, assemblyMotionState, loggerFactory);
                        })
                .onMessage(AssemblyMotionStateChangeMessage.class,
                        message -> {
                            log.info("AssemblyMotionStateChangeMessage Received");
                            // change the behavior state
                            return behavior(assemblyState, message.assemblyMotionState, loggerFactory);
                        })
                .onMessage(LocationEventMessage.class,
                        message -> {
                            log.info("LocationEventMessage Received");
                            return onLocationEventMessage(message);
                        })
                .onMessage(CurrentStateEventMessage.class,
                        message -> {
                            log.info("CurrentStateEventMessage Received");
                            return onCurrentStateEventMessage(message);
                        });
        return builder.build();
    }

    // this method would be called when a LocationEventMessage is received
    // this is to handle the case when the segment HCD cannot be located, or has been relocated
    // and changes the assembly state between Disconnected and Ready
    private Behavior<MonitorMessage> onLocationEventMessage(LocationEventMessage message) {

        if (message.segmentHcd.isPresent() ) {

            if (assemblyState == AssemblyState.Disconnected) {
                // TODO: this logic is oversimplified: just because the state is no longer disconnected, does not mean it is Ready
                return JMonitorActor.behavior(AssemblyState.Ready, assemblyMotionState, loggerFactory);
            } else {
                return this;
            }
        } else {
            // if templateHcd is null, then change state to disconnected
            return JMonitorActor.behavior(AssemblyState.Disconnected, assemblyMotionState, loggerFactory);
        }
    }


    // Handles the current state event message by just printing it out and keeping state the same
    // When state changes are dependent on data coming from the HCD (and lower level devices),
    // this is where that would be handled.
    private Behavior<MonitorMessage> onCurrentStateEventMessage(CurrentStateEventMessage message) {

        log.info("current state handler");

        CurrentState currentState = message.currentState;

        log.info("current state = " + currentState);

        // here the Monitor Actor can change its state depending on the current state of the HCD
        return JMonitorActor.behavior(assemblyState, assemblyMotionState, loggerFactory);

    }

}
