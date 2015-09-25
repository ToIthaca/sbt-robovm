package robovm

case class OSInfo(platform: Platform, arch: Architecture)

sealed trait Platform

case object Windows extends Platform
case object Linux extends Platform
case object Mac extends Platform

sealed trait Architecture

case object X86 extends Architecture
case object AMD64 extends Architecture
case object PPC extends Architecture

final class OSInfoOps(os: OSInfo) {
  def roboSupported: Boolean = os.platform == Mac
}

object OSInfo {

  implicit def info2Ops(os: OSInfo): OSInfoOps = new OSInfoOps(os)

  lazy val platforms = Set(Windows, Linux, Mac)
  lazy val architectures = Set(X86, AMD64, PPC)

  def classify[A](prop: String)(all: Set[A]): Option[A] = for {
    prop <- sys.props.get(prop)
    classified <- all.find(_.toString.contains(prop))
  } yield classified

  def apply(): Option[OSInfo] = for {
    p <- classify("os.name")(platforms)
    a <- classify("os.arch")(architectures)
  } yield OSInfo(p, a)

}


