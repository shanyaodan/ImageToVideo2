package imagetovideo.dyc.com.imagetovideo;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.view.View.MeasureSpec.getSize;


public class ImageToVideoUtil {

    private static final String TAG = "ImageToVideoUtil";

    MediaCodec mediaCodec;
    MediaMuxer mediaMuxer;
    boolean mMuxerStarted,  isRunning;
     int   mFrameRate =30;
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void init(int width, int height){

        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            //创建生成MP4初始化对象
            mediaMuxer = new MediaMuxer(Environment.getExternalStorageDirectory()+"/outmp.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, getColorFormat());
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);

        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void encode(Bitmap bitmap,int seconds) {
        final int TIMEOUT_USEC = 10000;
        isRunning = true;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        ByteBuffer[] buffers = null;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            buffers = mediaCodec.getInputBuffers();
        }
        byte[] input = getNV12(getSize(bitmap.getWidth()), getSize(bitmap.getHeight()), bitmap);
        long ptsUsec =0;
        for(int i =0;i<seconds;i++) {
            L.e(TAG,"encodeencode","encodeencode"+i);
            long generateIndex = 0;
            isRunning = true;
            while (isRunning) {
                int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufferIndex >= 0) {
                     ptsUsec += computePresentationTime(generateIndex);
                    L.e(TAG,"encodeencode","encodeencode"+ptsUsec);

                    if (generateIndex >= mFrameRate) {
                        if(i==seconds-1) {
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, ptsUsec, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isRunning = false;
                            drainEncoder(true, info);
                        }
                    } else {
//                    if (bitmap == null) {
//                        bitmap = mProvider.next();
//                    }
//                    bitmap = null;
                        //有效的空的缓存区
                        ByteBuffer inputBuffer = null;
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                            inputBuffer = buffers[inputBufferIndex];
                        } else {
                            inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);//inputBuffers[inputBufferIndex];
                        }
                        inputBuffer.clear();
                        inputBuffer.put(input);
                        //将数据放到编码队列
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, ptsUsec, 0);
                        drainEncoder(false, info);
                    }
                    generateIndex++;
                } else {
                    Log.i(TAG, "input buffer not available");
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        L.e(TAG,"encodeencode","encodeencode:"+ptsUsec);
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
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
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
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


    private void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                V = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128; // Previously U
                U = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128; // Previously V

                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }

                index++;
            }
        }
    }
    private void encodeYUV420P(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uIndex = frameSize;
        int vIndex = frameSize + width * height / 4;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                V = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128; // Previously U
                U = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128; // Previously V

                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[vIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                    yuv420sp[uIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                }

                index++;
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private byte[] getNV12(int inputWidth, int inputHeight, Bitmap scaled) {

        int[] argb = new int[inputWidth * inputHeight];

        //Log.i(TAG, "scaled : " + scaled);
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];

        switch (getColorFormat()) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar: // yuv420sp
                encodeYUV420SP(yuv, argb, inputWidth, inputHeight);
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar: // yuv420p
                encodeYUV420P(yuv, argb, inputWidth, inputHeight);
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar: // yuv420psp
//                encodeYUV420PSP(yuv, argb, inputWidth, inputHeight);
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar: // yuv420pp
//                encodeYUV420PP(yuv, argb, inputWidth, inputHeight);
                break;
        }
//        scaled.recycle();

        return yuv;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void finish() {
        isRunning = false;
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }
        if (mediaMuxer != null) {
            try {
                if (mMuxerStarted) {
                    mediaMuxer.stop();
                    mediaMuxer.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private long computePresentationTime(long frameIndex) {
        return 1000000 / mFrameRate;
    }

    int mTrackIndex;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void drainEncoder(boolean endOfStream, MediaCodec.BufferInfo bufferInfo) {
        final int TIMEOUT_USEC = 10000;

        ByteBuffer[] buffers = null;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            buffers = mediaCodec.getOutputBuffers();
        }

        if (endOfStream) {
            try {
                mediaCodec.signalEndOfInputStream();
            } catch (Exception e) {
            }
        }

        while (true) {
            int encoderStatus = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    break; // out of while
                } else {
                    Log.i(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }

                MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                mTrackIndex = mediaMuxer.addTrack(mediaFormat);
                mediaMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
                Log.i(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
            } else {
                ByteBuffer outputBuffer = null;
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                    outputBuffer = buffers[encoderStatus];
                } else {
                    outputBuffer = mediaCodec.getOutputBuffer(encoderStatus);
                }

                if (outputBuffer == null) {
                    throw new RuntimeException("encoderOutputBuffer "
                            + encoderStatus + " was null");
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    bufferInfo.size = 0;
                }

                if (bufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    Log.d(TAG, "BufferInfo: " + bufferInfo.offset + ","
                            + bufferInfo.size + ","
                            + bufferInfo.presentationTimeUs);

                    try {
                        mediaMuxer.writeSampleData(mTrackIndex, outputBuffer, bufferInfo);
                    } catch (Exception e) {
                        Log.i(TAG, "Too many frames");
                    }

                }

                mediaCodec.releaseOutputBuffer(encoderStatus, false);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.i(TAG, "reached end of stream unexpectedly");
                    } else {
                        Log.i(TAG, "end of stream reached");
                    }
                    break; // out of while
                }
            }
        }
    }

}
