package com.alexcassells.yachttracker.services;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.alexcassells.yachttracker.MainActivity;
import com.alexcassells.yachttracker.data.YachtTrackRepository;
import com.alexcassells.yachttracker.R;
import com.alexcassells.yachttracker.data.entities.TrackDataPoint;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by alexcassells on 07/06/2015.
 */
public class YachtLoggingService extends Service
{

    private final static String END_PONT = "https://endpoint.somewhere.on.the.web";

    private float[] accelerometerValues, magneticFieldValues;

    private float azimuth, pitch, roll;

    private NotificationManager notificationManager;

    private Notification.Builder notificationBuilder;

    private YachtTrackRepository repository;

    private final int notificationId = 1001;

    private int pointsLogged = 0;

    private boolean serviceIsStopping = false;

    SensorManager sensorManager;
    LocationManager locationManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        repository = new YachtTrackRepository();

        // Intialize the GPS listener
        this.registerGPSListeners();

        // Intialize the magnetic and gyro sensors
        this.registerSensorListeners();

        // Initialize the notification bar
        this.initializeNotificationBar();

        // Start the service in the foreground state with initialized notification builder
        startForeground(notificationId, notificationBuilder.build());

        // TODO uncomment this for uploads
        uploadData();

        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        serviceIsStopping = true;
        unregisterGPSListeners();
        unregisterSensorListeners();
    }

    private void initializeNotificationBar() {
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mainActivityIntent, 0);

        notificationBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setContentTitle("Recording race points")
                .setContentText("Recorded 0 points")
                .setTicker("Started recording race points")
                .setContentIntent(pendingIntent);
    }

    private void registerSensorListeners() {
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(magneticFieldListener, magneticFieldSensor, SensorManager.SENSOR_DELAY_UI);
    }

    private void unregisterSensorListeners() {
        sensorManager.unregisterListener(accelerometerListener);
        sensorManager.unregisterListener(magneticFieldListener);
    }

    private void registerGPSListeners() {

        String svcName = Context.LOCATION_SERVICE;
        locationManager = (LocationManager)getSystemService(svcName);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String provider = locationManager.getBestProvider(criteria, true);

        // One update every 1000 milliseconds
        long minTimeInMillisBetweenUpdates = 1000;

        // Update and log a point even if no distance changed
        float minDistanceChangedPerUpdate = 0;

        locationManager.requestLocationUpdates(provider, minTimeInMillisBetweenUpdates, minDistanceChangedPerUpdate, locationListener);
    }

    private void unregisterGPSListeners() {
        locationManager.removeUpdates(locationListener);
    }

    private void setNotification() {
        // TODO must be a nicer way of doing this than string concats
        notificationBuilder.setContentText("Recorded " + pointsLogged + " points");
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void setAzimuthPitchAndRoll() {
        float[] values = new float[3];
        float[] matrix = new float[9];
        if (accelerometerValues == null || magneticFieldValues == null) {
            return;
        } else {
            SensorManager.getRotationMatrix(matrix, null, accelerometerValues, magneticFieldValues);
            SensorManager.getOrientation(matrix, values);

            azimuth = (float) Math.toDegrees(values[0]);
            pitch = (float) Math.toDegrees(values[1]);
            roll = (float) Math.toDegrees(values[2]);
        }
    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    // BELOW THIS IS LOCATION STUFF
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            setAzimuthPitchAndRoll();
            repository.SaveTrackingPoint(location, azimuth, pitch, roll);
            pointsLogged ++;
            setNotification();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    // BELOW THIS IS SENSOR STUFF
    final SensorEventListener accelerometerListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accelerometerValues = event.values;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // NOT DOING ANYTHING HERE YET
        }
    };

    final SensorEventListener magneticFieldListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticFieldValues = event.values;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // NOT DOING ANYTHING HERE YET
        }
    };

    public Boolean internetIsAvailable() {
        try {
            Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1    www.google.com");
            int returnVal = p1.waitFor();
            boolean reachable = (returnVal==0);
            return reachable;
        } catch (Exception e) {
        }
        return false;
    }


    void uploadData() {

        Thread thread = new Thread() {

            @Override public void run() {
                final OkHttpClient client = new OkHttpClient();

                try {
                    while (!serviceIsStopping) {



                        // TODO check network state
                        if (internetIsAvailable()) {


                            TrackDataPoint latestPoint = repository.getLatestTrackingPoint();
                            if (latestPoint != null) {
                                RequestBody formBody = new FormEncodingBuilder()
                                        .add("latitude", Double.toString(latestPoint.latitude))
                                        .add("longitude", Double.toString(latestPoint.longitude))
                                        .add("altitude", Double.toString(latestPoint.altitude))
                                        .add("gpsSpeed", Float.toString(latestPoint.gpsSpeed))
                                        .add("gpsBearing", Float.toString(latestPoint.gpsBearing))
                                        .add("gpsAccuracy", Float.toString(latestPoint.gpsAccuracy))
                                        .add("gpsTime", Long.toString(latestPoint.gpsTime))
                                        .add("azimuth", Float.toString(latestPoint.azimuth))
                                        .add("pitch", Float.toString(latestPoint.pitch))
                                        .add("roll", Float.toString(latestPoint.roll))
                                        .build();


                                Request request = new Request.Builder()
                                        .url(END_PONT)
                                        .post(formBody)
                                        .build();

                                Response response = client.newCall(request).execute();

                                if (response.code() == 201) {
                                    repository.deleteTrackingPoint(latestPoint.getId());
                                }
                                Log.d("SERVICE", "RESPONSE CODE " + response.code());
                                Log.d("SERVICE", response.body().string());
                            }

                            sleep(250);
                        } else {
                            // no internet sleep for a long time
                            sleep(10000);
                        }
                    }
                } catch (InterruptedException e) {
                    // TODO determine what to do here, need some sort of recovery.
                    Log.d("SERVICE ISSUE", e.toString());
                } catch (IOException e) {
                    // TODO determine what to do here, need some sort of recovery.
                    Log.d("SERVICE ISSUE", e.toString());
                }
            }

        };

        thread.start();
    }


}
