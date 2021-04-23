package com.h86355.simplenote;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.h86355.simplenote.databinding.ActivityMainBinding;
import com.h86355.simplenote.fragments.LoginFragment;
import com.h86355.simplenote.fragments.MainFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initNightMode();
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void initNightMode() {
        boolean isNightMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("NIGHT_MODE", false);
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(binding.mainContainer.getId(), new MainFragment())
                        .commit();
            }
        } else {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(binding.mainContainer.getId(), new LoginFragment())
                        .commit();
            }
        }
    }
}