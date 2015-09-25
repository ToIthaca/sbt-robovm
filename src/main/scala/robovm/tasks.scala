package robovm

import java.io.File

import sbt.Keys._
import sbt._

trait ProGuard {

  lazy val settingsFile = "proguard-sbt.txt"

  def asInputOutput(inOut: (File, File)): List[proguard.ClassPathEntry] = List(
    new proguard.ClassPathEntry(inOut._1, false),
    new proguard.ClassPathEntry(inOut._2, true)
  )
  def asClasspath(fs: List[File]): proguard.ClassPath = {
    val cp = new proguard.ClassPath
    val ofs = fs.map(f => new File(f.getAbsolutePath + ".pro"))
    fs.zip(ofs).flatMap(asInputOutput).zipWithIndex.foreach {
      case (entry, idx) => cp.add(idx, entry)
    }
    cp
  }

  lazy val proGuard = Def.task {
    val log = streams.value.log
    val deps = (libraryDependencies in Compile).value
    val ovrs = (dependencyOverrides in  Compile).value
    val cp = (dependencyClasspath in Compile).value
    val comp = Commands.findCompiler(cp) _
    val robo = comp(ovrs.toList).orElse(comp(deps))

    val file = baseDirectory.value / settingsFile
    log.info(s"Using proguard version ${proguard.ProGuard.VERSION} with existing settings ${file.getAbsolutePath}")
    val cps = (fullClasspath in Runtime).value.files

    val config = new proguard.Configuration
    val configParser = new proguard.ConfigurationParser(file, System.getProperties)
    configParser.parse(config)
    config.programJars = asClasspath(cps.toList.filter(f => !f.equals(robo.get)))

    val libcp = new proguard.ClassPath
    libcp.add(new proguard.ClassPathEntry(new java.io.File(System.getProperty("java.home")), false))
    libcp.add(new proguard.ClassPathEntry(robo.get, false))

    config.libraryJars = libcp
    config.verbose = true
    val exec = new proguard.ProGuard(config)
    exec.execute()
    log.info("Finished obfusticating code using proguard")
    file
  }
}

object Tasks extends ProGuard {


  lazy val prepare = Def.taskDyn {
    val log = streams.value.log
    log.info("Preparing project for robovm compilation")
    val comp = (fullClasspath in Runtime).value.files

    if((Keys.useProguard in Keys.Robo).value) {
      proGuard
    } else {
      Def.task(comp(0))
    }
  }


  lazy val tasks = Seq(
    Keys.prepare in Keys.Robo <<= prepare
  )
}
