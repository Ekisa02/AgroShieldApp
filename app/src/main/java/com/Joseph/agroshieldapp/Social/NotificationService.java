package com.Joseph.agroshieldapp.Social;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.Joseph.agroshieldapp.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;

public class NotificationService extends FirebaseMessagingService {
    private static final String TAG = "NotificationService";
    private static final String CHANNEL_ID = "agroshield_notifications";
    private static final String CHANNEL_NAME = "AgroShield Notifications";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "Message received: " + remoteMessage.getData());

        if (remoteMessage.getData().size() > 0) {
            String type = remoteMessage.getData().get("type");
            String senderId = remoteMessage.getData().get("senderId");
            String senderName = remoteMessage.getData().get("senderName");

            if ("follow_back".equals(type) && senderName != null) {
                showFollowBackNotification(senderName);
            } else if ("new_follower".equals(type) && senderName != null) {
                showNewFollowerNotification(senderName);
            }
        }
    }

    private void showFollowBackNotification(String senderName) {
        if (!areNotificationsEnabled() || !checkNotificationPermission()) {
            Log.w(TAG, "Notifications not enabled or permission not granted");
            return;
        }

        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("New Follower Back!")
                    .setContentText(senderName + " started following you back!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_SOCIAL);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());

            Log.d(TAG, "Follow back notification shown for: " + senderName);
        } catch (Exception e) {
            Log.e(TAG, "Error showing follow back notification: " + e.getMessage());
        }
    }

    private void showNewFollowerNotification(String senderName) {
        if (!areNotificationsEnabled() || !checkNotificationPermission()) {
            Log.w(TAG, "Notifications not enabled or permission not granted");
            return;
        }

        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("New Follower")
                    .setContentText(senderName + " started following you!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_SOCIAL);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());

            Log.d(TAG, "New follower notification shown for: " + senderName);
        } catch (Exception e) {
            Log.e(TAG, "Error showing new follower notification: " + e.getMessage());
        }
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        // For Android versions below Tiramisu, notification permission is granted by default
        return true;
    }

    private boolean areNotificationsEnabled() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        return notificationManager.areNotificationsEnabled();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for social interactions and updates");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        saveTokenToFirestore(token);
    }

    private void saveTokenToFirestore(String token) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId != null) {
            FirebaseFirestore.getInstance().collection("userTokens")
                    .document(userId)
                    .set(new HashMap<String, Object>() {{
                        put("token", token);
                        put("timestamp", System.currentTimeMillis());
                        put("platform", "android");
                    }})
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "FCM token saved to Firestore"))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error saving FCM token: " + e.getMessage()));
        } else {
            Log.w(TAG, "User not authenticated, cannot save FCM token");
        }
    }
}