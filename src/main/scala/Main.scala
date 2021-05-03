import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.cluster.ClusterEvent._
import akka.cluster.typed._
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object Main extends App {
  import com.nat.examples.BasicClusterExample.BasicClusterExample._
  import com.nat.helpers.RootActorHelper._

  val sys1Port = 3001
  val sys2Port = 3002

  val nameOfActorSystem = "ClusterSystem"

  def config(port: Int) = ConfigFactory.parseString(
    s"""
       |akka.remote.classic.netty.tcp.port = $port
       |akka.remote.artery.canonical.tcp.port = $port
       |akka.cluster.jmx.multi-mbeans-in-same-jvm = on
       |akka.cluster.seed-nodes = [ "akka://$nameOfActorSystem@127.0.0.1:$sys1Port", "akka://$nameOfActorSystem@127.0.0.1:$sys2Port" ]
       |""".stripMargin)

  def spawnBehavior: Behavior[SpawnProtocol.Command] =
    Behaviors.setup(_ => SpawnProtocol())

  val system1 = ActorSystem[SpawnProtocol.Command](spawnBehavior, nameOfActorSystem, config(sys1Port).withFallback(configForSeed(nameOfActorSystem)))
  val system2 = ActorSystem[SpawnProtocol.Command](spawnBehavior, nameOfActorSystem, config(sys2Port).withFallback(configForMember(nameOfActorSystem)))

  val subscriber: ActorRef[MemberEvent] = system1.spawn {
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
  }(timeout = 3.second, system1.scheduler)

  try {
    val cluster1 = Cluster(system1)
    val cluster2 = Cluster(system2)


    cluster1.subscriptions ! Subscribe(subscriber, classOf[MemberEvent])

    cluster1.manager ! Join(cluster1.selfMember.address)
    cluster2.manager ! Join(cluster2.selfMember.address)

    cluster1.manager ! Leave(cluster1.selfMember.address)
    cluster2.manager ! Leave(cluster2.selfMember.address)
  } finally {

    system1.terminate()
    system2.terminate()
  }
}
