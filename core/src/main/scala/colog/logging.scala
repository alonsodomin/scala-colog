/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog

import cats.Monad
import cats.mtl.ApplicativeAsk
import cats.implicits._

trait Logging[F[_], E, A] {
  implicit def F: Monad[F]

  protected def A: ApplicativeAsk[F, E]
  protected def HL: HasLogger[F, E, A]

  final def logMsg(msg: A): F[Unit] = for {
    logger <- A.reader(HL.getLogger)
    _      <- logger.log(msg)
  } yield ()

}

trait StructuredLogging[F[_], E] extends Logging[F, E, LogRecord] {

  final def error(msg: String): F[Unit] =
    log(Severity.Error, msg)

  final def error(msg: String, error: Throwable): F[Unit] =
    log(Severity.Error, msg, error)

  final def warn(msg: String): F[Unit] =
    log(Severity.Warning, msg)

  final def warn(msg: String, error: Throwable): F[Unit] =
    log(Severity.Warning, msg, error)

  final def info(msg: String): F[Unit] =
    log(Severity.Info, msg)

  final def info(msg: String, error: Throwable): F[Unit] =
    log(Severity.Info, msg, error)

  final def debug(msg: String): F[Unit] =
    log(Severity.Debug, msg)

  final def debug(msg: String, error: Throwable): F[Unit] =
    log(Severity.Debug, msg, error)

  final def log(severity: Severity, msg: String): F[Unit] =
    logMsg(LogRecord(severity, msg))

  final def log(severity: Severity, msg: String, error: Throwable): F[Unit] =
    logMsg(LogRecord(severity, msg, Some(error)))

}

object Logging {

  def simple[F[_], E](
      implicit
      F0: Monad[F],
      A0: ApplicativeAsk[F, E],
      HL0: HasLogger[F, E, String]
  ): Logging[F, E, String] =
    new Logging[F, E, String] {
      val F = F0

      val A = A0

      val HL = HL0
    }

  def structured[F[_], E](
      implicit
      F0: Monad[F],
      A0: ApplicativeAsk[F, E],
      HL0: HasLogger[F, E, LogRecord]
  ): StructuredLogging[F, E] = new StructuredLogging[F, E] {
    val F = F0

    val A = A0

    val HL = HL0
  }

}