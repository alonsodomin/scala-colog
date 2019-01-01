package colog

import cats.{Order, Show}

sealed abstract class Severity(val ordinal: Int)
object Severity {
  case object Debug extends Severity(0)
  case object Info extends Severity(1)
  case object Warning extends Severity(2)
  case object Error extends Severity(3)

  implicit val severityOrd: Order[Severity] = Order.fromOrdering

  implicit val severityShow: Show[Severity] = Show.fromToString
}