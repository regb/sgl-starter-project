package mygame

import java.nio.file.Paths

import org.scalatest.funsuite.AnyFunSuite
import sgl.{CapturedFrame, DesktopSystemProvider, Input}
import sgl.desktop.DesktopFrameIO
import sgl.engine.gameobject._
import sgl.headless.opengl.HeadlessOpenGLApp
import sgl.math.Vec2
import sgl.util.NoLoggingProvider

/** Runs the real game on the headless OpenGL backend: no window, no GPU.
  *
  * The test drives the game like a player (key presses through
  * `app.Controller`), advances it one frame at a time with `app.step`, and
  * reads the game state through a probe system that it adds to the game.
  */
class GameTest extends AnyFunSuite {

  private val Dt = 1.0 / 60.0

  /** What the probe saw at the end of the last frame. */
  final class Snapshot(val player: Vec2, val coins: Vector[Vec2], val score: Int)

  /** The game, on the headless backend, with a probe that copies the state the
    * tests look at after every frame.
    */
  final class TestApp extends HeadlessOpenGLApp with Game with DesktopSystemProvider with NoLoggingProvider {
    override val frameDimension: (Int, Int) = (800, 600)

    @volatile var last: Snapshot = _

    override def configure(builder: GameBuilder): Unit = {
      super.configure(builder)
      builder.addSystem(Stage.PostUpdate, GameSystem("mygame.test-probe") {
        val context = summon[GameContext]
        // Nothing to record until the scene has created the player (a scene
        // that loads assets first, an AsyncScene, appears a few frames later).
        context.world.index[Player].toVector.headOption.foreach { player =>
          last = new Snapshot(
            player.collider.position,
            context.world.index[Coin].toVector.map(_.collider.position),
            context.global[Score].points
          )
        }
      })
    }

    /** Steps until the scene is ready, then `n` more frames. */
    def runFrames(n: Int): Snapshot = {
      var waited = 0
      while(last == null && waited < 300) { step(Dt); waited += 1 }
      assert(last != null, "the scene did not appear within 300 frames")
      for(_ <- 0 until n) step(Dt)
      last
    }

    def holding[A](key: Input.Keys.Key)(body: => A): A = {
      Controller.keyDown(key)
      try body
      finally Controller.keyUp(key)
    }
  }

  private def withGame(body: TestApp => Unit): Unit = {
    val app = new TestApp
    app.start()
    try body(app)
    finally app.stop()
  }

  test("the player starts in the center with a score of zero") {
    withGame { app =>
      val s = app.runFrames(1)
      assert(s.player == Vec2(0f, 0f))
      assert(s.score == 0)
      assert(s.coins.size == Arena.CoinCount)
    }
  }

  test("holding Right moves the player right, and Up moves it up") {
    withGame { app =>
      val start = app.runFrames(1).player
      // 30 frames at 260 units per second.
      val right = app.holding(Input.Keys.Right)(app.runFrames(30)).player
      assert(math.abs(right.x - start.x - 260f * 30 / 60) < 0.5f, s"moved from $start to $right")
      assert(right.y == start.y)

      val up = app.holding(Input.Keys.Up)(app.runFrames(10)).player
      assert(up.y < right.y, "y grows downwards, so Up decreases y")
    }
  }

  test("the player cannot leave the arena") {
    withGame { app =>
      val s = app.holding(Input.Keys.Left)(app.runFrames(300))
      assert(s.player.x == -Arena.Width / 2 + 18f)
    }
  }

  test("walking into a coin scores a point and moves the coin") {
    withGame { app =>
      var s = app.runFrames(1)
      val coinsBefore = s.coins
      val target = coinsBefore.head
      var frames = 0
      // Steer towards the first coin, one axis at a time, like a player would.
      while(s.score == 0 && frames < 600) {
        val d = target - s.player
        val key =
          if(math.abs(d.x) > 4f) (if(d.x > 0) Input.Keys.Right else Input.Keys.Left)
          else if(d.y > 0) Input.Keys.Down
          else Input.Keys.Up
        s = app.holding(key)(app.runFrames(1))
        frames += 1
      }
      assert(s.score == 1, s"no coin collected after $frames frames")
      assert(s.coins.size == Arena.CoinCount)
      assert(s.coins != coinsBefore, "the collected coin should have moved")
    }
  }

  test("the first frame of the scene shows the player in the center of the screen") {
    withGame { app =>
      app.runFrames(0) // wait for the scene
      val capture = app.captureNextFrame()
      app.step(Dt)
      val frame = capture.value.flatMap(_.toOption).getOrElse(fail("frame capture did not complete"))

      // Under `bazel test`, files written here are kept in
      // bazel-testlogs/game_test/test.outputs/ so you can look at them.
      sys.env.get("TEST_UNDECLARED_OUTPUTS_DIR").foreach { dir =>
        DesktopFrameIO.writePng(frame, Paths.get(dir, "first-frame.png"))
      }

      assert(frame.width == 800 && frame.height == 600)
      val (r, g, b) = pixel(frame, 400, 300)
      // The player is drawn with Color(0.35, 0.75, 1.0).
      assert(math.abs(r - 89) <= 3 && math.abs(g - 191) <= 3 && b >= 252, s"center pixel is ($r, $g, $b)")
      // The corner shows the clear color, Color(0.07, 0.08, 0.14).
      val (cr, cg, cb) = pixel(frame, 790, 590)
      assert(math.abs(cr - 18) <= 3 && math.abs(cg - 20) <= 3 && math.abs(cb - 36) <= 3, s"corner pixel is ($cr, $cg, $cb)")
    }
  }

  private def pixel(frame: CapturedFrame, x: Int, y: Int): (Int, Int, Int) = {
    val offset = (y * frame.width + x) * CapturedFrame.BytesPerPixel
    (frame.rgba(offset) & 0xff, frame.rgba(offset + 1) & 0xff, frame.rgba(offset + 2) & 0xff)
  }
}
