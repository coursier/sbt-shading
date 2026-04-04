
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

name := "sbt-shading"

enablePlugins(ScriptedPlugin)

def scala212 = "2.12.20"
def targetSbtVersion = "1.3.10"

crossScalaVersions := Seq(scala212, "3.8.4")
scalaVersion := scala212
scalacOptions ++= Seq(
  "-feature",
  "-deprecation"
)

// https://github.com/sbt/sbt/issues/5049#issuecomment-528960415
dependencyOverrides := {
  scalaBinaryVersion.value match {
    case "2.12" =>
      "org.scala-sbt" % "sbt" % targetSbtVersion :: Nil
    case _ =>
      Nil
  }
}
scriptedLaunchOpts ++= Seq(
  "-Xmx1024M",
  "-Dplugin.version=" + version.value,
  "-Dsbttest.base=" + (sourceDirectory.value / "sbt-test").getAbsolutePath
)
scriptedBufferLog := false
sbtPlugin := true
(pluginCrossBuild / sbtVersion) := {
  scalaBinaryVersion.value match {
    case "3" =>
      "2.0.0"
    case _ =>
      targetSbtVersion
  }
}

scriptedSbt := {
  scalaBinaryVersion.value match {
    case "3" =>
      (pluginCrossBuild / sbtVersion).value
    case _ =>
      "1.9.9"
  }
}

TaskKey[Unit]("scriptedTestSbt2") := Def.taskDyn {
  val values = sbtTestDirectory.value
    .listFiles(_.isDirectory)
    .flatMap { dir1 =>
      dir1.listFiles(_.isDirectory).map { dir2 =>
        dir1.getName -> dir2.getName
      }
    }
    .toList
  val log = streams.value.log
  val exclude: Set[(String, String)] = Set(
    "cross-project-shading" // TODO scala-js and sbt-scalajs-crossprojct plugin
  ).map("sbt-shading" -> _)
  val args = values.filterNot(exclude).map { case (x1, x2) => s"${x1}/${x2}" }
  val arg = args.mkString(" ", " ", "")
  log.info("scripted" + arg)
  scripted.toTask(arg)
}.value

libraryDependencies += "com.eed3si9n.jarjarabrams" %% "jarjar-abrams-core" % "1.16.0"

