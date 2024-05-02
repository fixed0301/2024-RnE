package com.example.myapplication;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;

public class FirebaseInstanceIDServices extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseIIDService";

    @Override
    public void onNewToken(String token) {
        Log.e(TAG, token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // 서버로 토큰을 전송하는 코드를 작성합니다.
    }
}
