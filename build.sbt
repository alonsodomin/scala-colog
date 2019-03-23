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
    "utf-8", // Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds", // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    "-Xfuture", // Turn on future language features.
    "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
    "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
    "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
    "-Xlint:option-implicit", // Option.apply used implicit view.
    "-Xlint:package-object-classes", // Class or object defined in package object.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match", // Pattern match may not be typesafe.
    "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification", // Enable partial unification in type constructor inference
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen", // Warn when numerics are widened.
    "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals", // Warn if a local definition is unused.
    "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates", // Warn if a private member is unused.
    "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
  ),
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
  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.9" cross CrossVersion.binary),
  wartremoverErrors ++= {
    val disabledWarts = Set(Wart.DefaultArguments, Wart.Any)
    Warts.unsafe.filterNot(disabledWarts)
  },
  coverageEnabled := true,
)

def scalaStyleSettings(config: Configuration) =
  inConfig(config)(
    Seq(
      scalastyleFailOnError := true,
      //(compile in config) := ((compile in config) dependsOn (scalastyle in config).toTask("")).value
    )
  )

lazy val defaultScalaStyleSettings = scalaStyleSettings(Compile) ++ scalaStyleSettings(Test)

lazy val colog = (project in file("."))
  .settings(globalSettings)
  .settings(
    skip in publish := true,
  )
  .aggregate(coreJS, coreJVM, standaloneJS, standaloneJVM, slf4j, examples, docs)

lazy val docs = (project in file("website"))
  .enablePlugins(WebsitePlugin)
  .settings(globalSettings)
  .settings(
    skip in publish := true,
    moduleName := "colog-docs",
    docusaurusProjectName := "scala-colog",
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    autoAPIMappings := true,
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
    },
  ).dependsOn(standaloneJVM, slf4j)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(globalSettings)
  .settings(defaultScalaStyleSettings)
  .settings(
    moduleName := "colog-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core"        % Versions.cats.main,
      "org.typelevel" %%% "cats-mtl-core"    % Versions.cats.mtl,
      "org.typelevel" %%% "cats-effect"      % Versions.cats.effect,
      "org.typelevel" %%% "cats-effect-laws" % Versions.cats.effect % Test,
      "org.typelevel" %%% "cats-testkit"     % Versions.cats.main % Test,
    ),
  )

lazy val coreJS  = core.js
lazy val coreJVM = core.jvm

lazy val standalone = crossProject(JSPlatform, JVMPlatform)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(globalSettings)
  .settings(defaultScalaStyleSettings)
  .settings(
    moduleName := "colog-standalone",
    initialCommands in console += Seq(
      "import colog.standalone._"
    ).mkString("\n", "\n", "")
  )
  .jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.6" % Optional
  )
  .dependsOn(core)

lazy val standaloneJS  = standalone.js
lazy val standaloneJVM = standalone.jvm

lazy val slf4j = project
  .enablePlugins(AutomateHeaderPlugin)
  .settings(globalSettings)
  .settings(defaultScalaStyleSettings)
  .settings(
    moduleName := "colog-slf4j",
    libraryDependencies ++= Seq(
      "org.slf4j"     % "slf4j-api"  % Versions.slf4j,
      "org.scalamock" %% "scalamock" % Versions.scalamock % Test,
    ),
    wartremoverErrors := {
      val disabledWarts = Set(Wart.Null)
      wartremoverErrors.value.filterNot(disabledWarts)
    },
  )
  .dependsOn(coreJVM % "compile->compile;test->test")

// === Examples

lazy val examples = (project in file("examples"))
  .settings(
    skip in publish := true
  )
  .aggregate(`example-scalajs`)

lazy val `example-scalajs` = (project in file("examples/scalajs"))
  .enablePlugins(AutomateHeaderPlugin, ScalaJSPlugin)
  .settings(globalSettings)
  .settings(
    moduleName := "colog-example-scalajs",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js"      %%% "scalajs-dom"     % "0.9.6",
      "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-RC1"
    )
  )
  .dependsOn(standaloneJS)

// Command aliases

addCommandAlias(
  "verify",
  Seq(
    "clean",
    "test",
    "scalafmtCheck",
    "scalafmtSbtCheck"
  ).mkString(";", ";", "")
)

addCommandAlias(
  "validateDocs",
  Seq(
    "docs/clean",
    "docs/generateWebsite"
  ).mkString(";", ";", "")
)