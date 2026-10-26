package coursier

import java.io.File
import sbt._
import sbt.Keys._

private[coursier] trait ShadingPluginCompat { self : ShadingPlugin.type =>
  def toVirtualFile(file: File): File = file

  def toFile(file: File): File = file

  type FileRef = File

  def ivyHome: Def.Initialize[Option[File]] = Def.setting(ivyPaths.value.ivyHome)
}
