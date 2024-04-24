package com.amazonaws.kinesisvideo.demoapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;
import com.amazonaws.kinesisvideo.demoapp.Threading;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;


public class Controler {
    private boolean isStreaming = false;
    private Thread streamingThread;
    @FXML
    TextField ip_add;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private Label ip_label;

    @FXML
    public void login(){
        String ip_str = ip_add.getText();
        Threading.setIpAdd(ip_str);
        ip_label.setText("IP: "+ip_str);
    }



    @FXML
    public void start_recording() throws IOException, InterruptedException {
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
        } else { // Handle errors
            System.out.println("Error: Server returned HTTP response code: " + responseCode);
        }
    }
    public void stop_recording() throws IOException, InterruptedException {
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
    public void stream_recording(){
        System.out.println("stream recording");
    }
    public void stream_live(){
        try {
            Threading.streaming();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void stop_live_stream(){
        try{
            Threading.stopStreaming();
        }
        catch (Exception e){
            throw  new RuntimeException(e);
        }
    }

    public void initialize() {
        startButton.setOnAction(event -> {
            if (!isStreaming) {
                isStreaming = true;
                streamingThread = new Thread(() -> {
                    try {
                        Threading.streaming();
                    } catch (Exception e) {
                        e.printStackTrace(); // Handle exception appropriately
                    }
                });
                streamingThread.start();
            }
        });

        stopButton.setOnAction(event -> {
            if (isStreaming) {
                isStreaming = false;
                Threading.stopStreaming();
            }
        });
    }
}
