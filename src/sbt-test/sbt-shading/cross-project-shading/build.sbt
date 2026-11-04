import PluginTestCompat._

lazy val root = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .enablePlugins(ShadingPlugin)
  .settings(
    shadingRules += ShadingRule.moveUnder("argonaut", "foo.shaded"),
    validNamespaces += "foo",
    shadedDependencies += "io.argonaut" %%% "argonaut" % "foo",
    libraryDependencies += "io.argonaut" %%% "argonaut" % "6.2.5",
    scalaJSUseMainModuleInitializer := true,
    scalaVersion := "2.12.21",
    organization := "com.github.sbt.test",
    name := "shading-cross-test",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
  )

lazy val jvm = root.jvm
lazy val js = root.js
