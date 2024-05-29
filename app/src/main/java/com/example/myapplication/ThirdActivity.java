package com.example.myapplication;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ThirdActivity extends AppCompatActivity {

    private TextView responseTextView;
    private static StringBuilder accumulatedResponse = new StringBuilder();
    private Handler handler;
    private Runnable updateTask;
    public static String currentStatus; // Static 변수로 변경

    private static final String CHANNEL_ID = "notification_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third);

        responseTextView = findViewById(R.id.responseTextView);

        // 기존 accumulatedResponse 내용을 복구합니다.
        responseTextView.setText(accumulatedResponse.toString());

        // Notification 채널 설정 (Android 8.0 이상)
        createNotificationChannel();

        // Notification 권한 요청 (Android 13 이상)
        requestNotificationPermission();

        // Handler와 Runnable을 이용하여 주기적으로 서버 상태를 업데이트합니다.
        handler = new Handler(Looper.getMainLooper());
        updateTask = new Runnable() {
            @Override
            public void run() {
                new GetRequestTask().execute("http://10.0.2.2:5000/");
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(updateTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTask); // 액티비티가 파괴될 때 루프를 멈춥니다.
    }

    private class GetRequestTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... urls) {
            String response = "";
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    urlConnection.setRequestMethod("GET");
                    int responseCode = urlConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            publishProgress(inputLine);
                        }
                        in.close();
                    } else {
                        response = "Server returned: " + responseCode;
                    }
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                response = e.getMessage();
            }
            return response;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (values.length > 0) {
                String statusMessage = values[0];
                if (statusMessage.contains("Server returned: 500")) {
                    handler.removeCallbacks(updateTask); // 요청을 중지합니다.
                    accumulatedResponse.append("서버 오류: 500 - 요청 중지됨\n");
                } else {
                    String status = extractStatus(statusMessage);
                    if (status != null) {
                        currentStatus = status; // 상태를 저장합니다.
                        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.HH:mm:ss");  // 날짜 포맷
                        String currentDateandTime = sdf.format(new Date());  // 현재 시간을 포맷에 맞게 가져옴
                        accumulatedResponse.append("현재 동작: ").append(status).append("    ").append(currentDateandTime).append("\n");

                        // 상태에 따라 알림을 표시합니다.
                        if ("Backward!".equals(status)) {
                            Log.d("ThirdActivity", "Backward detected, showing notification");
                            showNotification("알림: 미끄럼틀 역행 발생!", currentDateandTime);
                        } else if ("Walk!".equals(status)) {
                            Log.d("ThirdActivity", "Walk detected, showing notification");
                            showNotification("알림: 그네 충돌 위험!", currentDateandTime);
                        }
                    }
                }
                responseTextView.setText(accumulatedResponse.toString());

                // 자동 스크롤
                final int scrollAmount = responseTextView.getLayout().getLineTop(responseTextView.getLineCount()) - responseTextView.getHeight();
                if (scrollAmount > 0) {
                    responseTextView.scrollTo(0, scrollAmount);
                } else {
                    responseTextView.scrollTo(0, 0);
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (!result.isEmpty()) {
                if (result.contains("Server returned: 500")) {
                    handler.removeCallbacks(updateTask); // 요청을 중지합니다.
                    accumulatedResponse.append("서버 오류: 500 - 요청 중지됨\n");
                } else {
                    String status = extractStatus(result);
                    if (status != null) {
                        currentStatus = status; // 상태를 저장합니다.
                        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.HH:mm");  // 날짜 포맷
                        String currentDateandTime = sdf.format(new Date());  // 현재 시간을 포맷에 맞게 가져옴
                        accumulatedResponse.append("현재 동작: ").append(status).append("    ").append(currentDateandTime).append("\n");

                        // 상태에 따라 알림을 표시합니다.
                        if ("Backward!".equals(status)) {
                            Log.d("ThirdActivity", "Backward detected, showing notification");
                            showNotification("알림: 미끄럼틀 역행 발생!", currentDateandTime);
                        } else if ("Walk!".equals(status)) {
                            Log.d("ThirdActivity", "Walk detected, showing notification");
                            showNotification("알림: 그네 충돌 위험!", currentDateandTime);
                        }
                    }
                }
                responseTextView.setText(accumulatedResponse.toString());

                // 자동 스크롤
                final int scrollAmount = responseTextView.getLayout().getLineTop(responseTextView.getLineCount()) - responseTextView.getHeight();
                if (scrollAmount > 0) {
                    responseTextView.scrollTo(0, scrollAmount);
                } else {
                    responseTextView.scrollTo(0, 0);
                }
            }
        }

        private String extractStatus(String jsonResponse) {
            try {
                JSONObject jsonObject = new JSONObject(jsonResponse);
                return jsonObject.getString("state");
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    // Notification 채널 생성 (Android 8.0 이상 필요)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notification Channel";
            String description = "Channel for head-up notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Notification 표시
    private void showNotification(String message, String timestamp) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)  // 알림 아이콘 설정 (실제 존재하는 아이콘으로 변경)
                .setContentTitle("Alert")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());

        // 로그에 알림 추가
        accumulatedResponse.append(message).append("    ").append(timestamp).append("\n");
        responseTextView.setText(accumulatedResponse.toString());
        // 자동 스크롤
        final int scrollAmount = responseTextView.getLayout().getLineTop(responseTextView.getLineCount()) - responseTextView.getHeight();
        if (scrollAmount > 0) {
            responseTextView.scrollTo(0, scrollAmount);
        } else {
            responseTextView.scrollTo(0, 0);
        }
    }

    // Notification 권한 요청 (Android 13 이상 필요)
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }
}
