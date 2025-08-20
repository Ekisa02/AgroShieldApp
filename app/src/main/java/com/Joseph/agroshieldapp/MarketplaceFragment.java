package com.Joseph.agroshieldapp;

import static com.Joseph.agroshieldapp.R.*;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.Joseph.agroshieldapp.Adpters.MarketplaceAdpter;
import com.Joseph.agroshieldapp.models.Marketplace;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketplaceFragment extends Fragment {

    private ProgressDialog loadingDialog;
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int READ_EXTERNAL_STORAGE_REQUEST = 1002;

    private GridView productsGridView;
    private ProgressBar progressBar;
    private LinearLayout errorStateView, emptyStateView;
    private Button postProductButton;
    private EditText searchEditText;
    private Button searchButton;
    private LinearLayout categoriesLayout;

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;

    private List<Marketplace> productList = new ArrayList<>();
    private List<Marketplace> filteredProductList = new ArrayList<>();
    private MarketplaceAdpter adapter;
    private Uri selectedImageUri;
    private View dialogView; // Store dialog view reference for image updates

    private String currentCategory = "All";
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_marketplace_fragment, container, false);

        // Initialize views
        productsGridView = view.findViewById(R.id.productsGridView);
        progressBar = view.findViewById(R.id.progressBar);
        errorStateView = view.findViewById(R.id.errorStateView);
        emptyStateView = view.findViewById(R.id.emptyStateView);
        postProductButton = view.findViewById(R.id.postProductButton);
        searchEditText = view.findViewById(R.id.searchEditText);
        searchButton = view.findViewById(R.id.searchButton);
        categoriesLayout = view.findViewById(R.id.categoriesLayout);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Setup adapter
        adapter = new MarketplaceAdpter(getContext(), filteredProductList);
        productsGridView.setAdapter(adapter);

        // Check user role
        checkUserRole();

        // Load products
        loadProducts();

        // Set click listeners
        postProductButton.setOnClickListener(v -> showProductPostDialog());
        productsGridView.setOnItemClickListener((parent, view1, position, id) -> toggleProductDetails(position, view1));

        // Setup search functionality
        setupSearchFunctionality();

        // Setup category buttons
        setupCategoryButtons();

        return view;
    }

    private void setupSearchFunctionality() {
        // Search button click listener
        searchButton.setOnClickListener(v -> {
            currentSearchQuery = searchEditText.getText().toString().trim();
            filterProducts(currentCategory, currentSearchQuery);
        });

        // Real-time search as user types
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                filterProducts(currentCategory, currentSearchQuery);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryButtons() {
        // Set click listeners for all category buttons
        for (int i = 0; i < categoriesLayout.getChildCount(); i++) {
            View child = categoriesLayout.getChildAt(i);
            if (child instanceof Button) {
                Button categoryButton = (Button) child;
                categoryButton.setOnClickListener(v -> {
                    // Reset all buttons to default color
                    for (int j = 0; j < categoriesLayout.getChildCount(); j++) {
                        View childBtn = categoriesLayout.getChildAt(j);
                        if (childBtn instanceof Button) {
                            ((Button) childBtn).setBackgroundTintList(
                                    getResources().getColorStateList(R.color.category_default)
                            );
                        }
                    }

                    // Set selected button to active color
                    categoryButton.setBackgroundTintList(
                            getResources().getColorStateList(R.color.category_selected)
                    );

                    // Filter by category
                    currentCategory = categoryButton.getText().toString();
                    filterProducts(currentCategory, currentSearchQuery);
                });
            }
        }
    }

    private void filterProducts(String category, String searchQuery) {
        filteredProductList.clear();

        for (Marketplace product : productList) {
            boolean matchesCategory = category.equals("All") ||
                    (product.getCategory() != null &&
                            product.getCategory().equals(category));

            boolean matchesSearch = searchQuery.isEmpty() ||
                    (product.getName() != null &&
                            product.getName().toLowerCase().contains(searchQuery.toLowerCase())) ||
                    (product.getDescription() != null &&
                            product.getDescription().toLowerCase().contains(searchQuery.toLowerCase()));

            if (matchesCategory && matchesSearch) {
                filteredProductList.add(product);
            }
        }

        adapter.notifyDataSetChanged();

        // Show empty state if no products match the filter
        if (filteredProductList.isEmpty()) {
            showEmptyState();
        } else {
            emptyStateView.setVisibility(View.GONE);
            productsGridView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(getContext());
            loadingDialog.setMessage("Loading products...");
            loadingDialog.setCancelable(false);
        }
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void checkUserRole() {
        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_role_selection, null);

        // Initialize views
        TextView title = dialogView.findViewById(R.id.dialogTitle);
        CardView buyerCard = dialogView.findViewById(R.id.buyerCard);
        CardView farmerCard = dialogView.findViewById(R.id.farmerCard);
        ImageView buyerIcon = dialogView.findViewById(R.id.buyerIcon);
        ImageView farmerIcon = dialogView.findViewById(R.id.farmerIcon);
        TextView buyerText = dialogView.findViewById(R.id.buyerText);
        TextView farmerText = dialogView.findViewById(R.id.farmerText);

        // Create the dialog
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.CustomRoleDialogStyle)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Set click listeners
        buyerCard.setOnClickListener(v -> {
            postProductButton.setVisibility(View.GONE);
            dialog.dismiss();
        });

        farmerCard.setOnClickListener(v -> {
            if (currentUser == null) {
                dialog.dismiss();
                showLoginPrompt();
            } else {
                postProductButton.setVisibility(View.VISIBLE);
                dialog.dismiss();
            }
        });

        // Show the dialog
        dialog.show();

        // Add animation to cards
        animateCardEntrance(buyerCard, 0);
        animateCardEntrance(farmerCard, 100);
    }

    private void animateCardEntrance(View card, long delay) {
        card.setAlpha(0f);
        card.setScaleX(0.8f);
        card.setScaleY(0.8f);

        card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(delay)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    private void showLoginPrompt() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Authentication Required")
                .setMessage("You need to be logged in to post products. Would you like to login now?")
                .setPositiveButton("Login", (dialog, which) -> {
                    // Navigate to login screen
                    startActivity(new Intent(requireContext(), LoginActivity.class));
                }) // ← Closing the lambda properly
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    postProductButton.setVisibility(View.GONE);
                })
                .show();
    }


    private void loadProducts() {
        showLoadingDialog();

        progressBar.setVisibility(View.VISIBLE);
        errorStateView.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.GONE);

        firestore.collection("marketstore")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    hideLoadingDialog();

                    if (task.isSuccessful()) {
                        productList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Marketplace product = document.toObject(Marketplace.class);
                            product.setId(document.getId());
                            productList.add(product);
                        }

                        if (productList.isEmpty()) {
                            showEmptyState();
                        } else {
                            // Apply current filters
                            filterProducts(currentCategory, currentSearchQuery);
                        }
                    } else {
                        showErrorState(task.getException() != null ?
                                task.getException().getMessage() : "Failed to load products");
                    }
                });
    }

    private void showProductPostDialog() {
        // Inflate the dialog layout
        dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_post_product, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Post Your Product")
                .setView(dialogView)
                .setPositiveButton("Post", null) // Set to null to override default behavior
                .setNegativeButton("Cancel", null)
                .create();

        // Initialize dialog views
        ImageView imageView = dialogView.findViewById(R.id.productImageView);
        Button selectImageBtn = dialogView.findViewById(R.id.selectImageButton);
        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);

        // ==================== CATEGORY SPINNER SETUP ====================
        // Create an adapter for the spinner using the categories array from resources
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.categories_array, android.R.layout.simple_spinner_item);

        // Specify the layout for the dropdown items
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        categorySpinner.setAdapter(spinnerAdapter);
        // ==================== END CATEGORY SPINNER SETUP ====================

        // Set click listener for image selection
        selectImageBtn.setOnClickListener(v -> openImagePicker());

        // Handle the positive button click (Post button)
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                // Get references to all input fields
                EditText nameEditText = dialogView.findViewById(R.id.productNameEditText);
                EditText priceEditText = dialogView.findViewById(R.id.productPriceEditText);
                EditText descriptionEditText = dialogView.findViewById(R.id.productDescriptionEditText);
                EditText healthInfoEditText = dialogView.findViewById(R.id.healthInfoEditText);
                EditText locationEditText = dialogView.findViewById(R.id.locationEditText);
                EditText phoneEditText = dialogView.findViewById(R.id.phoneEditText);
                EditText whatsappEditText = dialogView.findViewById(R.id.whatsappEditText);

                // ==================== GET SELECTED CATEGORY ====================
                // Get the selected category from the spinner
                String category = categorySpinner.getSelectedItem().toString();
                // ==================== END GET SELECTED CATEGORY ====================

                // Extract text from input fields
                String name = nameEditText.getText().toString();
                String price = priceEditText.getText().toString();
                String description = descriptionEditText.getText().toString();
                String healthInfo = healthInfoEditText.getText().toString();
                String location = locationEditText.getText().toString();
                String phone = phoneEditText.getText().toString();
                String whatsapp = whatsappEditText.getText().toString();

                // Validate input fields (including category)
                if (validateProductInput(name, price, description, healthInfo, location, phone, category)) {
                    if (selectedImageUri != null) {
                        // If validation passes and image is selected, proceed with upload
                        uploadImageAndPostProduct(
                                name, price, description, healthInfo,
                                location, phone, whatsapp, category, dialog
                        );
                    } else {
                        Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // ==================== ENHANCED BUTTON STYLING ====================
            // Get the negative button here to style it
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            // Apply background drawables
            positiveButton.setBackgroundResource(R.drawable.btn_dialog_positive);
            negativeButton.setBackgroundResource(R.drawable.btn_dialog_negative);

            // Style text colors
            positiveButton.setTextColor(getResources().getColor(color.premium_gold));
            negativeButton.setTextColor(ContextCompat.getColor(requireContext(), color.white));

            // Add padding for a better touch area and appearance
            int paddingHorizontal = getResources().getDimensionPixelSize(R.dimen.dialog_button_padding_horizontal);
            int paddingVertical = getResources().getDimensionPixelSize(R.dimen.dialog_button_padding_vertical);
            positiveButton.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
            negativeButton.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

            // Set a minimum width for visual balance
            int minWidth = getResources().getDimensionPixelSize(R.dimen.dialog_button_min_width);
            positiveButton.setMinWidth(minWidth);
            negativeButton.setMinWidth(minWidth);

            // Optional: Set a click listener for the negative button if you need custom behavior beyond dismissing.
            // The default behavior (dismiss) is already handled by the AlertDialog.
            // negativeButton.setOnClickListener(v -> dialog.dismiss());
        });

        // Show the dialog
        dialog.show();
    }
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private boolean validateProductInput(String name, String price, String description,
                                         String healthInfo, String location, String phone, String category) {
        // Validate all required fields
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Product name is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (price.isEmpty()) {
            Toast.makeText(requireContext(), "Price is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Description is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (location.isEmpty()) {
            Toast.makeText(requireContext(), "Location is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (phone.isEmpty()) {
            Toast.makeText(requireContext(), "Phone number is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        // ==================== CATEGORY VALIDATION ====================
        // Validate that a category is selected (not the default prompt)
        if (category.equals(getString(R.string.category_prompt))) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            return false;
        }
        // ==================== END CATEGORY VALIDATION ====================

        return true;
    }

    // Updated uploadImageAndPostProduct method with category parameter
    private void uploadImageAndPostProduct(String name, String price, String description,
                                           String healthInfo, String location, String phone,
                                           String whatsapp, String category, AlertDialog dialog) {
        if (selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog for upload process
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setTitle("Uploading Product");
        progressDialog.setMessage("Preparing image... 0%");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setMax(100);
        progressDialog.show();

        // Run upload process in background thread
        new Thread(() -> {
            try {
                // Step 1: Read and compress image (with progress)
                requireActivity().runOnUiThread(() ->
                        progressDialog.setMessage("Compressing image... 0%"));

                InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedImageUri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();

                // Calculate sample size to reduce memory usage
                int scale = 1;
                while ((options.outWidth / scale / 2) >= 1000 &&
                        (options.outHeight / scale / 2) >= 1000) {
                    scale *= 2;
                }

                options.inJustDecodeBounds = false;
                options.inSampleSize = scale;
                inputStream = requireContext().getContentResolver().openInputStream(selectedImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();

                // Step 2: Convert to Base64 (with progress)
                requireActivity().runOnUiThread(() ->
                        progressDialog.setMessage("Encoding image... 25%"));

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);

                // Step 3: Upload to Firestore (with progress)
                requireActivity().runOnUiThread(() -> {
                    progressDialog.setMessage("Uploading data... 50%");
                    progressDialog.setProgress(50);
                });

                // Create product data with all fields including category
                Map<String, Object> product = new HashMap<>();
                product.put("name", name);
                product.put("price", price);
                product.put("description", description);
                product.put("healthInfo", healthInfo);
                product.put("location", location);
                product.put("phone", phone);
                product.put("whatsapp", whatsapp);
                // ==================== ADD CATEGORY TO PRODUCT DATA ====================
                product.put("category", category); // Add the selected category
                // ==================== END ADD CATEGORY TO PRODUCT DATA ====================
                product.put("imageBase64", base64Image);
                product.put("farmerId", currentUser.getUid());
                product.put("timestamp", FieldValue.serverTimestamp());

                // Upload to Firestore
                firestore.collection("marketstore")
                        .add(product)
                        .addOnSuccessListener(documentReference -> {
                            progressDialog.dismiss();
                            dialog.dismiss();
                            Toast.makeText(requireContext(), "Product posted successfully", Toast.LENGTH_SHORT).show();
                            loadProducts(); // Reload products to show the new one
                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

                // Simulate progress updates
                for (int i = 50; i <= 100; i += 5) {
                    final int progress = i;
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.setProgress(progress);
                        progressDialog.setMessage("Uploading data... " + progress + "%");
                    });
                    Thread.sleep(100);
                }

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void toggleProductDetails(int position, View view) {
        Marketplace product = filteredProductList.get(position);
        LinearLayout panel = view.findViewById(R.id.detailsPanel);

        if (panel.getVisibility() == View.VISIBLE) {
            panel.setVisibility(View.GONE);
        } else {
            panel.setVisibility(View.VISIBLE);
            // Set up contact buttons
            ImageButton whatsappButton = view.findViewById(R.id.whatsappButton);
            ImageButton callButton = view.findViewById(R.id.callButton);

            whatsappButton.setOnClickListener(v -> openWhatsApp(product.getWhatsapp()));
            callButton.setOnClickListener(v -> makePhoneCall(product.getPhone()));
        }
    }

    private void openWhatsApp(String number) {
        try {
            String url = "https://api.whatsapp.com/send?phone=" + number;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void makePhoneCall(String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to make call", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                selectedImageUri = data.getData();

                // Persist permission for long term access
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                requireContext().getContentResolver().takePersistableUriPermission(
                        selectedImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Preview image in dialog
                if (dialogView != null) {
                    ImageView imageView = dialogView.findViewById(R.id.productImageView);
                    Glide.with(requireContext())
                            .load(selectedImageUri)
                            .into(imageView);
                }
            }
        }
    }

    private void showEmptyState() {
        emptyStateView.setVisibility(View.VISIBLE);
        productsGridView.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        errorStateView.setVisibility(View.VISIBLE);
        productsGridView.setVisibility(View.GONE);
        TextView errorMessage = errorStateView.findViewById(R.id.errorMessage);
        errorMessage.setText(message);

        Button retryButton = errorStateView.findViewById(R.id.retryButton);
        retryButton.setOnClickListener(v -> loadProducts());
    }
}