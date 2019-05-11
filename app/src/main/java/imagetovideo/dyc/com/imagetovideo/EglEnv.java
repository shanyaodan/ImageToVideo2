package imagetovideo.dyc.com.imagetovideo;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.Surface;

public final class EglEnv {
   private EGLDisplay eglDisplay= EGL14.EGL_NO_DISPLAY;
   private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
   private EGLSurface eglSurface;
   private EGLConfig eglConfig;
   private final int width;
   private final int height;
   private int mGlVersion = -1;

   public final EglEnv setUpEnv() {
      this.eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);



      int[] version = new int[2];
      if(!EGL14.eglInitialize(this.eglDisplay, version, 0, version, 1)) {
      }

      int[] attribs = new int[]{ EGL14.EGL_BUFFER_SIZE, 32,
              EGL14.EGL_ALPHA_SIZE, 8,
              EGL14.EGL_BLUE_SIZE, 8,
              EGL14.EGL_GREEN_SIZE, 8,
              EGL14.EGL_RED_SIZE, 8,
              EGL14.EGL_RENDERABLE_TYPE,
              EGL14.EGL_OPENGL_ES2_BIT,
              EGL14.EGL_SURFACE_TYPE,
              EGL14.EGL_WINDOW_BIT,
              EGL14.EGL_NONE};
      EGLConfig[] configs = new EGLConfig[1];
      int[] numConfigs = new int[1];
      if(!EGL14.eglChooseConfig(this.eglDisplay, attribs, 0, configs, 0, configs.length, numConfigs, 0)) {

      }

      this.eglConfig = configs[0];
      int[] attributes = new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
      this.eglContext = EGL14.eglCreateContext(this.eglDisplay, this.eglConfig, EGL14.EGL_NO_CONTEXT, attributes, 0);


      return this;
   }

//   public final void buildOffScreenSurface() {
//      int[] pbufferAttributes = new int[]{12375, this.width, 12374, this.height, 12344};
//      this.eglSurface = EGL14.eglCreatePbufferSurface(this.eglDisplay, this.eglConfig, pbufferAttributes, 0);
//
//
//      this.makeCurrent();
//   }

   public final void buildWindowSurface( Surface surface) {
      int[] format = new int[1];
      if(!EGL14.eglGetConfigAttrib(this.eglDisplay, this.eglConfig, EGL14.EGL_NATIVE_VISUAL_ID, format, 0)) {
      }

      if (eglSurface != EGL14.EGL_NO_SURFACE) {

      }
         int[] surfaceAttribs = new int[]{EGL14.EGL_NONE};
         this.eglSurface = EGL14.eglCreateWindowSurface(this.eglDisplay, this.eglConfig, surface, surfaceAttribs, 0);
         this.makeCurrent();

   }

   private final void makeCurrent() {
      Log.d(this.getClass().getName(), " egl make current ");
      if(!EGL14.eglMakeCurrent(this.eglDisplay, this.eglSurface, this.eglSurface, this.eglContext)) {

      }

   }

   public final void setPresentationTime(long nsecs) {
      EGLExt.eglPresentationTimeANDROID(this.eglDisplay, this.eglSurface, nsecs);
   }

   public final boolean swapBuffers() {
      boolean result = EGL14.eglSwapBuffers(this.eglDisplay, this.eglSurface);

      return result;
   }

   public final void relase() {
         EGL14.eglMakeCurrent(this.eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
         EGL14.eglDestroySurface(this.eglDisplay, this.eglSurface);
         EGL14.eglDestroyContext(this.eglDisplay, this.eglContext);
         EGL14.eglReleaseThread();
         EGL14.eglTerminate(this.eglDisplay);


      this.eglSurface = EGL14.EGL_NO_SURFACE;
      this.eglContext = EGL14.EGL_NO_CONTEXT;
      this.eglDisplay = EGL14.EGL_NO_DISPLAY;
   }

   public EglEnv(int width, int height) {
      this.width = width;
      this.height = height;
      this.eglDisplay = EGL14.EGL_NO_DISPLAY;
      this.eglContext = EGL14.EGL_NO_CONTEXT;
      this.eglSurface = EGL14.EGL_NO_SURFACE;
   }
}
