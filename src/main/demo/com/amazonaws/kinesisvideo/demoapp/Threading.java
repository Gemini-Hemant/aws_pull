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
import org.jcodec.common.model.Picture;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.bytedeco.opencv.opencv_core.*;

import java.io.*;
import java.util.*;

//import org.apache.commons.lang.serialization.utils;
import java.awt.*;
import java.awt.image.BufferedImage;
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
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


public class Threading {
    private static volatile boolean stopStreaming = false;
    public static final String DEFAULT_REGION = "us-west-2";
    public static final String PUT_MEDIA_API = "/putMedia";

    /* the name of the stream */
    public static final String STREAM_NAME = "test-medialive-connection";

    /* sample MKV file */
    public static final String MKV_FILE_PATH = "src/main/resources/data/mkv/clusters.mkv";
    /* max upload bandwidth */
    public static final long MAX_BANDWIDTH_KBPS = 15 * 1024L;

    /* response read timeout */
    public static final int READ_TIMEOUT_IN_MILLIS = 1_000_000;

    /* connect timeout */
    public static final int CONNECTION_TIMEOUT_IN_MILLIS = 10_000;

    public static String IP_ADD = "";

    public static void setIpAdd(String ip){
        IP_ADD = ip;
        System.out.print(IP_ADD);
    }

    public static void main(String[] args) throws Exception {
        streaming();
    }
    public static void stopStreaming() {
        stopStreaming = true;
    }
    public static void streaming()  throws Exception  {

        nu.pattern.OpenCV.loadShared();
        final AmazonKinesisVideo frontendClient = AmazonKinesisVideoAsyncClient.builder()
                .withCredentials(AuthHelper.getSystemPropertiesCredentialsProvider())
                .withRegion(DEFAULT_REGION)
                .build();

        /* this is the endpoint returned by GetDataEndpoint API */
        final String dataEndpoint = frontendClient.getDataEndpoint(
                new GetDataEndpointRequest()
                        .withStreamName(STREAM_NAME)
                        .withAPIName("PUT_MEDIA")).getDataEndpoint();


        // Set up the OpenCVFrameGrabber to capture frames from the webcam
//        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("rtsp://10.50.0.7/live");
//        System.out.print("rtsp://10.50.0.7:554/live");
//        Scanner scanner = new Scanner(System.in);
//        String rtspUrl = scanner.nextLine();
//        FFmpegFrameGrabber grabber;
//        grabber = new FFmpegFrameGrabber(IP_ADD);
////        grabber.setFormat("h264");
//        grabber.setImageHeight(480);
//        grabber.setImageWidth(640);
//        grabber.setFrameRate(30);
//        grabber.start();

        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();
// 0 represents the default webcam


        // 4. Loop to capture frames
//        Frame frame;

        /* PutMedia client */
//        final AmazonKinesisVideoPutMedia dataClient = AmazonKinesisVideoPutMediaClient.builder()
//                .withRegion(DEFAULT_REGION)
//                .withEndpoint(URI.create(dataEndpoint))
//                .withCredentials(AuthHelper.getSystemPropertiesCredentialsProvider())
//                .withConnectionTimeoutInMillis(CONNECTION_TIMEOUT_IN_MILLIS)
//                .build();

        List<Frame> buffer = new ArrayList<>();

        /* send the same MKV file over and over */
        Thread captureThread = new Thread(() -> {
            while (!Thread.interrupted() && !stopStreaming) {
                Frame frame = null;
                try {
                    frame = grabber.grabFrame();
                } catch (FrameGrabber.Exception e) {
                    throw new RuntimeException(e);
                }
                buffer.add(frame.clone());
            }
            try {
                grabber.release();
            } catch (FrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
            System.out.println("Webcam capture thread stopped.");
        });

        // Kinesis Video Streams producer thread
        Thread producerThread = new Thread(() -> {
//            List<Frame> framesToSend = buffer;
            CanvasFrame canvas = new CanvasFrame("Webcam Display");
            while (!Thread.interrupted() && !stopStreaming) {
                if(buffer.size()>200){
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    int n = buffer.size();
                    int frame_h = grabber.getImageHeight();
                    int frame_w = grabber.getImageWidth();
                    FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputStream, frame_w, frame_h);
                    recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                    recorder.setFormat("matroska");
                    recorder.setFrameRate(30);
                    try {
                        recorder.start();
                    } catch (FFmpegFrameRecorder.Exception e) {
                        throw new RuntimeException(e);
                    }


                    for(int i = 0; i<n; i++){
                        canvas.setVisible(true);
                        canvas.showImage(buffer.get(i));

                        try {
                            recorder.record(buffer.get(i));
                        } catch (FFmpegFrameRecorder.Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    buffer.subList(0,n).clear();

                    try {
                        recorder.stop();
                    } catch (FFmpegFrameRecorder.Exception e) {
                        throw new RuntimeException(e);
                    }
                    byte[] combinedMKV = outputStream.toByteArray();
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }


                    final URI uri = URI.create(dataEndpoint + PUT_MEDIA_API);
                    final InputStream inputStream = new ByteArrayInputStream(combinedMKV);
                    final CountDownLatch latch = new CountDownLatch(1);
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

                    dataClient.putMedia(new PutMediaRequest()
                                    .withStreamName(STREAM_NAME)
                                    .withFragmentTimecodeType(FragmentTimecodeType.RELATIVE)
                                    .withPayload(inputStream)
                                    .withProducerStartTimestamp(Date.from(Instant.now())),
                            responseHandler);

                    /* wait for request/response to complete */
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    dataClient.close();
                }
            }
            try {
                grabber.release();
            } catch (FrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }

            buffer.clear();

            System.out.println("Kinesis Video Streams producer thread stopped.");
        });

        captureThread.start();
        producerThread.start();

        /* actually URI to send PutMedia request */


    }



}

