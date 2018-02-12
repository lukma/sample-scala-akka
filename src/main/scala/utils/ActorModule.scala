package utils

import akka.actor.ActorSystem

/**
  * Created by Gayo on 10/27/2016.
  */
trait ActorModule {
  val system: ActorSystem
}

trait ActorModuleImpl extends ActorModule {
  this: Configuration =>
  val system = ActorSystem("akkingslick", config)
}