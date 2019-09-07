/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog
package slf4j

import cats.effect.IO
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import org.slf4j.{Logger => JLogger}

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class Slf4jLoggersSpec extends AnyFlatSpec with Matchers with MockFactory {

  val baseLogger = mock[JLogger]
  val logger = Slf4jLoggers[IO](baseLogger)

  val severities = Seq(Severity.Debug, Severity.Info, Severity.Warning, Severity.Error)

  "Record severity" must "be translated to the appropriate method" in {
    for (severity <- severities) {
      val record = LogRecord(severity, "foo message", None)

      severity match {
        case Severity.Debug   => (baseLogger.debug(_: String, _: Throwable)).expects(record.message, null)
        case Severity.Info    => (baseLogger.info(_: String, _: Throwable)).expects(record.message, null)
        case Severity.Warning => (baseLogger.warn(_: String, _: Throwable)).expects(record.message, null)
        case Severity.Error   => (baseLogger.error(_: String, _: Throwable)).expects(record.message, null)
      }

      logger.log(record).unsafeRunSync()
    }
  }

}
