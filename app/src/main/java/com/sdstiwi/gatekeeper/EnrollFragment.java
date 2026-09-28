package com.sdstiwi.gatekeeper;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sdstiwi.gatekeeper.R;



import java.util.ArrayList;
import java.util.List;

public class EnrollFragment extends Fragment {

    private RecyclerView recyclerView;
    private ClassAdapter classAdapter;
    private List<ClassModel> classList;
    private DatabaseHelper databaseHelper;
    private EditText searchEditText;
    private ProgressDialog progressDialog;

    public EnrollFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.enroll_fragment, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        searchEditText = view.findViewById(R.id.searchEditText);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        LoginActivity.startSynchronization(requireContext());
        databaseHelper = new DatabaseHelper(getContext());
        classList = new ArrayList<>();
        classAdapter = new ClassAdapter(classList, getContext(), databaseHelper);
        recyclerView.setAdapter(classAdapter);

        loadClasses();

        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterClasses(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadClasses() {
        classList.clear();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM classes WHERE isActive = 1", null);

        if (cursor.moveToFirst()) {
            do {
                ClassModel classModel = new ClassModel(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(6),
                        cursor.getString(7)
                );
                classList.add(classModel);
            } while (cursor.moveToNext());
        }

        cursor.close();
        classAdapter.notifyDataSetChanged();
    }

    private void filterClasses(String query) {
        List<ClassModel> filteredList = new ArrayList<>();
        for (ClassModel classModel : classList) {
            if (classModel.getClassName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(classModel);
            }
        }
        classAdapter.updateList(filteredList);
    }
}
