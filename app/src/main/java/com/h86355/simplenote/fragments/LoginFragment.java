package com.h86355.simplenote.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.h86355.simplenote.R;
import com.h86355.simplenote.databinding.DialogForgetPasswordBinding;
import com.h86355.simplenote.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private static final int GOOGLE_SIGN_IN_REQUEST = 123;
    private FragmentLoginBinding binding;

    private Dialog loadingDialog;

    private FirebaseAuth auth = FirebaseAuth.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        Window window = getActivity().getWindow();
        window.setStatusBarColor(getActivity().getResources().getColor(android.R.color.transparent, getActivity().getTheme()));
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        boolean isNightMode = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("NIGHT_MODE", false);
        if (isNightMode) {
            binding.waveAnim1.setAnimation(R.raw.wave_night);
            binding.waveAnim2.setAnimation(R.raw.wave_night);
        } else {
            binding.waveAnim1.setAnimation(R.raw.wave_day);
            binding.waveAnim2.setAnimation(R.raw.wave_day);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginWithEmailAndPassword();
            }
        });

        binding.register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left,
                                R.anim.enter_from_left, R.anim.exit_to_right)
                        .add(R.id.main_container, new RegisterFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        binding.btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignIn();
            }
        });

        binding.textForget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendResetPasswordEmail();
            }
        });
    }

    private void sendResetPasswordEmail() {
        Dialog rsPasswordDialog = new Dialog(getActivity());
        DialogForgetPasswordBinding forgetPasswordBinding = DialogForgetPasswordBinding.inflate(LayoutInflater.from(getActivity()));
        rsPasswordDialog.setContentView(forgetPasswordBinding.getRoot());
        rsPasswordDialog.show();

        forgetPasswordBinding.btnForgetSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = forgetPasswordBinding.inputForgetEmail.getText().toString();
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    forgetPasswordBinding.textForgetPW.setText("Invalid email!");
                    return;
                }
                rsPasswordDialog.dismiss();
                loadingDialog.show();
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                binding.textSignin.setText("Your password reset email has been sent!");
                                loadingDialog.dismiss();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "onFailure: " + e.getMessage() );
                                binding.textSignin.setText("An error has occurred!");
                                loadingDialog.dismiss();
                            }
                        });
            }
        });

    }

    private void googleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
        startActivityForResult(new Intent(googleSignInClient.getSignInIntent()), GOOGLE_SIGN_IN_REQUEST);
        loadingDialog.show();
    }

    private void loginWithEmailAndPassword() {

        String email = binding.inputEmail.getText().toString();
        String password = binding.inputPassword.getText().toString();
        if (email.equals("") || password.equals("")) {
            binding.textSignin.setText("Email or password cannot be empty!");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textSignin.setText("Invalid email!");
            return;
        }
        if (password.length() < 6) {
            binding.textSignin.setText("Invalid password!");
            return;
        }
        loadingDialog.show();
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: login successful: " + auth.getCurrentUser().getEmail());
                            loadingDialog.dismiss();
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
                                binding.textSignin.setText("Invalid email or password!");
                            } catch (FirebaseNetworkException e) {
                                Log.e(TAG, "No internet connection");
                                binding.textSignin.setText("No internet connection!");
                            } catch (FirebaseAuthInvalidUserException e) {
                                Log.e(TAG, e.getMessage());
                                binding.textSignin.setText("Invalid email or password!");
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage());
                                binding.textSignin.setText("An error has occurred!");
                            }
                            loadingDialog.dismiss();
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_SIGN_IN_REQUEST) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e(TAG, "onActivityResult: " + e.getMessage());
                loadingDialog.dismiss();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success: " + auth.getCurrentUser().getEmail());
                            getFragmentManager()
                                    .beginTransaction()
                                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left,
                                            R.anim.enter_from_left, R.anim.exit_to_right)
                                    .replace(R.id.main_container, new MainFragment())
                                    .commit();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.d(TAG, "signInWithCredential:failed");
                            binding.textSignin.setText("An error has occurred!");
                        }
                        loadingDialog.dismiss();
                    }
                });
    }
}
