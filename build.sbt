lazy val publishSettings = Seq(
  name := "sbt-robovm",
  licenses += ("BSD 3-Clause", url("http://opensource.org/licenses/BSD-3-Clause")),
  organization := "com.ithaca",
  version := "0.1-SNAPSHOT"
)

lazy val commonSettings = Seq(
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  libraryDependencies ++= Seq(
    "org.jboss.shrinkwrap.resolver" % "shrinkwrap-resolver-depchain" % "2.2.0-alpha-2",
    "org.apache.commons" % "commons-compress" % "1.8.1",
    "org.robovm" % "robovm-dist-compiler" % "1.8.0" % "provided",
    "net.sf.proguard" % "proguard-base" % "5.0"
  )
)

lazy val root = (project in file("."))
  .settings(sbtPlugin := true)
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
