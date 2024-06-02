package com.example.myapplication;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

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

public class SecondActivity extends AppCompatActivity {

    private VideoView vv;
    private TextView responseTextView;
    private Handler handler;
    private Runnable updateTask;
    public static String currentStatus = ""; // static으로 변경
    private static final String CHANNEL_ID = "Notification_Channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 101;

    private DatabaseHelper databaseHelper; // 데이터베이스 헬퍼 객체

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        databaseHelper = new DatabaseHelper(this); // 데이터베이스 헬퍼 초기화

        Button bt = findViewById(R.id.bt_second);
        Button btGetArea = findViewById(R.id.bt_get_area);
        vv = findViewById(R.id.vv);
        responseTextView = findViewById(R.id.responseTextView);

        // 비디오 Uri 설정
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.slide_vid);

        // 비디오뷰의 재생, 일시정지 등을 할 수 있는 '컨트롤바'를 붙여주는 작업
        vv.setMediaController(new MediaController(this));

        // VideoView가 보여줄 동영상의 경로 주소(Uri) 설정하기
        vv.setVideoURI(videoUri);

        // 비디오 로딩 준비가 끝났을 때 실행하도록 리스너 설정
        vv.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // 비디오 시작
                vv.start();
            }
        });

        // 버튼 클릭 이벤트 설정
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 영상 재생
                if (!vv.isPlaying()) {
                    vv.start();
                }
            }
        });

        // '선택 영역 확인' 버튼 클릭 이벤트 설정
        btGetArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // GetArea 액티비티 시작
                Intent intent = new Intent(SecondActivity.this, GetArea.class);
                startActivity(intent);
            }
        });

        handler = new Handler(Looper.getMainLooper());
        updateTask = new Runnable() {
            @Override
            public void run() {
                new SecondActivity.GetRequestTask().execute("http://172.30.1.50:5000/");
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(updateTask);

        createNotificationChannel();
        requestNotificationPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vv != null) vv.stopPlayback();
        handler.removeCallbacks(updateTask); // 액티비티가 파괴될 때 루프를 멈춥니다.
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (vv != null && vv.isPlaying()) vv.pause();
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
                } else {
                    String status = extractStatus(statusMessage);
                    if (status != null) {
                        currentStatus = status; // 상태를 저장합니다.
                        responseTextView.setText("현재 동작: " + status);

                        // 현재 시간 가져오기
                        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.HH:mm:ss");
                        String currentDateAndTime = sdf.format(new Date());

                        // 데이터베이스에 상태와 시간 저장
                        databaseHelper.insertStatus(status, currentDateAndTime);

                        // 상태에 따라 알림을 표시합니다.
                        if ("Backward!".equals(status) || "Walk!".equals(status)) {
                            Log.d("SecondActivity", "Notification detected, showing notification");
                            showNotification(status.equals("Backward!") ? "알림: 미끄럼틀 역행 발생!" : "알림: 그네 충돌 위험!");
                        }
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (!result.isEmpty()) {
                if (result.contains("Server returned: 500")) {
                    handler.removeCallbacks(updateTask); // 요청을 중지합니다.
                } else {
                    String status = extractStatus(result);
                    if (status != null) {
                        currentStatus = status; // 상태를 저장합니다.
                        responseTextView.setText("현재 동작: " + status);

                        // 현재 시간 가져오기
                        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.HH:mm:ss");
                        String currentDateAndTime = sdf.format(new Date());

                        // 데이터베이스에 상태와 시간 저장
                        databaseHelper.insertStatus(status, currentDateAndTime);

                        // 상태에 따라 알림을 표시합니다.
                        if ("Backward!".equals(status) || "Walk!".equals(status)) {
                            Log.d("SecondActivity", "Notification detected, showing notification");
                            showNotification(status.equals("Backward!") ? "알림: 미끄럼틀 역행 발생!" : "알림: 그네 충돌 위험!");
                        }
                    }
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

    private void showNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)  // 알림 아이콘 설정 (실제 존재하는 아이콘으로 변경)
                .setContentTitle("Alert")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 1000})  // 진동 패턴 설정
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);  // 기본 알림 소리 설정

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }
}
