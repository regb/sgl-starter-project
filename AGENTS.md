# AGENTS.md

This is a game written with [SGL](https://libsgl.com), the Scala Game Library,
using its **GameObject engine** in 2D. It builds with Bazel and runs on the
desktop (JVM), in the browser (Scala.js + WebGL), and as a native executable
(Scala Native + SDL2). It started from the `regb/sgl-starter-project` template:
a small game, Coin Dash, that you are expected to replace with the user's game.

SGL is pre-0.1 and changes fast. Do not rely on what you remember about SGL:
read the docs and the pinned SGL source (see below), and copy patterns from its
examples.

## Commands

```bash
bazel build //...            # build everything except //:native (see below)
bazel test //...             # run the headless game test
bazel run //:desktop         # play in a desktop window (needs a display)
bazel run //:web             # serve the browser build on http://localhost:8080
bazel build //:native        # Scala Native executable; `bazel run //:native` to play
```

- The first build downloads Bazel, a JDK, Scala, and SGL, and takes a few
  minutes. Later builds are incremental.
- `//:native` is tagged `manual` by SGL, so `//...` skips it: build it by name.
  It needs clang, SDL2, SDL2_image, and OpenGL ES/EGL development libraries
  (Ubuntu: `sudo apt-get install clang libegl1-mesa-dev libgl1-mesa-dri libglu1-mesa-dev libsdl2-dev libsdl2-image-dev`).
- `bazel run //:web` keeps serving until stopped. Run it in the background if
  you need to check it, for example `curl -s localhost:8080/`.

## Verify your changes

Always run `bazel build //...` and `bazel test //...` after a change. You
usually cannot see a window, so the test is how you check the game:

- `test/GameTest.scala` starts the real game on SGL's **headless OpenGL
  backend** (Mesa software rendering: no display, no GPU), presses keys with
  `app.Controller.keyDown/keyUp`, advances frames with `app.step`, and reads
  the game state through a probe system it adds in `configure`.
- It also captures a frame. Under `bazel test`, the PNG is written to
  `bazel-testlogs/game_test/test.outputs/first-frame.png` (sometimes zipped
  as `outputs.zip` in that directory). Look at it to check what the game draws.
- When you change the game, update the test: keep the probe in sync with your
  components, and add a test for each mechanic you add (move, collect, lose,
  win). The headless backend needs Linux with Mesa (`libegl1`, `libgl1-mesa-dri`).
- On failure, the log is in `bazel-testlogs/game_test/test.log`.

## Files

| Path | What it is |
| --- | --- |
| `game/Game.scala` | The whole game: the `Game` trait, components, systems, the scene |
| `test/GameTest.scala` | Headless test of the game |
| `BUILD.bazel` | Targets: `:core` (the game library), `:desktop`, `:web`, `:native`, `:game_test` |
| `MODULE.bazel` | Bazel dependencies; pins the SGL commit with `git_override` |
| `.bazelrc` | Build flags (prebuilt protoc, Scala compiler worker) |
| `.github/workflows/ci.yml` | CI: build everything, build native, run the test |

New Scala files in `game/` and `test/` are picked up by the globs in
`BUILD.bazel`. The Scala package is `mygame`; if you rename it, also change
`core_abstract_class`, `package`, and `extra_mixins` in `BUILD.bazel`.

## How the game is structured (GameObject engine)

- `trait Game extends OpenGLGameObjectApp` configures the game in
  `configure(builder)`: installed modules, globals, systems, initial scene.
  Each target in `BUILD.bazel` mixes this trait with a platform backend.
- **Objects** are built with `context.spawn2D(name, Transform2D(...))` and a
  chain of `.attach(new Component(_, ...))`. Each `attach` grows the object's
  type.
- A **component** extends `ComponentOf(obj)` and declares the components it
  needs in the type of its parameter, e.g.
  `final class Coin(obj: TypedObject[Spatial2D & Collider])`, then reads them
  with `obj.get[Collider]`. Components with per-frame logic mix in
  `Updatable` (requires `UpdateCapabilitiesModule`).
- **Systems** (`GameSystem`, added with `builder.addSystem(Stage.X, ...)`) run
  once per frame over `context.world.index[SomeComponent]`.
- **Scenes** (`Scene`, or `AsyncScene[A]` when assets must load first) spawn
  the objects; loading another scene with `context.runtime.loadScene(...)`
  destroys the previous scene's objects.
- **Globals** hold one value per class: `builder.setGlobal(x)` for the whole
  game, `context.setGlobal(x)` in a scene for scene state, read with
  `context.global[X]`.
- World coordinates: the camera is centered on (0, 0), the view is 800x600
  world units, and **y grows downwards**.

## Rules that matter

- **The compiler checks object composition.** Errors such as *"This object is
  not known to have a Collider"* or *"This object already has a Collider"*
  mean the `attach` chain is wrong (wrong order, missing component, or
  duplicate). Fix the chain or the component's parameter type. Do not cast
  (`asInstanceOf`) or use `require`/`component` lookups to get around it.
- Every module a component needs must be installed in `configure`:
  `Updatable` needs `UpdateCapabilitiesModule`, `SpriteRenderer` needs
  `Render2DModule`, `TextOverlay` needs `TextOverlayModule`. A missing one
  fails at runtime with a `CompositionError` (the test catches it).
- `World` methods take a `(using Scope)`: a helper that spawns objects
  declares `(using Scope)` in its signature.
- **Never load assets in a plain `val`** of the game trait: the generated
  `Assets` is not initialized yet when the trait's fields are. In the
  GameObject engine, load in a scene (`AsyncScene.preload`, or
  `context.whenLoaded`), in `configure`, or in a `lazy val`.
- The `TextOverlay` font has only `A-Z`, `0-9`, space, and `- _ : . [ ] /`.
  Anything else, lowercase included, is drawn as a `?`: write overlay text in
  uppercase.
- Use SGL's APIs (`Input`, `Random` from `DefaultRandomProvider`, the
  `Loader`s) rather than JVM-only libraries: the same code must compile for
  Scala.js and Scala Native.

## Adding images

This recipe was tested with this template. Put PNGs in
`assets/drawable-xhdpi/`, then in `BUILD.bazel`:

```python
load("@sgl//bazel/sgl:assets.bzl", "sgl_drawable_assets")

sgl_drawable_assets(
    name = "drawables",
    srcs = glob(["assets/drawable-xhdpi/*.png"]),
    source_density = "xhdpi",
    strip_prefix = "assets/drawable-xhdpi",
    target_densities = ["mdpi", "xhdpi"],
)
```

- `:core`: add `assets = [":drawables"]` and `package = "mygame"`.
- `:desktop`: add `data = [":drawables"]` and `extra_mixins = ["mygame.GeneratedAssetsProvider"]`.
- `:web` and `:native`: add `assets = [":drawables"]`, `assets_strip_prefix = "assets"`,
  and the same `extra_mixins`.
- The test app: mix in `GeneratedAssetsProvider` (`... with Game with GeneratedAssetsProvider with DesktopSystemProvider ...`).
- `trait Game extends OpenGLGameObjectApp with AssetsProvider with DefaultRandomProvider`.

`assets/drawable-xhdpi/coin.png` becomes `Assets.drawable.coin`. `Assets` is a
member of the game trait, so a scene defined outside the trait gets it through
a global set in `configure`, or the scene is defined inside the trait (as in
SGL's `solar-system` example). Load the texture before the scene appears:

```scala
final case class CoinSprite(asset: sgl.assets.DrawableAsset)
// in configure:  .setGlobal(CoinSprite(Assets.drawable.coin))

object PlayScene extends AsyncScene[Texture2D] {
  override def preload(context: GameContext)(using Scope): sgl.util.Loader[Texture2D] =
    context.engine.loadDrawableTexture(context.global[CoinSprite].asset)

  override def load(context: GameContext, coin: Texture2D)(using Scope): Unit = {
    // ... .attach(new SpriteRenderer(_, SpriteVisual.Texture(coin), 20f, 20f))
  }
}
```

With an `AsyncScene`, the first frames run before the scene exists; the test's
`runFrames` already waits for the player to appear. Sounds
(`sgl_audio_assets`, `Audio.loadSound`) follow the same pattern; see the
Assets and Audio docs.

## Documentation and SGL source

- Docs: https://libsgl.com/docs/ , especially the GameObject engine page
  https://libsgl.com/docs/gameobject/ , plus Assets, Input, Testing, and
  Bazel rules.
- The pinned SGL source, which is the ground truth for the API, is fetched by
  Bazel into:

  ```bash
  $(bazel info output_base)/external/sgl.bzl+/
  ```

  (run a build first so it exists). Useful places in it:
  - `engines/gameobject/src/main/scala/sgl/engine/gameobject/`: the engine API.
  - `engines/gameobject/examples/asteroids/App.scala`: a complete 2D game
    (bullets, collisions, score, restart).
  - `engines/gameobject/examples/solar-system/`: assets, `AsyncScene`, 3D, audio.
  - `engines/gameobject/examples/headless-control/`: more headless tests.
  - `bazel/sgl/*.bzl`: the Bazel rules and all their attributes.
  - `website/src/content/docs/docs/*.mdx`: the docs, offline.
  - `core/src/main/scala/sgl/`: core APIs (`Input`, `Graphics`, `Audio`, ...).

## Upgrading SGL

Change `commit` in the `git_override` in `MODULE.bazel` to a newer commit of
https://github.com/regb/scala-game-library, then build and test. Keep
`scala_version` in `MODULE.bazel` equal to the one in SGL's `MODULE.bazel`, and
`.bazelversion` equal to SGL's. Expect API changes: compare with the updated
examples.
