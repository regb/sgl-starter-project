package com.regblanc.scalavator
package desktop

import core.ScalavatorGame

import sgl.awt.AWTApp
import sgl.awt.util.VerboseStdOutLoggingProvider

object Main extends ScalavatorGame with AWTApp
  with VerboseStdOutLoggingProvider {

  override val frameDimension = (400, 650)

}
