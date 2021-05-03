package com.nat.examples.BasicClusterExample

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.typed.Cluster
import com.typesafe.config.ConfigFactory

object BasicClusterExample {
  val configSystem1 = ConfigFactory.parseString(
    s"""
  akka {
    actor {
      provider = "cluster"
    }
    remote.artery {
      canonical {
        hostname = "127.0.0.1"
        port = 2551
      }
    }
    cluster {
      seed-nodes = [
        "akka://ClusterSystem@127.0.0.1:2551",
        "akka://ClusterSystem@127.0.0.1:2552"
      ]

      downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
    }
  }
  """)

  val configSystem2 = ConfigFactory.parseString(
    s"""
       |akka.remote.classic.netty.tcp.port = 0
       |akka.remote.artery.canonical.port = 0
       |""".stripMargin).withFallback(configSystem1)

  def illustrateJoinSeedNodes(): Unit = {
    val system: ActorSystem[_] = ???

    import akka.actor.{Address, AddressFromURIString}
    import akka.cluster.typed.JoinSeedNodes

    val seedNodes: List[Address] = List(
      "akka://ClusterSystem@127.0.0.1:2551",
      "akka://ClusterSystem@127.0.0.1:2552",
    ).map(AddressFromURIString.parse)

    Cluster(system).manager ! JoinSeedNodes(seedNodes)
  }

  object Backend {
    def apply(): Behavior[_] = Behaviors.empty
  }

  object Frontend {
    def apply(): Behavior[_] = Behaviors.empty
  }

  def illustrateRoles(): Unit = {
    val context: ActorContext[_] = ???

    val selfMember = Cluster(context.system).selfMember
    if (selfMember.hasRole("backend")) {
      context.spawn(Backend(), "back")
    } else if (selfMember.hasRole("frontend")) {
      context.spawn(Frontend(), "front")
    }
  }
}
