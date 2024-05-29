package com.example.myapplication;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView;

public class GetArea extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_area);

        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.first_image); // 이미지 리소스 설정
    }
}
