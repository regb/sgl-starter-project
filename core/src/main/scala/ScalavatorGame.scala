package com.regblanc.scalavator
package core

import sgl._
import sgl.geometry._

trait ScalavatorGame {
  this: GraphicsProvider with InputProvider with WindowProvider with SystemProvider
  with GameStateComponent =>

  class MainScreen extends GameScreen {
    override val name: String = "main-screen"

    private val characterWidth = 32
    private val characterHeight = 64

    private var characterPosition =
      Point(Window.width/2 - characterWidth/2, Window.height)
    private var characterVelocity = Vec(0, 0)

    private val Gravity = Vec(0, 430)

    def handleInput(ev: Input.InputEvent): Unit = ev match {
      case Input.TouchUpEvent(_, _, _) | Input.MouseUpEvent(_, _, Input.MouseButtons.Left) =>
      	characterVelocity = Vec(0, -320)
      case _ => ()
    }
    Input.setEventProcessor(handleInput)

    override def update(dt: Long): Unit = {
      characterPosition += characterVelocity*(dt/1000f)
      characterVelocity += Gravity*(dt/1000f)
      if(characterPosition.y.toInt - Window.height > 0) {
        characterVelocity = Vec(0, 0)
      }
    }

    override def render(canvas: Graphics.Canvas): Unit = {
      canvas.drawColor(Graphics.Color.White)
      canvas.drawRect(characterPosition.x, characterPosition.y - characterHeight, characterWidth, characterHeight, Graphics.defaultPaint.withColor(Graphics.Color.Green))
    }
  }

  override def startingScreen: GameScreen = new MainScreen
}
