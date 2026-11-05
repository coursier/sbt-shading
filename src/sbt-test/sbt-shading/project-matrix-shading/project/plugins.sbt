{
  val pluginVersion = sys.props.getOrElse(
    "plugin.version",
    throw new RuntimeException(
      """|The system property 'plugin.version' is not defined.
         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
    )
  )

  addSbtPlugin("com.github.sbt" % "sbt-shading" % pluginVersion)
}

// projectMatrix is in sbt 2's core. Scala.js 1.21.0 has no sbt 2 build, and 1.22.0 is the first
// that does, while the sbt 1 job runs on Java 8, where the other tests already pin 1.21.0.
libraryDependencies ++= {
  val sbtV = (pluginCrossBuild / sbtBinaryVersion).value
  val scalaV = (update / scalaBinaryVersion).value
  def plugin(org: String, name: String, rev: String) =
    Defaults.sbtPluginExtra(org % name % rev, sbtV, scalaV)
  if (sbtV.startsWith("1"))
    Seq(
      plugin("com.eed3si9n", "sbt-projectmatrix", "0.11.0"),
      plugin("org.scala-js", "sbt-scalajs", "1.21.0")
    )
  else Seq(plugin("org.scala-js", "sbt-scalajs", "1.22.0"))
}
