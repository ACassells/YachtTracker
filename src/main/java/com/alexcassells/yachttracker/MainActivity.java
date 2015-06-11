package com.alexcassells.yachttracker;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alexcassells.yachttracker.services.YachtLoggingService;


public class MainActivity extends ActionBarActivity {


    private float[] accelerometerValues, magneticFieldValues;

    private float azimuth, pitch, roll;

    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTrackingService(v);
            }
        });

        final Button stopButton = (Button)findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTrackingService(v);
            }
        });


        // BELOW THIS IS ALL LOCATION STUFF, NEED TO PUSH THIS OFF TO THE SERVICE

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


        // BELOW THIS IS THE SENSOR STUFF, NEED TO PUSH THIS OFF TO A SERVICE

        SensorManager sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor accSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor mfSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sm.registerListener(accelerometerListener, accSensor, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(magneticFieldListener, mfSensor, SensorManager.SENSOR_DELAY_UI);

    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    // BELOW THIS IS SENSOR STUFF
    final SensorEventListener accelerometerListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accelerometerValues = event.values;
                updateOrientationPitchAndRoll();
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
                updateOrientationPitchAndRoll();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // NOT DOING ANYTHING HERE YET
        }
    };


    // OK SO THIS NEEDS TO DO SOMETHING ELSE
    private void updateOrientationPitchAndRoll() {
        float[] values = new float[3];
        float[] matrix = new float[9];
        if (accelerometerValues == null || magneticFieldValues == null) {
            return;
        } else {
            SensorManager.getRotationMatrix(matrix, null, accelerometerValues, magneticFieldValues);
            SensorManager.getOrientation(matrix, values);

            TextView azimuthText = (TextView) findViewById(R.id.azimuth);
            TextView pitchText = (TextView) findViewById(R.id.pitch);
            TextView rollText = (TextView) findViewById(R.id.roll);

            azimuth = (float) Math.toDegrees(values[0]);
            pitch = (float) Math.toDegrees(values[1]);
            roll = (float) Math.toDegrees(values[2]);

            azimuthText.setText("Azimuth " + azimuth);
            pitchText.setText("Pitch " + pitch);
            rollText.setText("Roll " + roll);
        }
    }



    // BELOW THIS IS LOCATION STUFF
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            updateWithNewLocation(location);

            //(new TrackDataPoint(location, azimuth, pitch, roll)).save();
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

    private void updateWithNewLocation(Location location) {
        TextView myLocationText = (TextView)findViewById(R.id.myLocationText);
        String latLonString = "No location found";
        if (location != null) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            latLonString = "Lat: " + lat + "\nLon: " + lon;
        }
        myLocationText.setText("Your current position is:  \n" + latLonString );
    }


    // GENERIC ACTIVITY STUFF
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void startTrackingService(View view)
    {
        if (!isMyServiceRunning(YachtLoggingService.class)) {
            startService(new Intent(this, YachtLoggingService.class));
        }
    }

    public void stopTrackingService(View view)
    {
        if (isMyServiceRunning(YachtLoggingService.class)) {
            stopService(new Intent(this, YachtLoggingService.class));
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
