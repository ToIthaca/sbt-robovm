package robovm

import java.io.File
import java.net.URLClassLoader

import sbt.Keys._
import sbt._

import scala.util.Try

trait Install {

  def findCompiler(cp: sbt.Keys.Classpath)(deps: Seq[ModuleID]): Option[File] = {
    deps
      .find(m => m.name == "robovm-dist-compiler" && m.organization == "org.robovm")
      .flatMap(m => cp.find {
      case Attributed(file) => file.getName.contains(s"${m.name}-${m.revision}")
    }).map(_.data)
  }

  def addToClassLoader(s: State)(f: File): Try[Unit] = {
    Try {
      val cl = s.configuration.provider.scalaProvider.launcher.topLoader.asInstanceOf[URLClassLoader]
      val method = cl.getClass.getDeclaredMethod("addURL", classOf[URL])
      method.setAccessible(true)
      method.invoke(cl, f.toURI.toURL)
      method.setAccessible(false)
    }
  }

  def install(s: State): State = {
    val extracted = Project.extract(s)

    val os = extracted.get(Keys.osInfo in Keys.Robo)
    val key = Keys.compiler in Keys.Robo

    if (os.exists(_.roboSupported) && extracted.getOpt(key).flatten.isEmpty) {
      s.log.error("Skipping installation since robovm is not supported on platform")
      s
    } else {
      val deps = extracted.get(libraryDependencies in Compile)
      val ovrs = extracted.get(dependencyOverrides in  Compile)

      val (s2, cp) = extracted.runTask(fullClasspath in Compile, s)
      val comp = findCompiler(cp) _
      val dist = comp(ovrs.toList).orElse(comp(deps))

      if (dist.isEmpty)
        sys.error("Unable to find robovm compiler")

      s.log.info(s"Installing robovm compiler stored at ${dist.get}")

      val loaded = addToClassLoader(s)(dist.get)
      val key = Keys.compiler in Keys.Robo

      if(loaded.isSuccess) {
        extracted.append(Seq(key := Some(dist.get)) , s2)
      } else {
        s2
      }
    }
  }
}

object Commands extends Install {
  lazy val commands = Seq(
    Command.command("robovmInstall")(install)
  )
}
