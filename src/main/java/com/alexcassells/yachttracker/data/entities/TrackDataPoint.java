package com.alexcassells.yachttracker.data.entities;

import android.location.Location;

import com.orm.SugarRecord;

/**
 * Created by alexcassells on 07/06/2015.
 */
public class TrackDataPoint extends SugarRecord<TrackDataPoint> {

    public double latitude;
    public double longitude;
    public double altitude;
    public float gpsSpeed;
    public float gpsBearing;
    public float gpsAccuracy;
    public long gpsTime;

    public float azimuth;
    public float pitch;
    public float roll;

    public TrackDataPoint() {}

    public TrackDataPoint(Location location, float azimuth, float pitch, float roll) {

        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.altitude = location.hasAltitude() ? location.getAltitude() : Double.NaN;
        this.gpsSpeed = location.hasSpeed() ? location.getSpeed() : Float.NaN;
        this.gpsBearing = location.hasBearing() ? location.getBearing() : Float.NaN;
        this.gpsAccuracy = location.hasAccuracy() ? location.getAccuracy() : Float.NaN;
        this.gpsTime = location.getTime();

        this.azimuth = azimuth;
        this.pitch = pitch;
        this.roll = roll;
    }

    // TODO need to have some relationship to the Yacht in this ??

    // TODO need to have some relationship to the Track in this ??

    // TODO possibly UUID to identify unique point ??
}
