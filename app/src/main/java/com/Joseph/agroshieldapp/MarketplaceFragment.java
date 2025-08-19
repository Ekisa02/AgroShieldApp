package com.Joseph.agroshieldapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MarketplaceFragment extends Fragment {

    private ProgressDialog loadingDialog;
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int READ_EXTERNAL_STORAGE_REQUEST = 1002;

    private GridView productsGridView;
    private ProgressBar progressBar;
    private LinearLayout errorStateView, emptyStateView;
    private Button postProductButton;

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;

    private List<Marketplace> productList = new ArrayList<>();
    private MarketplaceAdpter adapter;
    private Uri selectedImageUri;
    private View dialogView; // Store dialog view reference for image updates

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

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Setup adapter
        adapter = new MarketplaceAdpter(getContext(), productList);
        productsGridView.setAdapter(adapter);

        // Check user role
        checkUserRole();

        // Load products
        loadProducts();

        // Set click listeners
        postProductButton.setOnClickListener(v -> showProductPostDialog());

        productsGridView.setOnItemClickListener((parent, view1, position, id) -> toggleProductDetails(position, view1));

        return view;
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
        String[] options = {"Browse as Buyer", "Post Products as Farmer"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("How would you like to proceed?")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Buyer
                            postProductButton.setVisibility(View.GONE);
                            break;
                        case 1: // Farmer
                            if (currentUser == null) {
                                showLoginPrompt();
                            } else {
                                postProductButton.setVisibility(View.VISIBLE);
                            }
                            break;
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showLoginPrompt() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Authentication Required")
                .setMessage("You need to be logged in to post products. Would you like to login now?")
                .setPositiveButton("Login", (dialog, which) -> {
                    // Navigate to login screen
                    // findNavController().navigate(R.id.action_to_login);
                })
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
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        showErrorState(task.getException() != null ?
                                task.getException().getMessage() : "Failed to load products");
                    }
                });
    }

    private void showProductPostDialog() {
        dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_post_product, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Post Your Product")
                .setView(dialogView)
                .setPositiveButton("Post", null) // Set to null to override default behavior
                .setNegativeButton("Cancel", null)
                .create();

        ImageView imageView = dialogView.findViewById(R.id.productImageView);
        Button selectImageBtn = dialogView.findViewById(R.id.selectImageButton);

        selectImageBtn.setOnClickListener(v -> openImagePicker());

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                EditText nameEditText = dialogView.findViewById(R.id.productNameEditText);
                EditText priceEditText = dialogView.findViewById(R.id.productPriceEditText);
                EditText descriptionEditText = dialogView.findViewById(R.id.productDescriptionEditText);
                EditText healthInfoEditText = dialogView.findViewById(R.id.healthInfoEditText);
                EditText locationEditText = dialogView.findViewById(R.id.locationEditText);
                EditText phoneEditText = dialogView.findViewById(R.id.phoneEditText);
                EditText whatsappEditText = dialogView.findViewById(R.id.whatsappEditText);

                String name = nameEditText.getText().toString();
                String price = priceEditText.getText().toString();
                String description = descriptionEditText.getText().toString();
                String healthInfo = healthInfoEditText.getText().toString();
                String location = locationEditText.getText().toString();
                String phone = phoneEditText.getText().toString();
                String whatsapp = whatsappEditText.getText().toString();

                if (validateProductInput(name, price, description, healthInfo, location, phone)) {
                    if (selectedImageUri != null) {
                        uploadImageAndPostProduct(
                                name, price, description, healthInfo,
                                location, phone, whatsapp, dialog
                        );
                    } else {
                        Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        dialog.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private boolean validateProductInput(String name, String price, String description,
                                         String healthInfo, String location, String phone) {
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
        return true;
    }

    // Update your uploadImageAndPostProduct method:
    private void uploadImageAndPostProduct(String name, String price, String description,
                                           String healthInfo, String location, String phone,
                                           String whatsapp, AlertDialog dialog) {
        if (selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setTitle("Uploading Product");
        progressDialog.setMessage("Preparing image... 0%");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setMax(100);
        progressDialog.show();

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

                // Create product data
                Map<String, Object> product = new HashMap<>();
                product.put("name", name);
                product.put("price", price);
                product.put("description", description);
                product.put("healthInfo", healthInfo);
                product.put("location", location);
                product.put("phone", phone);
                product.put("whatsapp", whatsapp);
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
                            loadProducts();
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
        Marketplace product = productList.get(position);
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