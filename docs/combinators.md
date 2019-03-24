---
id: combinators
title: Combinators
---

```scala mdoc:invisible
import cats.effect._
import colog._
import colog.standalone._
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
