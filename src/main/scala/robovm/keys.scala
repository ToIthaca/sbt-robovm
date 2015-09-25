package robovm

import sbt._

object Keys {
  val Robo = config("robovm").describedAs("RoboVM compilation configuration")

  val osInfo = settingKey[Option[OSInfo]]("The operating system resolved for robovm compilation purposes")
  val compiler = settingKey[Option[File]]("The version of robovm-compiler which the code will be compiled against")
  val prepare = taskKey[File]("Prepares the bundle of jars and resources to be compiled by robovm")
  val bundle = taskKey[String]("Bundles the jar ready")
  
  val useProguard = settingKey[Boolean]("")
}
