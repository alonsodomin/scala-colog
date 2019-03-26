---
id: testing
title: Testing
---

```scala mdoc:invisible
import colog._
import cats.implicits._
import cats.mtl.implicits._
```

When using the `colog` loggers in our code, we have the posibility of testing that the expected log statements are actually produced by using a pure logger type in our testing code.

The _pure_ logger can be instantiated using `Loggers.pure` and we need to provide it with a type that is able to buffer the log statements in memory for us. In the `core` module we have the `MemLog` (or it's transformer sibling `MemLogT`) type which we could use out of the box for that purpose:

```scala mdoc
type BufferedLog[A] = MemLog[String, A]
val bufferL = Loggers.pure[BufferedLog, String]
```

So now we can use that newly defined logger to emit our log statements:

```scala mdoc
val logged = bufferL.log("Hello")
```

The log statements have been accumulated in memory by a _Writer monad_, so we need to run it to obtain them:

```scala mdoc
val (logs, _) = logged.run
```

`Loggers.pure` can work with any functor that supports the `tell` operation (known as `FunctorTell` in `cats-mtl`), a writer monad is one of them (and that's what the `MemLog` type is) although it is not required to use it as long as a resolvable instance of a `FunctorTell[F[_], Vector[A]]` is available.