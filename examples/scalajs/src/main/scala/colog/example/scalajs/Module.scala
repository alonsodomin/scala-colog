package colog.example.scalajs

import cats.effect.Sync
import cats.implicits._

import colog._

final class Module[F[_]](implicit F: Sync[F], logging: StructuredLogging[F, Env]) {

  def doSomething(): F[Unit] = for {
    _ <- logging.debug("Starting to do something")
    _ <- F.delay(println("Application working"))
    _ <- logging.info("Finished doing something")
  } yield ()

}