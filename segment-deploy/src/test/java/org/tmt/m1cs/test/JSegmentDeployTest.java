package org.tmt.m1cs.test;


import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Source;
import akka.util.Timeout;
import csw.command.api.javadsl.ICommandService;
import csw.command.client.CommandServiceFactory;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.javadsl.JComponentType;
import csw.location.models.AkkaLocation;
import csw.location.models.ComponentId;
import csw.location.models.Connection;
import csw.logging.client.internal.LoggingSystem;
import csw.logging.client.javadsl.JLoggingSystemFactory;
import csw.params.commands.CommandName;
import csw.params.commands.CommandResponse;
import csw.params.commands.Setup;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.Prefix;
import csw.params.events.Event;
import csw.params.events.EventName;
import csw.params.events.SystemEvent;
import csw.params.javadsl.JKeyType;
import csw.testkit.javadsl.FrameworkTestKitJunitResource;
import csw.testkit.javadsl.JCSWService;
import org.junit.*;
import org.scalatestplus.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static csw.params.javadsl.JUnits.degree;

public class JSegmentDeployTest extends JUnitSuite {

    protected static ActorSystem actorSystem = ActorSystem.create(SpawnProtocol.behavior(), "base-system");
    protected static LoggingSystem loggingSystem;


    @ClassRule
    public static final FrameworkTestKitJunitResource testKit =
        new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.AlarmServer, JCSWService.EventServer));

    @BeforeClass
    public static void setup() {
        loggingSystem = JLoggingSystemFactory.start("Logger-Test", "SNAPSHOT-1.0", "localhost", actorSystem);
        // uncomment if you want one Assembly run for all tests
        //testKit.spawnStandalone(com.typesafe.config.ConfigFactory.load("JSegmentAssemblyStandalone.conf"));
        testKit.spawnContainer(com.typesafe.config.ConfigFactory.load("JSegmentContainer.conf"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        loggingSystem.javaStop().get();
        actorSystem.terminate();
        Await.result(actorSystem.whenTerminated(), scala.concurrent.duration.Duration.create(10, TimeUnit.SECONDS));
    }


    @Test
    public void testAssemblyShouldBeLocatableUsingLocationService() throws ExecutionException, InterruptedException {
        Connection.AkkaConnection connection = new Connection.AkkaConnection(new ComponentId("JControlAssembly", JComponentType.Assembly));
        ILocationService locationService = testKit.jLocationService();
        AkkaLocation location = locationService.resolve(connection, Duration.ofSeconds(10)).get().get();

        Assert.assertEquals(location.connection(), connection);

    }
    @Test
    public void testAssemblyHandlesCommand() throws ExecutionException, InterruptedException {
        Connection.AkkaConnection connection = new Connection.AkkaConnection(new ComponentId("JControlAssembly", JComponentType.Assembly));
        ILocationService locationService = testKit.jLocationService();
        AkkaLocation location = locationService.resolve(connection, Duration.ofSeconds(10)).get().get();

        ICommandService commandService = CommandServiceFactory.jMake(location, testKit.actorSystem());

        // send a command
        //prefix
        Prefix prefix = new Prefix("m1cs.control.test");

        //keys
        Key segmentKey    = JKeyType.IntKey().make("segment");
        Key config1Key   = JKeyType.DoubleKey().make("config1");
        Key config2Key   = JKeyType.DoubleKey().make("config2");

        Parameter segmentParam = segmentKey.set(356);
        Parameter config1Param   = config1Key.set(35.34).withUnits(degree);
        Parameter config2Param   = config2Key.set(0.34).withUnits(degree);

        CommandName commandName = new CommandName("setConfigurationParameters");

        Setup submitSetup = new Setup(prefix, commandName, Optional.empty()).add(segmentParam).add(config1Param).add(config2Param);

        // send the command via the command service


            CompletableFuture<CommandResponse.SubmitResponse> immediateCommandF =

                        commandService
                                .submitAndWait(submitSetup, new Timeout(new FiniteDuration(20, TimeUnit.SECONDS)))
                                .thenApply(
                                        response -> {
                                            if (response instanceof CommandResponse.Completed) {
                                                //do something with completed result
                                            } else {
                                                // do something with unexpected response
                                                Assert.fail("command response is " + response.toString());
                                            }
                                            return response;
                                        }
                            );



            immediateCommandF.get();

    }

    @Test
    public void testPublishEvents() throws ExecutionException, InterruptedException {

        Prefix prefix = new Prefix("m1cs.event.example");

        int n = 10;

        //#with-source
        Source<Event, CompletionStage<Done>> eventStream = Source
                .range(1, n)
                .map(number -> makeEvent(number, prefix, new EventName("example_event")))
                .watchTermination(Keep.right());

        testKit.jEventService().defaultPublisher().<CompletionStage<Done>>publish(eventStream, failure -> {
            /*do something*/
            Assert.fail(failure.getMessage());
        });
        //#with-source
    }

    // example event maker that uses the passed number to add to the paramset of the event
    private Event makeEvent(int n, Prefix prefix, EventName name) {

        Key segmentKey    = JKeyType.IntKey().make("segment");
        Parameter segmentParam = segmentKey.set(n);

        return new SystemEvent(prefix, name).add(segmentParam);
    }


}