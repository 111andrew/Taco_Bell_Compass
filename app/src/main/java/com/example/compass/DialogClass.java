package com.example.compass;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

// https://www.youtube.com/watch?v=ARezg1D9Zd0

public class DialogClass extends AppCompatDialogFragment {
    private EditText edit_text_Latitude;
    private EditText edit_text_Longitude;
    private ExampleDialogListener listener;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_dialog, null);

        builder.setView(view)
                .setTitle("Input Custom Lat Long")
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        float latitude = Float.valueOf(edit_text_Latitude.getText().toString());
                        float longitude = Float.valueOf(edit_text_Longitude.getText().toString());
                        listener.applyCustom(latitude, longitude);
                    }
                });
        edit_text_Latitude = view.findViewById(R.id.edit_latitude);
        edit_text_Longitude = view.findViewById(R.id.edit_longitude);

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (ExampleDialogListener) context;
        } catch (ClassCastException e){
            throw new ClassCastException(context.toString() +
                    "must implement ExampleDialogListener");
        }
    }

    public interface ExampleDialogListener{
        void applyCustom(float latitude, float longitude);
    }

}
