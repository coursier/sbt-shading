{
  val pluginVersion = sys.props.getOrElse(
    "plugin.version",
    throw new RuntimeException(
      """|The system property 'plugin.version' is not defined.
         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
    )
  )
  
  addSbtPlugin("io.get-coursier" % "sbt-shading" % pluginVersion)
}

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.12.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
