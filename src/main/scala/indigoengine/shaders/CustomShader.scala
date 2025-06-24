package indigoengine.shaders

import indigo.*
import ultraviolet.syntax.*

case class RGBAData(CUSTOM_COLOUR: vec4) derives ToUniformBlock

object CustomShader:
  class Env extends RGBAData(vec4(0.0f)) with FragmentEnvReference

  val customShaderId: ShaderId = ShaderId("custom-shader")

  val shader: ShaderProgram =
    UltravioletShader.entityFragment(
      customShaderId,
      EntityShader.fragment[Env](fragment, new Env)
    )

  inline def fragment: Shader[Env, Unit] =
    Shader[Env] { env =>
      ubo[RGBAData]

      def fragment(color: vec4): vec4 =
        env.CUSTOM_COLOUR
    }