/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog

import cats.effect.IO
import cats.effect.laws.discipline.ConcurrentTests
import cats.effect.laws.discipline.arbitrary._
import cats.effect.laws.util.TestInstances._
import cats.laws.discipline.{BifunctorTests, MonadTests}

class LogTSpec extends CologSuite {

  checkAll("Monad[LogT]", MonadTests[LogT[IO, String, ?]].monad[Int, Int, Int])
  checkAll(
    "Bifunctor[LogT]",
    BifunctorTests[LogT[IO, ?, ?]].bifunctor[String, String, String, Int, Int, Int]
  )
  checkAll(
    "Concurrent[LogT]",
    ConcurrentTests[LogT[IO, String, ?]].concurrent[String, String, String]
  )

}
