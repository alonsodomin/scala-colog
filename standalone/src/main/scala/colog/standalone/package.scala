package colog

import java.nio.channels.CompletionHandler

package object standalone {

  def toCompletionHandler[A](cb: Either[Throwable, A] => Unit): CompletionHandler[A, AnyRef] =
    new CompletionHandler[A, AnyRef] {
      override def completed(result: A, attachment: AnyRef): Unit = cb(Right(result))
      override def failed(exc: Throwable, attachment: AnyRef): Unit = cb(Left(exc))
    }

  def toCompletionHandlerU[A](cb: Either[Throwable, Unit] => Unit): CompletionHandler[Void, AnyRef] =
    toCompletionHandler(cb.compose(_.map(_ => ())))

}
