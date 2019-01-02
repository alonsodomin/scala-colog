package colog

import java.io.{File, FileOutputStream, PrintStream}

import cats.effect._
import cats.implicits._

package object io {

  def printStream[F[_]](printStream: PrintStream)(implicit F: LiftIO[F]): Logger[F, String] =
    Logger(msg => F.liftIO(IO(printStream.println(msg))))

  def stdout[F[_]](implicit F: LiftIO[F]): Logger[F, String] =
    Logger(str => F.liftIO(IO(println(str))))

  def stderr[F[_]](implicit F: LiftIO[F]): Logger[F, String] =
    printStream[F](System.err)

  def file[F[_]](fileName: String)(implicit F: Effect[F]): Resource[F, Logger[F, String]] = file[F](new File(fileName))

  def file[F[_]](file: File)(implicit F: Effect[F]): Resource[F, Logger[F, String]] = {
    val outResource = Resource.fromAutoCloseable(F.liftIO(IO(new FileOutputStream(file, true))))
    val printResource = outResource.flatMap(out => Resource.fromAutoCloseable(F.liftIO(IO(new PrintStream(out)))))
    printResource.map(printStream[F](_))
  }

}
