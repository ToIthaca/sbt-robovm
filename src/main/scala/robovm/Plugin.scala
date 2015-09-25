package robovm

import sbt._
import sbt.Keys._

object Plugin extends sbt.Plugin {

  lazy val iosBuild = Seq(
    Keys.osInfo in Keys.Robo := OSInfo(),
    libraryDependencies += "org.robovm" % "robovm-dist-compiler" % "1.8.0",
    Keys.compiler in Keys.Robo := None,
    commands ++= Commands.commands
  ) ++ Tasks.tasks
}

/*import org.robovm.compiler.config.Config
import sbt._

import scala.xml.Elem

object RobovmPlugin extends Plugin with RobovmUtils {
  val RoboVMVersion = BuildInfo.version

  /* General settings and tasks */
  val robovmHome = taskKey[Config.Home]("Return the home of RoboVM installation. Will download to local maven repository by default.")
  val robovmInputJars = taskKey[Seq[File]]("Jars fed into RoboVM compiler. fullClasspath in compile by default.")
  val robovmVerbose = settingKey[Boolean]("Propagates robovm Debug messages to Info level, to be visible")
  val robovmSimulatorDevice = settingKey[Option[String]]("Simulator device to be used in simulator task")
  val robovmProperties = taskKey[Either[File, Map[String, String]]]("Values that might be used in config-file substitutions")
  val robovmConfiguration = taskKey[Either[File,Elem]]("robovm.xml configuration")
  val robovmDebugPort = settingKey[Int]("Port on which debugger will listen (when enabled)")
  val robovmDebug = settingKey[Boolean]("Whether to enable robovm debugger (Needs commercial license, run robovmLicense task to enter one)")
  val robovmTarget64bit = settingKey[Boolean]("Whether to build 64bit executables")

  /* Specific settings and tasks */
  // iOS Only
  val simulator = taskKey[Unit]("Start package on specified device")
  val device = taskKey[Unit]("Start package on device after installation")
  val iphoneSim = taskKey[Unit]("Start package on iphone simulator")
  val ipadSim = taskKey[Unit]("Start package on ipad simulator")
  val ipa = taskKey[Unit]("Create an ipa file for the app store")

  val skipSigning = settingKey[Option[Boolean]]("Whether to override signing behavior")
  val provisioningProfile = settingKey[Option[String]]("Specify provisioning profile to use when signing iOS code.")
  val signingIdentity = settingKey[Option[String]]("Specify signing identity to use when signing iOS code.")
  val preferredDevices = settingKey[Seq[String]]("List of iOS device ID's from which device will be chosen if multiple are detected.")
  // Native Only
  val native = taskKey[Unit]("Run as native console application")
  val nativeBuild = taskKey[Unit]("Compile and archive for distribution as native application")

  /* Tools */
  val robovmLicense = taskKey[Unit]("Launch UI for entering a RoboVM license key.")
  // iOS Only
  val simulatorDevices = taskKey[Unit]("Prints all available simulator devices to be used in simulator-device-name setting")

  val iOSProject = RobovmProjects.iOSProject
  val NativeProject = RobovmProjects.NativeProject
}
*/