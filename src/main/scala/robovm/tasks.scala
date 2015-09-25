package robovm

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.zip.GZIPInputStream

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.utils.IOUtils
import org.jboss.shrinkwrap.resolver.api.MavenResolver
import org.jboss.shrinkwrap.resolver.api.maven._
import sbt.Keys._
import sbt._

import scala.util.{Failure, Try}

trait CompilerInstall {

  type InstallResolver = ConfigurableMavenResolverSystem

  lazy val compilerDist = "org.robovm:robovm-dist:tar.gz"
  lazy val tempDist = "robovm"

  def offline: InstallResolver = MavenResolver().workOffline()

  def depVersions(deps: Seq[ModuleID]) = {
    deps
      .find(m => m.organization == "org.robovm")
  }

  def resolve(res: Seq[Resolver])(artifact: String): Try[File] = {
    val rs = offline :: res.filter {
      case MavenRepository(_, _) => true
      case _ => false
    }.map {
      case MavenRepository(n, r) =>
        MavenResolver().withRemoteRepo(n, s"$r/", "default")
    }.toList

    rs.foldLeft(Failure[File](new IllegalStateException("No resolvers to resolve file")): Try[File])( (l, r) => {
      if(l.isFailure)
        Try {
          r.resolve(artifact).withoutTransitivity().asSingleFile()
        }
      else
        l
    })
  }

  def unzipDistro(target: File)(tar: File): File = {

    val dir = new File(target.absolutePath, tempDist)
    dir.mkdir()
    val unTar = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(tar)))
    var currentTar = unTar.getNextTarEntry

    while(currentTar != null) {
      val of = new File(dir, currentTar.getName)
      if(currentTar.isDirectory) {
        println(of.getAbsolutePath + "her...")
        of.mkdirs()
      } else {
        of.createNewFile()
        //of.mkdirs()
        println(of.getAbsolutePath + "here?")
        //of.createNewFile()
        val os = new FileOutputStream(of)
        IOUtils.copy(unTar, os)
        os.close()
      }
      currentTar = unTar.getNextTarEntry
    }
    dir
  }

  lazy val install = Def.task {
    val log = streams.value.log
    val versions = depVersions((libraryDependencies in Compile).value)
    val platformTarget = (Keys.platformTarget in Keys.Robo).value

    versions.foreach(v => {
      if(v.revision != platformTarget)
        log.warn(s"${v.name} is not consistent with platform $platformTarget")
    })

    log.info(s"Installing robovm compiler $platformTarget")
    val res = resolve(resolvers.value) _
    val artifact = s"$compilerDist:$platformTarget"
    val file = res(artifact)

    if(file.isFailure)
      sys.error(s"Failed to resolve $artifact")

    log.info(s"Installed robovm compiler $platformTarget")
    unzipDistro(target.value)(file.get)
  }
}

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
    val exec = new proguard.ProGuard(config)
    exec.execute()
    log.info("Finished obfusticating code using proguard")
    file
  }
}

object Tasks extends ProGuard with CompilerInstall {


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
    Keys.prepare in Keys.Robo <<= prepare,
    Keys.dist in Keys.Robo <<= install
  )
}
