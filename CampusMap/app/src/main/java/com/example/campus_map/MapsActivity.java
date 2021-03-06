package com.example.campus_map;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;

/*******************************************************************************
 * Reference 1
 * Title: Overview | Maps SDK for Android
 * URL: https://developers.google.com/maps/documentation/android-sdk/overview
 *
 * Reference 2
 * Title: CodePath Android Cliffnotes
 * URL: https://guides.codepath.com/android
 ********************************************************************************/
public class MapsActivity extends AppCompatActivity
        implements
        OnMyLocationButtonClickListener,
        OnMyLocationClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        OnMapReadyCallback {

//    private GoogleMap mMap;
    private static final String TAG = MapFragment.class.getSimpleName();
    private SupportMapFragment mapFragment = null;

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean permissionDenied = false;

    private GoogleMap map;

    private RadioButton checkedMode;
    private TextView estTime;

    private static final float DEFAULT_ZOOM = 15f;

    //database variable
    private DatabaseHelper db;

    private ArrayList<ArrayList<String>> routeData;
    private ArrayList<ArrayList<String>> routeDirection;
    private ArrayList<ArrayList<String>> buildings;
    private ArrayList<String> route;
    private ArrayList<String> places;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Toolbar topToolbar = (Toolbar) findViewById(R.id.locationsToolbar);
        setSupportActionBar(topToolbar);

        mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        places = getIntent().getStringArrayListExtra("places");

        final ImageButton goButton = (ImageButton) findViewById(R.id.goButton);
        final TextInputEditText fromAddressEdit = (TextInputEditText) findViewById(R.id.editAddess_From);
        final TextInputEditText toAddressEdit = (TextInputEditText) findViewById(R.id.editAddess_To);
        estTime = (TextView) findViewById(R.id.estimateTime);
        final Button homeBtn = (Button)findViewById(R.id.returnBtn);
        //final RadioGroup modeButtons = (RadioGroup) findViewById(R.id.modeButtonGroup);
        //checkedMode = getCheckedMode(modeButtons);

        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent launchActivity = new Intent(MapsActivity.this, MainActivity.class);
                startActivity(launchActivity);

            }
        });

        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(places == null){
                    displayAlertDialog("Missing Address", "Please Select Starting and Destination Building");
                }else{
                    String[] direction = db.getRouteData(places.get(1), places.get(0)).get(3).split(";");
                    String routeDetail = "";
                    for(String s : direction) {
                        s = s.replaceFirst("\\s", "");
                        routeDetail += (s + "\n");
                    }
                    displayAlertDialog("Route Detail", routeDetail);
                }
            }
        });

        fromAddressEdit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent launchActivity = new Intent(MapsActivity.this, BuildingActivity.class);
                startActivity(launchActivity);
            }
        });

        toAddressEdit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent launchActivity = new Intent(MapsActivity.this, BuildingActivity.class);
                startActivity(launchActivity);
            }
        });

        // test Route and Direction table
        db = new DatabaseHelper(this);
        routeData = db.getAllRouteData();
        route = db.getRouteData("Quentin Burdick Building", "Minard Hall");
        routeDirection = db.getRouteDirection(2);
        buildings = db.getBuildingData();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //move mylocationButton to the right-bottom
        Log.d(TAG, "onMapReady()");

        map = googleMap;
        enableMyLocation();

        View mapView = mapFragment.getView();
        View locationButton = mapView.findViewWithTag("GoogleMapMyLocationButton");
        if (locationButton != null) {
            locationButton.setVisibility(View.GONE);
        }
        findViewById(R.id.locatorButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapView != null && locationButton != null) {
                    locationButton.callOnClick();
                }
            }
        });

        map = googleMap;
        enableMyLocation();


        // Add a marker in Sydney and move the camera
        LatLng ndsu = new LatLng(46.898008230849385, -96.80244942610898);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(ndsu, DEFAULT_ZOOM));

        //safe check for direct view map
        if(places != null) {

            TextInputLayout layout = findViewById(R.id.editAddress_From_Layout);
            layout.setHint("From: " + places.get(0));
            TextInputLayout layout_to = findViewById(R.id.editAddess_To_Layout);
            layout_to.setHint("To: " + places.get(1));


            route = db.getRouteData(places.get(1), places.get(0));
            Log.d("routeprint", places.get(0) + places.get(1));
            Log.d("routeprint", route.toString());
            routeDirection = db.getRouteDirection(Integer.parseInt(route.get(0)));
            Log.d("routeprint", routeDirection.toString());


            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

            int size = routeDirection.size();
            LatLng Start = new LatLng(Double.parseDouble(routeDirection.get(0).get(2)), Double.parseDouble(routeDirection.get(0).get(3)));
            LatLng Destination = new LatLng(Double.parseDouble(routeDirection.get(size - 1).get(2)), Double.parseDouble(routeDirection.get(size - 1).get(3)));
            boundsBuilder.include(Start);
            boundsBuilder.include(Destination);

            map.addMarker(new MarkerOptions().position(Start).title("You Start From Here"));
            map.addMarker(new MarkerOptions().position(Destination).title("This is Destination"));


            for (int i = 0; i < size - 1; i++) {
                Double srcLat = Double.parseDouble(routeDirection.get(i).get(2).toString());
                Double srcLong = Double.parseDouble(routeDirection.get(i).get(3).toString());
                Double destLat = Double.parseDouble(routeDirection.get(i + 1).get(2).toString());
                Double destLong = Double.parseDouble(routeDirection.get(i + 1).get(3).toString());

                // mMap is the Map Object
                Polyline line = map.addPolyline(
                        new PolylineOptions().add(
                                new LatLng(srcLat, srcLong),
                                new LatLng(destLat, destLong)
                        ).color(Color.rgb(51, 204, 255))
                );
            }

            estTime.setText("Estimate Time: " + route.get(4));

            map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 800, 800, 0));

        }




        map.setOnMyLocationButtonClickListener(this);
        map.setOnMyLocationClickListener(this);


    }

    /**
     * Move the compass button to the left bottom corner.
     */
    public void moveCompassButton(View mapView) {
        try {
            assert mapView != null; // skip this if the mapView has not been set yet

            Log.d(TAG, "moveCompassButton()");

            View view = mapView.findViewWithTag("GoogleMapMyLocationButton");

            // move the compass button to the right side, centered
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(200, 0, 0, 170);


            view.setLayoutParams(layoutParams);
        } catch (Exception ex) {
            Log.e(TAG, "moveCompassButton() - failed: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (map != null) {
                map.setMyLocationEnabled(true);
            }
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "Moving to current location", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Permission was denied. Display an error message
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            permissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    public RadioButton getCheckedMode(RadioGroup modes) {
        return (RadioButton) findViewById(modes.getCheckedRadioButtonId());
    }

    private void setCheckedMode(RadioGroup modes, int modeID) {
        modes.check(modeID);
    }

    private void setCheckedModeStyle(RadioGroup modes) {
        getCheckedMode(modes).setBackgroundColor(getResources().getColor(R.color.bisonYello));
        getCheckedMode(modes).setTextColor(getResources().getColor(R.color.bisonGreen));
    }

    private void displayAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setTitle(title);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

}