package imagetovideo.dyc.com.imagetovideo;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

//import com.infomedia.yunbian.videorecord.gpufilter.basefilter.GPUImageFilter;
//import com.infomedia.yunbian.videorecord.gpufilter.helper.MagicFilterFactory;
//import com.infomedia.yunbian.videorecord.gpufilter.helper.MagicFilterType;
//import com.infomedia.yunbian.videorecord.media.VideoInfo;
//import com.infomedia.yunbian.videorecord.widget.WaterPositionView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.media.MediaExtractor.SEEK_TO_PREVIOUS_SYNC;

/**
 * Created by Administrator on 2017/6/19 0019.
 * desc：用于视频裁剪的类
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class VideoClipper {
    private final String TAG = "VideoClipper";
    final int TIMEOUT_USEC =800;
    private String mInputVideoPath;
    private String mOutputVideoPath;

    MediaCodec videoDecoder;
    MediaCodec videoEncoder;
    MediaCodec audioDecoder;
    MediaCodec audioEncoder;

    MediaExtractor mVideoExtractor;
    MediaExtractor mAudioExtractor;
    MediaMuxer mMediaMuxer;
    static ExecutorService executorService = Executors.newFixedThreadPool(4);
    int muxVideoTrack = -1;
    int muxAudioTrack = -1;
    int videoTrackIndex = -1;
    int audioTrackIndex = -1;
    long startPosition;
    long clipDur;
    public int videoWidth;
    public int videoHeight;
    int videoRotation;
//    OutputSurface outputSurface = null;
//    InputSurface inputSurface = null;
    MediaFormat videoFormat;
    MediaFormat audioFormat;
//    GPUImageFilter mFilter;
    boolean isOpenBeauty;
    boolean videoFinish = false;
    boolean audioFinish = false;
    boolean released = false;
    long before;
    long after;
    Object lock = new Object();
    boolean muxStarted = false;
    OnVideoCutFinishListener listener;
//    ArrayList<WaterPositionView.WaterMark> waterMarks = new ArrayList<>();

    //初始化音视频解码器和编码器
//    public VideoClipper(ArrayList<WaterPositionView.WaterMark> waterMarks) {
    public VideoClipper() {
//        this.waterMarks = waterMarks;
        try {
            videoDecoder = MediaCodec.createDecoderByType("video/avc");
            videoEncoder = MediaCodec.createEncoderByType("video/avc");
            audioDecoder = MediaCodec.createDecoderByType("audio/mp4a-latm");
            audioEncoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setInputVideoPath(String inputPath) {
        mInputVideoPath = inputPath;
        initVideoInfo();
    }

    public void setOutputVideoPath(String outputPath) {
        mOutputVideoPath = outputPath;
    }


    public void setOnVideoCutFinishListener(OnVideoCutFinishListener listener) {
        this.listener = listener;
    }

    /**
     * 设置滤镜
     */
//    public void setFilter(GPUImageFilter filter) {
//        if (filter == null) {
//            mFilter = null;
//            return;
//        }
//        mFilter = filter;
//    }

//    public void setFilterType(MagicFilterType type) {
//        if (type == null || type == MagicFilterType.NONE) {
//            mFilter = null;
//            return;
//        }
//        mFilter = MagicFilterFactory.initFilters(type);
//    }

    /**
     * 开启美颜
     */
    public void showBeauty() {
        isOpenBeauty = true;
    }

    /**
     * 裁剪视频
     *
     * @param startPosition 微秒级
     * @param clipDur       微秒级
     * @throws IOException
     */
    public void clipVideo(long startPosition, long clipDur) throws IOException {
        before = System.currentTimeMillis();
        this.startPosition = startPosition;

        mVideoExtractor = new MediaExtractor();
        mAudioExtractor = new MediaExtractor();
        mVideoExtractor.setDataSource(mInputVideoPath);
        mAudioExtractor.setDataSource(mInputVideoPath);
        mMediaMuxer = new MediaMuxer(mOutputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mMediaMuxer.setOrientationHint(videoRotation);
        //音轨和视轨初始化
        for (int i = 0; i < mVideoExtractor.getTrackCount(); i++) {
            MediaFormat format = mVideoExtractor.getTrackFormat(i);
            if (format.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                videoTrackIndex = i;
                videoFormat = format;
                continue;
            }
            if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                audioTrackIndex = i;
                audioFormat = format;
//                muxAudioTrack = mMediaMuxer.addTrack(format);
                continue;
            }
        }
        this.clipDur = clipDur;
//        this.clipDur = videoFormat.getLong(MediaFormat.KEY_DURATION);
        executorService.execute(videoCliper);
        executorService.execute(audioCliper);
    }

    private Runnable videoCliper = new Runnable() {
        @Override
        public void run() {
            mVideoExtractor.selectTrack(videoTrackIndex);
            long firstVideoTime = mVideoExtractor.getSampleTime();
            mVideoExtractor.seekTo(firstVideoTime + startPosition, SEEK_TO_PREVIOUS_SYNC);
            initVideoCodec();//暂时统一处理,为音频转换采样率做准备
            startVideoCodec2(videoDecoder, videoEncoder, mVideoExtractor, firstVideoTime, startPosition, clipDur);
            videoFinish = true;
            release();
        }
    };

    private Runnable audioCliper = new Runnable() {
        @Override
        public void run() {
            try {
                mAudioExtractor.selectTrack(audioTrackIndex);
                initAudioCodec();
                startAudioCodec(audioDecoder, audioEncoder, mAudioExtractor, mAudioExtractor.getSampleTime(), startPosition, clipDur);
                audioFinish = true;
                release();
            }catch (Throwable t){
                t.printStackTrace();
                audioFinish = true;
                listener.onError();
            }

        }
    };

    private void initVideoInfo() {
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        retr.setDataSource(mInputVideoPath);
        String width = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String height = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String rotation = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        videoWidth = Integer.parseInt(width);
        videoHeight = Integer.parseInt(height);
        videoRotation = Integer.parseInt(rotation);

    }

    private void initAudioCodec() {
        audioDecoder.configure(audioFormat, null, null, 0);
        audioDecoder.start();

        MediaFormat format = MediaFormat.createAudioFormat(audioFormat.getString(MediaFormat.KEY_MIME), audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE), /*channelCount*/audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));//这里一定要注意声道的问题
        try {
            format.setInteger(MediaFormat.KEY_BIT_RATE, audioFormat.getInteger(MediaFormat.KEY_BIT_RATE));//比特率
        } catch (Exception e) {
            e.printStackTrace();
            format.setInteger(MediaFormat.KEY_BIT_RATE, VideoEncoderCore.audioRate);//比特率
        }

        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        audioEncoder.start();
    }

    private void startAudioCodec(MediaCodec decoder, MediaCodec encoder, MediaExtractor extractor, long firstSampleTime, long startPosition, long duration) {
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        ByteBuffer[] decoderOutputBuffers = decoder.getOutputBuffers();
        ByteBuffer[] encoderInputBuffers = encoder.getInputBuffers();
        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        MediaCodec.BufferInfo outputInfo = new MediaCodec.BufferInfo();
        boolean done = false;//用于判断整个编解码过程是否结束
        boolean inputDone = false;
        boolean decodeDone = false;
        extractor.seekTo(firstSampleTime + startPosition, SEEK_TO_PREVIOUS_SYNC);
        int decodeinput = 0;
        int encodeinput = 0;
        int encodeoutput = 0;
        long lastEncodeOutputTimeStamp = -1;
        while (!done) {
            if (!inputDone) {
                int inputIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = decoderInputBuffers[inputIndex];
                    inputBuffer.clear();
                    int readSampleData = extractor.readSampleData(inputBuffer, 0);
                    long dur = extractor.getSampleTime() - firstSampleTime - startPosition;
                    if ((dur < duration) && readSampleData > 0) {
                        decoder.queueInputBuffer(inputIndex, 0, readSampleData, extractor.getSampleTime(), 0);
                        decodeinput++;
                        System.out.println("videoCliper audio decodeinput" + decodeinput + " dataSize" + readSampleData + " sampeTime" + extractor.getSampleTime());
                        extractor.advance();
                    } else {
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        System.out.println("videoCliper audio decodeInput end");
                        inputDone = true;
                    }
                }
            }
            if (!decodeDone) {
                int index = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    decoderOutputBuffers = decoder.getOutputBuffers();
                } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // expected before first buffer of data
                    MediaFormat newFormat = decoder.getOutputFormat();
                } else if (index < 0) {
                } else {
                    boolean canEncode = (info.size != 0 && info.presentationTimeUs - firstSampleTime > startPosition);
                    boolean endOfStream = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    if (canEncode && !endOfStream) {
                        ByteBuffer decoderOutputBuffer = decoderOutputBuffers[index];

                        int encodeInputIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                        if (encodeInputIndex >= 0) {
                            ByteBuffer encoderInputBuffer = encoderInputBuffers[encodeInputIndex];
                            encoderInputBuffer.clear();
//                            if (info.size < 4096) {//这里看起来应该是16位单声道转16位双声道
//                                byte[] chunkPCM = new byte[info.size];
//                                decoderOutputBuffer.get(chunkPCM);
//                                decoderOutputBuffer.clear();
//                                //说明是单声道的,需要转换一下
//                                Log.e("VideoCliper","声道转换");
//                                byte[] stereoBytes = new byte[info.size * 2];
//                                for (int i = 0; i < info.size; i += 2) {
//                                    stereoBytes[i * 2 + 0] = chunkPCM[i];
//                                    stereoBytes[i * 2 + 1] = chunkPCM[i + 1];
//                                    stereoBytes[i * 2 + 2] = chunkPCM[i];
//                                    stereoBytes[i * 2 + 3] = chunkPCM[i + 1];
//                                }
//                                encoderInputBuffer.put(stereoBytes);
//                                encoder.queueInputBuffer(encodeInputIndex, 0, stereoBytes.length, info.presentationTimeUs, 0);
//                                encodeinput++;
//                                System.out.println("videoCliper audio encodeInput" + encodeinput + " dataSize" + info.size + " sampeTime" + info.presentationTimeUs);
//                            } else {
                                Log.e("VideoCliper","不用声道转换");


                                encoderInputBuffer.put(decoderOutputBuffer);
                                encoder.queueInputBuffer(encodeInputIndex, info.offset, info.size, info.presentationTimeUs, 0);
                                encodeinput++;
                                System.out.println("videoCliper audio encodeInput" + encodeinput + " dataSize" + info.size + " sampeTime" + info.presentationTimeUs);
//                            }
                        }
                    }
                    if (endOfStream)                {
                        int encodeInputIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                        while (encodeInputIndex==-1){
                            encodeInputIndex = encoder.dequeueInputBuffer(0);
                            System.out.println("encoderInputBuffers等等");
                        }
                        encoder.queueInputBuffer(encodeInputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        System.out.println("videoCliper audio encodeInput end");
                        decodeDone = true;
                    }
                    decoder.releaseOutputBuffer(index, false);
                }
            }
            boolean encoderOutputAvailable = true;
            while (encoderOutputAvailable) {
                int encoderStatus = encoder.dequeueOutputBuffer(outputInfo, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    encoderOutputAvailable = false;
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    encoderOutputBuffers = encoder.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = encoder.getOutputFormat();
                    startMux(newFormat, 1);
                } else if (encoderStatus < 0) {
                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    done = (outputInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    if (done) {
                        encoderOutputAvailable = false;
                    }
                    // Write the data to the output "file".
                    if (outputInfo.presentationTimeUs == 0 && !done) {
                        continue;
                    }
                    if (outputInfo.size != 0 && outputInfo.presentationTimeUs > 0) {
                        /*encodedData.position(outputInfo.offset);
                        encodedData.limit(outputInfo.offset + outputInfo.size);*/
                        if (!muxStarted) {
                            synchronized (lock) {
                                if (!muxStarted) {
                                    try {
                                        lock.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        if (outputInfo.presentationTimeUs > lastEncodeOutputTimeStamp) {//为了避免有问题的数据
                            encodeoutput++;
                            System.out.println("videoCliper audio encodeOutput" + encodeoutput + " dataSize" + outputInfo.size + " sampeTime" + outputInfo.presentationTimeUs);
                            mMediaMuxer.writeSampleData(muxAudioTrack, encodedData, outputInfo);
                            lastEncodeOutputTimeStamp = outputInfo.presentationTimeUs;
                        }
                    }

                    encoder.releaseOutputBuffer(encoderStatus, false);
                }
                if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // Continue attempts to drain output.
                    continue;
                }
            }
        }
    }
    private void startVideoCodec2(MediaCodec decoder, MediaCodec encoder, MediaExtractor extractor, long firstSampleTime, long startPosition, long duration) {
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        ByteBuffer[] decoderOutputBuffers = decoder.getOutputBuffers();
        ByteBuffer[] encoderInputBuffers = encoder.getInputBuffers();
        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        MediaCodec.BufferInfo outputInfo = new MediaCodec.BufferInfo();
        boolean done = false;//用于判断整个编解码过程是否结束
        boolean inputDone = false;
        boolean decodeDone = false;
        extractor.seekTo(firstSampleTime + startPosition, SEEK_TO_PREVIOUS_SYNC);
        int decodeinput = 0;
        int encodeinput = 0;
        int encodeoutput = 0;
        int encoderStatus =0;
        long lastEncodeOutputTimeStamp = -1;
        while (!done) {
            if (!inputDone) {
                int inputIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = decoderInputBuffers[inputIndex];
                    inputBuffer.clear();
                    int readSampleData = extractor.readSampleData(inputBuffer, 0);
                    long dur = extractor.getSampleTime() - firstSampleTime - startPosition;
                    if ((dur < duration) && readSampleData > 0) {
                        decoder.queueInputBuffer(inputIndex, 0, readSampleData, extractor.getSampleTime(), 0);

                        decodeinput++;
                        System.out.println("videoCliper video decodeinput" + decodeinput + " dataSize" + readSampleData + " sampeTime" + extractor.getSampleTime());
                        extractor.advance();
                    } else {
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        System.out.println("videoCliper video decodeInput end");
                        inputDone = true;
                    }
                }
            }

            if (!decodeDone) {
                int index = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    decoderOutputBuffers = decoder.getOutputBuffers();
                } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // expected before first buffer of data
                    MediaFormat newFormat = decoder.getOutputFormat();
                } else if (index < 0) {
                } else {
                    boolean canEncode = (info.size != 0 && info.presentationTimeUs - firstSampleTime > startPosition);
                    boolean endOfStream = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    if (canEncode && !endOfStream) {
                        ByteBuffer decoderOutputBuffer = decoderOutputBuffers[index];
                        int encodeInputIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                        if (encodeInputIndex >= 0) {
                            ByteBuffer encoderInputBuffer = encoderInputBuffers[encodeInputIndex];
                            encoderInputBuffer.clear();
                            encoderInputBuffer.put(decoderOutputBuffer);
                            encoder.queueInputBuffer(encodeInputIndex, info.offset, info.size, info.presentationTimeUs, 0);
                            encodeinput++;
                            System.out.println("videoCliper video encodeInput" + encodeinput + " dataSize" + info.size + " sampeTime" + info.presentationTimeUs);
                        }
                    }
                    if (endOfStream) {
                        ByteBuffer decoderOutputBuffer = decoderOutputBuffers[index];
                        int encodeInputIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                        while (encodeInputIndex==-1){
                            encodeInputIndex = encoder.dequeueInputBuffer(0);
                            System.out.println("encoderInputBuffers等等");
                        }
                        try {
                            ByteBuffer encoderInputBuffer = encoderInputBuffers[encodeInputIndex];
                            encoderInputBuffer.clear();
                            encoderInputBuffer.put(decoderOutputBuffer);
                        }catch (Throwable t){
                            t.printStackTrace();
//                          encodeInputIndex = encoderInputBuffers.length-1;
                            System.out.println("encoderInputBuffers"+encoderInputBuffers.length+"sssssINdex"+encodeInputIndex);
                        }
                        System.out.println("encoderInputBuffers"+encoderInputBuffers.length+"success"+encodeInputIndex);
                        encoder.queueInputBuffer(encodeInputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        decodeDone = true;
                    }
                    decoder.releaseOutputBuffer(index, false);
                }
            }
            System.out.println("videoCliper video encodeInput before while");
            boolean encoderOutputAvailable = true;
            while (encoderOutputAvailable) {
                 encoderStatus = encoder.dequeueOutputBuffer(outputInfo, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    System.out.println("videoCliper video encodeOutput  MediaCodec.INFO_TRY_AGAIN_LATER");
                    encoderOutputAvailable = false;
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    encoderOutputBuffers = encoder.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = encoder.getOutputFormat();
                    startMux(newFormat, 0);
                } else if (encoderStatus < 0) {
                    System.out.println("videoCliper video encodeOutput release encoderStatus < 0");
                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    done = (outputInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    if (done) {
                        encoderOutputAvailable = false;
                    }
                    if (outputInfo.presentationTimeUs == 0 && !done) {
                        System.out.println("videoCliper video encodeOutput  outputInfo.presentationTimeUs == 0 ");
                        continue;
                    }
                    if (outputInfo.size != 0 && outputInfo.presentationTimeUs > 0) {
                        /*encodedData.position(outputInfo.offset);
                        encodedData.limit(outputInfo.offset + outputInfo.size);*/
                        if (!muxStarted) {
                            synchronized (lock) {
                                if (!muxStarted) {
                                    try {
                                        lock.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        if (outputInfo.presentationTimeUs > lastEncodeOutputTimeStamp) {//为了避免有问题的数据
                            encodeoutput++;
                            System.out.println("videoCliper video encodeOutput" + encodeoutput + " dataSize" + outputInfo.size + " sampeTime" + outputInfo.presentationTimeUs);
                            mMediaMuxer.writeSampleData(muxVideoTrack, encodedData, outputInfo);
                            lastEncodeOutputTimeStamp = outputInfo.presentationTimeUs;
                        }
                    }
                    System.out.println("videoCliper video encodeOutput release");
                    encoder.releaseOutputBuffer(encoderStatus, false);
                }
                if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {

                    continue;
                }
            }
        }
    }


    private void initVideoCodec() {
        //不对视频进行压缩

        try {
            int encodeW = videoWidth;
            int encodeH = videoHeight;
            final int bitrate = VideoEncoderCore.videBitrate;
            //设置视频的编码参数
//            if((encodeW&1)==1){
//                encodeW --;
//            }
//            if((encodeH&1)==1){
//                encodeH --;
//            }
            int width = videoFormat.getInteger(MediaFormat.KEY_WIDTH);
            int height = videoFormat.getInteger(MediaFormat.KEY_HEIGHT);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(videoFormat.getString(MediaFormat.KEY_MIME),width, height);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 27);




            if(width>height){
                videoRotation =90;
            }else if(width<height){

                videoRotation =0;
            }else {

                videoRotation =videoFormat.getInteger(MediaFormat.KEY_ROTATION);
            }
            mediaFormat.setInteger(MediaFormat.KEY_ROTATION,videoRotation);
//            Log.e(TAG,"KEY_ROTATION"+videoRotation);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

            if (Build.VERSION.SDK_INT >= 23) {
                Bundle params = new Bundle();
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                videoEncoder.setParameters(params);
            }

            videoEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//            inputSurface = new InputSurface(videoEncoder.createInputSurface());
//            inputSurface.makeCurrent();
            videoEncoder.start();


//            VideoInfo info = new VideoInfo();
//            info.width = videoWidth;
//            info.height = videoHeight;
//            info.rotation = videoRotation;
//            outputSurface = new OutputSurface(info, waterMarks);
//            outputSurface.isBeauty(isOpenBeauty);
//
//            if (mFilter != null) {
//                Log.e("hero", "---gpuFilter 不为null哟----设置进outputSurface里面");
//                outputSurface.addGpuFilter(mFilter);
//            }

            videoDecoder.configure(videoFormat, null, null, 0);
            videoDecoder.start();//解码器启动
        } catch (Exception e) {
            if (null != videoEncoder) {
                videoEncoder.release();
            }
            if (null != videoDecoder) {
                videoDecoder.release();
            }
            if (null != listener) {
                listener.onError();
            }
        }
    }

    /**
     * 将两个关键帧之间截取的部分重新编码
     *
     * @param decoder
     * @param encoder
     * @param extractor
     * @param firstSampleTime 视频第一帧的时间戳
     * @param startPosition   微秒级
     * @param duration        微秒级
     */
    private void startVideoCodec(MediaCodec decoder, MediaCodec encoder, MediaExtractor extractor,   long firstSampleTime, long startPosition, long duration) {

        try {
            ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
            ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo outputInfo = new MediaCodec.BufferInfo();
            boolean done = false;//用于判断整个编解码过程是否结束
            boolean inputDone = false;
            boolean decodeDone = false;
            while (!done) {
                if (!inputDone) {
                    int inputIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = decoderInputBuffers[inputIndex];
                        inputBuffer.clear();
                        int readSampleData = extractor.readSampleData(inputBuffer, 0);
                        long dur = extractor.getSampleTime() - firstSampleTime - startPosition;//当前已经截取的视频长度
                        if ((dur < duration) && readSampleData > 0) {
                            decoder.queueInputBuffer(inputIndex, 0, readSampleData, extractor.getSampleTime(), 0);
                            extractor.advance();
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        }
                    }
                }
                if (!decodeDone) {
                    int index = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                    if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // no output available yet
                    } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        //decoderOutputBuffers = decoder.getOutputBuffers();
                    } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // expected before first buffer of data
                        MediaFormat newFormat = decoder.getOutputFormat();
                    } else if (index < 0) {
                    } else {

                        boolean doRender = (info.size != 0 && info.presentationTimeUs - firstSampleTime > startPosition);
                        decoder.releaseOutputBuffer(index, false);
                        if (doRender) {
                            // This waits for the image and renders it after it arrives.
//                            outputSurface.awaitNewImage();
////                            outputSurface.drawImage(info.presentationTimeUs);
////                            // Send it to the encoder.
////                            inputSurface.setPresentationTime(info.presentationTimeUs * 1000);
////                            inputSurface.swapBuffers();
                        }
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                            encoder.signalEndOfInputStream();
                            decodeDone = true;
                        }
                    }
                }
                boolean encoderOutputAvailable = true;
                while (encoderOutputAvailable) {
                    int encoderStatus = encoder.dequeueOutputBuffer(outputInfo, TIMEOUT_USEC);
                    if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // no output available yet
                        encoderOutputAvailable = false;
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        encoderOutputBuffers = encoder.getOutputBuffers();
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat newFormat = encoder.getOutputFormat();
                        startMux(newFormat, 0);
                    } else if (encoderStatus < 0) {
                    } else {
                        ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                        done = (outputInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                        if (done) {
                            encoderOutputAvailable = false;
                        }
                        // Write the data to the output "file".
                        if (outputInfo.presentationTimeUs == 0 && !done) {
                            continue;
                        }
                        if (outputInfo.size != 0) {
                            encodedData.position(outputInfo.offset);
                            encodedData.limit(outputInfo.offset + outputInfo.size);
                            if (!muxStarted) {
                                synchronized (lock) {
                                    if (!muxStarted) {
                                        try {
                                            lock.wait();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                            mMediaMuxer.writeSampleData(muxVideoTrack, encodedData, outputInfo);
                        }
                        encoder.releaseOutputBuffer(encoderStatus, false);
                    }
                    if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // Continue attempts to drain output.
                        continue;
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            release();
            if(null!=listener){
                listener.onError();
            }
        }
    }

    /**
     * @param mediaFormat
     * @param flag        0 video,1 audio
     */
    private void startMux(MediaFormat mediaFormat, int flag) {
        if (flag == 0) {
            muxVideoTrack = mMediaMuxer.addTrack(mediaFormat);
        } else if (flag == 1) {
            muxAudioTrack = mMediaMuxer.addTrack(mediaFormat);
        }
        synchronized (lock) {
            if (muxAudioTrack != -1 && muxVideoTrack != -1 && !muxStarted) {
                mMediaMuxer.start();
                muxStarted = true;
                lock.notify();
            }
        }
    }

    private synchronized void release() {
        if (!videoFinish || !audioFinish || released) {
            return;
        }

//        if (outputSurface != null) {
//            outputSurface.release();
//        }
//        if (inputSurface != null) {
//            inputSurface.release();
//        }
        mVideoExtractor.release();
        mAudioExtractor.release();
        mMediaMuxer.stop();
        mMediaMuxer.release();


        videoDecoder.stop();
        videoDecoder.release();
        videoEncoder.stop();
        videoEncoder.release();
        audioDecoder.stop();
        audioDecoder.release();
        audioEncoder.stop();
        audioEncoder.release();




        released = true;
        after = System.currentTimeMillis();
        System.out.println("cutVideo count1=" + (after - before));
        if (listener != null) {
            listener.onFinish();
        }
    }

    public interface OnVideoCutFinishListener {
        void onFinish();

        void onError();
    }
}
