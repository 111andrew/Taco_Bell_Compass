
package com.example.compass;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import android.location.LocationListener;
import android.widget.Toast;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener, DialogClass.ExampleDialogListener {

    ImageView img_compass;
    TextView txt_azimuth;
    int mAzimuth;
    private SensorManager mSensorManager;
    //tries out 3 sensors depending on phone capabilities
    private Sensor mRotationV, mAccelerometer, mMagnetometer;
    boolean haveSensor = false, haveSensor2 = false;
    float[] rMat = new float[9];
    float[] orientation = new float[3];
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    //set default to point to north pole
    float goalLong = 0;
    float goalLat = 90;
    double currentLong;
    double currentLat;

    float tacoLong = 0;
    float tacoLat = 90;

    String selectedItemText;

    LocationManager locationManager;
    TextView locationText;

    Spinner locationSelection;

    String currentAddressText;

    @Override
    public void applyCustom(float latitude, float longitude) {
        goalLong = longitude;
        goalLat = latitude;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        img_compass = (ImageView) findViewById(R.id.img_compass);
        //img_compass.setAlpha(127);
        //img_compass.setColorFilter(Color.BLACK, PorterDuff.Mode.CLEAR);
        txt_azimuth = (TextView) findViewById(R.id.txt_azimuth);

        locationText = (TextView) findViewById(R.id.locationText);

        locationSelection = (Spinner) findViewById(R.id.spinnerSelection);

        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.locations));
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSelection.setAdapter(myAdapter);

        //ask for and check permission on gps and internet
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);

        }

        //starts the compassing
        start();

        locationSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                selectedItemText = (String) adapterView.getItemAtPosition(i);

                if (i == 0) { //FIXXXXXXX
                    goalLong = 0;
                    goalLat = 90;
                } else if (i == 1) {
                    goalLong = 0;
                    goalLat = -90;
                } else if (i == 2) {
                    new webScrape().execute();
                    goalLong = tacoLong;
                    goalLat = tacoLat;
                } else if (i ==3){
                    goalLong = -83;
                    goalLat = 42;
                }
                else if (i ==4){
                    // custom lat long
                    openDialog();
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public void openDialog(){
        DialogClass exampleDialog = new DialogClass();
        exampleDialog.show(getSupportFragmentManager(), "example dialog");

    }

    //function gets the location through location manager
    void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    //override what happens when your location changes
    @Override
    public void onLocationChanged(Location location) {
        // locationText.setText("Latitude: " + location.getLatitude() + "\n Longitude: " + location.getLongitude());
        currentLat = (float) location.getLatitude();
        currentLong = (float) location.getLongitude();
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            //locationText.setText(locationText.getText() + "\n"+addresses.get(0).getAddressLine(0)+", "+
            //      addresses.get(0).getAddressLine(1)+", "+addresses.get(0).getAddressLine(2));
            //hopefully street, city, zip and only gets city and zip?
            String[] tempAddress = addresses.get(0).getAddressLine(0).split(",");
            currentAddressText = tempAddress[1] + tempAddress[2];
        } catch (Exception e) {

        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(MainActivity.this, "Please Enable GPS and Internet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //add to get location whenever sensor changes
        getLocation();

        double yTemp = 0;
        double xTemp = 0;
        double delta_psi = 0;
        double delta_lambda = 0;
        // radius of earth in meters
        double rad_earth = 6371000;
        double temp_a = 0;
        double temp_c = 0;
        float distance = 0;

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rMat, event.values);
            yTemp = Math.sin(Math.toRadians(goalLong) - Math.toRadians(currentLong)) * Math.cos(Math.toRadians(goalLat));
            xTemp = Math.cos(Math.toRadians(currentLat)) * Math.sin(Math.toRadians(goalLat)) - Math.sin(Math.toRadians(currentLat)) *
                    Math.cos(Math.toRadians(goalLat)) * Math.cos(Math.toRadians(goalLong) - Math.toRadians(currentLong));
            // mAzimuth = (int) (Math.atan2(yTemp,xTemp) + 360) % 360;
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) - Math.toDegrees(Math.atan2(yTemp, xTemp)) + 360) % 360;
            //mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastMagnetometerSet && mLastAccelerometerSet) {
            SensorManager.getRotationMatrix(rMat, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(rMat, orientation);
            yTemp = Math.sin(Math.toRadians(goalLong) - Math.toRadians(currentLong)) * Math.cos(Math.toRadians(goalLat));
            xTemp = Math.cos(Math.toRadians(currentLat)) * Math.sin(Math.toRadians(goalLat)) - Math.sin(Math.toRadians(currentLat)) *
                    Math.cos(Math.toRadians(goalLat)) * Math.cos(Math.toRadians(goalLong) - Math.toRadians(currentLong));
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) - Math.toDegrees(Math.atan2(yTemp, xTemp)) + 360) % 360;
            // mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + Math.atan2(yTemp,xTemp) + 360) % 360;
            //mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) +360) % 360;
        }

        //set compass image
        mAzimuth = Math.round(mAzimuth);
        img_compass.setRotation(-mAzimuth);

        String where = "NW";
        //change the string to show what the cardinal directions are

        if (mAzimuth >= 350 || mAzimuth <= 10)
            where = "N";
        if (mAzimuth < 350 && mAzimuth > 280)
            where = "NW";
        if (mAzimuth <= 280 && mAzimuth > 260)
            where = "W";
        if (mAzimuth <= 260 && mAzimuth > 190)
            where = "SW";
        if (mAzimuth <= 190 && mAzimuth > 170)
            where = "S";
        if (mAzimuth <= 170 && mAzimuth > 100)
            where = "SE";
        if (mAzimuth <= 100 && mAzimuth > 80)
            where = "E";
        if (mAzimuth <= 80 && mAzimuth > 10)
            where = "NE";

        txt_azimuth.setText(mAzimuth + " Degree " + where);

        // Add field to calculate the distance
        delta_psi = Math.toRadians(goalLat) - Math.toRadians(currentLat);
        delta_lambda = Math.toRadians(goalLong) - Math.toRadians(currentLong);
        temp_a = Math.sin(delta_psi / 2) * Math.sin(delta_psi) + Math.cos(Math.toRadians(goalLat)) *
                Math.cos(Math.toRadians(currentLong)) * Math.sin(delta_lambda/2) * Math.sin(delta_lambda/2);
        temp_c = 2 * Math.atan2(Math.sqrt(temp_a), Math.sqrt(1-temp_a));
        distance = Math.round(rad_earth * temp_c);
        String distance_text = "Far";
        if (distance > 1000)
            distance_text = "Way Far > 1 km";
        else if (distance > 100)
            distance_text = "Far 1 km ~ 100 m";
        else if (distance > 10)
            distance_text = "Near 100 m ~ 10 m";
        else if (distance <= 10)
            distance_text = "Here! < 10 m";
        locationText.setText("Goal Latitude: " + goalLat + "\n Goal Longitude: " + goalLong+ " \n " + distance_text);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void start() {
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null || mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
                noSensorAlert();
            } else {
                mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

                haveSensor = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
                haveSensor2 = mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
            }
        } else {
            mRotationV = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            haveSensor = mSensorManager.registerListener(this, mRotationV, SensorManager.SENSOR_DELAY_UI);

        }
    }

    public void noSensorAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Your device doesn't support the compass")
                .setCancelable(false)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        alertDialog.show();
    }

    public void stop() {
        if (haveSensor && haveSensor2) {
            mSensorManager.unregisterListener(this, mAccelerometer);
            mSensorManager.unregisterListener(this, mMagnetometer);
        } else if (haveSensor) {
            mSensorManager.unregisterListener(this, mRotationV);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        start();
    }

    public class webScrape extends AsyncTask<Void, Void, Void> {

        String storeLocation;

        @Override
        protected Void doInBackground(Void... voids) {
            String baseUrl = "https://www.google.com/search?q=";
            String searchTextBase = "Taco+Bell+At+";
            String searchLocation = currentAddressText;
            String url = baseUrl + searchTextBase + searchLocation;
            String selector_String = "#rso > div > div > div:nth-child(1) > div > div > div.r > a > h3";
            try {
                Document doc = Jsoup.connect(url).get();
                Elements storeLocationElement = doc.select(selector_String);
                //taco bell in green valley, arizona - 80W duval mine rd | taco bell
                String[] tempStoreLocation = storeLocationElement.text().split("Taco Bell in");
                String[] tempStoreLocation2 = tempStoreLocation[1].split("-");
                String[] tempStoreLocation3 = tempStoreLocation2[1].split("Taco Bell");
                storeLocation = tempStoreLocation3[0] + tempStoreLocation2[0];

                //update lat long
                getLocationFromAddress(storeLocation);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            locationText.setText(storeLocation);
        }
    }

    public void getLocationFromAddress(String strAddress) {
        //once get store location, update goal?
        Geocoder coder = new Geocoder(this);
        List<Address> address;
        try {
            address = coder.getFromLocationName(strAddress, 5);
            Address location = address.get(0);
            tacoLat = (float) location.getLatitude();
            tacoLong = (float) location.getLongitude();
            goalLong = tacoLong;
            goalLat = tacoLat;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
