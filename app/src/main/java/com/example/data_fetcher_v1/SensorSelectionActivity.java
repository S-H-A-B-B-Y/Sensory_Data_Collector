package com.example.data_fetcher_v1;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
public class SensorSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_SENSORS = "selected_sensors";

    private SensorAdapter sensorAdapter;
    private List<ParcelableSensor> sensorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_selection);

        RecyclerView recyclerView = findViewById(R.id.recycler_view_sensors);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get the list of available sensors
        sensorList = getAvailableSensors();

        // Set up the RecyclerView with the SensorAdapter
        sensorAdapter = new SensorAdapter(this, sensorList);
        recyclerView.setAdapter(sensorAdapter);

        Button okButton = findViewById(R.id.button_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<ParcelableSensor> selectedSensors = sensorAdapter.getSelectedSensors();
                if (selectedSensors.isEmpty()) {
                    Toast.makeText(SensorSelectionActivity.this, "Please select at least one sensor", Toast.LENGTH_SHORT).show();
                } else {
                    // Construct a string representation of selected sensors
                    StringBuilder selectedSensorsStringBuilder = new StringBuilder("Selected Sensors:\n");
                    for (ParcelableSensor sensor : selectedSensors) {
                        selectedSensorsStringBuilder.append(sensor.getName()).append("\n");
                    }
                    // Show toast with the list of selected sensors
                    Toast.makeText(SensorSelectionActivity.this, selectedSensorsStringBuilder.toString(), Toast.LENGTH_LONG).show();

                    // Return selected sensors to MainActivity
                    Intent resultIntent = new Intent();
                    resultIntent.putParcelableArrayListExtra(EXTRA_SELECTED_SENSORS, new ArrayList<>(selectedSensors));
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            }
        });

    }

    // Method to get a list of available sensors (for demonstration purposes)
    private List<ParcelableSensor> getAvailableSensors() {
        List<ParcelableSensor> sensors = new ArrayList<>();
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);

        for (Sensor sensor : sensorList) {
            ParcelableSensor parcelableSensor = new ParcelableSensor(sensor.getType(), sensor.getName());
            //ParcelableSensor parcelableSensor = new ParcelableSensor(sensor);
            sensors.add(parcelableSensor);
        }

        return sensors;
    }

}
