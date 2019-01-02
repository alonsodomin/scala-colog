package colog
package standalone

import java.io.{File, FileOutputStream, FilenameFilter, PrintStream}
import java.nio.channels.AsynchronousFileChannel
import java.nio.file._

import cats.effect._
import cats.effect.concurrent._
import cats.implicits._

import scala.util.Try

object FileLoggers {

  def single[F[_]](fileName: String)(implicit F: Effect[F]): Resource[F, Logger[F, String]] =
    single[F](new File(fileName))

  def single[F[_]](file: File)(implicit F: Effect[F]): Resource[F, Logger[F, String]] = {
    val outResource = Resource.fromAutoCloseable(F.liftIO(IO(new FileOutputStream(file, true))))
    val printResource = outResource.flatMap(out => Resource.fromAutoCloseable(F.liftIO(IO(new PrintStream(out)))))
    printResource.map(IOLoggers.printStream[F](_))
  }

  def autoCleanRolling[F[_]](fileName: String, maxFileSize: Long, maxFiles: Long)(
    implicit F: Effect[F]
  ): Resource[F, Logger[F, String]] =
    rolling(fileName, maxFileSize, maxFiles)(f => F.delay(f.delete()).void)

  def rolling[F[_]](
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

    def listSiblings(f: File): F[List[File]] = F.delay {
      val (baseName, _) = splitFileName(f)
      f.getParentFile.listFiles(new FilenameFilter {
        def accept(dir: File, name: String): Boolean = name.startsWith(baseName)
      }).toList
    }

    def maxFileIndex(path: File): F[Long] = for {
      siblings <- listSiblings(path)
      max      <- F.pure(siblings.mapFilter(logFileIndex).maximumOption.getOrElse(0L))
    } yield max

    def renameFileToNumber(file: File, n: Long): F[Unit] =
      F.delay(file.renameTo(new File(file.getParent, s"$fileName.$n"))).void

    def findOldFiles(f: File): F[List[File]] = {
      def isOld(f: File): Boolean = logFileIndex(f).exists(_ > maxFiles)

      for {
        siblings <- listSiblings(f)
        oldOnes  <- F.pure(siblings.filter(isOld))
      } yield oldOnes
    }

    def cleanUpAndRotate(handle: Ref[F, FileHandle]): F[Unit] = {
      handle.get.flatMap { case (p, ch) =>
        for {
          _         <- F.delay(ch.close())
          maxN      <- maxFileIndex(p.toFile)
          _         <- renameFileToNumber(p.toFile, maxN + 1)
          oldFiles  <- findOldFiles(p.toFile)
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
          l            <- F.pure(fileChannel[F](h._2))
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

  private def fileChannel[F[_]](channel: AsynchronousFileChannel)(implicit F: Effect[F]): Logger[F, String] = {
    ???
  }

}
