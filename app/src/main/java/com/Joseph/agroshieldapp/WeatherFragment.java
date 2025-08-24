package com.Joseph.agroshieldapp;


import com.google.android.gms.location.LocationServices;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Joseph.agroshieldapp.Adpters.CropAdapter;
import com.Joseph.agroshieldapp.models.Crop;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherFragment extends Fragment {


// CROP INFOR
private ShapeableImageView profileImage;

    private RecyclerView cropsRecyclerView;
    private ProgressBar loadingIndicator;
    private TextView progressText;
    private CropAdapter adapter;
    private List<Crop> cropList = new ArrayList<>();
    private FirebaseFirestore db;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private Uri imageUri;


    public WeatherFragment() {
        // Required empty public constructor
    }


    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String WEATHER_API_KEY = "fcf0362341801f89d7875f001e5c2534";

    // Weather UI Components
    private TextView userFullName, temp, weatherCondition, humidity, wind, pressure, uvIndex;
    private Chip locationChip;
    private ImageView weatherIcon;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    // Location
    private FusedLocationProviderClient fusedLocationClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_weather_fragment, container, false);

        initWeatherComponents(view);
        loadUserData();
        checkLocationAndLoadData();

        cropsRecyclerView = view.findViewById(R.id.cropsRecyclerView);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        progressText = view.findViewById(R.id.progressText);
        FloatingActionButton fab = view.findViewById(R.id.fab_add_crop);

        cropsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new CropAdapter(getContext(), cropList);
        cropsRecyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadRealtimeData();

        fab.setOnClickListener(v -> showImageSourceDialog());



        //profile image
        profileImage = view.findViewById(R.id.profileImage);

        fetchAndDisplayProfileImage();

        return view;
    }

    //fetching profile image
    private void fetchAndDisplayProfileImage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference docRef = FirebaseFirestore.getInstance()
                .collection("profileimages")
                .document(user.getUid());

        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String base64 = documentSnapshot.getString("imageBase64");
                        if (base64 != null && !base64.isEmpty()) {
                            Bitmap bmp = decodeBase64ToBitmap(base64);
                            if (bmp != null) {
                                profileImage.setImageBitmap(bmp);
                            } else {
                                profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                            }
                        } else {
                            profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                        }
                    } else {
                        profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                });
    }
    //profile helper reference
    private String getCachedProfileImage() {
        return requireContext()
                .getSharedPreferences("profile_cache", Context.MODE_PRIVATE)
                .getString("cached_profile_image", null);
    }







    //NETWORK STATUS CHECKING


    private void showImageSourceDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Select Option")
                .setItems(new CharSequence[]{"Take a Photo", "Upload from Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        dispatchTakePictureIntent();
                    } else {
                        pickImageFromGallery();
                    }
                }).show();
    }

   // IMAGE PICKER


    private void initWeatherComponents(View view) {
        userFullName = view.findViewById(R.id.userFullName);
        locationChip = view.findViewById(R.id.locationChip);
        weatherIcon = view.findViewById(R.id.weatherIcon);
        temp = view.findViewById(R.id.temp);
        weatherCondition = view.findViewById(R.id.weatherCondition);
        humidity = view.findViewById(R.id.humidity);
        wind = view.findViewById(R.id.wind);
        pressure = view.findViewById(R.id.pressure);
        uvIndex = view.findViewById(R.id.uvIndex);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("Users");
    }

    private void loadUserData() {
        String uid = mAuth.getCurrentUser().getUid();
        userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    userFullName.setText(name != null ? name : "Name not found");
                } else {
                    userFullName.setText("User not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                userFullName.setText("Error loading name");
            }
        });
    }

    private void checkLocationAndLoadData() {
        if (!isLocationEnabled()) {
            showLocationOffDialog();
        } else {
            requestLocationPermission();
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void showLocationOffDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Location Required")
                .setMessage("Make sure your location is on to see real-time weather information.")
                .setPositiveButton("Turn On", (dialog, which) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            getDeviceLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                fetchWeatherFromLatLon(location.getLatitude(), location.getLongitude());
            } else {
                locationChip.setText("Location unavailable");
            }
        });
    }

    private void fetchWeatherFromLatLon(double lat, double lon) {
        new Thread(() -> {
            try {
                String urlString = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat +
                        "&lon=" + lon + "&appid=" + WEATHER_API_KEY + "&units=metric";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    JSONObject obj = new JSONObject(result.toString());
                    updateWeatherUI(obj);
                }
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    locationChip.setText("Weather data error");
                    temp.setText("--°C");
                    weatherCondition.setText("Data unavailable");
                });
            }
        }).start();
    }

    private void updateWeatherUI(JSONObject weatherData) throws Exception {
        JSONObject main = weatherData.getJSONObject("main");
        JSONObject windObj = weatherData.getJSONObject("wind");
        JSONObject weather = weatherData.getJSONArray("weather").getJSONObject(0);

        double temperature = main.getDouble("temp");
        int humidityVal = main.getInt("humidity");
        double windSpeed = windObj.getDouble("speed");
        int pressureVal = main.getInt("pressure");
        String description = weather.getString("description");
        String cityName = weatherData.getString("name");
        String iconCode = weather.getString("icon");

        requireActivity().runOnUiThread(() -> {
            locationChip.setText(cityName);
            temp.setText(String.format("%.1f°C", temperature));
            weatherCondition.setText(capitalizeFirstLetter(description));
            humidity.setText("💧 Humidity: " + humidityVal + "%");
            wind.setText("🌬️ Wind: " + windSpeed + " m/s");
            pressure.setText("📊 Pressure: " + pressureVal + " hPa");
            uvIndex.setText("☀️ UV Index: N/A");
            weatherIcon.setImageResource(getWeatherIconResource(iconCode));
        });
    }

    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private int getWeatherIconResource(String iconCode) {
        switch (iconCode) {
            case "01d": return R.drawable.ic_sunny;
            case "01n": return R.drawable.ic_clear_night;
            case "02d": return R.drawable.ic_partly_cloudy;
            case "02n": return R.drawable.ic_night_cloudy;
            case "03d":
            case "03n": return R.drawable.ic_cloudy;
            case "04d":
            case "04n": return R.drawable.ic_broken_clouds;
            case "09d":
            case "09n": return R.drawable.ic_rain;
            case "10d":
            case "10n": return R.drawable.ic_rain;
            case "11d":
            case "11n": return R.drawable.ic_thunderstorm;
            case "13d":
            case "13n": return R.drawable.ic_snow;
            case "50d":
            case "50n": return R.drawable.ic_mist;
            default: return R.drawable.ic_weather_default;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getDeviceLocation();
            } else {
                locationChip.setText("Permission denied");
                temp.setText("--°C");
                weatherCondition.setText("No data");
            }
        }
    }



    //IMAGE ANALYSIS/CROP DATA
    private void dispatchTakePictureIntent() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickImageFromGallery() {
        try {
            Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open gallery", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) return;

        try {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                handleImageProcessing(photo);
            } else if (requestCode == REQUEST_IMAGE_PICK) {
                Uri selectedImage = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImage);
                handleImageProcessing(bitmap);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleImageProcessing(Bitmap bitmap) {
        new Thread(() -> {
            requireActivity().runOnUiThread(() -> showProgressStage("Processing..."));

            try {
                Thread.sleep(1000);
                requireActivity().runOnUiThread(() -> showProgressStage("Analyzing..."));

                Thread.sleep(1000);
                requireActivity().runOnUiThread(() -> showProgressStage("Detecting..."));

                Thread.sleep(1000);
                requireActivity().runOnUiThread(() -> showProgressStage("Finalizing..."));

                String crop = "Unknown";
                String disease = "None";
                int severity = 0;

                boolean isCrop = runTFLiteCropCheck(bitmap);
                if (isCrop) {
                    crop = "Corn";
                    disease = "Blight";
                    severity = 65;
                }

                addCropToFirestore(crop, disease, severity, "https://example.com/crop.jpg");

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error analyzing image", Toast.LENGTH_SHORT).show()
                );
            } finally {
                requireActivity().runOnUiThread(() -> hideProgress());
            }
        }).start();
    }

    private void showProgressStage(String stage) {
        loadingIndicator.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        progressText.setText(stage);
    }

    private void hideProgress() {
        loadingIndicator.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);
    }

    private boolean runTFLiteCropCheck(Bitmap bitmap) {
        // TODO: Implement TensorFlow Lite inference here
        // For now, simulate always detecting a crop
        return true;
    }

    private void addCropToFirestore(String crop, String disease, int severity, String imageUrl) {
        try {
            Map<String, Object> cropData = new HashMap<>();
            cropData.put("crop", crop);
            cropData.put("disease", disease);
            cropData.put("severity", severity);
            cropData.put("image_url", imageUrl);

            db.collection("affected_crops").add(cropData)
                    .addOnSuccessListener(documentReference ->
                            Toast.makeText(getContext(), "Added to list", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to add crop", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error saving to database", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRealtimeData() {
        db.collection("affected_crops")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    cropList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String crop = doc.getString("crop");
                        String disease = doc.getString("disease");
                        Long severity = doc.getLong("severity");
                        String imageUrl = doc.getString("image_url");
                        cropList.add(new Crop(crop, disease, severity != null ? severity.intValue() : 0, imageUrl));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private Bitmap decodeBase64ToBitmap(String base64) {
        byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
    }


    @Override
    public void onResume() {
        super.onResume();

        // 1. Try cache first
        String cachedBase64 = getCachedProfileImage();
        if (cachedBase64 != null && !cachedBase64.isEmpty()) {
            Bitmap bmp = decodeBase64ToBitmap(cachedBase64);
            if (bmp != null) {
                profileImage.setImageBitmap(bmp);
            }
        }
        // 2. Then fetch latest from Firestore to refresh if changed
        fetchAndDisplayProfileImage();
    }

}