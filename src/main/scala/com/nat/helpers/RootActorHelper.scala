package com.nat.helpers

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed._
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern._

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object RootActorHelper {
  def behavior: Behavior[SpawnProtocol.Command] = Behaviors.setup { _ =>
    SpawnProtocol()
  }

  implicit class ActorSpawnHelper(actorSystem: ActorSystem[SpawnProtocol.Command]) {
    def spawn[A](
      behavior: Behavior[A], name: String = ""
    )(
      implicit timeout: Timeout, scheduler: Scheduler
    ): ActorRef[A] = {
      Await.result(
        spawnF(behavior, name),
        3.seconds
      )
    }

    def spawnF[A](
      behavior: Behavior[A], name: String
    )(
      implicit timeout: Timeout, scheduler: Scheduler
    ): Future[ActorRef[A]] = {
      actorSystem
        .ask[ActorRef[A]](replyTo =>
          SpawnProtocol.Spawn(behavior, name, props = Props.empty, replyTo)
        )
    }
  }
}
