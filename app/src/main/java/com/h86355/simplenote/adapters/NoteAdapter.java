package com.h86355.simplenote.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.h86355.simplenote.databinding.ItemNoteBinding;
import com.h86355.simplenote.models.Note;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class NoteAdapter extends FirestoreRecyclerAdapter<Note, NoteAdapter.NoteHolder> {
    private Context context;
    private INote listener;
    private SimpleDateFormat simpleDateFormat;
    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public NoteAdapter(@NonNull FirestoreRecyclerOptions<Note> options, INote listener) {
        super(options);
        this.listener = listener;
        simpleDateFormat = new SimpleDateFormat("EE, dd-MMMM-yyyy", Locale.getDefault());
    }

    @Override
    protected void onBindViewHolder(@NonNull NoteHolder holder, int position, @NonNull Note model) {
        if (model.getTitle().equals("")) {
            holder.binding.itemTitle.setVisibility(View.GONE);
        } else {
            holder.binding.itemTitle.setText(model.getTitle());
        }

        if (model.getContent().equals("")) {
            holder.binding.itemContent.setVisibility(View.GONE);
        } else {
            holder.binding.itemContent.setText(model.getContent());
        }

        holder.binding.itemNote.setBackgroundColor(Color.parseColor(model.getNoteColor()));
        holder.binding.itemTimestamp.setText(simpleDateFormat.format(model.getTimestamp()));
    }

    @NonNull
    @Override
    public NoteHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteHolder(
                ItemNoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), listener);
    }

    public Note getSelectedNote(int position) {
        return getSnapshots().get(position);
    }

    public String getSelectedNoteId(int position) {
        return getSnapshots().getSnapshot(position).getId();
    }

    public class NoteHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ItemNoteBinding binding;
        private INote listener;
        public NoteHolder(ItemNoteBinding binding, INote listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onNoteClick(getAdapterPosition());
        }
    }

}
