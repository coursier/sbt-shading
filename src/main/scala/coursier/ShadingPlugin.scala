package coursier

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipFile
import com.eed3si9n.jarjar.Rule
import com.eed3si9n.jarjar.misplaced.MisplacedClassProcessorFactory
import com.eed3si9n.jarjar.util.CoursierJarProcessor
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName
import sbt.librarymanagement.ScalaModuleInfo

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.xml.{Comment, Elem, Node => XmlNode}
import scala.xml.transform.{RewriteRule, RuleTransformer}

object ShadingPlugin extends AutoPlugin with ShadingPluginCompat {

  override def trigger = noTrigger
  override def requires = JvmPlugin

  object Rule {
    import com.eed3si9n.jarjar.Rule

    def moveUnder(from: String, to: String): Rule =
      rename(s"$from.**", s"$to.$from.@1")

    def rename(from: String, to: String): Rule = {
      val rule = new Rule
      rule.setPattern(from)
      rule.setResult(to)
      rule
    }
  }

  private def updateIvyXml(file: File, removeDeps: Set[(String, String)]): Unit = {
    val content = scala.xml.XML.loadFile(file)

    val updatedContent = content.copy(child = content.child.map {
      case elem: Elem if elem.label == "dependencies" =>
        elem.copy(child = elem.child.map {
          case elem: Elem if elem.label == "dependency" =>
            (elem.attributes.get("org"), elem.attributes.get("name")) match {
              case (Some(Seq(orgNode)), Some(Seq(nameNode))) =>
                val org = orgNode.text
                val name = nameNode.text
                val remove = removeDeps.contains((org, name))
                if (remove)
                  Comment(
                    s""" shaded dependency $org:$name
                       | $elem
                       |""".stripMargin
                  )
                else
                  elem
              case _ => elem
            }
          case n => n
        })
      case n => n
    })

    val printer = new scala.xml.PrettyPrinter(Int.MaxValue, 2)
    val updatedFileContent = """<?xml version="1.0" encoding="UTF-8"?>""" + '\n' +
      "<!-- hello -->\n" +
      printer.format(updatedContent)
    Files.write(file.toPath, updatedFileContent.getBytes(StandardCharsets.UTF_8))
  }

  object autoImport {
    // to be set by users
    val shadedModules = settingKey[Set[OrganizationArtifactName]]("")
    val shadedDependencies = settingKey[Set[ModuleID]]("")
    @transient
    val shadingRules = taskKey[Seq[Rule]]("")
    val validNamespaces = taskKey[Set[String]]("")
    @transient
    val validEntries = taskKey[Set[String]]("")

    // to be set optionally by users
    val shadingVerbose = taskKey[Boolean]("")

    // set by ShadingPlugin
    @transient
    val shadedJars = taskKey[Seq[File]]("")
    @transient
    val shadedPackageBin = taskKey[FileRef]("")

    val ShadingRule = Rule

    implicit class ModuleIDShadingOps(private val moduleId: ModuleID) extends AnyVal {
      def module: OrganizationArtifactName =
        if (moduleId.crossVersion == CrossVersion.disabled)
          moduleId.organization % moduleId.name
        else if (moduleId.crossVersion == CrossVersion.binary)
          moduleId.organization %% moduleId.name
        else
          sys.error(s"Don't know how to build a module for cross version ${moduleId.crossVersion}")
    }
  }

  import autoImport._

  private def orgName(modId: ModuleID, scalaModuleInfoOpt: Option[ScalaModuleInfo]): (String, String) = {
    (modId.organization, CrossVersion(modId, scalaModuleInfoOpt).fold(modId.name)(_(modId.name)))
  }

  private def onlyNamespaces(isValid: String => Boolean, jar: File, println: String => Unit): Unit = {
    val zf = new ZipFile(jar)
    val unrecognized = zf.entries()
      .asScala
      .map(_.getName)
      .filter(!isValid(_))
      .toVector
      .sorted
    for (u <- unrecognized)
      println(s"Unrecognized: $u")
    assert(unrecognized.isEmpty)
  }

  private lazy val shadedOrgNames = Def.setting[Set[(String, String)]] {
    val scalaModuleInfoOpt = scalaModuleInfo.value
    val allShadedDeps = shadedDependencies.value.iterator ++
      shadedModules.value.iterator.map(_ % "foo")
    allShadedDeps.map(orgName(_, scalaModuleInfoOpt)).toSet
  }

  /**
   *  - sbt 2.1 publishes without Ivy, and writes the local ivy.xml from the same project as
   *    dependency resolution, so rewrite the file it wrote, and its checksums.
   *  - sbt 1 and 2.0 publish with Ivy and need the shaded ivy.xml republished, but a build
   *    without Ivy cannot even load a class that refers to it.
   */
  private def ivyXmlSettings =
    try IvyRepublish.settings
    catch {
      case _: NoClassDefFoundError =>
        Seq(publishLocal := rewriteLocalIvyXml.dependsOn(publishLocal).value)
    }

  private val rewriteLocalIvyXml = Def.task {
    val shaded = shadedOrgNames.value
    val (org, name) = orgName(projectID.value, scalaModuleInfo.value)
    val revision = version.value
    def prop(name: String) = sys.props.get(name).map(_.trim).filter(_.nonEmpty)
    // where sbt 2.1 looks for the Ivy home when `ivyPaths` does not set one
    val home = ivyHome.value.orElse(prop("sbt.ivy.home").orElse(prop("ivy.home")).map(new File(_)))
      .getOrElse(new File(sys.props("user.home"), ".ivy2"))
    for {
      ivyXml <- (home / "local" / org / name ** "ivy.xml").get()
      if ivyXml.getParentFile.getParentFile.getName == revision
    } {
      updateIvyXml(ivyXml, shaded)
      def update(ext: String, algorithm: String): Unit = {
        val checksum = new File(s"$ivyXml.$ext")
        if (checksum.exists) {
          val digest = MessageDigest.getInstance(algorithm).digest(IO.readBytes(ivyXml))
          IO.write(checksum, digest.map("%02x".format(_)).mkString)
        }
      }
      update("md5", "MD5")
      update("sha1", "SHA-1")
    }
  }

  override lazy val projectSettings = Def.settings(

    shadedModules := Set.empty,
    shadedDependencies := Set.empty,
    shadingRules := Nil,
    validNamespaces := Set.empty,
    validEntries := Set.empty,

    shadingVerbose := false,

    shadedJars := {
      val scalaModuleInfoOpt = scalaModuleInfo.value
      val shadedModules0 = shadedOrgNames.value

      val thisOrgName = orgName(projectID.value, scalaModuleInfoOpt)
      val classpathTypes = Keys.classpathTypes.value

      val updateReport = updateFull.value

      val report = updateReport
        .configurations
        .find(_.configuration.name == Compile.name)
        .getOrElse {
          sys.error(s"Configuration ${Compile.name} not found in update report (found configs: ${updateReport.configurations.map(_.configuration.name)})")
        }
      val moduleReports = report
        .modules
        // .filter(!_.evicted)
      val dependencies = for {
        modRep <- moduleReports
        caller <- modRep.callers
      } yield orgName(caller.caller, scalaModuleInfoOpt) -> orgName(modRep.module, scalaModuleInfoOpt)
      val dependencyMap = dependencies
        .groupBy(_._1)
        .mapValues(_.map(_._2))
        .toMap
      val roots = dependencyMap.getOrElse(thisOrgName, Nil)
      val nonShadedRoots = roots.filterNot(shadedModules0)

      @tailrec
      def nonShaded(newMods: Set[(String, String)], acc: Set[(String, String)]): Set[(String, String)] =
        if (newMods.isEmpty) acc
        else {
          val newMods0 = newMods
            .flatMap(dependencyMap.getOrElse(_, Nil))
            .filter(!newMods.contains(_))
            .filter(!acc.contains(_))
          nonShaded(newMods0, acc ++ newMods)
        }

      val nonShaded0 = nonShaded(nonShadedRoots.toSet, Set.empty)
      val shaded = dependencies.map(_._2).toSet.filterNot(nonShaded0)

      for {
        modRep <- moduleReports
        orgName0 = orgName(modRep.module, scalaModuleInfoOpt)
        if shaded(orgName0)
        (a, f) <- modRep.artifacts
        if classpathTypes.contains(a.`type`)
      } yield f
    },

    shadedPackageBin := {
      val log = streams.value.log
      val verbose = shadingVerbose.value
      val rules = shadingRules.value
      val shadedJars0 = shadedJars.value
      val validPrefixes = validNamespaces.value.map(_.replace('.', '/') + "/")
      val validEntries = autoImport.validEntries.value
      val orig = toFile((Compile / packageBin).value)
      val dest = orig.getParentFile / s"${orig.getName.stripSuffix(".jar")}-shading.jar"
      if (!dest.exists() || dest.lastModified() < orig.lastModified()) {
        import com.eed3si9n.jarjar.JJProcessor
        val processor = new JJProcessor(rules, verbose, false, MisplacedClassProcessorFactory.Strategy.FATAL.toString)
        CoursierJarProcessor.run((orig +: shadedJars0).toArray, dest, processor, true)
      }
      def isValid(entry: String): Boolean = 
        validEntries.contains(entry) || 
          entry.startsWith("META-INF/") || 
          validPrefixes.exists(entry.startsWith)
      onlyNamespaces(isValid, dest, log.error(_))
      toVirtualFile(dest)
    },

    addArtifact(Compile / packageBin / artifact, shadedPackageBin),

    ivyXmlSettings,

    deliverLocal := {
      val shadedModules0 = shadedOrgNames.value

      val file = deliverLocal.value
      updateIvyXml(file, shadedModules0)
      file
    },

    pomPostProcess := {
      val previous = pomPostProcess.value

      val shadedModules0 = shadedOrgNames.value

      // Originally based on https://github.com/olafurpg/coursier-small/blob/408528d10cea1694c536f55ba1b023e55af3e0b2/build.sbt#L44-L56
      val transformer = new RuleTransformer(new RewriteRule {
        override def transform(node: XmlNode) = node match {
          case _: Elem if node.label == "dependency" =>
            val org = node.child.find(_.label == "groupId").fold("")(_.text.trim)
            val name = node.child.find(_.label == "artifactId").fold("")(_.text.trim)
            val isShaded = shadedModules0.contains((org, name))
            if (isShaded)
              Comment(
                s""" shaded dependency $org:$name
                   | $node
                   |""".stripMargin
              )
            else
              node
          case _ => node
        }
      })

      node =>
        val node0 = previous(node)
        transformer.transform(node0).head
    }
  )

}
