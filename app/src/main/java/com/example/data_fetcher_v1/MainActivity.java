package com.example.data_fetcher_v1;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private EditText portEditText, serverAddressEditText,userIdEditText;
    private static final String TAG = "Main_Activity";

    private static String serverAddress;
    private static String port ;
    private static String userId ;

    private static AsyncTask<Void, Void, Boolean> network;


    private List<ParcelableSensor> selectedSensors = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        portEditText = findViewById(R.id.edit_text_port);
        serverAddressEditText = findViewById(R.id.edit_text_server_address);
        userIdEditText = findViewById(R.id.edit_text_userId);
        Button sensorSelectionButton = findViewById(R.id.button_sensor_selection);
        sensorSelectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSensorSelectionActivity();
            }
        });

        Button startStopButton = findViewById(R.id.button_start_stop);
        startStopButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    try {
                        startRecording();
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    stopRecording();
                }
            }
        });
    }

    private static final int SENSOR_SELECTION_REQUEST_CODE = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SENSOR_SELECTION_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                selectedSensors.clear();
                selectedSensors.addAll(Objects.requireNonNull(data.getParcelableArrayListExtra(SensorSelectionActivity.EXTRA_SELECTED_SENSORS)));
            }
        }
    }

    private void openSensorSelectionActivity() {
        Intent intent = new Intent(this, SensorSelectionActivity.class);
        startActivityForResult(intent, SENSOR_SELECTION_REQUEST_CODE);
    }

    private boolean isRecording = false;

    private void startRecording() throws ExecutionException, InterruptedException {
        serverAddress = serverAddressEditText.getText().toString();
        port = portEditText.getText().toString();
        userId=userIdEditText.getText().toString();

        // Check if connected to Wi-Fi
        if (!isConnectedToWifi()) {
            Toast.makeText(this, "Please connect to Wi-Fi to proceed", Toast.LENGTH_SHORT).show();
            return;
        }
        if (serverAddress.isEmpty() || port.isEmpty()) {
            Toast.makeText(this, "Please enter Server Address and Port", Toast.LENGTH_SHORT).show();
            return;
        }
        /*try {
            network=new CheckNetworkTask().execute();
            boolean isNetworkReachable = network.get(); // This will wait for the AsyncTask to finish and retrieve its result
            if (!isNetworkReachable) {
                // Handle when URL is not reachable
                Toast.makeText(this, "Server not reachable", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            // Handle the exception
            return;
        }*/
        // Start the SendDataService to send data to server
        Intent intent = new Intent(this, SendDataService.class);
        intent.setAction("START_SENDING");
        intent.putExtra("server_address", serverAddress);
        intent.putExtra("port", port);
        intent.putExtra("userId", userId);
        intent.putParcelableArrayListExtra("selected_sensors", new ArrayList<>(selectedSensors));
        startService(intent);

        // Update recording status
        isRecording = true;
        // Update button text
        Button startStopButton = findViewById(R.id.button_start_stop);
        startStopButton.setText("Stop");
        Log.d(TAG,"Recording Started");
    }

    private void stopRecording() {
        // Stop the service to stop sending data
        Intent intent = new Intent(this, SendDataService.class);
        intent.setAction("STOP_SENDING");
        startService(intent);

        // Update recording status
        isRecording = false;
        // Update button text
        Button startStopButton = findViewById(R.id.button_start_stop);
        startStopButton.setText("Start");
        Log.d(TAG,"Recording Stopped");
    }

    private class CheckNetworkTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            return isURLReachable(MainActivity.this);
        }

        @Override
        protected void onPostExecute(Boolean result) {
        }
         public boolean isURLReachable(Context context) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()) {
                try {
                    URL url = new URL("http://"+serverAddress+":"+port);   // Change to "http://google.com" for www  test.
                    HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                    urlc.setConnectTimeout(5 * 1000);          // 5 s.
                    urlc.connect();
                    Log.wtf( "isURLReachable: ","Server Response " +urlc.getResponseCode());
                    if (urlc.getResponseCode() != 404) {        // 404 = "Server not found" because at this address we have nothing to return in request that's why 404 code (http connection is fine).
                        Log.wtf("Connection", "Success !");
                        return true;
                    } else {
                        return false;
                    }
                } catch (MalformedURLException e1) {
                    return false;
                } catch (IOException e) {
                    return false;
                }
            }
            return false;
        }
    }
    private boolean isConnectedToWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }
}
