package com.example.data_fetcher_v1;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.List;

public class SendDataService extends Service implements SensorEventListener {

    private static final int SERVER_PORT = 12345;

    private SensorManager sensorManager;
    private static final String TAG = "SendData_Activity";

    static String userId;
    static String serverAddress;
    List<ParcelableSensor> selectedSensors;
    List<Sensor> sensorList;
    private static boolean isSending = false;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SendDataService", "started activity");
        if (intent != null) {
            String action = intent.getAction();
            if ("START_SENDING".equals(action)) {
                sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                userId = intent.getStringExtra("user_id");
                serverAddress = intent.getStringExtra("server_address");
                selectedSensors = intent.getParcelableArrayListExtra("selected_sensors");

                startSensorReading();
                // Show a toast message to indicate that the service is started
                Toast.makeText(this, "Sending data to server started", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Data Sending Started");

                // Start sending data to server
                isSending = true;
                //new SendSensorDataTask().execute(userId, serverAddress, selectedSensors);
            } else if ("STOP_SENDING".equals(action)) {

                //stopSensorListeners();
                // Show a toast message to indicate that the service is stopped
                Toast.makeText(this, "Sending data to server stopped", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Data Sending Stopped");

                // Stop sending data
                isSending = false;
                stopSensorReading();
                stopSelf();
            }
        }

        return START_NOT_STICKY; // Service will not be restarted if it's stopped by the system
    }

    private void startSensorReading() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        for (ParcelableSensor parcelableSensor : selectedSensors) {
            Sensor sensor = sensorManager.getDefaultSensor(parcelableSensor.getType());
            if (sensor != null) {
                sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    private void stopSensorReading() {
        sensorManager.unregisterListener(sensorEventListener);
    }

    private String buildSensorDataString(String sensorType, float[] values) {
        // Format sensor data string as needed for the server
        // Adding user ID and timestamp at the beginning
        StringBuilder builder = new StringBuilder("Source: Smart_Phone").append(", User ID: ").append(userId).append(", Timestamp: ").append(System.currentTimeMillis()).append(", ");

        // Adding sensor type
        builder.append("Sensor Type: " + sensorType).append(": ");

        for (float value : values) {
            builder.append(value).append(", ");
        }
        builder.delete(builder.length() - 2, builder.length());
        builder.append("\n");
        return builder.toString();
    }

    private void sendDataToServer(String sensorType, float[] sensorValues) {
        String sensorData = buildSensorDataString(sensorType, sensorValues);

        Log.d("SensorDataService", "Sensor Data: " + sensorData);

        // Replace "your_server_host" and "your_server_port" with the actual server's IP address/host and port
        //String serverUrl = "http://192.168.18.92:8080/HAR_Server/StatusCheckServlet";
        String serverUrl = "http://" + serverAddress + ":8080/HAR_Server/StatusCheckServlet";
        //"192.168.60.99:8080
        // Check the server status before sending data
        try {
            // Check the server status
            boolean isServerRunning = new CheckServerStatusTask().execute(serverUrl).get();
            Log.d("Server Status", " " + isServerRunning);
            if (isServerRunning) {
                // Server is running, proceed to send sensor data
                Log.d("Status", "Data Sent");
                new SendSensorDataTask().execute(sensorData);
            } else {
                Log.d("Status", "Data not sent Server unavailable");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Process sensor data here
            float[] sensorValues = event.values;

            for (ParcelableSensor sensor : selectedSensors) {
                if (event.sensor.getType() == sensor.getType()) {
                    sendDataToServer(sensor.getName(), sensorValues);
                    break;
                }
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Handle accuracy changes if needed
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    class CheckServerStatusTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            String serverUrl = params[0];

            try {
                URL url = new URL(serverUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();

                // Assuming a specific response code indicates that the server is running (e.g., 200 OK)
                Log.d("Server Res", "" + responseCode);
                return responseCode == HttpURLConnection.HTTP_OK;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

        }
    }

    class SendSensorDataTask extends AsyncTask<String, Void, Void> {

        Socket socket = null;

        @Override
        protected Void doInBackground(String... params) {
            //int responseCode = -1;
            try {
                socket = new Socket(serverAddress, SERVER_PORT);


                OutputStream outputStream = socket.getOutputStream();

                //Log.d("Sensor", "Sensor Data: Before BufferedWriter" );
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                writer.write(params[0]);
                writer.flush();

                System.out.println("Data Sent");

                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
            //return responseCode;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
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
                case Sensor.TYPE_MAGNETIC_FIELD:
                    sensorName = "Magnetometer";
                    break;
                default:
                    sensorName = "UNKNOWN";
                    break;
            }
            return sensorName;

        }

    }
}