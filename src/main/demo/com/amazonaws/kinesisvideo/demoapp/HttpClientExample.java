package com.amazonaws.kinesisvideo.demoapp;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.core.Mat;

import java.io.*;
import java.io.IOException;
import java.net.*;


public class HttpClientExample{

    public static void main(String[] args) throws Exception {
        start_recording();
    }
    private static Frame convertMatToFrame(Mat frame) {
        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
        return converterToMat.convert(frame);
    }
    public static void start_recording() throws IOException, InterruptedException {
        String url = "http://10.50.0.7/cgi-bin/foream_remote_control?list_files=/tmp/SD0/DCIM"; // Replace with your API endpoint
        String method = "GET"; // Adjust for POST, PUT, etc.

        URL requestUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestMethod(method);

        int responseCode = connection.getResponseCode();
        System.out.println("Sending '" + method + "' request to URL : " + url);
        System.out.println("Status Code: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) { // Handle successful responses
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            System.out.println("Response body: " + response.toString());
        } else {
            System.out.println("Error: Server returned HTTP response code: " + responseCode);
        }
    }
    public static void stop_recording() throws IOException, InterruptedException {
        String url = "http://10.50.0.7/cgi-bin/foream_remote_control?stop_record"; // Replace with your API endpoint
        String method = "GET"; // Adjust for POST, PUT, etc.

        URL requestUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestMethod(method);

        int responseCode = connection.getResponseCode();
        System.out.println("Sending '" + method + "' request to URL : " + url);
        System.out.println("Status Code: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) { // Handle successful responses
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            System.out.println("Response body: " + response.toString());
        } else { // Handle errors
            System.out.println("Error: Server returned HTTP response code: " + responseCode);
        }
    }
    public static void download_video(String path) throws IOException {


        // Make the API request (refer to previous responses for request logic)
        // ... (Replace with your API request code)

        // Assuming the API response contains a "download_url" field
        String downloadUrl = "http://10.50.0.7/DCIM/"+path/* Extract download URL from API response */;

        if (downloadUrl == null || downloadUrl.isEmpty()) {
            throw new RuntimeException("Download URL not found in API response");
        }

        String fileName = "downloaded_video.mkv"; // Adjust filename as needed

        URL url = new URL(downloadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET"); // Adjust if API requires a different method

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream out = new FileOutputStream(fileName)) {

                byte[] data = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(data)) != -1) {
                    out.write(data, 0, bytesRead);
                }
                System.out.println("Video downloaded successfully: " + fileName);
            }
        } else {
            System.out.println("Error downloading video: HTTP response code " + responseCode);
        }

        connection.disconnect();
    }





}
