package com.h86355.simplenote.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.h86355.simplenote.R;
import com.h86355.simplenote.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {

    private static final String TAG = "RegisterFragment";
    private FragmentRegisterBinding binding;
    private Dialog loadingDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);

        boolean isNightMode = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("NIGHT_MODE", false);
        if (isNightMode) {
            binding.registerAnim1.setAnimation(R.raw.wave_night);
            binding.registerAnim2.setAnimation(R.raw.wave_night);
        } else {
            binding.registerAnim1.setAnimation(R.raw.wave_day);
            binding.registerAnim2.setAnimation(R.raw.wave_day);
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        handleClick();
        setDialog();
    }

    private void setDialog() {
        loadingDialog = new Dialog(getActivity());
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        loadingDialog.setCanceledOnTouchOutside(false);
    }

    private void handleClick() {
        binding.login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager().popBackStack();
            }
        });

        binding.btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerAccount();
            }
        });
    }

    private void registerAccount() {
        String email = binding.inputRegisterEmail.getText().toString();
        String password = binding.inputRegisterPassword.getText().toString();
        String conf_password = binding.inputRegisterConfPassword.getText().toString();

        if (!password.equals(conf_password)) {
            binding.textRegister.setText("Password doesn't match! Try again");
            return;
        }
        if (email.equals("") || password.equals("")) {
            binding.textRegister.setText("Email or password cannot be empty!");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textRegister.setText("Invalid email!");
            return;
        }
        if (password.length() < 6) {
            binding.textRegister.setText("Password must be at least 6 characters!");
            return;
        }

        loadingDialog.show();
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            getFragmentManager()
                                    .beginTransaction()
                                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left,
                                            R.anim.enter_from_left, R.anim.exit_to_right)
                                    .replace(R.id.main_container, new MainFragment())
                                    .commit();
                        } else {
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                Log.e(TAG, "Invalid email or password");
                                binding.textRegister.setText("Invalid email or password!");
                            } catch (FirebaseNetworkException e) {
                                Log.e(TAG, "No internet connection");
                                binding.textRegister.setText("No internet connection!");
                            } catch (FirebaseAuthInvalidUserException e) {
                                Log.e(TAG, e.getMessage());
                                binding.textRegister.setText("Invalid email or password!");
                            } catch (FirebaseAuthUserCollisionException e) {
                                Log.e(TAG, e.getMessage());
                                binding.textRegister.setText("The email address is already in use!");
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage());
                                binding.textRegister.setText("An error has occurred!");
                            }
                        }
                        loadingDialog.dismiss();
                    }
                });
    }
}
