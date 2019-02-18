# Co-Log Scala

Cross-platform pure functional logging library for Scala. Integrates with well known JVM logging frameworks and
 can also be used standalone in ScalaJS or Scala Native.
 
## Usage

Add the required dependency to your project

```
val cologVersion = "0.1.0"

libraryDependencies ++= Seq(
  "colog" %% "colog-core"       % cologVersion,
  "colog" %% "colog-standalone" % cologVersion
)
```

Now define an environment for your application:

```scala
import cats.effect.IO
import colog.{HasLogger, LogRecord, Logger}

case class Env(logger: Logger[IO, LogRecord])
object Env {

  implicit val envHasLogger: HasLogger[IO, Env, LogRecord] = new HasLogger[IO, Env, LogRecord] {
    override def getLogger(env: Env): Logger[IO, LogRecord] = env.logger

    override def setLogger(newLogger: Logger[IO, LogRecord], env: Env): Env =
      env.copy(logger = newLogger)
  }

}
```

Define also the effect type for your application, the simplest one is based on a ReaderT monad:

```scala
type AppIO[A] = ReaderT[IO, Env, A]
```

And you can get an instance of the logging API:

```scala
import cats.mtl.implicits._

val logging = Logging.structured[AppIO, Env]

logging.info("Hello")
// res0: AppIO[Unit]
```

So, with the previous, we are now able to emit log messages, which will be embedded in the `AppIO` effect we just created.
To be able to run those effects, we need to initialize the desired environment:

```scala
import colog.standalone._

final val env = Env(SysLoggers.stdout[IO].formatWithF(LogRecord.defaultFormat[IO]))
```

So now we can do the following:

```scala
logging.info("Hello").run(env).unsafeRunSync()
[Info] - Hello
```

## Acknowledgments

You gotta give credit where credit is due. `scala-colog` is a Scala fork of [`colog`](https://github.com/kowainik/co-log),
the Haskell library that initially explored the idea of composable loggers. For more information about the internals
read [this very descriptive blog post](https://kowainik.github.io/posts/2018-09-25-co-log) by its Author. 