package com.amazonaws.kinesisvideo.demoapp;



import com.amazonaws.kinesisvideo.demoapp.auth.AuthHelper;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideo;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoAsyncClient;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoPutMedia;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoPutMediaClient;
import com.amazonaws.services.kinesisvideo.PutMediaAckResponseHandler;
import com.amazonaws.services.kinesisvideo.model.AckEvent;
import com.amazonaws.services.kinesisvideo.model.FragmentTimecodeType;
import com.amazonaws.services.kinesisvideo.model.GetDataEndpointRequest;
import com.amazonaws.services.kinesisvideo.model.PutMediaRequest;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.CanvasFrame;
import org.opencv.core.Mat;

import java.util.*;

//import org.apache.commons.lang.serialization.utils;
//import javaio.ByteArrayOutputStream;

//import org.jcodec.common.Codec;
//import org.jcodec.common.Muxer;
//import org.jcodec.common.model.ColorSpace;
//import org.jcodec.containers.mkv.MKVEncoder;
//import org.jcodec.codecs.h264.H264Encoder;
//import org.jcodec.codecs.picture.Picture;
//import org.jcodec.scale.AWTUtil;

import java.util.ArrayList;
import java.util.List;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class opencv_2 {
    private static final String DEFAULT_REGION = "us-west-2";
    private static final String PUT_MEDIA_API = "/putMedia";

    /* the name of the stream */
    private static final String STREAM_NAME = "test-medialive-connection";

    /* sample MKV file */
    private static final String MKV_FILE_PATH = "src/main/resources/data/mkv/clusters.mkv";
    /* max upload bandwidth */
    private static final long MAX_BANDWIDTH_KBPS = 15 * 1024L;

    /* response read timeout */
    private static final int READ_TIMEOUT_IN_MILLIS = 1_000_000;

    /* connect timeout */
    private static final int CONNECTION_TIMEOUT_IN_MILLIS = 10_000;


    public static void main(String[] args)  throws Exception  {

        nu.pattern.OpenCV.loadShared();
        final AmazonKinesisVideo frontendClient = AmazonKinesisVideoAsyncClient.builder()
                .withCredentials(com.amazonaws.kinesisvideo.demoapp.auth.AuthHelper.getSystemPropertiesCredentialsProvider())
                .withRegion(DEFAULT_REGION)
                .build();

        /* this is the endpoint returned by GetDataEndpoint API */
        final String dataEndpoint = frontendClient.getDataEndpoint(
                new GetDataEndpointRequest()
                        .withStreamName(STREAM_NAME)
                        .withAPIName("PUT_MEDIA")).getDataEndpoint();


        // Set up the OpenCVFrameGrabber to capture frames from the webcam
//        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("rtsp://10.50.0.7/live");
        FrameGrabber grabber = new OpenCVFrameGrabber(0);
//        System.out.print("rtsp://10.50.0.7:554/live");
////        Scanner scanner = new Scanner(System.in);
////        String rtspUrl = scanner.nextLine();
//        FFmpegFrameGrabber grabber;
//        grabber = new FFmpegFrameGrabber("rtsp://10.50.0.7/live");
////        grabber.setFormat("h264");
//        grabber.setFrameRate(30);
        grabber.start();

// 0 represents the default webcam


        // 4. Loop to capture frames
//        Frame frame;

        List<Frame> buffer = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        /* send the same MKV file over and over */


        while (true) {
            Frame frame = grabber.grabFrame();
//


//            // 5. Show the CanvasFrame
            buffer.add(frame.clone());


            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime >= 30000){

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                int frame_h = grabber.getImageHeight();
                int frame_w = grabber.getImageWidth();
                FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputStream, frame_w, frame_h);
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setFormat("matroska");
                recorder.setFrameRate(30);
                recorder.start();
//                CanvasFrame canvas = new CanvasFrame("Webcam Display");

//                Iterator<Frame> iterator = buffer.iterator();
                CanvasFrame canvas = new CanvasFrame("Webcam Display");
                for(int i = 0; i<buffer.size(); i++){
                    canvas.setVisible(true);
                    canvas.showImage(buffer.get(i));
                    recorder.record(buffer.get(i));
                }
//                while (iterator.hasNext()) {
//                    Frame b_frame = iterator.next();
//
//                    recorder.record(b_frame);
//                    // Process each frame here
//
////                    canvas.setVisible(true);
////                    canvas.showImage(b_frame);
////                    System.out.println("Frame: " + frame); // Replace with your processing logic
//                }

//                for (Frame bufferedFrame : buffer) {
//
//                // 5. Show the CanvasFrame
////
//
//                    recorder.record(bufferedFrame);
//                }
//                byte[] mkvData = outputStream.toByteArray();
                recorder.stop();
                byte[] combinedMKV = outputStream.toByteArray();
                outputStream.close();




                final URI uri = URI.create(dataEndpoint + PUT_MEDIA_API);

                /* input stream for sample MKV file */
//                final InputStream inputStream = new FileInputStream("output.mkv");
                final InputStream inputStream = new ByteArrayInputStream(combinedMKV);

                /* use a latch for main thread to wait for response to complete */
                final CountDownLatch latch = new CountDownLatch(1);

                /* PutMedia client */
                final AmazonKinesisVideoPutMedia dataClient = AmazonKinesisVideoPutMediaClient.builder()
                        .withRegion(DEFAULT_REGION)
                        .withEndpoint(URI.create(dataEndpoint))
                        .withCredentials(AuthHelper.getSystemPropertiesCredentialsProvider())
                        .withConnectionTimeoutInMillis(CONNECTION_TIMEOUT_IN_MILLIS)
                        .build();

                final PutMediaAckResponseHandler responseHandler = new PutMediaAckResponseHandler()  {
                    @Override
                    public void onAckEvent(AckEvent event) {
                        System.out.println("onAckEvent " + event);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        latch.countDown();
                        System.out.println("onFailure: " + t.getMessage());
                        // TODO: Add your failure handling logic here
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                        latch.countDown();
                    }
                };
                // Read a frame from the video capture


//                final InputStream mkvInputStream_2 = new ByteArrayInputStream(combinedMKV);


                /* start streaming video in a background thread */
                long pstartTime = System.currentTimeMillis();
                dataClient.putMedia(new PutMediaRequest()
                                .withStreamName(STREAM_NAME)
                                .withFragmentTimecodeType(FragmentTimecodeType.RELATIVE)
                                .withPayload(inputStream)
                                .withProducerStartTimestamp(Date.from(Instant.now())),
                        responseHandler);

                /* wait for request/response to complete */
                latch.await();
                long pendTime = System.currentTimeMillis();
                double pelapsedTimeInSeconds = (pendTime - pstartTime) / (double) 1000;
                System.out.println("Process pexecution time: " + pelapsedTimeInSeconds + " seconds");

                buffer.clear();
                startTime = System.currentTimeMillis();
                /* close the client */
                dataClient.close();
            }
            /* actually URI to send PutMedia request */

        }
    }

    public static byte [] convertMatToMKV(Frame frame, int frame_h, int frame_w) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputStream, frame_w, frame_h);
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("matroska");
            recorder.setFrameRate(30);
            recorder.start();
            Frame convertedFrame = frame;
            recorder.record(convertedFrame);
            recorder.stop();
            byte[] mkvData = outputStream.toByteArray();
            return mkvData;
//            return new ByteArrayInputStream(mkvData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Frame convertMatToFrame(Mat frame) {
        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
        return converterToMat.convert(frame);
    }

    // Sample JCodec options (adjust based on your needs)
    private static Map<String, String> getCodecOptionsForJCodec() {
        Map<String, String> options = new HashMap<>();
        options.put("bitrate", "1000000"); // Adjust bitrate (higher = better quality, larger size)
        return options;
    }

}