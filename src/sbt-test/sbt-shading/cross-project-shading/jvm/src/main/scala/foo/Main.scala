package foo

import java.io.File
import java.nio.file.Files

import argonaut._

import Foo._

object Main extends App {
  val expectedClassName0 = expectedClassName(args.headOption == Some("--shaded"))

  if (className != expectedClassName0)
    sys.error(s"Expected JVM class name $expectedClassName0, got $className")

  Console.err.println(s"Expected JVM class name: $expectedClassName0, got the same")

  val msg = Json.obj().nospaces

  Files.write(new File("output").toPath, msg.getBytes("UTF-8"))
}
