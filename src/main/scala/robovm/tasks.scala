package robovm

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.net.URLClassLoader
import java.util.zip.GZIPInputStream

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.utils.IOUtils
import org.jboss.shrinkwrap.resolver.api.MavenResolver
import org.jboss.shrinkwrap.resolver.api.maven._
import org.robovm.compiler.AppCompiler
import sbt.Keys._
import sbt._

import scala.util.{Failure, Try}

trait Install {

  type InstallResolver = ConfigurableMavenResolverSystem

  lazy val compilerDist = "org.robovm:robovm-dist:tar.gz"
  lazy val compilerJar = "robovm-dist-compiler"
  lazy val tempDist = "robovm"

  def offline: InstallResolver = MavenResolver().workOffline()

  def roboDeps(deps: Seq[ModuleID]) = deps.filter(m => m.organization == "org.robovm")

  //TODO: Use Ivy once I can work out the godawful API
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
  
  def distroLocation(target: File): File = new File(target.absolutePath, tempDist)

  def unzipDistro(log: Logger)(target: File, tar: File): File = {

    val dir = distroLocation(target)
    IO.createDirectory(dir)
    val unTar = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(tar)))
    var currentTar = unTar.getNextTarEntry
    while(currentTar != null) {
      val of = new File(dir, currentTar.getName)
      if(currentTar.isDirectory) {
        IO.createDirectory(of)
      } else {
        val bytes = new ByteArrayOutputStream()
        IOUtils.copy(unTar, bytes)
        IO.write(of, bytes.toByteArray)
      }
      log.debug(s"Extracted file from distribution ${of.absolutePath}")
      currentTar = unTar.getNextTarEntry
    }
    dir
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

  def checkInstalled(s: State): Boolean = Try { Class.forName("org.robovm.compiler.AppCompiler") }.isSuccess

  lazy val install = Def.task {
    val s = state.value
    val log = streams.value.log
    val versions = roboDeps((libraryDependencies in Compile).value)
    val platformTarget = (Keys.platformTarget in Keys.Robo).value

    versions.foreach(v => {
      if (v.revision != platformTarget)
        log.warn(s"${v.name} is not consistent with platform $platformTarget")
    })

    if (!checkInstalled(s)) {
      log.info(s"Installing robovm compiler $platformTarget")
      val res = resolve(resolvers.value) _
      val artifact = s"$compilerDist:$platformTarget"
      val file = res(artifact)

      if (file.isFailure)
        sys.error(s"Failed to resolve $artifact")

      log.info(s"Installed robovm compiler $platformTarget")
      val distro = unzipDistro(log)(target.value, file.get)
      val compiler = compilerJar(distro, platformTarget)
      addToClassLoader(s)(compiler)
      distro
    } else {
      log.info("Already installed compiler")
      distroLocation(target.value)
    }
  }

  def compilerJar(installDir: File, target: String): File = {
    roboJar(compilerJar)(installDir, target)
  }

  def providedJars(installDir: File, target: String)(mods: Seq[ModuleID]): Seq[File] = {
    mods.map(m => roboJar(m.name)(installDir, target))
  }

  private[this] def roboJar(lib: String)(installDir: File, target: String): File = {
    val f = new File(s"${installDir.absolutePath}/robovm-$target/lib/$lib.jar")
    if(!f.exists())
      sys.error(s"Attempted to access non-existent file ${f.absolutePath}")
    f
  }

}

trait ProGuard extends Install {

  lazy val settingsFile = "proguard-sbt.txt"

  def asInputOutput(inOut: (File, File)): List[proguard.ClassPathEntry] = List(
    new proguard.ClassPathEntry(inOut._1, false),
    new proguard.ClassPathEntry(inOut._2, true)
  )
  def asClasspath(log: Logger)(fs: List[File], targetDir: File): (proguard.ClassPath, Seq[File]) = {
    val cp = new proguard.ClassPath

    val ofs = fs.map(f => {
      val nf = new File(s"$targetDir/${f.getName}")
      log.debug(s"Proguard file from ${f.absolutePath} to ${f.absolutePath}")
      nf
    })
    fs.zip(ofs).flatMap(asInputOutput).zipWithIndex.foreach {
      case (entry, idx) => cp.add(idx, entry)
    }
    cp -> ofs
  }

  lazy val proGuard = Def.task {
    val log = streams.value.log
    val robo = (Keys.dist in Keys.Robo).value
    val target = (Keys.platformTarget in  Keys.Robo).value
    val roboCompiler = compilerJar(robo, target)
    val rdeps = roboDeps((libraryDependencies in Compile).value)

    val file = baseDirectory.value / settingsFile
    log.info(s"Using proguard version ${proguard.ProGuard.VERSION} with existing settings ${file.getAbsolutePath}")
    val cps = (fullClasspath in Runtime).value.files

    val config = new proguard.Configuration
    val configParser = new proguard.ConfigurationParser(file, System.getProperties)
    configParser.parse(config)

    //TODO: Use rdeps to be more robust

    val proguardDir = new File(robo, "proguard")
    IO.createDirectory(proguardDir)

    val (pcp, fs) = asClasspath(log)(cps.toList.filter(f => !f.getAbsolutePath.contains("robovm")), proguardDir)
    config.programJars = pcp

    val libcp = new proguard.ClassPath
    libcp.add(new proguard.ClassPathEntry(new java.io.File(System.getProperty("java.home")), false))

    providedJars(robo, target)(rdeps).foreach(j => {
      log.debug(s"Adding robovm provided jar to skip obfustication ${j.absolutePath}")
      libcp.add(new proguard.ClassPathEntry(j, false))
    })

    config.libraryJars = libcp
    val exec = new proguard.ProGuard(config)
    log.info(s"Starting proguard obfustication")
    exec.execute()
    log.info("Finished obfusticating code using proguard")
    fs
  }
}

trait Bundle {
  lazy val bundle = Def.task {
    println(new AppCompiler(null))
    "asd"
  }
}

object Tasks extends ProGuard with Bundle {


  def prepare(config: Configuration) = Def.taskDyn {
    val log = streams.value.log
    log.info("Preparing project for robovm compilation")
    val comp = (fullClasspath in Runtime).value.files

    if((Keys.useProguard in config).value)
      proGuard
    else {
      Def.task(comp)
    }
  }


  private[robovm] def tasks(config: Configuration) = Seq(
    Keys.dist in Keys.Robo <<= install,
    Keys.bundle in  config <<= bundle,
    Keys.prepare in config <<= prepare(config)
  )
}
