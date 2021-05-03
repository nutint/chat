import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.cluster.ClusterEvent._
import akka.cluster.typed._
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._

object Main extends App {
  import com.nat.examples.BasicClusterExample.BasicClusterExample._
  import com.nat.helpers.RootActorHelper._

  val sys1Port = 3001
  val sys2Port = 3002

  val nameOfActorSystem = "ClusterSystem"

  def config(port: Int, systemName: String) = ConfigFactory.parseString(
    s"""
       |akka.remote.classic.netty.tcp.port = $port
       |akka.remote.artery.canonical.tcp.port = $port
       |akka.cluster.jmx.multi-mbeans-in-same-jvm = on
       |akka.cluster.seed-nodes = [ "akka://$systemName@127.0.0.1:$sys1Port", "akka://$systemName@127.0.0.1:$sys2Port" ]
       |""".stripMargin)

  def createSpawnProtocolActor(config: Config, systemName: String): ActorSystem[SpawnProtocol.Command] =
    ActorSystem[SpawnProtocol.Command](behavior, systemName, config)

  val seedNodeActor = createSpawnProtocolActor(config(sys1Port, nameOfActorSystem).withFallback(configForSeed(nameOfActorSystem)), nameOfActorSystem)
  val memberNodeActor = createSpawnProtocolActor(config(sys2Port, nameOfActorSystem).withFallback(configForMember(nameOfActorSystem)), nameOfActorSystem)

  val subscriber: ActorRef[MemberEvent] = seedNodeActor.spawn {
    Behaviors.receiveMessage[MemberEvent] {
      case MemberUp(member) =>
        println("memberUp")
        Behaviors.same
      case MemberDowned(member) =>
        println("memberUp")
        Behaviors.same
      case MemberJoined(member) =>
        println("memberUp")
        Behaviors.same
      case MemberLeft(member) =>
        println("memberUp")
        Behaviors.same
      case memberOtherEvent: MemberEvent =>
        println(s"member other event $memberOtherEvent")
        Behaviors.same
    }
  }(timeout = 3.second, seedNodeActor.scheduler)

  try {
    val cluster1 = Cluster(seedNodeActor)
    val cluster2 = Cluster(memberNodeActor)


    cluster1.subscriptions ! Subscribe(subscriber, classOf[MemberEvent])

    cluster1.manager ! Join(cluster1.selfMember.address)
    cluster2.manager ! Join(cluster2.selfMember.address)

    cluster1.manager ! Leave(cluster1.selfMember.address)
    cluster2.manager ! Leave(cluster2.selfMember.address)
  } finally {

    seedNodeActor.terminate()
    memberNodeActor.terminate()
  }
}
