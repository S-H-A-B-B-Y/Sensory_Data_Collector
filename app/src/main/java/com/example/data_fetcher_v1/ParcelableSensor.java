package com.example.data_fetcher_v1;

import android.hardware.Sensor;
import android.os.Parcel;
import android.os.Parcelable;

public class ParcelableSensor implements Parcelable {
    private int type;
    private String name;
    private Sensor sensor;
    private float[] values;

    public float[] getValues() {
        return values;
    }

    // Add a constructor to initialize the Sensor object
    public ParcelableSensor(Sensor sensor) {
        this.sensor = sensor;
        this.type = sensor.getType();
        this.name = sensor.getName();
    }
    public ParcelableSensor(int type, String name) {
        this.type = type;
        this.name = name;
    }

    protected ParcelableSensor(Parcel in) {
        type = in.readInt();
        name = in.readString();
    }

    public static final Creator<ParcelableSensor> CREATOR = new Creator<ParcelableSensor>() {
        @Override
        public ParcelableSensor createFromParcel(Parcel in) {
            return new ParcelableSensor(in);
        }

        @Override
        public ParcelableSensor[] newArray(int size) {
            return new ParcelableSensor[size];
        }
    };

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Sensor getSensor() {
        return sensor;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeString(name);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
