package foo

import argonaut._

import Foo._

object Main extends App {
  val expectedClassName0 = expectedClassName(args.headOption == Some("--shaded"))

  if (className != expectedClassName0)
    sys.error(s"Expected JS class name $expectedClassName0, got $className")

  Console.err.println(s"Expected JS class name: $expectedClassName0, got the same")
}
