package com.Joseph.agroshieldapp.models;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

public class ImagePickerDialogFragment extends DialogFragment {

    public interface ImagePickerListener {
        void onCameraSelected();
        void onGallerySelected();
    }

    private ImagePickerListener listener;

    public void setListener(ImagePickerListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose Image Source")
                .setItems(new CharSequence[]{"Camera", "Gallery"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                if (listener != null) listener.onCameraSelected();
                                break;
                            case 1:
                                if (listener != null) listener.onGallerySelected();
                                break;
                        }
                    }
                });
        return builder.create();
    }
}