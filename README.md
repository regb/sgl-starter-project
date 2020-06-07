# Starter Project for SGL

This is a sample project for getting started with [the Scala Game
Library (SGL)](https://github.com/regb/scala-game-library), a cross-platform
game engine for the Scala programming language.

## How to run

To use it, you will need SBT, git, a working Scala Native setup (install
required software [from the official
documentation](https://scala-native.readthedocs.io/en/v0.3.9-docs/user/setup.html#installing-clang-and-runtime-dependencies).

Then you need to compile and publish SGL locally:

    git clone git@github.com:regb/scala-game-library.git
    cd scala-game-library
    sbt publishLocal

Once SGL is published locally, you should be able to play with the starter project.
Go to the root directory (of this repository, not the scala-game-library). And
run:

    sbt desktop/run

This will start the sample app running on the JVM. You can run it natively with:

    sbt native/run

And you can compile it to Javascript and run it in the browser with:

    sbt html5/fastOptJS

All you need to run it is a `<canvas id="scalavator_canvas">` tag and a script
tag loading the compiled game. Here's [an example](html5/index.html).

## Where to go from there

You can play around with the game code in
[ScalavatorGame.scala](core/src/main/scala/ScalavatorGame.scala) using the
ScalaDoc of SGL to figure out the API (it should have been published locally as
well). Just edit the code, and run `sbt desktop/run` to see the result.

You can take a look at the [Scalavator
game](https://github.com/regb/scalavator) which is one possible evolution from
this starter project into a complete game with graphics, that runs on multiple
platforms.
