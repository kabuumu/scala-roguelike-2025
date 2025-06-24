package indigoengine.shaders

import indigo.*
import ultraviolet.syntax.*

object Darken:
  val shader: ShaderProgram =
    UltravioletShader.entityFragment(
      ShaderId("custom shader"),
      EntityShader.fragment[FragmentEnv](fragment, FragmentEnv.reference)
    )

  inline def fragment: Shader[FragmentEnv, Unit] =
    Shader[FragmentEnv] { env =>
      def fragment(color: vec4): vec4 = {
        color // Darken the color by multiplying with a factor
      }
    }
