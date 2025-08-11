package com.Joseph.agroshieldapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import org.tensorflow.lite.Interpreter;

public class WeatherFragment extends Fragment {


    private static final int REQUEST_PERMISSION = 100;

    private FloatingActionButton fabAddCrop;

    // Constants

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String WEATHER_API_KEY = "fcf0362341801f89d7875f001e5c2534";
    private static final String FAO_API_BASE_URL = "https://www.fao.org/faostat/api/v1/";

    // Weather UI Components
    private TextView userFullName, temp, weatherCondition, humidity, wind, pressure, uvIndex;
    private Chip locationChip;
    private ImageView weatherIcon;

    // Crop Disease UI Components
    private LinearLayout diseaseContainer;
    private ProgressBar diseaseProgressBar;
    private TextView diseaseEmptyView;
    private List<CropDisease> diseaseList = new ArrayList<>();

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    // Location
    private FusedLocationProviderClient fusedLocationClient;

    // TensorFlow Lite Model
    private Interpreter tflite;

    // Disease Info Mapping (model output index → {diseaseName, suggestion, advice})
    private final String[][] diseaseInfo = {
            {"Black Rot", "Use copper-based bactericides", "Rotate crops and remove infected debris"},
            {"Downy Mildew", "Apply fungicide containing mancozeb", "Ensure proper spacing for airflow"},
            {"Maize Lethal Necrosis", "Remove infected plants", "Plant certified virus-free seeds"},
            {"Gray Leaf Spot", "Apply fungicide at VT stage", "Use resistant maize varieties"}
    };

    // Activity Launchers for Camera/Gallery
    private final ActivityResultLauncher<Intent> imageCaptureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                    if (bitmap != null) new DiseaseDetectionTask().execute(bitmap);
                }
            });

    private final ActivityResultLauncher<Intent> imagePickLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                        if (bitmap != null) new DiseaseDetectionTask().execute(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_weather_fragment, container, false);

        // Initialize Weather Components
        initWeatherComponents(view);

        // Initialize Crop Disease Components
        initCropDiseaseComponents(view);

        // Load User Data
        loadUserData();

        // Check Location and Load Data
        checkLocationAndLoadData();

        FloatingActionButton fabAddCrop = view.findViewById(R.id.fab_add_crop);

        // Check Camera & Storage Permissions
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        }

        // Load TensorFlow Lite Model
        try {
            tflite = new Interpreter(loadModelFile("crop_disease_model.tflite"));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Model load failed", Toast.LENGTH_SHORT).show();
        }

        fabAddCrop.setOnClickListener(v -> showImageSourceDialog());

        return view;
    }


    /**
     * Shows a dialog to choose between Camera or Gallery for disease detection.
     */
    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        imageCaptureLauncher.launch(takePictureIntent);
                    } else {
                        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        imagePickLauncher.launch(pickPhotoIntent);
                    }
                })
                .show();
    }

    /**
     * Loads a TensorFlow Lite model file from assets.
     * @param modelFileName Name of the .tflite file
     * @return MappedByteBuffer containing the model
     * @throws IOException If file not found
     */
    private MappedByteBuffer loadModelFile(String modelFileName) throws IOException {
        FileInputStream fis = new FileInputStream(requireContext().getAssets().openFd(modelFileName).getFileDescriptor());
        FileChannel fileChannel = fis.getChannel();
        long startOffset = requireContext().getAssets().openFd(modelFileName).getStartOffset();
        long declaredLength = requireContext().getAssets().openFd(modelFileName).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * AsyncTask to detect crop diseases from an image using TensorFlow Lite.
     */
    private class DiseaseDetectionTask extends AsyncTask<Bitmap, Void, Integer> {
        @Override
        protected Integer doInBackground(Bitmap... bitmaps) {
            Bitmap resized = Bitmap.createScaledBitmap(bitmaps[0], 224, 224, true);
            float[][][][] input = new float[1][224][224][3];

            // Normalize pixel values
            for (int x = 0; x < 224; x++) {
                for (int y = 0; y < 224; y++) {
                    int pixel = resized.getPixel(x, y);
                    input[0][y][x][0] = ((pixel >> 16) & 0xFF) / 255.0f; // R
                    input[0][y][x][1] = ((pixel >> 8) & 0xFF) / 255.0f;  // G
                    input[0][y][x][2] = (pixel & 0xFF) / 255.0f;        // B
                }
            }

            float[][] output = new float[1][diseaseInfo.length];
            tflite.run(input, output);

            // Find the disease with highest probability
            int predictedIndex = 0;
            float maxProb = 0;
            for (int i = 0; i < output[0].length; i++) {
                if (output[0][i] > maxProb) {
                    maxProb = output[0][i];
                    predictedIndex = i;
                }
            }
            return predictedIndex;
        }

        @Override
        protected void onPostExecute(Integer index) {
            String disease = diseaseInfo[index][0];
            String suggestion = diseaseInfo[index][1];
            String advice = diseaseInfo[index][2];

            new AlertDialog.Builder(requireContext())
                    .setTitle("Detection Result")
                    .setMessage("Issue: " + disease + "\n\nSuggestion: " + suggestion + "\n\nAdvice: " + advice)
                    .setPositiveButton("Find Nearest Agrovet", (dialog, which) -> {
                        String query = "Agrovet near me";
                        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        startActivity(mapIntent);
                    })
                    .setNegativeButton("Close", null)
                    .show();
        }
    }


    /* ------------------------- */
    /* WEATHER FUNCTIONALITY */
    /* ------------------------- */

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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Location Required");
        builder.setMessage("Make sure your location is on to see real-time weather information.");
        builder.setPositiveButton("Turn On", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new android.content.Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.setCancelable(false);
        builder.show();
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
            case "03d": case "03n": return R.drawable.ic_cloudy;
            case "04d": case "04n": return R.drawable.ic_broken_clouds;
            case "09d": case "09n": return R.drawable.ic_rain;
            case "10d": case "10n": return R.drawable.ic_rain;
            case "11d": case "11n": return R.drawable.ic_thunderstorm;
            case "13d": case "13n": return R.drawable.ic_snow;
            case "50d": case "50n": return R.drawable.ic_mist;
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

    /* ------------------------- */
    /* CROP DISEASE FUNCTIONALITY */
    /* ------------------------- */

    private void initCropDiseaseComponents(View view) {
        diseaseContainer = view.findViewById(R.id.diseaseContainer);
        diseaseProgressBar = view.findViewById(R.id.diseaseProgressBar);
        diseaseEmptyView = view.findViewById(R.id.emptyView);
        fetchCropDiseasesFromFAO();
    }

    private void fetchCropDiseasesFromFAO() {
        showLoadingState();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(FAO_API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        FAOApiService service = retrofit.create(FAOApiService.class);

        Call<FAODiseaseResponse> call = service.getCropDiseases(
                "crop_diseases",
                "maize",
                "json",
                "https://fenixservices.fao.org/faostat/api/v1/en/QA/COUNTRIES"
                // Replace with actual API key
        );

        call.enqueue(new Callback<FAODiseaseResponse>() {
            @Override
            public void onResponse(Call<FAODiseaseResponse> call, Response<FAODiseaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processDiseaseData(response.body());
                } else {
                    showErrorState("API Error: " + response.message());
                    loadSampleData();
                }
            }

            @Override
            public void onFailure(Call<FAODiseaseResponse> call, Throwable t) {
                showErrorState("Network Error: " + t.getMessage());
                loadSampleData();
            }
        });
    }

    private void processDiseaseData(FAODiseaseResponse response) {
        if (response.getData() != null && !response.getData().isEmpty()) {
            diseaseList.clear();
            for (FAODiseaseResponse.Disease disease : response.getData()) {
                diseaseList.add(new CropDisease(
                        disease.getDiseaseName(),
                        disease.getDescription(),
                        disease.getImageUrl(),
                        disease.getAffectedCrop()
                ));
            }
            showDiseaseCards();
        } else {
            showEmptyState("No disease data available");
            loadSampleData();
        }
    }

    private void showDiseaseCards() {
        diseaseContainer.removeAllViews();

        for (CropDisease disease : diseaseList) {
            View diseaseCard = LayoutInflater.from(getContext())
                    .inflate(R.layout.disease_card_layout, diseaseContainer, false);

            ImageView diseaseImage = diseaseCard.findViewById(R.id.diseaseImage);
            TextView diseaseName = diseaseCard.findViewById(R.id.diseaseName);
            TextView diseaseCrop = diseaseCard.findViewById(R.id.diseaseCrop);
            MaterialCardView cardView = diseaseCard.findViewById(R.id.diseaseCard);

            Picasso.get()
                    .load(disease.getImageUrl())
                    .placeholder(R.drawable.ic_plant)
                    .error(R.drawable.ic_plant)
                    .into(diseaseImage);

            diseaseName.setText(disease.getName());
            diseaseCrop.setText(disease.getCropType());

            cardView.setOnClickListener(v -> showDiseaseDetails(disease));
            diseaseContainer.addView(diseaseCard);
        }

        diseaseContainer.setVisibility(View.VISIBLE);
        diseaseProgressBar.setVisibility(View.GONE);
        diseaseEmptyView.setVisibility(View.GONE);
    }

    private void showDiseaseDetails(CropDisease disease) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(disease.getName());

        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.disease_detail_layout, null);

        ImageView detailImage = view.findViewById(R.id.detailImage);
        TextView detailDescription = view.findViewById(R.id.detailDescription);
        TextView detailCrop = view.findViewById(R.id.detailCrop);

        Picasso.get().load(disease.getImageUrl()).into(detailImage);
        detailDescription.setText(disease.getDescription());
        detailCrop.setText("Affects: " + disease.getCropType());

        builder.setView(view);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void loadSampleData() {
        diseaseList.add(new CropDisease(
                "Maize Lethal Necrosis",
                "A viral disease causing yellowing and drying of leaves. Spread by insects and infected seeds.",
                "https://www.fao.org/fileadmin/templates/agphome/images/maize-disease1.jpg",
                "Maize"
        ));

        diseaseList.add(new CropDisease(
                "Maize Rust",
                "Fungal disease appearing as yellow-orange pustules on leaves. Favors humid conditions.",
                "https://www.fao.org/fileadmin/templates/agphome/images/maize-disease2.jpg",
                "Maize"
        ));

        showDiseaseCards();
    }

    private void showLoadingState() {
        diseaseProgressBar.setVisibility(View.VISIBLE);
        diseaseContainer.setVisibility(View.GONE);
        diseaseEmptyView.setVisibility(View.GONE);
    }

    private void showEmptyState(String message) {
        diseaseProgressBar.setVisibility(View.GONE);
        diseaseContainer.setVisibility(View.GONE);
        diseaseEmptyView.setVisibility(View.VISIBLE);
        diseaseEmptyView.setText(message);
    }

    private void showErrorState(String error) {
        diseaseProgressBar.setVisibility(View.GONE);
        diseaseContainer.setVisibility(View.GONE);
        diseaseEmptyView.setVisibility(View.VISIBLE);
        diseaseEmptyView.setText("Error: " + error);
        Toast.makeText(getContext(), "Failed to load disease data", Toast.LENGTH_SHORT).show();
    }

    /* ------------------------- */
    /* DATA MODEL CLASSES */
    /* ------------------------- */

    private static class CropDisease {
        private final String name;
        private final String description;
        private final String imageUrl;
        private final String cropType;

        public CropDisease(String name, String description, String imageUrl, String cropType) {
            this.name = name;
            this.description = description;
            this.imageUrl = imageUrl;
            this.cropType = cropType;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getImageUrl() { return imageUrl; }
        public String getCropType() { return cropType; }
    }

    private interface FAOApiService {
        @GET("data")
        Call<FAODiseaseResponse> getCropDiseases(
                @Query("dataset") String dataset,
                @Query("crop") String crop,
                @Query("format") String format,
                @Query("key") String apiKey
        );
    }

    private static class FAODiseaseResponse {
        private List<Disease> data;

        public List<Disease> getData() { return data; }

        public static class Disease {
            private String diseaseName;
            private String description;
            private String imageUrl;
            private String affectedCrop;

            public String getDiseaseName() { return diseaseName; }
            public String getDescription() { return description; }
            public String getImageUrl() { return imageUrl; }
            public String getAffectedCrop() { return affectedCrop; }
        }
    }
}