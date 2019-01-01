package colog

case class LogRecord(level: LogRecord.Level, message: String)
object LogRecord {

  sealed trait Level
  object Level {
    case object Info extends Level
  }

  val defaultFormatter: LogRecord => String =
    record => s"[${record.level}] - ${record.message}"

}