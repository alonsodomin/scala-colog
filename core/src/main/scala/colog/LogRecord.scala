/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog

import java.io.{ByteArrayOutputStream, PrintStream}
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

  def defaultTimestampedFormat: Timestamped[LogRecord] => LogRecord =
    timestampedFormat(DateTimeFormatter.ISO_INSTANT)

  def timestampedFormat(formatter: DateTimeFormatter): Timestamped[LogRecord] => LogRecord = {
    case (time, rec) =>
      LogRecord(rec.severity, s"[${formatter.format(time)}] ${rec.message}", rec.error)
  }

}
