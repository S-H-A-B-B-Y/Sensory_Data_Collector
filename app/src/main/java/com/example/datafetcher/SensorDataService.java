package com.example.datafetcher;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SensorDataService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private List<Sensor> sensorList;
    private String userId;
    private String serverUrl;
    // Add other necessary variables

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        // Initialize other variables if needed
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        userId = intent.getStringExtra("user_id");
        serverUrl = intent.getStringExtra("server_url");
        startSensorListeners();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopSensorListeners();
        super.onDestroy();
    }

    private void startSensorListeners() {
        for (Sensor sensor : sensorList) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void stopSensorListeners() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Process sensor data and send it to the server
        // You need to implement this part based on your server communication logic

        float[] values = event.values;
        String sensorType = getSensorType(event.sensor.getType());
        String sensorData = buildSensorDataString(sensorType, values);

        //Log.d("SensorDataService", "Sensor Data: " + sensorData);

        // Now, you can send this data to the server using a network library
        sendSensorDataToServer(sensorData);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }
    private void sendSensorDataToServer(String data) {
        try {
            URL url = new URL(serverUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            // Set the request method to POST
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");

            // Enable input/output streams
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);

            // Write data to the server
            DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream());
            outputStream.writeBytes(data);
            outputStream.flush();
            outputStream.close();

            // Get the response from the server (optional)
            int responseCode = urlConnection.getResponseCode();
            Log.d("SensorDataService", "Server Response Code: " + responseCode);

            urlConnection.disconnect();
        } catch (Exception e) {
            Log.e("SensorDataService", "Error sending data to server: " + e.getMessage());
        }
    }
    private String getSensorType(int sensorType) {
        String sensorName = "";

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                sensorName = "ACCELEROMETER";
                break;
            case Sensor.TYPE_GYROSCOPE:
                sensorName = "GYROSCOPE";
                break;
            default:
                sensorName = "UNKNOWN";
                break;
        }

        // Assuming sensor names are stored as strings
        if (!sensorName.equals("UNKNOWN")) {
            return sensorName;
        }

        // If the sensor type is unknown, try to get the sensor name dynamically
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensorList = sensorManager.getSensorList(sensorType);

        if (!sensorList.isEmpty()) {
            Sensor sensor = sensorList.get(0);
            return sensor.getName().toUpperCase(); // Assuming the sensor names are in uppercase
        } else {
            return "UNKNOWN";
        }
    }


    private String buildSensorDataString(String sensorType, float[] values) {
        // Format sensor data string as needed for the server
        // Adding user ID and timestamp at the beginning
        StringBuilder builder = new StringBuilder("Source: Smart Phone").append(", User ID: ").append(userId).append(", Timestamp: ").append(System.currentTimeMillis()).append(", ");

        // Adding sensor type
        builder.append(sensorType).append(": ");

        for (float value : values) {
            builder.append(value).append(", ");
        }
        return builder.toString();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
