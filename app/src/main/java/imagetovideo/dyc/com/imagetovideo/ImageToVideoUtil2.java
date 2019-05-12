package imagetovideo.dyc.com.imagetovideo;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageToVideoUtil2 {
    MediaCodec mediaCodec;
    MediaMuxer mediaMuxer;
    boolean mMuxerStarted,  isRunning;

    public static final String TAG = "ImageToVideoUtil2";
    MediaCodec.BufferInfo mBufferInfo=null;
    private int mVideoTrackIndex;
    EglSurfaceBase eglSurfaceBase;
    EglEnv eglEnv;
    EncodeProgram2 encodeProgram2;
    public void init(int width, int height){
        mBufferInfo = new MediaCodec.BufferInfo();
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            //创建生成MP4初始化对象
            mediaMuxer = new MediaMuxer(Environment.getExternalStorageDirectory().getAbsolutePath()+"/outputbitmap.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,  MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 16);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        Surface surface= mediaCodec.createInputSurface();
        mediaCodec.start();
        EglCore eglCore =  new EglCore();
        eglSurfaceBase = new EglSurfaceBase(eglCore);
        eglSurfaceBase.createWindowSurface(surface,width,height);
        eglSurfaceBase.makeCurrent();
//         eglEnv = new EglEnv(width,height);
//        eglEnv.setUpEnv();
//        eglEnv.buildWindowSurface(surface);
//         encodeProgram2 = new EncodeProgram2(new int[]{width,height});
//        encodeProgram2.build();
    }
    public int getColorFormat(){
        int colorFormat = 0;
        int[] formats = this.getMediaCodecList();

        lab:
        for (int format : formats) {
            switch (format) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar: // yuv420sp
                    colorFormat = format;
                    break lab;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar: // yuv420p
                    colorFormat = format;
                    break lab;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar: // yuv420psp
                    colorFormat = format;
                    break lab;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar: // yuv420pp
                    colorFormat = format;
                    break lab;
            }
        }

        if (colorFormat <= 0) {
            colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        }
        return colorFormat;
    }
    public int[] getMediaCodecList() {
        //获取解码器列表
        int numCodecs = MediaCodecList.getCodecCount();
        MediaCodecInfo codecInfo = null;
        for (int i = 0; i < numCodecs && codecInfo == null; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if (!info.isEncoder()) {
                continue;
            }
            String[] types = info.getSupportedTypes();
            boolean found = false;
            //轮训所要的解码器
            for (int j = 0; j < types.length && !found; j++) {
                if (types[j].equals("video/avc")) {
                    found = true;
                }
            }
            if (!found) {
                continue;
            }
            codecInfo = info;
        }
        Log.d(TAG, "found" + codecInfo.getName() + "supporting" + " video/avc");
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
        return capabilities.colorFormats;
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     * <p>
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */
    public void drainEncoder(boolean endOfStream) {

        final int TIMEOUT_USEC = 10000;
        L.i(TAG, "drainEncoder(" + endOfStream + ")");

        if (endOfStream) {
            L.i(TAG, "sending EOS to encoder");
            mediaCodec.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = mediaCodec.getOutputBuffers();
        while (true) {
            int encoderStatus = mediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);


            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                L.i(TAG, "encoderStatus == MediaCodec MediaCodec.INFO_TRY_AGAIN_LATER: ");
                // no output available yet
                if (!endOfStream) {
                    L.i(TAG, "no output available, break not end");
                    break;      // out of while
                } else {
                     L.i(TAG, "no output available, spinning to await EOS");
//                     break;
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder\
                L.i(TAG, "encoderStatus == MediaCodec INFO_OUTPUT_BUFFERS_CHANGED: 获取数据");
                encoderOutputBuffers = mediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                L.i(TAG, "encoderStatus == MediaCodec MediaCodec.INFO_OUTPUT_FORMAT_CHANGED: ");
                // should happen before receiving buffers, and should only happen once
//                synchronized (lock) {
                    if (mMuxerStarted) {
                        throw new RuntimeException("format changed twice");
                    }
//                    if(!mMuxerStarted) {
                        MediaFormat newFormat = mediaCodec.getOutputFormat();
                        L.i(TAG, "encoder output format changed: " + newFormat);

                        // now that we have the Magic Goodies, start the muxer
                        mVideoTrackIndex = mediaMuxer.addTrack(newFormat);
                        if (mVideoTrackIndex >= 0) {
                            mediaMuxer.start();
                            mMuxerStarted = true;
                        }
//                    }
//                }
            } else if (encoderStatus < 0) {
                L.i(TAG, "unexpected result from encoder.dequeueOutputBuffer: encoderStatus < 0" +
                        encoderStatus);
                // let's ignore it
            } else {
                Log.d(TAG, "encoderStatus > 0");
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    if (mMuxerStarted) {
                        mediaMuxer.writeSampleData(mVideoTrackIndex, encodedData, mBufferInfo);
                            L.i(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                    mBufferInfo.presentationTimeUs);
                    }
                }
                mediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    L.i(TAG, "BUFFER_FLAG_END_OF_STREAM");
                    if (!endOfStream) {
                        L.i(TAG, "reached end of stream unexpectedly");
                    } else {
                          L.i(TAG, "end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
    }

    public void drawframe(Bitmap bitmap,int num){
        eglSurfaceBase.drawFrame(bitmap,1000000/16*1000L*num);
//        encodeProgram2.renderBitmap(bitmap);
//        eglEnv.setPresentationTime(1000000/16*1000L*num);
//        eglEnv.swapBuffers();
        drainEncoder(false);
    }

    public void  drainEnd() {
       drainEncoder(true);
        eglSurfaceBase.releaseEglSurface();
//        eglEnv.relase();
        mediaCodec.stop();
        mediaCodec.release();
        mediaMuxer.stop();
        mediaMuxer.release();
        mediaMuxer = null;
    }


}
