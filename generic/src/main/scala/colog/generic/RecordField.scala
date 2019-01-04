package colog.generic

import java.time.Instant
import java.util.concurrent.TimeUnit
//import java.util.concurrent.TimeUnit

import cats.effect._
import shapeless._
import shapeless.labelled._
import shapeless.syntax.SingletonOps
import shapeless.ops.record.{Keys, Values}
import shapeless.syntax.singleton._

object RecordField {
  type RecordFieldType[A] = FieldType[SingletonOps#T, IO[A]]

  type ThreadId = RecordFieldType[Long]
  type Timestamp = RecordFieldType[Instant]

  def threadId =
    "threadId" ->> IO(Thread.currentThread().getId)

  def currentInstant(implicit timer: Timer[IO]) =
    'timestamp ->> timer.clock.realTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli)

  /*def threadId: ThreadId =
    'threadId ->> IO(Thread.currentThread().getId)

  def currentInstant(implicit timer: Timer[IO]): Timestamp =
    'timestamp ->> timer.clock.realTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli)

  def defaultFieldMap(implicit timer: Timer[IO]) = {
    threadId :: currentInstant :: HNil
  }*/

  def format[Rec <: HList, K <: HList, V <: HList](rec: Rec)(
    implicit
    keys: Keys.Aux[Rec, K],
    values: Values.Aux[Rec, V]
  ): Rec => String = rec => {


    ???
  }

}
