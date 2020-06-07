import sbtcrossproject.{crossProject, CrossType}

lazy val commonSettings = Seq(
  version        := "1.0",
  scalaVersion   := "2.12.11",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
)

val scalaNativeVer = "2.11.8"

lazy val core = (crossProject(JSPlatform, JVMPlatform, NativePlatform).crossType(CrossType.Pure) in file("./core"))
  .settings(commonSettings: _*)
  .settings(
    name := "scalavator-core",
    libraryDependencies += "com.regblanc.sgl" %%% "sgl-core" % "0.0.1"
  )
  .nativeSettings(scalaVersion := scalaNativeVer)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js
lazy val coreNative = core.native

lazy val desktop = (project in file("./desktop"))
  .settings(commonSettings: _*)
  .settings(
    name                := "scalavator-desktop",
    libraryDependencies += "com.regblanc.sgl" %% "sgl-desktop-awt" % "0.0.1",
    fork in run := true,
  )
  .dependsOn(coreJVM)

lazy val html5 = (project in file("./html5"))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "scalavator-html5",
    libraryDependencies += "com.regblanc.sgl" %%% "sgl-html5" % "0.0.1",
    scalaJSUseMainModuleInitializer := true
  )
  .dependsOn(coreJS)

lazy val native = (project in file("./native"))
  .enablePlugins(ScalaNativePlugin)
  .settings(commonSettings: _*)
  .settings(scalaVersion := scalaNativeVer)
  .settings(
    name := "scalavator-native",
    libraryDependencies += "com.regblanc.sgl" %%% "sgl-desktop-native" % "0.0.1",
    // This only works on Linux, if you are running on Mac you
    // need these linking options instead:
    //    nativeLinkingOptions ++= Seq("-framework", "OpenGL")
    nativeLinkingOptions += "-lGL"
  )
  .dependsOn(coreNative)
