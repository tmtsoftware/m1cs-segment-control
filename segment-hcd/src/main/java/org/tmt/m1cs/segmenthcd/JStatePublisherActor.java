package org.tmt.m1cs.segmenthcd;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import csw.framework.CurrentStatePublisher;
import csw.logging.api.javadsl.ILogger;
import csw.logging.client.javadsl.JLoggerFactory;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.Prefix;
import csw.params.core.states.CurrentState;
import csw.params.core.states.StateName;
import csw.params.javadsl.JKeyType;
import scala.concurrent.duration.Duration;

import java.time.Instant;


import static csw.params.javadsl.JUnits.degree;


public class JStatePublisherActor extends AbstractBehavior<JStatePublisherActor.StatePublisherMessage> {


    // add messages here
    interface StatePublisherMessage {}

    public static final class StartMessage implements StatePublisherMessage { }
    public static final class StopMessage implements StatePublisherMessage { }
    public static final class PublishMessage implements StatePublisherMessage { }


    private JLoggerFactory loggerFactory;
    private CurrentStatePublisher currentStatePublisher;
    private ILogger log;
    private TimerScheduler<StatePublisherMessage> timer;


    //prefix
    Prefix prefix = new Prefix("m1cs.hcd");

    //keys
    Key timestampKey    = JKeyType.UTCTimeKey().make("timestampKey");

    Key azPosKey        = JKeyType.DoubleKey().make("azPosKey");
    Key azPosErrorKey   = JKeyType.DoubleKey().make("azPosErrorKey");
    Key elPosKey        = JKeyType.DoubleKey().make("elPosKey");
    Key elPosErrorKey   = JKeyType.DoubleKey().make("elPosErrorKey");
    Key azInPositionKey = JKeyType.BooleanKey().make("azInPositionKey");
    Key elInPositionKey = JKeyType.BooleanKey().make("elInPositionKey");

    private static final Object TIMER_KEY = new Object();

    private JStatePublisherActor(TimerScheduler<StatePublisherMessage> timer, CurrentStatePublisher currentStatePublisher, JLoggerFactory loggerFactory) {
        this.timer = timer;
        this.loggerFactory = loggerFactory;
        this.log = loggerFactory.getLogger(getClass());
        this.currentStatePublisher = currentStatePublisher;
    }

    public static <StatePublisherMessage> Behavior<StatePublisherMessage> behavior(CurrentStatePublisher currentStatePublisher, JLoggerFactory loggerFactory) {
        return Behaviors.withTimers(timers -> {
            return (AbstractBehavior<StatePublisherMessage>) new JStatePublisherActor((TimerScheduler<JStatePublisherActor.StatePublisherMessage>)timers, currentStatePublisher, loggerFactory);
        });
    }


    @Override
    public Receive<StatePublisherMessage> createReceive() {

        ReceiveBuilder<StatePublisherMessage> builder = newReceiveBuilder()
                .onMessage(StartMessage.class,
                        command -> {
                            log.info("StartMessage Received");
                            onStart(command);
                            return Behaviors.same();
                        })
                .onMessage(StopMessage.class,
                        command -> {
                            log.info("StopMessage Received");
                            onStop(command);
                            return Behaviors.same();
                        })
                .onMessage(PublishMessage.class,
                        command -> {
                            log.info("PublishMessage Received");
                            onPublishMessage(command);
                            return Behaviors.same();
                        });
        return builder.build();
    }

    private void onStart(StartMessage message) {

        log.info("Start Message Received ");

        timer.startPeriodicTimer(TIMER_KEY, new PublishMessage(), java.time.Duration.ofMillis(1000));

        log.info("start message completed");


    }

    private void onStop(StopMessage message) {

        log.info("Stop Message Received ");
    }

    private void onPublishMessage(PublishMessage message) {

        log.info("Publish Message Received ");

        // example parameters for a current state

        Parameter azPosParam        = azPosKey.set(35.34).withUnits(degree);
        Parameter azPosErrorParam   = azPosErrorKey.set(0.34).withUnits(degree);
        Parameter elPosParam        = elPosKey.set(46.7).withUnits(degree);
        Parameter elPosErrorParam   = elPosErrorKey.set(0.03).withUnits(degree);
        Parameter azInPositionParam = azInPositionKey.set(false);
        Parameter elInPositionParam = elInPositionKey.set(true);

        Parameter timestamp = timestampKey.set(Instant.now());

        //create CurrentState and use sequential add
        CurrentState currentState = new CurrentState(prefix, new StateName("whatever"))
                .add(azPosParam)
                .add(elPosParam)
                .add(azPosErrorParam)
                .add(elPosErrorParam)
                .add(azInPositionParam)
                .add(elInPositionParam)
                .add(timestamp);

        currentStatePublisher.publish(currentState);


    }


}
