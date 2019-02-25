/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog

import cats.effect.IO
import cats.effect.laws.discipline.arbitrary._
import cats.effect.laws.util.TestInstances._
import cats.kernel.laws.discipline.MonoidTests
import cats.laws.discipline.{ContravariantMonoidalTests, MonoidKTests}

class LoggerSpec extends CologSuite {

  checkAll("Monoid[Logger]", MonoidTests[Logger[IO, String]].monoid)
  checkAll("MonoidK[Logger]", MonoidKTests[Logger[IO, ?]].monoidK[String])
  checkAll(
    "ContravariantMonoidal[Logger]",
    ContravariantMonoidalTests[Logger[IO, ?]].contravariantMonoidal[String, String, String]
  )

}
