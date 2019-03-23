/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import java.time.Instant

import cats._
import cats.effect.IO

package object colog {
  type Log[A, B]              = LogT[Id, A, B]
  type LogIO[A, B]            = LogT[IO, A, B]
  type StructuredLog[F[_], A] = LogT[F, LogRecord, A]
  type StructuredLogIO[A]     = StructuredLog[IO, A]

  type Timestamped[A] = (Instant, A)

  implicit val severityOrdering: Ordering[Severity] = Ordering.by(_.ordinal)

}
