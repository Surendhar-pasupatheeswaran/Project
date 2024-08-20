package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 101;
    private static final int LOCATION_REQUEST_CODE = 100;

    private FusedLocationProviderClient fusedLocationClient;
    private TextView locationTextView;
    private double currentLatitude, currentLongitude;
    private LocationCallback locationCallback;

    // Firebase Database reference
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTextView = findViewById(R.id.locationTextView);
        Button navigateButton = findViewById(R.id.navigateButton);
        Button alertButton = findViewById(R.id.alertButton);

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if user is registered
        checkIfRegistered();

        // Request location permission
        if (checkLocationPermission()) {
            getUserLocation();
        } else {
            requestLocationPermission();
        }

        navigateButton.setOnClickListener(v -> navigateToLocation());
        alertButton.setOnClickListener(v -> sendLocationAlert());

        // Check for deep link intent
        checkForDeepLink();
    }

    private void checkIfRegistered() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
        String phoneNumber = sharedPreferences.getString("phoneNumber", null);

        if (phoneNumber == null) {
            // Launch the RegisterActivity if not registered
            Intent intent = new Intent(MainActivity.this, Register.class);
            startActivity(intent);
            finish();
        }
    }

    // Check location and SMS permissions
    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    // Request location and SMS permissions
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS},
                LOCATION_REQUEST_CODE);
    }

    // Get user location
    private void getUserLocation() {
        if (!checkLocationPermission()) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(300000); // 5 minutes
        locationRequest.setFastestInterval(60000); // 1 minute
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    currentLatitude = location.getLatitude();
                    currentLongitude = location.getLongitude();
                    locationTextView.setText("Location: " + currentLatitude + ", " + currentLongitude);
                    fusedLocationClient.removeLocationUpdates(locationCallback); // Stop updates after getting location
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    // Navigate to a specific location using Google Maps
    private void navigateToLocation() {
        double destinationLatitude = 40.7580;
        double destinationLongitude = -73.9855;

        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destinationLatitude + "," + destinationLongitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    // Send an alert with the user's location
    private void sendLocationAlert() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        } else {
            sendSms();
        }
    }

    // Send SMS with the location
    private void sendSms() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
        String phoneNumber = sharedPreferences.getString("phoneNumber", null);

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number is not registered. Please register.", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "Emergency! My current location is: https://maps.google.com/?q=" + currentLatitude + "," + currentLongitude;

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Alert sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show();
        }
    }

    // Handle deep link for triggering alert
    private void checkForDeepLink() {
        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null && "myapp".equals(data.getScheme()) && "alert".equals(data.getHost())) {
            // Trigger the alert when the deep link is activated
            sendLocationAlert();
        }
    }
}
