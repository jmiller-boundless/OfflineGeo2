package com.boundlessgeo.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.boundlessgeo.model.GeoPackageHelper;

import java.util.ArrayList;
import java.util.List;

import boundlessgeo.com.offlinegeo2.R;

/**
 * Created by jmiller on 8/28/2014.
 */
public class PickLayersAlert extends DialogFragment {
    String type;
    List mSelectedItems;
    String[] layers;
    NoticeDialogListener mListener;
    String pathToDB;
    GeoPackageHelper ghelper;

    public void setGhelper(GeoPackageHelper ghelper) {
        this.ghelper = ghelper;
    }



    public void setPathToDB(String path){
        pathToDB=path;
    }

    public List getmSelectedItems() {
        return mSelectedItems;
    }
    public String[]getLayers(){
        return layers;
    }
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mSelectedItems = new ArrayList();  // Where we track the selected items
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //Use GeoPackageHelper to Open the Package and Pull out a list of layers
        layers = ghelper.getLayerList();
        // Set the dialog title
        builder.setTitle(R.string.pick_toppings)
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setMultiChoiceItems(layers, null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    mSelectedItems.add(which);
                                } else if (mSelectedItems.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    mSelectedItems.remove(Integer.valueOf(which));
                                }
                            }
                        })
                        // Set the action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the mSelectedItems results somewhere
                        // or return them to the component that opened the dialog
                        mListener.onDialogPositiveClick(PickLayersAlert.this);

                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNegativeClick(PickLayersAlert.this);
                    }
                });

        return builder.create();

    }
}
