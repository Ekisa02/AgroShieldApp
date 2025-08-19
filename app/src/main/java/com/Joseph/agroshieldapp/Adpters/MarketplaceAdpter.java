package com.Joseph.agroshieldapp.Adpters;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.Joseph.agroshieldapp.R;
import com.Joseph.agroshieldapp.models.Marketplace;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class MarketplaceAdpter extends BaseAdapter {



    private final Context context;
    private final List<Marketplace> products;
    private static final String WHATSAPP_URL = "https://wa.me/";

    public MarketplaceAdpter(Context context, List<Marketplace> products) {
        this.context = context;
        this.products = products;
    }

    @Override
    public int getCount() {
        return products.size();
    }

    @Override
    public Marketplace getItem(int position) {
        return products.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_product_card, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Marketplace product = getItem(position);
        holder.bind(product);

        convertView.setOnClickListener(v -> showProductDetailsDialog(product));

        return convertView;
    }

    private void showProductDetailsDialog(Marketplace product) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_product_details, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme)
                .setView(dialogView)
                .setPositiveButton("Contact to Buy", null)
                .setNegativeButton("Cancel", null)
                .create();

        // Initialize dialog views
        ImageView detailImage = dialogView.findViewById(R.id.detailImage);
        TextView detailName = dialogView.findViewById(R.id.detailName);
        TextView detailPrice = dialogView.findViewById(R.id.detailPrice);
        TextView detailDescription = dialogView.findViewById(R.id.detailDescription);
        TextView detailHealth = dialogView.findViewById(R.id.detailHealth);
        TextView detailLocation = dialogView.findViewById(R.id.detailLocation);

        // Load data
        loadBase64Image(detailImage, product.getImageBase64());

        detailName.setText(product.getName());
        detailPrice.setText(formatPrice(product.getPrice()));
        detailDescription.setText(product.getDescription());
        detailHealth.setText(context.getString(R.string.health_info,
                product.getHealthInfo() != null ? product.getHealthInfo() : "Not specified"));
        detailLocation.setText(context.getString(R.string.location_info,
                product.getLocation() != null ? product.getLocation() : "Unknown"));

        dialog.show();

        // Custom positive button behavior
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            dialog.dismiss();
            showContactOptionsDialog(product);
        });
    }

    private void loadBase64Image(ImageView imageView, String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) {
            imageView.setImageResource(R.drawable.placeholder_product);
            return;
        }

        try {
            // Decode Base64 string to byte array
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);

            // Use Glide to load the byte array directly
            Glide.with(context)
                    .asBitmap()
                    .load(decodedBytes)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .placeholder(R.drawable.placeholder_product)
                    .error(R.drawable.placeholder_product)
                    .into(imageView);
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.placeholder_product);
        }
    }
    private Bitmap base64ToBitmap(String base64Image) {
        try {
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void showContactOptionsDialog(Marketplace product) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_contact_options, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme)
                .setView(dialogView)
                .setTitle("Contact Seller")
                .create();

        Button btnWhatsApp = dialogView.findViewById(R.id.btnWhatsApp);
        Button btnCall = dialogView.findViewById(R.id.btnCall);

        btnWhatsApp.setOnClickListener(v -> {
            openWhatsApp(product.getWhatsapp());
            dialog.dismiss();
        });

        btnCall.setOnClickListener(v -> {
            makePhoneCall(product.getPhone());
            dialog.dismiss();
        });

        dialog.show();
    }

    private String formatPrice(String price) {
        try {
            double amount = Double.parseDouble(price);
            return String.format("KSh %,.2f", amount);
        } catch (NumberFormatException e) {
            return "KSh " + price;
        }
    }

    private void openWhatsApp(String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(WHATSAPP_URL + number));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        }
    }


    private void makePhoneCall(String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + number));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Cannot make call", Toast.LENGTH_SHORT).show();
        }
    }


    private static class ViewHolder {
        private final ImageView productImage;
        private final TextView productName;
        private final TextView productPrice;
        private final TextView productLocation;

        ViewHolder(View view) {
            productImage = view.findViewById(R.id.productImage);
            productName = view.findViewById(R.id.productName);
            productPrice = view.findViewById(R.id.productPrice);
            productLocation = view.findViewById(R.id.productLocation);
        }

        void bind(Marketplace product) {
            // Load Base64 image correctly
            if (product.getImageBase64() != null && !product.getImageBase64().isEmpty()) {
                byte[] decodedString = Base64.decode(product.getImageBase64(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                productImage.setImageBitmap(decodedByte);
            } else {
                productImage.setImageResource(R.drawable.placeholder_product);
            }

            productName.setText(product.getName());
            productPrice.setText("KSh " + product.getPrice());
            productLocation.setText(product.getLocation());
        }
    }
}