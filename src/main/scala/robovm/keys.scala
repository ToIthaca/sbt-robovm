package robovm

import sbt._

object Keys {
  
  val Robo = config("robovm").describedAs("RoboVM compilation configuration")
  val iOS = config("ios").describedAs("iOS compilation configuration")

  val osInfo = settingKey[Option[OSInfo]]("The operating system resolved for robovm compilation purposes")
  val compiler = settingKey[Option[File]]("The version of robovm-compiler which the code will be compiled against")
  val platformTarget = settingKey[String]("The version of robovm to compiler with")
  val dist = taskKey[File]("Installs robovm compiler")
  val prepare = taskKey[Seq[File]]("Prepares the bundle of jars and resources to be compiled by robovm")
  val bundle = taskKey[String]("Bundles the jar ready")
  
  val useProguard = settingKey[Boolean]("")
}
