import cats._
import cats.effect.IO

package object colog {
  type Log[A, B] = LogT[Id, A, B]
  type LogIO[A, B] = LogT[IO, A, B]
  type StructuredLog[F[_], A] = LogT[F, LogRecord, A]
}
