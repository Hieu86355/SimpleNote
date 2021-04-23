package com.h86355.simplenote.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.h86355.simplenote.R;
import com.h86355.simplenote.databinding.FragmentSettingBinding;

import java.io.ByteArrayOutputStream;

import static androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;


public class SettingFragment extends Fragment {
    private static final String TAG = "SettingFragment";
    private static final int PICK_IMAGE_REQUEST = 321;
    private FragmentSettingBinding binding;
    private Dialog loadingDialog;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingBinding.inflate(inflater, container, false);
        binding.userEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());

        boolean isNightMode = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("NIGHT_MODE", false);
        RequestOptions requestOptions = new RequestOptions();
        if (isNightMode) {
            requestOptions.placeholder(R.drawable.user_night);
        } else {
            requestOptions.placeholder(R.drawable.user_day);
        }
        Glide.with(getActivity())
                .applyDefaultRequestOptions(requestOptions)
                .load(FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl())
                .into(binding.imgProfile);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        handleClick();
        setDialog();
    }

    private void handleClick() {
        binding.logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingDialog.show();
                logoutUser();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.dismiss();
                        getFragmentManager().popBackStackImmediate(null, POP_BACK_STACK_INCLUSIVE);
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.main_container, new LoginFragment())
                                .commit();
                    }
                }, 1000);
            }
        });

        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager().popBackStack();
            }
        });

        binding.imgProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            }
        });
    }

    private void setDialog() {
        loadingDialog = new Dialog(getActivity());
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        loadingDialog.setCanceledOnTouchOutside(false);
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            binding.imgProgress.setVisibility(View.VISIBLE);
            upLoadProfileImage(data.getData());
        }
    }

    private void upLoadProfileImage(Uri uri) {
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference()
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("ProfileImage")
                .child("profile_img.jpg");
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
            byte[] data = baos.toByteArray();

            UploadTask uploadTask = storageRef.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "onSuccess: upLoadProfileImage: " + e.getMessage());
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.d(TAG, "onSuccess: ");
                    binding.imgProgress.setVisibility(View.GONE);
                    taskSnapshot.getStorage().getDownloadUrl()
                            .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    updateUserProfile(uri);
                                }
                            });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "upLoadProfileImage: " + e.getMessage());
        }
    }

    private void updateUserProfile(Uri uri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();
        user.updateProfile(profileUpdate)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Glide.with(getActivity())
                                    .load(FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl())
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .into(binding.imgProfile);
                            binding.imgProgress.setVisibility(View.GONE);
                        } else {
                            Log.e(TAG, "onComplete: updateUserProfile: " + task.getException().getMessage());
                        }
                    }
                });
    }
}
