/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog.standalone

import java.io.{File, FilenameFilter}
import java.nio.channels.AsynchronousFileChannel
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Path, Paths, StandardOpenOption}

import cats.data.StateT
import cats.effect.concurrent.Ref
import cats.effect.{Async, Resource, Sync}
import cats.implicits._
import cats.mtl.implicits._
import colog.Logger

import scala.util.Try

object FileLoggers {

  def single[F[_]](fileName: String)(implicit F: Async[F]): Resource[F, Logger[F, String]] =
    single[F](Paths.get(fileName))

  def single[F[_]](file: Path, charset: Charset = StandardCharsets.UTF_8)(
      implicit F: Async[F]
  ): Resource[F, Logger[F, String]] = {
    val channelResource = Resource.make(openLogFileChannel(file))(ch => F.delay(ch.close()))
    channelResource.map(fileChannel[F](_, charset))
  }

  def autoCleanRolling[F[_]](
      fileName: String,
      maxFileSize: Long,
      maxFiles: Long,
      charset: Charset = StandardCharsets.UTF_8
  )(
      implicit F: Async[F]
  ): Resource[F, Logger[F, String]] =
    rolling(fileName, maxFileSize, maxFiles, charset)(f => F.delay(f.delete()).void)

  def rolling[F[_]](
      fileName: String,
      maxFileSize: Long,
      maxFiles: Long,
      charset: Charset = StandardCharsets.UTF_8
  )(onFileTooOld: File => F[Unit])(
      implicit F: Async[F]
  ): Resource[F, Logger[F, String]] = {
    type FileHandle = (Path, AsynchronousFileChannel)

    def openLogFile: F[FileHandle] = {
      val path = new File(fileName).toPath.toAbsolutePath
      openLogFileChannel(path).map(path -> _)
    }

    def closeLogFile(handle: FileHandle): F[Unit] =
      F.delay(handle._2.close())

    def logFileResource: Resource[F, Ref[F, FileHandle]] =
      Resource.make(openLogFile)(closeLogFile).map(Ref.unsafe[F, FileHandle])

    def isFileSizeLimitReached(handle: FileHandle): F[Boolean] =
      F.delay(handle._2.size() >= maxFileSize)

    def splitFileName(path: File): (String, Option[String]) = {
      val fileName = path.getName
      val lastDot  = fileName.lastIndexOf('.')
      if (lastDot <= 0) (fileName, None)
      else (fileName.substring(0, lastDot), Some(fileName.substring(lastDot + 1, fileName.length)))
    }

    def logFileIndex(path: File): Option[Long] = {
      val (_, ext) = splitFileName(path)
      ext.flatMap(e => Either.fromTry(Try(e.toLong)).toOption)
    }

    def listSiblings(f: File): F[List[File]] = F.delay {
      val (baseName, _) = splitFileName(f)
      f.getParentFile
        .listFiles(new FilenameFilter {
          def accept(dir: File, name: String): Boolean = name.startsWith(baseName)
        })
        .toList
    }

    def maxFileIndex(path: File): F[Long] =
      for {
        siblings <- listSiblings(path)
        max      <- F.pure(siblings.mapFilter(logFileIndex).maximumOption.getOrElse(0L))
      } yield max

    def renameFileToNumber(file: File, n: Long): F[Unit] =
      F.delay(file.renameTo(new File(file.getParent, s"$fileName.${n.toString}"))).void

    def findOldFiles(f: File): F[List[File]] = {
      def isOld(f: File): Boolean = logFileIndex(f).exists(_ > maxFiles)

      for {
        siblings <- listSiblings(f)
        oldOnes  <- F.pure(siblings.filter(isOld))
      } yield oldOnes
    }

    def cleanUpAndRotate(handle: Ref[F, FileHandle]): F[Unit] =
      handle.get.flatMap {
        case (p, ch) =>
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

    def rotatingLogger(handleRef: Ref[F, FileHandle]): Resource[F, Logger[F, String]] = {
      def openLogger: F[Logger[F, String]] =
        F.delay(Logger[F, String] { msg =>
          for {
            h            <- handleRef.get
            l            <- F.pure(fileChannel[F](h._2, charset))
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

  private def openLogFileChannel[F[_]](
      path: Path
  )(implicit F: Sync[F]): F[AsynchronousFileChannel] =
    F.delay(AsynchronousFileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE))

  private def fileChannel[F[_]](channel: AsynchronousFileChannel, charset: Charset)(
      implicit F: Async[F]
  ): Logger[F, String] = {
    val baseLogger = IOLoggers.fileChannel[StateT[F, Long, ?]](channel, charset)

    Logger[F, String] { bytes =>
      for {
        channelSize <- F.delay(channel.size())
        _           <- baseLogger.log(bytes).runA(channelSize)
      } yield ()
    }
  }

}
