package com.example.fcmtest;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
	private TextView tokenView;
	private TextView statusView;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Yêu cầu quyền notification cho Android 13+
		requestNotificationPermission();

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		int pad = (int) (16 * getResources().getDisplayMetrics().density);
		root.setPadding(pad, pad, pad, pad);

		statusView = new TextView(this);
		statusView.setText("Fetching FCM token...");
		root.addView(statusView);

		tokenView = new TextView(this);
		tokenView.setTextIsSelectable(true);
		ScrollView scroll = new ScrollView(this);
		scroll.addView(tokenView);
		root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

		Button refreshBtn = new Button(this);
		refreshBtn.setText("Lấy lại token");
		refreshBtn.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) { fetchToken(); }
		});
		root.addView(refreshBtn);

		Button forceRefreshBtn = new Button(this);
		forceRefreshBtn.setText("Force Refresh Token");
		forceRefreshBtn.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) { forceRefreshToken(); }
		});
		root.addView(forceRefreshBtn);

		Button copyBtn = new Button(this);
		copyBtn.setText("Sao chép token");
		copyBtn.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) { copyToken(); }
		});
		root.addView(copyBtn);

		setContentView(root);
		fetchToken();
	}

	private void requestNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
				!= PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, 
					new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
			}
		}
	}

	private void fetchToken() {
		statusView.setText("Đang lấy token...");
		// Force refresh token by deleting and getting new one
		FirebaseMessaging.getInstance().deleteToken()
				.addOnCompleteListener(deleteTask -> {
					FirebaseMessaging.getInstance().getToken()
							.addOnCompleteListener(task -> {
								if (!task.isSuccessful()) {
									statusView.setText("Lấy token thất bại: " + task.getException().getMessage());
									Log.e("FCM", "Failed to get token", task.getException());
									return;
								}
								String token = task.getResult();
								Log.d("FCM", "token=" + token);
								Log.d("FCM", "token length=" + (token != null ? token.length() : "null"));
								Log.d("FCM", "token valid format=" + isValidFCMToken(token));
								tokenView.setText(token);
								statusView.setText("Đã lấy token mới thành công");
							});
				});
	}
	
	private void forceRefreshToken() {
		statusView.setText("Force refreshing token...");
		// Force refresh by deleting token and getting new one
		FirebaseMessaging.getInstance().deleteToken()
				.addOnCompleteListener(deleteTask -> {
					if (!deleteTask.isSuccessful()) {
						Log.e("FCM", "Failed to delete token", deleteTask.getException());
					}
					// Wait a bit then get new token
					new android.os.Handler().postDelayed(() -> {
						FirebaseMessaging.getInstance().getToken()
								.addOnCompleteListener(task -> {
									if (!task.isSuccessful()) {
										statusView.setText("Force refresh failed: " + task.getException().getMessage());
										Log.e("FCM", "Failed to get token after force refresh", task.getException());
										return;
									}
									String token = task.getResult();
									Log.d("FCM", "Force refreshed token=" + token);
									Log.d("FCM", "Force refreshed token length=" + (token != null ? token.length() : "null"));
									Log.d("FCM", "Force refreshed token valid format=" + isValidFCMToken(token));
									tokenView.setText(token);
									statusView.setText("Force refresh completed");
								});
					}, 2000); // Wait 2 seconds
				});
	}

	private boolean isValidFCMToken(String token) {
		if (token == null || token.trim().isEmpty()) {
			return false;
		}
		// FCM token should be at least 140 characters and contain valid characters including colons
		return token.length() >= 140 && token.matches("[A-Za-z0-9_:.-]+");
	}

	private void copyToken() {
		String token = tokenView.getText().toString();
		if (token.isEmpty()) {
			Toast.makeText(this, "Chưa có token để sao chép", Toast.LENGTH_SHORT).show();
			return;
		}
		ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("FCM Token", token);
		clipboard.setPrimaryClip(clip);
		Toast.makeText(this, "Đã sao chép token", Toast.LENGTH_SHORT).show();
	}
}
