package org.tmt.m1cs.controlassembly;


import akka.Done;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Source;
import csw.command.api.javadsl.ICommandService;

import csw.command.client.CommandServiceFactory;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.javadsl.JComponentType;
import csw.location.models.AkkaLocation;
import csw.location.models.ComponentId;

import csw.location.models.Connection;

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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;
import akka.util.Timeout;
import scala.concurrent.duration.FiniteDuration;


import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


import static csw.params.javadsl.JUnits.degree;

public class JSegmentAssemblyTest extends JUnitSuite {



    @ClassRule
    public static final FrameworkTestKitJunitResource testKit =
        new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.AlarmServer, JCSWService.EventServer));

    @BeforeClass
    public static void setup() {
        // uncomment if you want one Assembly run for all tests
        //testKit.spawnStandalone(com.typesafe.config.ConfigFactory.load("JSegmentAssemblyStandalone.conf"));
        testKit.spawnContainer(com.typesafe.config.ConfigFactory.load("JSegmentContainer.conf"));
    }

    @Test
    public void testAssemblyShouldBeLocatableUsingLocationService() throws ExecutionException, InterruptedException {
        Connection.AkkaConnection connection = new Connection.AkkaConnection(new ComponentId("JControlAssembly", JComponentType.Assembly));
        ILocationService locationService = testKit.jLocationService();
        AkkaLocation location = locationService.resolve(connection, Duration.ofSeconds(10)).get().get();

        Assert.assertEquals(location.connection(), connection);

    }


}