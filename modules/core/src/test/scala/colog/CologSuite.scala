/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog

import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.charset.StandardCharsets

import cats.instances.AllInstances
import cats.syntax.AllSyntax
import cats.effect.IO
import cats.effect.laws.util.{TestContext, TestInstances}

import org.typelevel.discipline.Laws
import org.typelevel.discipline.scalatest.Discipline

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.util.control.NonFatal
import org.scalatest.matchers.should.Matchers

abstract class CologSuite extends AnyFunSuite with Matchers with ScalaCheckDrivenPropertyChecks with Discipline with AllInstances with AllSyntax with TestInstances with CologInstances {
  type TestLogIOF[A] = MemLogT[IO, String, A]
  type TestLogF[A] = MemLog[String, A]

  def checkAllAsync(name: String, f: TestContext => Laws#RuleSet): Unit = {
    val context = TestContext()
    val ruleSet = f(context)

    for ((id, prop) <- ruleSet.all.properties)
      test(name + "." + id) {
        silenceSystemErr(check(prop))
      }

  }

  /**
    * Silences `System.err`, only printing the output in case exceptions are
    * thrown by the executed `thunk`.
    */
  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  private def silenceSystemErr[A](thunk: => A): A = synchronized {
    // Silencing System.err
    val oldErr    = System.err
    val outStream = new ByteArrayOutputStream()
    val fakeErr   = new PrintStream(outStream)
    System.setErr(fakeErr)
    try {
      val result = thunk
      System.setErr(oldErr)
      result
    } catch {
      case NonFatal(e) =>
        System.setErr(oldErr)
        // In case of errors, print whatever was caught
        fakeErr.close()
        val out = new String(outStream.toByteArray, StandardCharsets.UTF_8)
        if (out.nonEmpty) oldErr.println(out)
        throw e
    }
  }

}
