package com.h86355.simplenote.fragments;

import android.animation.Animator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.h86355.simplenote.NoteViewModel;
import com.h86355.simplenote.R;
import com.h86355.simplenote.adapters.INote;
import com.h86355.simplenote.adapters.NoteAdapter;
import com.h86355.simplenote.databinding.FragmentMainBinding;
import com.h86355.simplenote.models.Note;

public class MainFragment extends Fragment implements INote {

    private static final String TAG = "MainFragment";
    private FragmentMainBinding binding;

    private NoteAdapter noteAdapter;
    private NoteViewModel noteViewModel;

    private boolean isNightMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);

        Window window = getActivity().getWindow();
        window.setStatusBarColor(getActivity().getResources().getColor(R.color.background, getActivity().getTheme()));
        isNightMode = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("NIGHT_MODE", false);
        if (isNightMode){
            binding.btnNightMode.setProgress(0.7f);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } else {
            binding.btnNightMode.setProgress(0.4f);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        noteViewModel = new ViewModelProvider(requireActivity()).get(NoteViewModel.class);
        initRecyclerView();
        handleClick();
        initSearchFilter();
    }

    @Override
    public void onStart() {
        super.onStart();
        noteAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        noteAdapter.stopListening();
    }


    private void initRecyclerView() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() != null) {
            CollectionReference noteRef = db
                    .collection("Users")
                    .document(auth.getCurrentUser().getUid())
                    .collection("notes");
            Query query = noteRef.orderBy("timestamp", Query.Direction.DESCENDING);
            FirestoreRecyclerOptions<Note> options = new FirestoreRecyclerOptions.Builder<Note>()
                    .setQuery(query, Note.class)
                    .build();
            noteAdapter = new NoteAdapter(options, this);
            binding.recyclerNote.setHasFixedSize(true);
            binding.recyclerNote.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
            binding.recyclerNote.setAdapter(noteAdapter);

        }
    }

    private void handleClick() {
        binding.btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                noteViewModel.setSelectedNote(null);
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left,
                                R.anim.enter_from_left, R.anim.exit_to_right)
                        .add(R.id.main_container, new AddNoteFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        binding.profileSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right,
                                R.anim.enter_from_right, R.anim.exit_to_left)
                        .replace(R.id.main_container, new SettingFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        binding.btnNightMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.btnNightMode.setMinAndMaxProgress(0.4f, 0.7f);
                if (!isNightMode) {
                    binding.btnNightMode.setSpeed(1.5f);
                } else {
                    binding.btnNightMode.setSpeed(-1.5f);
                }
                binding.btnNightMode.addAnimatorListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        if (isNightMode) {
                            isNightMode = false;
                            saveNightModeState(isNightMode);
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            getActivity().recreate();
                        } else {
                            isNightMode = true;
                            saveNightModeState(isNightMode);
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            getActivity().recreate();
                        }

                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                });
                binding.btnNightMode.playAnimation();

            }
        });

    }

    private void initSearchFilter() {
        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                if (auth.getCurrentUser() != null) {
                    CollectionReference noteRef = db
                            .collection("Users")
                            .document(auth.getCurrentUser().getUid())
                            .collection("notes");

                    Query query = noteRef.orderBy("timestamp", Query.Direction.DESCENDING);
                    FirestoreRecyclerOptions<Note> options = new FirestoreRecyclerOptions.Builder<Note>()
                            .setQuery(query, Note.class)
                            .build();

                    Query searchQuery = noteRef
                            .whereGreaterThanOrEqualTo("title", editable.toString())
                            .whereLessThanOrEqualTo("title", editable.toString() + "\uf8ff");
                    FirestoreRecyclerOptions<Note> searchOptions = new FirestoreRecyclerOptions.Builder<Note>()
                            .setQuery(searchQuery, Note.class)
                            .build();
                    if (editable.toString().equals("")) {
                        noteAdapter.updateOptions(options);
                    } else {
                        noteAdapter.updateOptions(searchOptions);
                    }
                }
            }
        });

        binding.inputSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    binding.inputSearch.clearFocus();
                }
                return false;
            }
        });
    }

    private void saveNightModeState(boolean b) {
        SharedPreferences sf = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sf.edit();
        editor.putBoolean("NIGHT_MODE", b);
        editor.apply();
    }

    @Override
    public void onNoteClick(int position) {
        noteViewModel.setSelectedNote(noteAdapter.getSelectedNote(position));
        noteViewModel.setSelectedNoteId(noteAdapter.getSelectedNoteId(position));
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left,
                        R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.main_container, new AddNoteFragment())
                .addToBackStack(null)
                .commit();
    }

}
