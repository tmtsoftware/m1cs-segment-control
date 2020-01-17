package org.tmt.m1cs.controlassembly;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import csw.command.api.javadsl.ICommandService;
import csw.event.api.javadsl.IEventService;
import csw.event.api.javadsl.IEventSubscriber;
import csw.logging.api.javadsl.ILogger;
import csw.logging.client.javadsl.JLoggerFactory;
import csw.params.core.models.Prefix;
import csw.params.core.states.CurrentState;
import csw.params.events.EventKey;
import csw.params.events.EventName;

import java.util.Optional;
import java.util.Set;

public class JEventHandlerActor extends AbstractBehavior<JEventHandlerActor.EventHandlerMessage> {


    // add messages here
    interface EventHandlerMessage {
    }

    // message
    public static final class EventHandlerMessage1 implements EventHandlerMessage {

        public EventHandlerMessage1(EventHandlerMessage1 message1) {
        }
    }


    private ActorContext<EventHandlerMessage> actorContext;
    private JLoggerFactory loggerFactory;
    private ILogger log;
    private IEventService eventService;


    private JEventHandlerActor(ActorContext<JEventHandlerActor.EventHandlerMessage> actorContext, IEventService eventService, JLoggerFactory loggerFactory) {
        this.actorContext = actorContext;
        this.loggerFactory = loggerFactory;
        this.log = loggerFactory.getLogger(actorContext, getClass());
        this.eventService = eventService;

        // set up subscription to the "exampleEvent" from the source: "m1cs.event.example"
        IEventSubscriber subscriber = eventService.defaultSubscriber();
        Prefix prefix = new Prefix("m1cs.event.example");

        EventKey exampleEventKey = new EventKey(prefix, new EventName("example_event"));
        subscriber.subscribeCallback(Set.of(exampleEventKey), event -> {

            // here is the callback where we process the event
            // in our example, we just log it
            log.info("Event Received, event = " + event);

        });

    }

    public static <EventHandlerMessage> Behavior<EventHandlerMessage> behavior(IEventService eventService, JLoggerFactory loggerFactory) {
        return Behaviors.setup(ctx -> {
            return (AbstractBehavior<EventHandlerMessage>) new JEventHandlerActor((ActorContext<JEventHandlerActor.EventHandlerMessage>) ctx, eventService, loggerFactory);
        });
    }

    @Override
    public Receive<EventHandlerMessage> createReceive() {

        ReceiveBuilder<EventHandlerMessage> builder = newReceiveBuilder()

                .onMessage(EventHandlerMessage1.class,
                        message -> {
                            log.info("AssemblyStateChangeMessage Received");
                            // change the behavior state
                            return behavior(eventService, loggerFactory);
                        });
        return builder.build();
    }

}