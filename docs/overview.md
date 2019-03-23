---
id: overview
title: Overview
---

Cross-platform pure functional logging library for Scala. Integrates with well known JVM logging frameworks and
 can also be used standalone in ScalaJS or Scala Native.
 
## Usage

Add the required dependency to your project

```scala
val cologVersion = "@VERSION@"

libraryDependencies ++= Seq(
  "com.github.alonsodomin.colog" %% "colog-core"       % cologVersion,
  "com.github.alonsodomin.colog" %% "colog-standalone" % cologVersion,    // Cross-platform
  "com.github.alonsodomin.colog" %% "colog-slf4j"      % cologVersion,    // JVM only
)
```

Now define an environment for your application:

```scala mdoc
import cats.effect.IO
import colog.{HasLogger, LogRecord, Logger}

case class Env(logger: Logger[IO, LogRecord])
object Env {

  implicit val envHasLogger: HasLogger[IO, Env, LogRecord] = new HasLogger[IO, Env, LogRecord] {
    override def getLogger(env: Env): Logger[IO, LogRecord] = env.logger

    override def setLogger(env: Env)(newLogger: Logger[IO, LogRecord]): Env =
      env.copy(logger = newLogger)
  }

}
```

Define also the effect type for your application, the simplest one is based on a ReaderT monad:

```scala mdoc
import cats.data.ReaderT

type AppIO[A] = ReaderT[IO, Env, A]
```

And you can get an instance of the logging API:

```scala mdoc
import colog.Logging
import cats.mtl.implicits._

val logging = Logging.structured[AppIO, Env]

logging.info("Hello")
```

So, with the previous, we are now able to emit log messages, which will be embedded in the `AppIO` effect we just created.
To be able to run those effects, we need to initialize the desired environment:

```scala mdoc
import colog.standalone._

final val env = Env(SysLoggers.stdout[IO].formatWithF(LogRecord.defaultFormat[IO]))
```

So now we can do the following:

```scala mdoc
val logAction = logging.info("Hello")
 
logAction.run(env).unsafeRunSync()
```

If interested on also having timestamps in your log statements, we just need to use a timestamped logger:

```scala mdoc
import scala.concurrent.ExecutionContext

implicit val timer = IO.timer(ExecutionContext.global)

final val timestampedEnv = Env(
  SysLoggers.stdout[IO]    
    .formatWithF(LogRecord.defaultFormat[IO])
    .timestampedWith(LogRecord.defaultTimestampedFormat)
)

logAction.run(timestampedEnv).unsafeRunSync()
```