# AGENTS.md

A game written with [SGL](https://libsgl.com), the Scala Game Library, and its
**GameObject engine**. It builds with Bazel for the desktop (JVM), the browser
(Scala.js), and as a native executable (Scala Native).

SGL is pre-0.1 and changes often. Do not rely on what you remember about it:
check the docs and the pinned source (see the end).

## Commands

```bash
bazel build //...      # everything except //:native
bazel test //...       # headless test of the game (Linux, Mesa)
bazel run //:desktop   # play in a window
bazel run //:web       # serve on http://localhost:8080 until stopped
bazel build //:native  # needs clang, SDL2, SDL2_image, and OpenGL ES
```

Run `bazel build //...` and `bazel test //...` after every change.

## Files

- `game/`: the game, portable Scala. `trait Game` configures it.
- `test/GameTest.scala`: plays the real game without a display. It presses keys,
  steps frames, reads the state through a probe system, and captures a frame to
  `bazel-testlogs/game_test/test.outputs/`. Look at the frame after changing what
  the game draws, and add a test for each mechanic.
- `BUILD.bazel`: the targets. `MODULE.bazel`: dependencies, with the SGL commit.

## Rules

- The compiler checks how objects are composed. *"This object is not known to
  have a Collider"* means that the `attach` chain is wrong: fix the chain or the
  component's parameter type. Do not cast.
- Install the module a component needs in `configure`. A missing one fails at
  runtime with a `CompositionError`.
- Code that spawns objects takes a `(using Scope)`.
- Do not load assets in a plain `val` of the game trait: `Assets` is not
  initialized yet. Use a scene, `configure`, or a `lazy val`.
- Use SGL's APIs, not JVM-only libraries: the game also compiles to JavaScript
  and to native code.
- `y` grows downwards.

## Documentation

- https://libsgl.com/llms.txt is the index. Start with the GameObject engine,
  Assets, and Testing pages.
- The SGL source at the pinned commit is the ground truth. After a build, it is
  in `external/sgl.bzl+/` under the folder that `bazel info output_base` prints.
  `engines/gameobject/examples/` has complete games, and `bazel/sgl/` the rules.
