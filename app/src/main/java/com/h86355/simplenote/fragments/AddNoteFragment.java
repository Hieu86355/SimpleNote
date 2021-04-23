package com.h86355.simplenote.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.h86355.simplenote.NoteViewModel;
import com.h86355.simplenote.R;
import com.h86355.simplenote.databinding.DialogColorPickerBinding;
import com.h86355.simplenote.databinding.FragmentAddNoteBinding;
import com.h86355.simplenote.models.Note;
import com.h86355.simplenote.utils.TextViewUndoRedo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddNoteFragment extends Fragment {
    private static final String TAG = "AddNoteFragment";
    private FragmentAddNoteBinding binding;

    private NoteViewModel noteViewModel;
    private Note updateNote;
    private String selectedNoteColor;
    private String pickedColor = "";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddNoteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        subscribeObservers();
        handleClick();

    }

    private void handleClick() {
        binding.btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                getFragmentManager().popBackStack();
            }
        });

        binding.btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.inputTitle.equals("") && binding.inputContent.equals("")) {
                    binding.btnGoBack.performClick();
                    return;
                }
                saveNote();
                try {
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                binding.btnGoBack.performClick();
            }
        });


        // menu options
        PopupMenu popupMenu = new PopupMenu(getActivity(), binding.btnMore);
        popupMenu.getMenuInflater().inflate(R.menu.add_note_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_delete:
                        deleteNote();
                        break;
                    case R.id.menu_color:
                        setColorPicker();
                        break;
                }
                return false;
            }
        });
        binding.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupMenu.show();
            }
        });

        //Undo - Redo
        TextViewUndoRedo titleUndoRedo = new TextViewUndoRedo(binding.inputTitle);
        TextViewUndoRedo contentUndoRedo = new TextViewUndoRedo(binding.inputContent);

        binding.btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.inputTitle.isFocused()) {
                    titleUndoRedo.undo();
                }
                if (binding.inputContent.isFocused()) {
                    contentUndoRedo.undo();
                }
            }
        });

        binding.btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.inputTitle.isFocused()) {
                    titleUndoRedo.redo();
                }
                if (binding.inputContent.isFocused()) {
                    contentUndoRedo.redo();
                }
            }
        });


    }

    private void subscribeObservers() {
        noteViewModel = new ViewModelProvider(requireActivity()).get(NoteViewModel.class);
        noteViewModel.getSelectedNote().observe(getViewLifecycleOwner(), new Observer<Note>() {
            @Override
            public void onChanged(Note note) {
                if (note == null) {
                    // Add new note
                    newNoteUI();

                } else {
                    // Update selected note
                    selectedNoteUI(note);
                }
            }
        });
    }

    private void newNoteUI() {
        updateNote = null;
        selectedNoteColor = "#" + Integer.toHexString(getResources().getColor(R.color.note_color_default, getActivity().getTheme()));
        GradientDrawable gd = (GradientDrawable) binding.noteIndicator.getBackground();
        gd.setColor(Color.parseColor(selectedNoteColor));
        binding.inputContent.requestFocus();
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(binding.inputContent, InputMethodManager.SHOW_IMPLICIT);
        } catch (NullPointerException e) {
            Log.e(TAG, "newNoteUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void selectedNoteUI(Note note) {
        updateNote = note;
        selectedNoteColor = note.getNoteColor();
        pickedColor = note.getNoteColor();
        binding.inputTitle.setText(note.getTitle());
        binding.inputContent.setText(note.getContent());
        binding.timestamp.setText(
                new SimpleDateFormat("EE, dd-MMMM-yyyy", Locale.getDefault()).format(note.getTimestamp()));
        GradientDrawable gradientDrawable = (GradientDrawable) binding.noteIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(note.getNoteColor()));
    }

    private void saveNote() {
        Note save_note = new Note();

        if (updateNote == null) {
            // add new note
            String title = binding.inputTitle.getText().toString();
            String content = binding.inputContent.getText().toString();
            if (title.equals("") && content.equals("")) {
                return;
            }
            save_note.setTitle(title);
            save_note.setContent(content);
            save_note.setTimestamp(Calendar.getInstance().getTime());
            save_note.setNoteColor(selectedNoteColor);

            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseFirestore
                    .getInstance()
                    .collection("Users")
                    .document(auth.getCurrentUser().getUid())
                    .collection("notes")
                    .add(save_note)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "onFailure: add new note: " + e.getMessage() );
                        }
                    });


        } else {
            // Update existing note
            if (updateNote.getTitle().equals(binding.inputTitle.getText().toString())
                    && updateNote.getContent().equals(binding.inputContent.getText().toString())
                    && updateNote.getNoteColor().equals(selectedNoteColor)) {
                binding.btnGoBack.performClick();
                return;
            }

            save_note.setTitle(binding.inputTitle.getText().toString());
            save_note.setContent(binding.inputContent.getText().toString());
            save_note.setTimestamp(Calendar.getInstance().getTime());
            save_note.setNoteColor(selectedNoteColor);

            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseFirestore
                    .getInstance()
                    .collection("Users")
                    .document(auth.getCurrentUser().getUid())
                    .collection("notes")
                    .document(noteViewModel.getSelectedNoteId())
                    .set(save_note)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "onFailure: update note: " + e.getMessage());
                        }
                    });

        }
    }

    private void setColorPicker() {
        Dialog dialog = new Dialog(getContext());
        DialogColorPickerBinding dialogBinding =
                DialogColorPickerBinding.inflate(LayoutInflater.from(getContext()), null, false);
        dialog.setContentView(dialogBinding.getRoot());
        dialog.show();

        setSelectedColor(dialogBinding);
        setDialogButtonClick(dialogBinding, dialog);

    }

    private void setSelectedColor(DialogColorPickerBinding dialogBinding) {
        String defaultNoteColor = "#" + Integer.toHexString(getActivity().getResources().getColor(R.color.note_color_default, getActivity().getTheme()));
        String noteColor1 = "#" + Integer.toHexString(getActivity().getResources().getColor(R.color.note_color_1, getActivity().getTheme()));
        String noteColor2 = "#" + Integer.toHexString(getActivity().getResources().getColor(R.color.note_color_2, getActivity().getTheme()));
        String noteColor3 = "#" + Integer.toHexString(getActivity().getResources().getColor(R.color.note_color_3, getActivity().getTheme()));
        String noteColor4 = "#" + Integer.toHexString(getActivity().getResources().getColor(R.color.note_color_4, getActivity().getTheme()));
        String noteColor5 = "#" + Integer.toHexString(getActivity().getResources().getColor(R.color.note_color_5, getActivity().getTheme()));
        String noteColor6 = "#" + Integer.toHexString(getActivity().getResources().getColor(R.color.note_color_6, getActivity().getTheme()));

        if (selectedNoteColor.equals(defaultNoteColor)) {
            dialogBinding.doneColorDefault.setVisibility(View.VISIBLE);
            dialogBinding.doneColor1.setVisibility(View.GONE);
            dialogBinding.doneColor2.setVisibility(View.GONE);
            dialogBinding.doneColor3.setVisibility(View.GONE);
            dialogBinding.doneColor4.setVisibility(View.GONE);
            dialogBinding.doneColor5.setVisibility(View.GONE);
            dialogBinding.doneColor6.setVisibility(View.GONE);
        } else if (selectedNoteColor.equals(noteColor1)) {
            dialogBinding.doneColor1.setVisibility(View.VISIBLE);
            dialogBinding.doneColorDefault.setVisibility(View.GONE);
            dialogBinding.doneColor2.setVisibility(View.GONE);
            dialogBinding.doneColor3.setVisibility(View.GONE);
            dialogBinding.doneColor4.setVisibility(View.GONE);
            dialogBinding.doneColor5.setVisibility(View.GONE);
            dialogBinding.doneColor6.setVisibility(View.GONE);
        } else if (selectedNoteColor.equals(noteColor2)) {
            dialogBinding.doneColor2.setVisibility(View.VISIBLE);
            dialogBinding.doneColorDefault.setVisibility(View.GONE);
            dialogBinding.doneColor1.setVisibility(View.GONE);
            dialogBinding.doneColor3.setVisibility(View.GONE);
            dialogBinding.doneColor4.setVisibility(View.GONE);
            dialogBinding.doneColor5.setVisibility(View.GONE);
            dialogBinding.doneColor6.setVisibility(View.GONE);
        } else if (selectedNoteColor.equals(noteColor3)) {
            dialogBinding.doneColor3.setVisibility(View.VISIBLE);
            dialogBinding.doneColorDefault.setVisibility(View.GONE);
            dialogBinding.doneColor1.setVisibility(View.GONE);
            dialogBinding.doneColor2.setVisibility(View.GONE);
            dialogBinding.doneColor4.setVisibility(View.GONE);
            dialogBinding.doneColor5.setVisibility(View.GONE);
            dialogBinding.doneColor6.setVisibility(View.GONE);
        } else if (selectedNoteColor.equals(noteColor4)) {
            dialogBinding.doneColor4.setVisibility(View.VISIBLE);
            dialogBinding.doneColorDefault.setVisibility(View.GONE);
            dialogBinding.doneColor1.setVisibility(View.GONE);
            dialogBinding.doneColor2.setVisibility(View.GONE);
            dialogBinding.doneColor3.setVisibility(View.GONE);
            dialogBinding.doneColor5.setVisibility(View.GONE);
            dialogBinding.doneColor6.setVisibility(View.GONE);
        } else if (selectedNoteColor.equals(noteColor5)) {
            dialogBinding.doneColor5.setVisibility(View.VISIBLE);
            dialogBinding.doneColorDefault.setVisibility(View.GONE);
            dialogBinding.doneColor1.setVisibility(View.GONE);
            dialogBinding.doneColor2.setVisibility(View.GONE);
            dialogBinding.doneColor3.setVisibility(View.GONE);
            dialogBinding.doneColor4.setVisibility(View.GONE);
            dialogBinding.doneColor6.setVisibility(View.GONE);
        } else if (selectedNoteColor.equals(noteColor6)) {
            dialogBinding.doneColor6.setVisibility(View.VISIBLE);
            dialogBinding.doneColorDefault.setVisibility(View.GONE);
            dialogBinding.doneColor1.setVisibility(View.GONE);
            dialogBinding.doneColor2.setVisibility(View.GONE);
            dialogBinding.doneColor3.setVisibility(View.GONE);
            dialogBinding.doneColor4.setVisibility(View.GONE);
            dialogBinding.doneColor5.setVisibility(View.GONE);
        }
    }

    private void setDialogButtonClick(DialogColorPickerBinding dialogBinding, Dialog dialog) {
        dialogBinding.noteColorDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogBinding.doneColorDefault.setVisibility(View.VISIBLE);
                dialogBinding.doneColor1.setVisibility(View.GONE);
                dialogBinding.doneColor2.setVisibility(View.GONE);
                dialogBinding.doneColor3.setVisibility(View.GONE);
                dialogBinding.doneColor4.setVisibility(View.GONE);
                dialogBinding.doneColor5.setVisibility(View.GONE);
                dialogBinding.doneColor6.setVisibility(View.GONE);
                pickedColor = "#" + Integer.toHexString(
                        getActivity().getResources().getColor(R.color.note_color_default, getActivity().getTheme()));

            }
        });

        dialogBinding.noteColor1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogBinding.doneColor1.setVisibility(View.VISIBLE);
                dialogBinding.doneColorDefault.setVisibility(View.GONE);
                dialogBinding.doneColor2.setVisibility(View.GONE);
                dialogBinding.doneColor3.setVisibility(View.GONE);
                dialogBinding.doneColor4.setVisibility(View.GONE);
                dialogBinding.doneColor5.setVisibility(View.GONE);
                dialogBinding.doneColor6.setVisibility(View.GONE);
                pickedColor = "#" + Integer.toHexString(
                        getActivity().getResources().getColor(R.color.note_color_1, getActivity().getTheme()));
            }
        });

        dialogBinding.noteColor2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogBinding.doneColor2.setVisibility(View.VISIBLE);
                dialogBinding.doneColorDefault.setVisibility(View.GONE);
                dialogBinding.doneColor1.setVisibility(View.GONE);
                dialogBinding.doneColor3.setVisibility(View.GONE);
                dialogBinding.doneColor4.setVisibility(View.GONE);
                dialogBinding.doneColor5.setVisibility(View.GONE);
                dialogBinding.doneColor6.setVisibility(View.GONE);
                pickedColor = "#" + Integer.toHexString(
                        getActivity().getResources().getColor(R.color.note_color_2, getActivity().getTheme()));
            }
        });

        dialogBinding.noteColor3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogBinding.doneColor3.setVisibility(View.VISIBLE);
                dialogBinding.doneColorDefault.setVisibility(View.GONE);
                dialogBinding.doneColor1.setVisibility(View.GONE);
                dialogBinding.doneColor2.setVisibility(View.GONE);
                dialogBinding.doneColor4.setVisibility(View.GONE);
                dialogBinding.doneColor5.setVisibility(View.GONE);
                dialogBinding.doneColor6.setVisibility(View.GONE);
                pickedColor = "#" + Integer.toHexString(
                        getActivity().getResources().getColor(R.color.note_color_3, getActivity().getTheme()));
            }
        });

        dialogBinding.noteColor4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogBinding.doneColor4.setVisibility(View.VISIBLE);
                dialogBinding.doneColorDefault.setVisibility(View.GONE);
                dialogBinding.doneColor1.setVisibility(View.GONE);
                dialogBinding.doneColor2.setVisibility(View.GONE);
                dialogBinding.doneColor3.setVisibility(View.GONE);
                dialogBinding.doneColor5.setVisibility(View.GONE);
                dialogBinding.doneColor6.setVisibility(View.GONE);
                pickedColor = "#" + Integer.toHexString(
                        getActivity().getResources().getColor(R.color.note_color_4, getActivity().getTheme()));
            }
        });

        dialogBinding.noteColor5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogBinding.doneColor5.setVisibility(View.VISIBLE);
                dialogBinding.doneColorDefault.setVisibility(View.GONE);
                dialogBinding.doneColor1.setVisibility(View.GONE);
                dialogBinding.doneColor2.setVisibility(View.GONE);
                dialogBinding.doneColor3.setVisibility(View.GONE);
                dialogBinding.doneColor4.setVisibility(View.GONE);
                dialogBinding.doneColor6.setVisibility(View.GONE);
                pickedColor = "#" + Integer.toHexString(
                        getActivity().getResources().getColor(R.color.note_color_5, getActivity().getTheme()));
            }
        });

        dialogBinding.noteColor6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogBinding.doneColor6.setVisibility(View.VISIBLE);
                dialogBinding.doneColorDefault.setVisibility(View.GONE);
                dialogBinding.doneColor1.setVisibility(View.GONE);
                dialogBinding.doneColor2.setVisibility(View.GONE);
                dialogBinding.doneColor3.setVisibility(View.GONE);
                dialogBinding.doneColor4.setVisibility(View.GONE);
                dialogBinding.doneColor5.setVisibility(View.GONE);
                pickedColor = "#" + Integer.toHexString(
                        getActivity().getResources().getColor(R.color.note_color_6, getActivity().getTheme()));
            }
        });

        dialogBinding.btnColorOkay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedNoteColor = pickedColor;
                GradientDrawable gd = (GradientDrawable) binding.noteIndicator.getBackground();
                gd.setColor(Color.parseColor(selectedNoteColor));
                dialog.dismiss();
            }
        });

        dialogBinding.btnColorCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                pickedColor = "";
            }
        });

    }

    private void deleteNote() {
        if (updateNote == null || noteViewModel.getSelectedNoteId().equals("")) {
            binding.btnGoBack.performClick();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore
                .getInstance()
                .collection("Users")
                .document(auth.getCurrentUser().getUid())
                .collection("notes")
                .document(noteViewModel.getSelectedNoteId())
                .delete();

        Toast.makeText(getActivity(), "Deleted Note", Toast.LENGTH_SHORT).show();
        binding.btnGoBack.performClick();
    }
}
