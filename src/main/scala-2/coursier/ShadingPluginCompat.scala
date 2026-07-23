package coursier

import java.io.File

private[coursier] trait ShadingPluginCompat { self : ShadingPlugin.type =>
  def toVirtualFile(file: File): File = file

  def toFile(file: File): File = file

  type FileRef = File
}
