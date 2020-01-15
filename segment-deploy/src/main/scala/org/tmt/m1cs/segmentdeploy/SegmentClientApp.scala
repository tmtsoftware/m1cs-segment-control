package org.tmt.m1cs.segmentdeploy

import akka.Done
import akka.util.Timeout
import csw.client.utils.Extensions.FutureExt
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.framework.CswClientWiring
import csw.framework.commons.CoordinatedShutdownReasons.ApplicationFinishedReason
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.models.ComponentType.{Assembly, HCD}
import csw.location.models.Connection.AkkaConnection
import csw.params.commands.{CommandName, CommandResponse, Setup}
import csw.params.core.generics.KeyType
import csw.params.core.models.Units.degree
import csw.params.core.models.{ObsId, Prefix}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

/**
 * A demo client to test locating and communicating with the segment HCD
 * This can also be used to locate and communicate with the control Assembly.
 *
 * This is written in Scala as currently there are no Java examples for a client app
 * However, the syntax for creating 'tests' to run is easy to follow, so Java programmers
 * would not find this difficult to extend.
 *
 */
object StageClientApp extends App {

  lazy val clientWiring = new CswClientWiring
  import clientWiring._
  import wiring._
  import actorRuntime._

  def assemblyCommandService(assemblyName: String): CommandService = createCommandService(getAkkaLocation(assemblyName, Assembly))

  def hcdCommandService(hcdName: String): CommandService = createCommandService(getAkkaLocation(hcdName, HCD))

  def shutdown(): Done = wiring.actorRuntime.shutdown(ApplicationFinishedReason).await()

  private def getAkkaLocation(name: String, cType: ComponentType): AkkaLocation = {
    val maybeLocation = locationService.resolve(AkkaConnection(ComponentId(name, cType)), timeout).await()
    maybeLocation.getOrElse(throw new RuntimeException(s"Location not found for component: name:[$name] type:[${cType.name}]"))
  }

  private def createCommandService: AkkaLocation ⇒ CommandService = CommandServiceFactory.make

  println("ABOUT TO GET HCD COMMAND")
  private val hcdCommand = hcdCommandService("JSegmentHcd")

  println("DONE")

  private val maybeObsId = None

  val resp1 = Await.result(sendCommand(maybeObsId), 10.seconds)
  println(s"sendCommand: $resp1")

  /**
   * Sends "configure" message to the HCD and returns the response
   */
  private def sendCommand(obsId: Option[ObsId]): Future[CommandResponse] = {

    implicit val timeout: Timeout = Timeout(10.seconds)

    val prefix = Prefix("m1cs.control.test")

    //keys
    val segmentKey = KeyType.IntKey.make("segment")
    val config1Key = KeyType.DoubleKey.make("config1")
    val config2Key = KeyType.DoubleKey.make("config2")

    val segmentParam = segmentKey.set(356)
    val config1Param = config1Key.set(35.34).withUnits(degree)
    val config2Param = config2Key.set(0.34).withUnits(degree)

    val commandName = CommandName("configure")

    val setup = Setup(prefix, commandName, obsId).add(segmentParam).add(config1Param).add(config2Param)

    hcdCommand.submitAndWait(setup)

  }

}
