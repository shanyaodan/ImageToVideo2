package imagetovideo.dyc.com.imagetovideo;

import android.opengl.EGL14;
import android.opengl.GLES20;
import android.util.Log;

public  class GLFunction2 {
//   private static final String TAG = "GLFunction";
//
//   public static final int createProgram(String vertexSource, String fragmentSource) {
//
//      int[] ints = new int[1];
//      GLES20.glGetIntegerv(34921, ints, 0);
//      Log.d(TAG, "create program max vertex attribs : " + ints[0]);
//      int vertexShader = loadShader(35633, vertexSource);
//      Log.d(TAG, "createProgram vertexShader: " + vertexShader + ' ');
//      if (vertexShader == 0) {
//         return 0;
//      } else {
//         int pixelShader = loadShader(35632, fragmentSource);
//         Log.d(TAG, "createProgram vertexShader: " + pixelShader + ' ');
//         if (pixelShader == 0) {
//            return 0;
//         } else {
//            int program = GLES20.glCreateProgram();
//            if (program == 0) {
//               Log.e(TAG, "Could not create program");
//            }
//
//            GLES20.glAttachShader(program, vertexShader);
//            checkGlError("glAttachShader");
//            GLES20.glAttachShader(program, pixelShader);
//            checkGlError("glAttachShader");
//            GLES20.glLinkProgram(program);
//            int[] linkStatus = new int[1];
//            GLES20.glGetProgramiv(program, 35714, linkStatus, 0);
//            if (linkStatus[0] != 1) {
//               Log.e(TAG, "Could not link program: ");
//               Log.e(TAG, GLES20.glGetProgramInfoLog(program));
//               GLES20.glDeleteProgram(program);
//               program = 0;
//            }
//
//            if (program == 0) {
//
//            } else {
//               return program;
//            }
//         }
//      }
//      return 0;
//   }
//
////   public static final int loadShader(int shaderType, String source) {
////
////      int shader = GLES20.glCreateShader(shaderType);
////      checkGlError("glCreateShader type=" + shaderType);
////      GLES20.glShaderSource(shader, source);
////      GLES20.glCompileShader(shader);
////      int[] compiled = new int[1];
////      GLES20.glGetShaderiv(shader, 35713, compiled, 0);
////      if (compiled[0] == 0) {
////         Log.e(TAG, "Could not compile shader " + shaderType + ':');
////         Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
////         GLES20.glDeleteShader(shader);
////         shader = 0;
////      }
////
////      return shader;
////   }
//
//   public static final int getAttrib(int program,  String name) {
//
//      int location = GLES20.glGetAttribLocation(program, name);
//      checkLocation(location, name);
//      return location;
//   }
//
//   public static final int getUniform(int program,  String name) {
//
//      int uniform = GLES20.glGetUniformLocation(program, name);
//      checkLocation(uniform, name);
//      return uniform;
//   }
//
//   public static final void checkEglError( String msg) {
//
//      int error = EGL14.eglGetError();
//      if (error != 12288) {
//
//      }
//   }
//
//   public static final void checkGlError( String op) {
//      int error = GLES20.glGetError();
//      if (error != 0) {
//
//      }
//   }
//
//   public static final void checkLocation(int location,  String label) {
//
//      if (location < 0) {
//
//      }
//   }
}
