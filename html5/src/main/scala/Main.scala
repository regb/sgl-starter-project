package com.regblanc.scalavator
package html5

import core.ScalavatorGame

import sgl.html5._
import sgl.html5.themes.FixedWindowTheme
import sgl.html5.util._

object Main extends ScalavatorGame with Html5App 
  with Html5VerboseConsoleLoggingProvider {

  override val GameCanvasID = "scalavator_canvas"

  override val theme = new FixedWindowTheme {
    override val frameSize = (400, 650)
  }

}
