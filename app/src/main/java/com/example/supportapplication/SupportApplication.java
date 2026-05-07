package com.example.supportapplication;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class SupportApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
