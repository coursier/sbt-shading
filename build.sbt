
inThisBuild(List(
  organization := "io.get-coursier",
  homepage := Some(url("https://github.com/coursier/sbt-coursier")),
  licenses := Seq("Apache 2.0" -> url("http://opensource.org/licenses/Apache-2.0")),
  developers := List(
    Developer(
      "alexarchambault",
      "Alexandre Archambault",
      "",
      url("https://github.com/alexarchambault")
    )
  )
))

enablePlugins(ScriptedPlugin)

def scala212 = "2.12.17"
def targetSbtVersion = "1.3.10"

crossScalaVersions := Seq(scala212)
scalaVersion := scala212
scalacOptions ++= Seq(
  "-feature",
  "-deprecation"
)

// https://github.com/sbt/sbt/issues/5049#issuecomment-528960415
dependencyOverrides := "org.scala-sbt" % "sbt" % targetSbtVersion :: Nil
scriptedLaunchOpts ++= Seq(
  "-Xmx1024M",
  "-Dplugin.version=" + version.value,
  "-Dsbttest.base=" + (sourceDirectory.value / "sbt-test").getAbsolutePath
)
scriptedBufferLog := false
sbtPlugin := true
sbtVersion.in(pluginCrossBuild) := targetSbtVersion

libraryDependencies += "com.eed3si9n.jarjarabrams" %% "jarjar-abrams-core" % "1.8.1"

