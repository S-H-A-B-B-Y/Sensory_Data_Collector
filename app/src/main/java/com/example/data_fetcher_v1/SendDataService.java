package com.example.data_fetcher_v1;

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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class SendDataService extends Service implements SensorEventListener {

    private static final String TAG = "SendData_Activity";
    private static final String RABBITMQ_QUEUE_NAME = "sensor_data_queue";
    private static final String RABBITMQ_USERNAME = "test";
    private static final String RABBITMQ_PASSWORD = "test";
    //private static final String RABBITMQ_HOST = "Sensors"; // replace with your RabbitMQ host

    private SensorManager sensorManager;
    private List<ParcelableSensor> selectedSensors;
    private ExecutorService executorService;

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    private String serverAddress;
    private int serverPort;
    private String userId;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "started activity");
        if (intent != null) {
            String action = intent.getAction();
            if ("START_SENDING".equals(action)) {
                sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                selectedSensors = intent.getParcelableArrayListExtra("selected_sensors");

                serverAddress = intent.getStringExtra("server_address");
                serverPort = intent.getIntExtra("server_port", -1);
                userId =intent.getStringExtra("userId");
                // Initialize RabbitMQ
                //initializeRabbitMQ();
                // Initialize RabbitMQ using AsyncTask
                new InitializeRabbitMQTask().execute();


                // Start sensor reading
                //startSensorReading();

                // Show a toast message to indicate that the service is started
                Toast.makeText(this, "Sending data to server started", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Data Sending Started");

            } else if ("STOP_SENDING".equals(action)) {
                // Show a toast message to indicate that the service is stopped
                Toast.makeText(this, "Sending data to server stopped", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Data Sending Stopped");

                // Stop sensor reading
                stopSensorReading();

                // Close RabbitMQ connection
                closeRabbitMQ();

                stopSelf();
            }
        }

        return START_NOT_STICKY; // Service will not be restarted if it's stopped by the system
    }

    private void initializeRabbitMQ() {
        factory = new ConnectionFactory();
        factory.setHost(serverAddress);
        factory.setPort(serverPort);
        factory.setUsername(RABBITMQ_USERNAME);
        factory.setPassword(RABBITMQ_PASSWORD);

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(RABBITMQ_QUEUE_NAME, true, false, false, null);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to initialize RabbitMQ", e);
        }
    }

    private void closeRabbitMQ() {
        try {
            if (channel != null) {
                channel.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to close RabbitMQ", e);
        }
    }

    private void startSensorReading() {
        //executorService = Executors.newFixedThreadPool(selectedSensors.size());
        executorService = Executors.newFixedThreadPool(3);
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
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private String buildSensorDataString(String sensorType, float[] values) {
        StringBuilder builder = new StringBuilder("Source: Smart_Phone")
                .append(", User ID: ").append(userId) // Add user_id here if needed
                .append(", Timestamp: ").append(System.currentTimeMillis())
                .append(", Sensor Type: ").append(sensorType).append(": ");

        for (float value : values) {
            builder.append(value).append(", ");
        }
        builder.delete(builder.length() - 2, builder.length());
        builder.append("\n");
        return builder.toString();
    }

    private void sendDataToRabbitMQ(String sensorType, float[] sensorValues) {
        String sensorData = buildSensorDataString(sensorType, sensorValues);

        executorService.submit(() -> {
            try {
                channel.basicPublish("", RABBITMQ_QUEUE_NAME, null, sensorData.getBytes());
                Log.d(TAG, "Sensor Data Sent: " + sensorData);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to send sensor data to RabbitMQ", e);
            }
        });
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] sensorValues = event.values;

            for (ParcelableSensor sensor : selectedSensors) {
                if (event.sensor.getType() == sensor.getType()) {
                    sendDataToRabbitMQ(sensor.getName(), sensorValues);
                    break;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Handle accuracy changes if needed
        }
    };

    private class InitializeRabbitMQTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            factory = new ConnectionFactory();
            factory.setHost(serverAddress);
            factory.setPort(serverPort);
            factory.setUsername(RABBITMQ_USERNAME);
            factory.setPassword(RABBITMQ_PASSWORD);

            try {
                connection = factory.newConnection();
                channel = connection.createChannel();
                channel.queueDeclare(RABBITMQ_QUEUE_NAME, true, false, false, null);
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to initialize RabbitMQ", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Start sensor reading after RabbitMQ initialization
            startSensorReading();
            Log.d(TAG, "RabbitMQ initialized and sensor reading started");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Not used
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }
}
