# SGL starter project

A template for making a game with [SGL](https://libsgl.com), the Scala Game
Library. It contains a small but complete game, **Coin Dash** (move with the
arrow keys, collect the coins), written with SGL's GameObject engine, and
targets for the desktop, the browser, and a native executable, plus a test
that runs the game without a display.

## Use this template

Click **Use this template** on GitHub, or:

```bash
gh repo create my-game --template regb/sgl-starter-project --clone
cd my-game
```

Or ask a coding agent: *"Make a game with SGL: follow https://libsgl.com/start.md"*.
Agents read [AGENTS.md](AGENTS.md).

## Run

You need [Bazelisk](https://github.com/bazelbuild/bazelisk) (installed as
`bazel`). Bazel downloads the JDK, Scala, and SGL itself; the first build takes
a few minutes.

```bash
bazel run //:desktop     # desktop window
bazel run //:web         # browser: open http://localhost:8080
bazel run //:native      # Scala Native; needs clang, SDL2, SDL2_image, OpenGL ES
bazel test //...         # headless test (Linux, Mesa)
bazel build //...        # build everything (except native)
```

## Layout

- `game/Game.scala`: the game.
- `test/GameTest.scala`: plays the game on the headless OpenGL backend.
- `BUILD.bazel`: the targets. `MODULE.bazel`: dependencies, including the SGL
  commit this project is pinned to.

## Learn more

- Documentation: https://libsgl.com/docs/
- The GameObject engine: https://libsgl.com/docs/gameobject/
- SGL source and examples: https://github.com/regb/scala-game-library

## License

MIT, see [LICENSE](LICENSE). Replace it with your own license if you like.
