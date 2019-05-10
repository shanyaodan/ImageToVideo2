package imagetovideo.dyc.com.imagetovideo;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.util.Size;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class EncodeProgram2 {
        private final String VERTEX_SHADER;
        private final String FRAGMENT_SHADER;
        private final float[] pointData;
        private final float[] texVertex;
        private final float[] projectionMatrix;
        private FloatBuffer vertexData;
        private FloatBuffer texVertexBuffer;
        private int mAPositionLocation;
        private int uTextureUnitLocation;
        private int program;
        private int textureID;
        private int aTextCoordLocation;
        private final int []size;
    public EncodeProgram2(int[] size) {

        this.size = size;
        this.VERTEX_SHADER = "\n                uniform mat4 u_Matrix;\n                attribute vec4 a_Position;\n                attribute vec2 a_TexCoord;\n                varying vec2 v_TexCoord;\n                void main() {\n                    v_TexCoord = a_TexCoord;\n                    gl_Position = u_Matrix * a_Position;\n                }\n        ";
        this.FRAGMENT_SHADER = "\n                precision mediump float;\n                varying vec2 v_TexCoord;\n                uniform sampler2D u_TextureUnit;\n                void main() {\n                    gl_FragColor = texture2D(u_TextureUnit, v_TexCoord);\n                }\n                ";
        this.pointData = new float[]{-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, 1.0F, -1.0F};
        this.texVertex = new float[]{0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F};
        this.projectionMatrix = new float[]{1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};
        this.vertexData = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.vertexData.put(this.pointData);
        this.vertexData.position(0);
        this.texVertexBuffer =ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texVertexBuffer.put(this.texVertex);
        this.texVertexBuffer.position(0);
    }
        public final void build() {
            this.program =uCreateGlProgram(this.VERTEX_SHADER, this.FRAGMENT_SHADER);
            glError(0,program);
            GLES20.glUseProgram(this.program);
            this.initLocation();
        }

     private final void initLocation() {
            this.mAPositionLocation = GLES20.glGetAttribLocation(program, "a_Position");
            glError(1,mAPositionLocation);
            int uMatrixLocation  = GLES20.glGetUniformLocation(program, "u_Matrix");
            glError(2,uMatrixLocation);
            this.aTextCoordLocation = GLES20.glGetAttribLocation(this.program, "a_TexCoord");
            glError(3,aTextCoordLocation);
            this.uTextureUnitLocation =GLES20.glGetAttribLocation(this.program, "u_TextureUnit");
            glError(4,uTextureUnitLocation);
            this.textureID = createTextureID();
            GLES20.glVertexAttribPointer(this.aTextCoordLocation, 2, GLES20.GL_FLOAT, false, 0, this.texVertexBuffer);
            GLES20.glEnableVertexAttribArray(this.aTextCoordLocation);
            GLES20.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            // 开启纹理透明混合，这样才能绘制透明图片
             GLES20.glEnable(GL10.GL_BLEND);
            GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glViewport(0, 0, size[0], size[1]);
            GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, this.projectionMatrix, 0);
        }

        public final void renderBitmap( Bitmap b) {

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0);
//        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
//            b.recycle()
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

            GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT);
            vertexData.position(0);

            GLES20.glVertexAttribPointer(mAPositionLocation, 2,
                    GLES20.GL_FLOAT, false, 0, vertexData);
            GLES20.glEnableVertexAttribArray(mAPositionLocation);

            // 设置当前活动的纹理单元为纹理单元0
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

            // 将纹理ID绑定到当前活动的纹理单元上
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);

            GLES20.glUniform1i(uTextureUnitLocation, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, pointData.length / 2);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);


//            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0);
//
//            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
//            GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT);
//            GLES20.glVertexAttribPointer(mAPositionLocation, 2, GLES20.GL_FLOAT, false, 0, vertexData);
//            GLES20.glEnableVertexAttribArray(this.mAPositionLocation);
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);
//            GLES20.glUniform1i(this.uTextureUnitLocation, 0);
//            GLES20.glDrawArrays(6, 0, this.pointData.length / 2);
//            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        }

    /**
     * 创建显示的texture
     */
    private int createTextureID() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

    //创建GL程序
    public static int uCreateGlProgram(String vertexSource, String fragmentSource) {
        int vertex = uLoadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertex == 0) return 0;
        int fragment = uLoadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragment == 0) return 0;
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertex);
            GLES20.glAttachShader(program, fragment);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                glError(1, "Could not link program:" + GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    /**
     * 加载shader
     */
    public static int uLoadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (0 != shader) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                glError(1, "Could not compile shader:" + shaderType);
                glError(1, "GLES20 Error:" + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }
    public static void glError(int code, Object index) {
        if (code != 0) {
            Log.e("EncodeProgram2", "glError:" + code + "---" + index);
        }
    }
}
