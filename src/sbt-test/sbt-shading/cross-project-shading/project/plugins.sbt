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

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.22.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.4.0")
