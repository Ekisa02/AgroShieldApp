package com.Joseph.agroshieldapp.Adpters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Joseph.agroshieldapp.R;
import com.Joseph.agroshieldapp.models.Crop;
import com.bumptech.glide.Glide;
import java.util.List;

public class CropAdapter extends RecyclerView.Adapter<CropAdapter.CropViewHolder> {
    private Context context;
    private List<Crop> cropList;

    public CropAdapter(Context context, List<Crop> cropList) {
        this.context = context;
        this.cropList = cropList;
    }

    @NonNull
    @Override
    public CropViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_crop_card, parent, false);
        return new CropViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CropViewHolder holder, int position) {
        Crop crop = cropList.get(position);
        holder.cropName.setText(crop.getName());
        holder.diseaseName.setText(crop.getDisease());
        holder.severityProgress.setProgress(crop.getSeverity());

        Glide.with(context)
                .load(crop.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .into(holder.cropImage);
    }

    @Override
    public int getItemCount() {
        return cropList.size();
    }

    public static class CropViewHolder extends RecyclerView.ViewHolder {
        TextView cropName, diseaseName;
        ProgressBar severityProgress;
        ImageView cropImage;

        public CropViewHolder(@NonNull View itemView) {
            super(itemView);
            cropName = itemView.findViewById(R.id.cropName);
            diseaseName = itemView.findViewById(R.id.diseaseName);
            severityProgress = itemView.findViewById(R.id.severityProgress);
            cropImage = itemView.findViewById(R.id.cropImage);
        }
    }
}
