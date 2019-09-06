package com.thad.mdp_27;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class DirectionFragment extends DialogFragment {

    private static final String TAG = "DirectionFragment";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    Button saveBtn, cancelDirectionBtn;
    EditText directionValueEditView;
    String direction = "";
    View rootView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        showLog("Entering onCreateView");
        rootView = inflater.inflate(R.layout.activity_direction, container, false);
        super.onCreate(savedInstanceState);

        getDialog().setTitle("Change Direction");
        sharedPreferences = getActivity().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        saveBtn = rootView.findViewById(R.id.saveBtn);
        cancelDirectionBtn = rootView.findViewById(R.id.cancelDirectionBtn);
        directionValueEditView = rootView.findViewById(R.id.directionValueEditText);

        directionValueEditView.setSelectAllOnFocus(true);
        direction = sharedPreferences.getString("direction","");
        directionValueEditView.setText(direction);

        if (savedInstanceState != null)
            direction = savedInstanceState.getString("direction");

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked saveBtn");
                direction = String.valueOf(directionValueEditView.getText());
                editor.putString("direction",direction);
                directionValueEditView.setText(direction);
                ((MainActivity)getActivity()).refreshDirection(direction);
                Toast.makeText(getActivity(), "Saving direction...", Toast.LENGTH_SHORT).show();
                showLog("Exiting saveBtn");
                editor.commit();
                getDialog().dismiss();
            }
        });

        cancelDirectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked cancelDirectionBtn");
                directionValueEditView.setText(direction);
                showLog( "Exiting cancelDirectionBtn");
                getDialog().dismiss();
            }
        });
        showLog("Exiting onCreateView");
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        showLog("Entering onSaveInstanceState");
        super.onSaveInstanceState(outState);
        saveBtn = rootView.findViewById(R.id.saveBtn);
        showLog("Exiting onSaveInstanceState");
        outState.putString(TAG, direction);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        showLog("Entering onDismiss");
        super.onDismiss(dialog);
        directionValueEditView.clearFocus();
        showLog("Exiting onDismiss");
    }

    private void showLog(String message) {
        Log.d(TAG, message);
    }
}

