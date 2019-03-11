/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog

import cats.effect.IO
import cats.effect.laws.discipline.SyncTests
import cats.effect.laws.discipline.arbitrary._
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.{BifunctorTests, MonadTests, MonadErrorTests}

class LogTSpec extends CologSuite {

  checkAllAsync("Monad[LogT]", implicit ec => MonadTests[LogT[IO, String, ?]].monad[Int, Int, Int])
  checkAllAsync(
    "Bifunctor[LogT]",
    implicit ec => BifunctorTests[LogT[IO, ?, ?]].bifunctor[String, String, String, Int, Int, Int]
  )
  checkAllAsync(
    "MonadError[LogT]",
    implicit ec => MonadErrorTests[LogT[IO, String, ?], Throwable].monadError[Int, Int, Int]
  )
  checkAllAsync(
    "Sync[LogT]",
    implicit ec => {
      implicit val cs = ec.contextShift[IO]

      SyncTests[LogT[IO, String, ?]].sync[Int, Int, Int]
    }
  )
  /*checkAll(
    "Concurrent[LogT]",
    ConcurrentTests[LogT[IO, String, ?]].concurrent[String, String, String]
  )*/

}
