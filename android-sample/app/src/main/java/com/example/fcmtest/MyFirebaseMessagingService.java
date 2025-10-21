package com.example.fcmtest;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
	private static final String CHANNEL_ID = "fcm_default_channel";
	private static final int NOTIFICATION_ID = 1;

	@Override
	public void onMessageReceived(RemoteMessage message) {
		Log.d("FCM", "Message from: " + message.getFrom());
		
		// Tạo notification channel nếu chưa có
		createNotificationChannel();
		
		// Hiển thị thông báo
		showNotification(message);
	}

	@Override
	public void onNewToken(String token) {
		Log.d("FCM", "New token: " + token);
		TokenUploader.uploadToken(token, "MyPhone");
	}

	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
				CHANNEL_ID,
				"FCM Notifications",
				NotificationManager.IMPORTANCE_HIGH
			);
			channel.setDescription("Channel for FCM notifications");
			
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
		}
	}

	private void showNotification(RemoteMessage message) {
		String title = "FCM Notification";
		String body = "You have a new message";
		
		// Lấy title và body từ message
		if (message.getNotification() != null) {
			if (message.getNotification().getTitle() != null) {
				title = message.getNotification().getTitle();
			}
			if (message.getNotification().getBody() != null) {
				body = message.getNotification().getBody();
			}
		}
		
		// Tạo intent để mở app khi click vào notification
		Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(
			this, 0, intent, 
			PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
		);
		
		// Tạo notification
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
			.setSmallIcon(android.R.drawable.ic_dialog_info)
			.setContentTitle(title)
			.setContentText(body)
			.setAutoCancel(true)
			.setContentIntent(pendingIntent)
			.setPriority(NotificationCompat.PRIORITY_HIGH);
		
		// Hiển thị notification
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
		
		Log.d("FCM", "Notification displayed: " + title + " - " + body);
	}
}
