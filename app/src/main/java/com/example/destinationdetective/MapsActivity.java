package com.example.destinationdetective;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import androidx.multidex.MultiDexApplication;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import com.google.android.material.snackbar.Snackbar;

import java.util.Random;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/** * @noinspection ALL */

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView timerTextView;
    private int secondsPassed = 0;
    private Handler handler = new Handler();
    private Runnable runnable;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private MediaPlayer mediaPlayer;

    // Database variables
    private DatabaseReference myDatabase;

    // Score related variables
    int score = 0;
    private TextView scoreTextView;

    // Firebase authentication variables
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseUser currentUser = mAuth.getCurrentUser();

    //Text view variable for the attempted display of the username at the top of the screen.
    private TextView userNameTextView;

    // Variables for  clue and destination
    private TextView clueTextView;
    private Clue[] clues;
    private Clue pinnedClue;
    private CoordinatorLayout coordinatorLayout;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Initialize the TextView for displaying clues.
        clueTextView = findViewById(R.id.clueTextView);

        //Initialise the coordinatorlayout for Snackbars.
        coordinatorLayout = findViewById(R.id.coordinatorLayout);

        // Initialize the clues array, can easily be extended if I need to.
        clues = new Clue[]{
                new Clue("Marine Gardens", "A sea park where Worthing bowls teams play and families play golf.", 50.807147, -0.396977),
                new Clue("St Andrews Church", "A church Andrew once went to.", 50.824629, -0.395676),
                new Clue("Thomas A Becket", "A pub that's name contains a saint.", 50.830069, -0.391759),
                new Clue("Worthing Football Club", "Home of the Mackerel men", 50.820227, -0.383599)
        };

        // Initialize timerTextView
        timerTextView = findViewById(R.id.timerTextView);
        // Initialize scoreTextView
        scoreTextView = findViewById(R.id.scoreTextView);

        // Initialize Firebase database reference
        myDatabase = FirebaseDatabase.getInstance("https://destination-detective-77000-default-rtdb.europe-west1.firebasedatabase.app/").getReference();

        // Initialize MediaPlayer with background music file, set the track to loop when it is finished and start playing.
        mediaPlayer = MediaPlayer.create(this, R.raw.backgroundmusic);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        //  Colorful Flowers by Tokyo Music Walker | https://soundcloud.com/user-356546060
        //  Music promoted by https://www.chosic.com/free-music/all/
        //  Creative Commons CC BY 3.0
        //  https://creativecommons.org/licenses/by/3.0/

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Start the timer
        startTimer();

        // Start updating the score
        startScoreUpdate();

    }

    // Method to periodically update the score
    private void startScoreUpdate() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Update the score and scoreTextView
                updateScore(score);
                // Schedule the next score update to occur at 10 second intervals.
                handler.postDelayed(this, 10000);
            }
        }, 10000);
    }


    // Method to update the user's score in the database and for the users score display.
    // Method to update the user's score in the database and scoreTextView.
    // Could not find the method to correctly implement this as even though the score was saving to the database
    // it would reset the value to 0 each time the app started up.
    private void updateScore(int newScore) {

        scoreTextView.setText("Score: " + newScore);

        // Update the score in the Firebase Realtime Database
        if (currentUser != null) {
            String userId = currentUser.getUid();
            myDatabase.child("users").child(userId).child("score").setValue(score);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        mMap.setMyLocationEnabled(true);

        startLocationUpdates();
        selectRandomClue();
        monitorUserLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }

        // Check if the screen is locked and stop the background music
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null && keyguardManager.isKeyguardLocked()) {
            mediaPlayer.pause();
        }
    }

    // Fix for the problem of the music continuing after the screen is locked or
    // the application is minimised. Call onResume to check if media player has been
    // initiated, was playing, and if it was, resume the track.
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    // Method to handle the location updates, centring the map on the user at app
    // startup and periodically, by using their latitude and longitude.
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(5000); // 5 seconds interval

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {

                    Location location = locationResult.getLastLocation();
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    CameraPosition cameraPosition = new CameraPosition.Builder().target(currentLocation)
                            .zoom(17)
                            .tilt(45)
                            .build();

                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    // Begin a timer as the user logs in and the game is loaded,
    // should be adapted to initiate when the user is presented
    // with a clue and count up from there

    private void startTimer() {
        runnable = new Runnable() {
            @Override
            public void run() {
                secondsPassed++;
                int hours = secondsPassed / 3600;
                int minutes = (secondsPassed % 3600) / 60;
                int seconds = secondsPassed % 60;
                timerTextView.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                handler.postDelayed(this, 1000); // Run this runnable every second

            }
        };
        handler.post(runnable);
    }

    private void resetTimer() {
        secondsPassed = 0;
        handler.removeCallbacks(runnable);
        startTimer();
    }

    // Logic to handle the clue being provided, being set as a hidden target and cheking to see
    // if a users lat and long is within a radius of the clues destination.

    public class Clue {
        private String name;
        private String clueText;
        private double latitude;
        private double longitude;

        public Clue(String name, String clueText, double latitude, double longitude) {
            this.name = name;
            this.clueText = clueText;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getName() {
            return name;
        }

        public String getClueText() {
            return clueText;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }


    private void selectRandomClue() {
        if (clues != null && clues.length > 0) {
            Random random = new Random();
            pinnedClue = clues[random.nextInt(clues.length)];
            Log.d("Clue", "Selected Clue: " + pinnedClue.getClueText());
            clueTextView.setText(pinnedClue.getClueText());
        }
    }


    private void monitorUserLocation() {
        if (pinnedClue == null) {
            selectRandomClue();
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    float[] results = new float[1];
                    Location.distanceBetween(currentLocation.latitude, currentLocation.longitude,
                            pinnedClue.getLatitude(), pinnedClue.getLongitude(), results);
                    float distanceInMeters = results[0];

                    if (distanceInMeters < 40) {

                        Snackbar snackbar = Snackbar.make(coordinatorLayout, "You found the location: " + pinnedClue.getName() + "!" + " You have been awarded 100 points!", Snackbar.LENGTH_LONG);
                        snackbar.show();

                        score += 100;
                        updateScore(score);

                        selectRandomClue();
                        resetTimer();
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    // Stop the applications processes when the app is closed,
    // stop the location updates, destroy the media player
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the callback to prevent memory leaks
        handler.removeCallbacks(runnable);

        // Stop location updates
        stopLocationUpdates();

        // Stop and release MediaPlayer when activity is destroyed
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
