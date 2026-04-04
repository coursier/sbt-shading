package coursier

import java.io.File
import sbt.Keys.*
import xsbti.VirtualFileRef

private[coursier] trait ShadingPluginCompat { self : ShadingPlugin.type =>
  inline def toVirtualFile(file: File) =
    fileConverter.value.toVirtualFile(file.toPath)

  inline def toFile(file: VirtualFileRef): File =
    fileConverter.value.toPath(file).toFile

  type FileRef = sbt.HashedVirtualFileRef
}
