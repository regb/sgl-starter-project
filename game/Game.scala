package mygame

import sgl._
import sgl.engine.gameobject._
import sgl.engine.gameobject.extras._
import sgl.engine.gameobject.render2d._
import sgl.engine.gameobject.spatial2d._
import sgl.geometry.Vec2
import sgl.util.DefaultRandomProvider

/** Coin Dash: move the player with the arrow keys and collect the coins.
  *
  * This trait is the whole game. It only depends on SGL's portable APIs; the
  * BUILD file mixes it with a backend (desktop, web, native, or the headless
  * backend in the tests) to make a runnable application.
  */
trait Game extends OpenGLGameObjectApp with DefaultRandomProvider {

  override def configure(builder: GameBuilder): Unit = {
    builder
      .install(UpdateCapabilitiesModule) // runs every Updatable component
      .install(Render2DModule) // draws SpriteRenderer and uses the Camera2D
      .install(TextOverlayModule) // draws TextOverlay (the score)
      .setGlobal(ClearColor(0.07f, 0.08f, 0.14f, 1f))
      .setGlobal(FixedDeltaTime(1.0 / 60.0))
      .setGlobal(GameRandom(Random.fromSeed(42)))
      .addSystem(Stage.PostUpdate, CoinPickupSystem)
      .setInitialScene(PlayScene)
  }
}

/** World size, in world units. The camera is centered on (0, 0), and y grows
  * downwards, so the visible area is x in [-400, 400] and y in [-300, 300].
  */
object Arena {
  val Width = 800f
  val Height = 600f
  val CoinCount = 3
}

/** A world global: one value per class, read with `context.global[GameRandom]`. */
final case class GameRandom(random: sgl.util.RandomProvider#Random)

/** Scene state, set with `context.setGlobal` in the scene: it goes away (and
  * the score resets) when the scene is loaded again.
  */
final class Score(var points: Int)

// Components take the typed object they attach to. The type parameter lists
// the components that must already be on the object, and the compiler checks
// it at each `attach`.

/** A circle used for overlap tests. Needs a Spatial2D for its position. */
final class Collider(obj: GameObject2D, val radius: Float) extends ComponentOf(obj) {
  private val spatial = obj.spatial

  def position: Vec2 = spatial.global.position

  def overlaps(other: Collider): Boolean = {
    val d = position - other.position
    val reach = radius + other.radius
    d.x * d.x + d.y * d.y <= reach * reach
  }
}

/** Moves its object with the arrow keys and keeps it inside the arena. */
final class Player(obj: TypedObject[Spatial2D & Collider], val speed: Float) extends ComponentOf(obj) with Updatable {
  val collider: Collider = obj.get[Collider]
  private val spatial = obj.spatial

  override def update(context: GameContext)(using Scope): Unit = {
    var dx = 0f
    var dy = 0f
    if(Input.isKeyPressed(Input.Keys.Left)) dx -= 1f
    if(Input.isKeyPressed(Input.Keys.Right)) dx += 1f
    if(Input.isKeyPressed(Input.Keys.Up)) dy -= 1f
    if(Input.isKeyPressed(Input.Keys.Down)) dy += 1f

    val step = speed * context.dt
    val p = spatial.local.position + Vec2(dx * step, dy * step)
    val r = collider.radius
    val x = p.x max (r - Arena.Width / 2) min (Arena.Width / 2 - r)
    val y = p.y max (r - Arena.Height / 2) min (Arena.Height / 2 - r)
    spatial.local = spatial.local.copy(position = Vec2(x, y))
  }
}

/** Marks a collectible. Needs a Collider, so it must be attached after one. */
final class Coin(obj: TypedObject[Spatial2D & Collider]) extends ComponentOf(obj) {
  val collider: Collider = obj.get[Collider]
  private val spatial = obj.spatial

  def moveTo(position: Vec2): Unit =
    spatial.local = spatial.local.copy(position = position)
}

/** Shows the score. Reads the scene's Score global every frame. */
final class ScoreHud(obj: TypedObject[TextOverlay]) extends ComponentOf(obj) with Updatable {
  private val overlay = obj.get[TextOverlay]

  override def update(context: GameContext)(using Scope): Unit =
    overlay.lines = Vector(s"SCORE ${context.global[Score].points}")
}

/** A system runs once per frame in its stage, over all objects of the world. */
object CoinPickupSystem extends GameSystem {
  override val id: SystemId = SystemId("mygame.coin-pickup")

  override def run(context: GameContext)(using Scope): Unit = {
    val coins = context.world.index[Coin]
    context.world.index[Player].foreach { player =>
      coins.foreach { coin =>
        if(player.collider.overlaps(coin.collider)) {
          context.global[Score].points += 1
          coin.moveTo(PlayScene.randomCoinPosition(context))
        }
      }
    }
  }
}

object PlayScene extends Scene {
  override def load(context: GameContext)(using Scope): Unit = {
    context.setGlobal(new Score(0))

    context.spawn2D("Camera").attach(new Camera2D(_, Vec2(Arena.Width, Arena.Height)))

    context.spawn2D("Player", Transform2D(position = Vec2(0f, 0f)))
      .attach(new Collider(_, 18f))
      .attach(new Player(_, speed = 260f))
      .attach(new SpriteRenderer(_, SpriteVisual.Circle(Color(0.35f, 0.75f, 1f, 1f)), 36f, 36f, z = 1f))

    for(i <- 0 until Arena.CoinCount) {
      context.spawn2D(s"Coin-$i", Transform2D(position = randomCoinPosition(context)))
        .attach(new Collider(_, 10f))
        .attach(new Coin(_))
        .attach(new SpriteRenderer(_, SpriteVisual.Circle(Color(1f, 0.82f, 0.2f, 1f)), 20f, 20f))
    }

    context.spawn("ScoreHud")
      .attach(new TextOverlay(_, Vector("SCORE 0"), x = 16f, y = 16f, scale = 4f))
      .attach(new ScoreHud(_))
  }

  /** A random point in the arena, at least 60 units from the center where
    * the player starts, and clear of the edges and of the score at the top.
    */
  def randomCoinPosition(context: GameContext): Vec2 = {
    val random = context.global[GameRandom].random
    def coordinate(halfSize: Float): Float = {
      val v = 60f + random.nextFloat().toFloat * (halfSize - 60f)
      if(random.nextBoolean()) v else -v
    }
    Vec2(coordinate(Arena.Width / 2 - 40f), coordinate(Arena.Height / 2 - 100f))
  }
}
