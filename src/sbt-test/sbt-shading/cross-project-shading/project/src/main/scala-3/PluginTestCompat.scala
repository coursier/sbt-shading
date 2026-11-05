import sbt.*

object PluginTestCompat {
  extension (organization: String) {
    def %%%(name: String) = organization %% name
  }
}
