package io.scalaland.catnip.internals

private[internals] trait Loggers {

  sealed abstract class Level(val name: String, val ordinal: Int) extends Product with Serializable {
    override def toString: String = name.toUpperCase
  }
  object Level {
    case object Trace extends Level("trace", 0)
    case object Debug extends Level("debug", 1)
    case object Off extends Level("off", 2)
    val values = Seq(Off, Debug, Trace)
    def findByName(name: String): Option[Level] = values.find(_.name.equalsIgnoreCase(name))
  }

  protected def withDebugLog[T](msg: String)(thunk: => T): T = withLog(Level.Debug)(msg)(thunk)

  protected def withTraceLog[T](msg: String)(thunk: => T): T = withLog(Level.Trace)(msg)(thunk)

  private def withLog[T](level: Level)(msg: String)(thunk: => T): T = {
    val value = thunk
    if (shouldLog(level)) { println(s"[$level] $msg:\n$value") } // scalastyle:ignore regex
    value
  }

  private def shouldLog(level: Level): Boolean =
    Option(System.getProperty("catnip.debug")).flatMap(Level.findByName).getOrElse(Level.Off).ordinal <= level.ordinal
}
