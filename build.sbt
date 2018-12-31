scalaVersion := "2.12.8"

val catsVersion = "1.5.0"

addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.8" cross CrossVersion.binary)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core"     % catsVersion,
  "org.typelevel" %% "cats-mtl-core" % "0.4.0",
  "org.typelevel" %% "cats-effect"   % "1.1.0"
)
