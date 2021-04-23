package com.h86355.simplenote;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.h86355.simplenote.models.Note;

public class NoteViewModel extends ViewModel {
    private MutableLiveData<Note> selectedNote = new MutableLiveData<>();
    private String selectedNoteId = "";

    public MutableLiveData<Note> getSelectedNote() {
        return selectedNote;
    }

    public void setSelectedNote(Note note) {
        selectedNote.setValue(note);
    }

    public void setSelectedNoteId(String id) {
        selectedNoteId = id;
    }

    public String getSelectedNoteId() {
        return selectedNoteId;
    }
}
