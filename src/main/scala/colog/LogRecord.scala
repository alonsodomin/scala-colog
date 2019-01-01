package colog

import java.io.{ByteArrayOutputStream, PrintStream}
import java.time.Instant
import java.time.format.DateTimeFormatter

import cats.effect.{Resource, Sync}
import cats.implicits._

final case class LogRecord(severity: Severity, message: String, error: Option[Throwable] = None)
object LogRecord {

  def defaultFormat[F[_]](implicit F: Sync[F]): LogRecord => F[String] = record => {
    val stackTrace: F[Option[String]] = record.error.traverse { err =>
      val outResource = Resource.fromAutoCloseable(F.delay(new ByteArrayOutputStream()))
      outResource.use { out =>
        val psResource = Resource.fromAutoCloseable(F.delay(new PrintStream(out)))
        psResource.use(ps => F.delay(err.printStackTrace(ps))) *> F.delay(out.toString)
      }
    }

    stackTrace.map { st =>
      val baseText = show"[${record.severity}] - ${record.message}"
      st.fold(baseText)(ex => s"$baseText\n$ex")
    }
  }

}

final case class TimestampedLogRecord(instant: Instant, record: LogRecord)
object TimestampedLogRecord {

  def defaultFormat: TimestampedLogRecord => LogRecord =
    formatWith(DateTimeFormatter.ISO_INSTANT)

  def formatWith(formatter: DateTimeFormatter): TimestampedLogRecord => LogRecord = {
    case TimestampedLogRecord(time, rec) =>
      LogRecord(rec.severity, s"[${formatter.format(time)}] ${rec.message}", rec.error)
  }

}