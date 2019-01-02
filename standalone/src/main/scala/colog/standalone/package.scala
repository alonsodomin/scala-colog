package colog

import java.io.{File, FileOutputStream, FilenameFilter, PrintStream}
import java.nio.channels.AsynchronousFileChannel
import java.nio.file._

import cats.effect._
import cats.effect.concurrent._
import cats.implicits._

import scala.util.Try

package object standalone {

  def printStream[F[_]](printStream: PrintStream)(implicit F: LiftIO[F]): Logger[F, String] =
    Logger(msg => F.liftIO(IO(printStream.println(msg))))

  def stdout[F[_]](implicit F: LiftIO[F]): Logger[F, String] =
    Logger(str => F.liftIO(IO(println(str))))

  def stderr[F[_]](implicit F: LiftIO[F]): Logger[F, String] =
    printStream[F](System.err)

  def file[F[_]](fileName: String)(implicit F: Effect[F]): Resource[F, Logger[F, String]] =
    file[F](new File(fileName))

  def file[F[_]](file: File)(implicit F: Effect[F]): Resource[F, Logger[F, String]] = {
    val outResource = Resource.fromAutoCloseable(F.liftIO(IO(new FileOutputStream(file, true))))
    val printResource = outResource.flatMap(out => Resource.fromAutoCloseable(F.liftIO(IO(new PrintStream(out)))))
    printResource.map(printStream[F](_))
  }

  private def fileChannel[F[_]](channel: AsynchronousFileChannel)(implicit F: Effect[F]): Logger[F, String] = {
    ???
  }

  def rollingFile[F[_]](
    fileName: String, maxFileSize: Long, maxFiles: Long
  )(onFileTooOld: File => F[Unit])(
    implicit F: Effect[F]
  ): Resource[F, Logger[F, String]] = {
    type FileHandle = (Path, AsynchronousFileChannel)

    def openLogFile: F[FileHandle] = F.delay {
      val path = new File(fileName).toPath.toAbsolutePath
      val channel = AsynchronousFileChannel.open(path, StandardOpenOption.APPEND)
      (path, channel)
    }

    def closeLogFile(handle: FileHandle): F[Unit] =
      F.delay(handle._2.close())

    def logFileResource: Resource[F, Ref[F, FileHandle]] =
      Resource.make(openLogFile)(closeLogFile).map(Ref.unsafe[F, FileHandle])

    def isFileSizeLimitReached(handle: FileHandle): F[Boolean] =
      F.delay(handle._2.size() >= maxFileSize)

    def splitFileName(path: File): (String, Option[String]) = {
      val fileName = path.getName
      val lastDot = fileName.lastIndexOf('.')
      if (lastDot <= 0) (fileName, None)
      else (fileName.substring(0, lastDot), Some(fileName.substring(lastDot + 1, fileName.length)))
    }

    def logFileIndex(path: File): Option[Long] = {
      val (_, ext) = splitFileName(path)
      ext.flatMap(e => Either.fromTry(Try(e.toLong)).toOption)
    }

    def maxFileIndex(path: File): F[Long] = {
      val (baseName, _) = splitFileName(path)
      val listSibilings = F.delay {
        path.getParentFile.listFiles(new FilenameFilter {
          def accept(dir: File, name: String): Boolean = name.startsWith(baseName)
        }).toList
      }

      for {
        sibilings <- listSibilings
        max       <- F.delay(sibilings.mapFilter(logFileIndex).maximumOption.getOrElse(0L))
      } yield max
    }

    def renameFileToNumber(file: File, n: Long): F[Unit] =
      F.delay(file.renameTo(new File(file.getParent, s"$fileName.$n"))).void

    def findOldFiles: F[List[File]] = ???

    def cleanUpAndRotate(handle: Ref[F, FileHandle]): F[Unit] = {
      handle.get.flatMap { case (p, ch) =>
        for {
          _         <- F.delay(ch.close())
          maxN      <- maxFileIndex(p.toFile)
          _         <- renameFileToNumber(p.toFile, maxN + 1)
          oldFiles  <- findOldFiles
          _         <- oldFiles.traverse_(onFileTooOld)
          newHandle <- openLogFile
          _         <- handle.set(newHandle)
        } yield ()
      }
    }

    def rotatingLogger(handleRef: Ref[F, FileHandle]): Resource[F, Logger[F, String]] = {
      def openLogger: F[Logger[F, String]] = F.delay(Logger[F, String] { msg =>
        for {
          h            <- handleRef.get
          l            <- F.delay(fileChannel[F](h._2))
          _            <- l.log(msg)
          limitReached <- isFileSizeLimitReached(h)
          _            <- F.whenA(limitReached)(cleanUpAndRotate(handleRef))
        } yield ()
      })

      def closeLogger: F[Unit] = handleRef.get >>= closeLogFile

      Resource.make(openLogger)(_ => closeLogger)
    }

    logFileResource >>= rotatingLogger
  }

}
