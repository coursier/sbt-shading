package coursier

import sbt._
import sbt.Keys._

/* Ivy publishing delivers an ivy.xml of its own, so republish the shaded one that `deliverLocal`
 * wrote. Only a build that has Ivy may load this object. */
private[coursier] object IvyRepublish {
  private val reallyUpdateIvyXml = Def.task {
    val baseFile = deliverLocal.value
    val log = streams.value.log
    val resolverName = publishLocalConfiguration.value.resolverName.getOrElse(???)
    ivyModule.value.withModule(log) { case (ivy, md, _) =>
      val resolver = ivy.getSettings.getResolver(resolverName)
      val artifact = new org.apache.ivy.core.module.descriptor.MDArtifact(md, "ivy", "ivy", "xml", true)
      log.info(s"Writing ivy.xml with shading at $baseFile")
      resolver.publish(artifact, baseFile, true)
    }
  }

  val settings = Seq(publishLocal := reallyUpdateIvyXml.dependsOn(publishLocal).value)
}
