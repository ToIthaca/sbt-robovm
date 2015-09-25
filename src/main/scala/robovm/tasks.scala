package robovm

import sbt.Keys._
import sbt.{Attributed, _}

import org.robovm.compiler.AppCompiler


object Tasks {



  lazy val tasks = Seq(
    Keys.bundle in Keys.Robo := {
      val a = new AppCompiler(null)
      println(a)
      "dummy"
    }

    /*Keys.compiler in Keys.Robo := {
      val log = streams.value.log
      val cp = (fullClasspath in Compile).value
      val deps = libraryDependencies.value
      val compilerDist = deps
        .find(m => m.name == "robovm-dist-compiler" && m.organization == "org.robovm")
        .flatMap(m => cp.find {
        case Attributed(file) => file.getName.contains(s"${m.name}-${m.revision}")
      })
        .map(_.data)
      if(compilerDist.isEmpty) {
        sys.error("Unable to find robovm compiler")
      }
      log.info(s"Using robovm compiler version ${compilerDist.get}")
      augment(Seq(compilerDist.get))
    }*/
  )
}
