package com.regblanc.scalavator
package core

import sgl._
import sgl.geometry._

trait ScalavatorGame {
  this: GraphicsProvider
  with InputProvider
  with WindowProvider
  with SystemProvider
  with GameStateComponent =>

  class MainScreen extends GameScreen {
    override val name: String = "main-screen"

    private val CharacterWidth = 32
    private val CharacterHeight = 64
    private val Gravity = Vec(0, 430)
    private val JumpImpulsion = Vec(0, -320)

    private var characterPosition =
      Point(Window.width/2 - CharacterWidth/2, Window.height)
    private var characterVelocity = Vec(0, 0)

    def handleInput(ev: Input.InputEvent): Unit = ev match {
      case Input.TouchUpEvent(_, _, _) | Input.MouseUpEvent(_, _, Input.MouseButtons.Left) =>
      	characterVelocity = JumpImpulsion
      case _ => ()
    }
    Input.setEventProcessor(handleInput)

    override def update(dt: Long): Unit = {
      characterVelocity += Gravity*(dt/1000f)
      characterPosition += characterVelocity*(dt/1000f)
      if(characterPosition.y - Window.height > 0) {
        characterVelocity = Vec(0, 0)
        characterPosition = characterPosition.withY(Window.height)
      }
    }

    override def render(canvas: Graphics.Canvas): Unit = {
      canvas.drawColor(Graphics.Color.White)
      canvas.drawRect(characterPosition.x, characterPosition.y - CharacterHeight, CharacterWidth, CharacterHeight, Graphics.defaultPaint.withColor(Graphics.Color.Green))
    }
  }

  override def startingScreen: GameScreen = new MainScreen
}
