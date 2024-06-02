package com.example.myapplication;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ThirdActivity extends AppCompatActivity {

    private TextView statusTextView;
    private DatabaseHelper databaseHelper; // 데이터베이스 헬퍼 객체

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third);

        statusTextView = findViewById(R.id.statusTextView);
        databaseHelper = new DatabaseHelper(this); // 데이터베이스 헬퍼 초기화

        loadStatusLog();
    }

    private void loadStatusLog() {
        Cursor cursor = databaseHelper.getAllStatus();
        StringBuilder stringBuilder = new StringBuilder();

        while (cursor.moveToNext()) {
            String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
            stringBuilder.append(timestamp).append(", Status: ").append(status).append("\n");
        }
        cursor.close();

        statusTextView.setText(stringBuilder.toString());
    }
}
