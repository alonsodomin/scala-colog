import cats._
import cats.data.{State, StateT}
import cats.effect.IO

package object colog {
  type Log[A, B] = LogT[Id, A, B]
  type LogIO[A, B] = LogT[IO, A, B]
  type StructuredLog[F[_], A] = LogT[F, LogRecord, A]

  type PureLogT[F[_], A, B] = LogT[StateT[F, Vector[A], ?], A, B]
  type PureLog[A, B] = LogT[State[Vector[A], ?], A, B]

  implicit val severityOrdering: Ordering[Severity] = Ordering.by(_.ordinal)

}
