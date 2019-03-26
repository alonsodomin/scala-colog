---
id: combinators
title: Combinators
---

```scala mdoc:invisible
import cats.effect._
import cats.implicits._
import colog._
import colog.standalone._
```

## Combining Loggers

The `Logger` type has instances for `Semigroup` and `Monoid`, allowing us to combine different logging methods into a single one. As an example, let's define a logger for the standard output and another one for the standard error:

```scala mdoc:silent
val stdoutL = SysLoggers.stdout[IO]
val stderrL = SysLoggers.stderr[IO]
```

Now we can combine them into a single logger instance using the semigroup _combine_ operation:

```scala mdoc:silent
val stdL = stdoutL |+| stderrL
```

This new combined logger can now be used to send log statements to both the standard output channels:

```scala mdoc
stdL.log("This message should be sent to both stdout and stderr").unsafeRunSync()
```

Now, this is cool but not particularly interesting since we won't be that really interested on sending log statements to all the standard output channels. When this becomes more useful is when one of our logger outputs is a file on disk. The `standalone` module provides with several file loggers that we can use for that purpose:

```scala mdoc:silent
val fileL = FileLoggers.single[IO]("/tmp/colog.log")
```

The key difference in here is that we get a `Resource[IO, Logger[IO, String]]` type, since there is an underlying system resource that needs to be handled; so we can not combine this directly with the logger for the standard out, we need to map over the resource instead:

```scala mdoc:silent
val fileAndOutL = fileL.map(_ |+| stdoutL)
```

And now we are ready to use the resource and send a log statement to both, the standard out, and the file specified previously:

```scala mdoc
fileAndOutL
  .use(_.log("This message should be sent to stdout and a file"))
  .unsafeRunSync()
```

## Case Classes

The composability of the `Logger` type can take us to the extent of wanting to emit entire case classes via them. In order to do so, let's start by defining our domain:

```scala mdoc
case class User(name: String, role: String)
```

And now lest compose our logger by combining different small ones:

```scala mdoc:silent
val stringL = SysLoggers.stdout[IO]
def constL(msg: String): Logger[IO, Unit] =
  msg >:: stringL

val userLogger: Logger[IO, User] =
  (constL("username: ") *< stringL >*< constL("role: ") *< stringL)
  .formatWithOption(User.unapply)
```

So now we can just simply use it as follows:

```scala mdoc
val aUser = User("foo", "bar")
userLogger.log(aUser).unsafeRunSync()
```
