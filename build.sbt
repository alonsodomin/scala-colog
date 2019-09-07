import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

inThisBuild(
  Seq(
    organizationName := "A. Alonso Dominguez",
    organization := "com.github.alonsodomin.colog",
    startYear := Some(2018),
    licenses += ("MPL-2.0", url("https://www.mozilla.org/en-US/MPL/")),
    homepage := Some(url("https://github.com/alonsodomin/scala-colog")),
    developers += Developer(
      "alonsodomin",
      "A. Alonso Dominguez",
      "",
      url("https://github.com/alonsodomin")
    )
  )
)

lazy val globalSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "utf-8",                         // Specify character encoding used by source files.
    "-explaintypes",                 // Explain type errors in more detail.
    "-feature",                      // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",        // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds",         // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-unchecked",                    // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                   // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",              // Fail the compilation if there are any warnings.
    "-Xlint:adapted-args",           // Warn if an argument list is modified to match the receiver.
    "-Xlint:delayedinit-select",     // Selecting member of DelayedInit.
    "-Xlint:doc-detached",           // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",           // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",              // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",   // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",       // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",           // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",        // Option.apply used implicit view.
    "-Xlint:package-object-classes", // Class or object defined in package object.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",         // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",            // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",  // A local type parameter shadows a type already in scope.
    "-Ywarn-dead-code",              // Warn when dead code is identified.
    "-Ywarn-numeric-widen",          // Warn when numerics are widened.
    "-Ywarn-value-discard"           // Warn when non-Unit expression results are unused.
  ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 12 =>
        Seq(
          "-Xlint:constant",
          "-Ywarn-extra-implicit",
          "-Ywarn-unused:imports",
          "-Ywarn-unused:locals",
          "-Ywarn-unused:patvars",
          "-Ywarn-unused:privates"
        )
      case Some((2, n)) if n == 11 =>
        Seq(
          "-Ywarn-unused"
        )
      case _ => Nil
    }
  },
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n < 13 =>
        Seq(
          "-Xfuture",
          "-Xlint:by-name-right-associative",
          "-Xlint:unsound-match",
          "-Ypartial-unification",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit"
        )
      case _ => Nil
    }
  },
  scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),
  initialCommands in console := Seq(
    "import cats._",
    "import cats.data._",
    "import cats.implicits._",
    "import cats.effect._",
    "import cats.effect.implicits._",
    "import cats.mtl._",
    "import cats.mtl.implicits._",
    "import colog._",
    "import scala.concurrent.ExecutionContext",
    "implicit val globalCS = IO.contextShift(ExecutionContext.global)",
    "implicit val globalTimer = IO.timer(ExecutionContext.global)"
  ).mkString("\n"),
  apiURL := Some(url("https://alonsodomin.github.io/scala-colog/api/")),
  autoAPIMappings := true,
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary),
  wartremoverErrors in (Compile, compile) ++= {
    val disabledWarts = Set(Wart.DefaultArguments, Wart.Any)
    Warts.unsafe.filterNot(disabledWarts)
  },
  wartremoverErrors in (Compile, console) := Nil,
  coverageEnabled := true,
  sonatypeProfileName := "com.github.alonsodomin"
)

lazy val commonJsSettings = Seq(
  scalaJSOptimizerOptions := scalaJSOptimizerOptions.value.withBatchMode(isTravisBuild.value),
  scalacOptions += {
    val tagOrHash = {
      if (isSnapshot.value)
        sys.process.Process("git rev-parse HEAD").lineStream_!.head
      else version.value
    }
    val a = (baseDirectory in LocalRootProject).value.toURI.toString
    val g = "https://raw.githubusercontent.com/alonsodomin/scala-colog/" + tagOrHash
    s"-P:scalajs:mapSourceURI:$a->$g/"
  }
)

def scalaStyleSettings(config: Configuration) =
  inConfig(config)(
    Seq(
      scalastyleFailOnError := true
      //(compile in config) := ((compile in config) dependsOn (scalastyle in config).toTask("")).value
    )
  )

lazy val defaultScalaStyleSettings = scalaStyleSettings(Compile) ++ scalaStyleSettings(Test)

lazy val colog = (project in file("."))
  .settings(globalSettings)
  .settings(
    skip in publish := true
  )
  .aggregate(coreJS, coreJVM, standaloneJS, standaloneJVM, slf4j, examples, docs)

lazy val docs = (project in file("website"))
  .enablePlugins(WebsitePlugin)
  .settings(globalSettings)
  .settings(
    skip in publish := true,
    coverageEnabled := false,
    moduleName := "colog-docs",
    docusaurusProjectName := "scala-colog",
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(coreJVM, standaloneJVM, slf4j),
    fork in (ScalaUnidoc, unidoc) := true,
    scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
      "-Xfatal-warnings",
      "-doc-source-url",
      scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
      "-sourcepath",
      baseDirectory.in(LocalRootProject).value.getAbsolutePath,
      "-diagrams"
    ),
    wartremoverErrors := {
      val disabledWarts = Set(Wart.NonUnitStatements)
      wartremoverErrors.value.filterNot(disabledWarts)
    }
  )
  .dependsOn(standaloneJVM, slf4j)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/core"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(globalSettings)
  .settings(defaultScalaStyleSettings)
  .settings(
    moduleName := "colog-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core"            % Versions.cats.main,
      "org.typelevel" %%% "cats-mtl-core"        % Versions.cats.mtl,
      "org.typelevel" %%% "cats-laws"            % Versions.cats.main % Test,
      "org.typelevel" %%% "cats-effect"          % Versions.cats.effect,
      "org.typelevel" %%% "cats-effect-laws"     % Versions.cats.effect % Test,
      "org.typelevel" %%% "cats-testkit"         % Versions.cats.main % Test,
      "org.typelevel" %%% "discipline-scalatest" % Versions.discipline % Test
    )
  )
  .jsSettings(commonJsSettings)

lazy val coreJS  = core.js
lazy val coreJVM = core.jvm

lazy val standalone = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/standalone"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(globalSettings)
  .settings(defaultScalaStyleSettings)
  .settings(
    moduleName := "colog-standalone",
    initialCommands in console += Seq(
      "import colog.standalone._"
    ).mkString("\n", "\n", "")
  )
  .jsSettings(commonJsSettings)
  .jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % Versions.scalajs.dom % Optional
  )
  .dependsOn(core)

lazy val standaloneJS  = standalone.js
lazy val standaloneJVM = standalone.jvm

lazy val slf4j = (project in file("modules/slf4j"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(globalSettings)
  .settings(defaultScalaStyleSettings)
  .settings(
    moduleName := "colog-slf4j",
    libraryDependencies ++= Seq(
      "org.slf4j"     % "slf4j-api"  % Versions.slf4j,
      "org.scalamock" %% "scalamock" % Versions.scalamock % Test
    ),
    wartremoverErrors := {
      val disabledWarts = Set(Wart.Null)
      wartremoverErrors.value.filterNot(disabledWarts)
    }
  )
  .dependsOn(coreJVM % "compile->compile;test->test")

// === Examples

lazy val examples = (project in file("examples"))
  .settings(
    skip in publish := true,
    coverageEnabled := false
  )
  .aggregate(`example-scalajs`)

lazy val `example-scalajs` = (project in file("examples/scalajs"))
  .enablePlugins(AutomateHeaderPlugin, ScalaJSPlugin)
  .settings(globalSettings)
  .settings(commonJsSettings)
  .settings(
    moduleName := "colog-example-scalajs",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js"      %%% "scalajs-dom"     % Versions.scalajs.dom,
      "io.github.cquiroz" %%% "scala-java-time" % Versions.scalaTime,
      "org.scalatest"     %%% "scalatest"       % Versions.scalaTest % Test
    )
  )
  .dependsOn(standaloneJS)

// Command aliases

addCommandAlias(
  "validateModules",
  Seq(
    "clean",
    "coverage",
    "test",
    "coverageReport",
    "coverageAggregate"
  ).mkString(";")
)

addCommandAlias(
  "validateStyle",
  Seq(
    "scalafmtCheck",
    "scalafmtSbtCheck",
    "scalastyle"
  ).mkString(";")
)

addCommandAlias(
  "validateDocs",
  Seq(
    "docs/clean",
    "docs/docusaurusCreateSite"
  ).mkString(";")
)

addCommandAlias(
  "validate",
  Seq(
    "validateModules",
    "validateStyle",
    "validateDocs"
  ).mkString(";")
)

addCommandAlias(
  "fmt",
  Seq(
    "scalafmt",
    "scalafmtSbt"
  ).mkString(";")
)

addCommandAlias(
  "chkfmt",
  Seq(
    "scalafmtCheck",
    "scalafmtSbtCheck"
  ).mkString(";")
)
