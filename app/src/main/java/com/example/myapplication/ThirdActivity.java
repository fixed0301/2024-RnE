package com.example.myapplication;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import org.json.JSONObject;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ThirdActivity extends AppCompatActivity {

    private TextView responseTextView;
    private static StringBuilder accumulatedResponse = new StringBuilder(); // 정적 변수로 변경
    private Handler handler;
    private Runnable updateTask;
    private String currentStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third);

        responseTextView = findViewById(R.id.responseTextView);

        // 기존 accumulatedResponse 내용을 복구합니다.
        responseTextView.setText(accumulatedResponse.toString());

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
                        accumulatedResponse.append("현재 동작: ").append(status).append("\n");
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
                        accumulatedResponse.append("현재 동작: ").append(status).append("\n");
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
                // 분류 결과를 실시간 영상 보기 쪽으로 전송
                Intent intent = new Intent(ThirdActivity.this, SecondActivity.class);
                intent.putExtra("status", currentStatus);
                Log.d("ThirdActivity", "Sending status: " + currentStatus); // Log 메시지 추가
                setResult(RESULT_OK, intent);
                finish();

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
}
