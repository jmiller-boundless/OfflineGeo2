package boundlessgeo.com.offlinegeo2;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.boundlessgeo.model.GeoPackageHelper;
import com.boundlessgeo.view.PickLayersAlert;
import com.boundlessgeo.view.SimpleAlertDialogFragment;
import com.boundlessgeo.view.UpdateDBDialogFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.jeo.data.Dataset;
import org.jeo.geopkg.GeoPkgWorkspace;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class MapsActivity extends FragmentActivity implements PickLayersAlert.NoticeDialogListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    ProgressDialog pDialog;
    String file_url="http://10.0.3.2:8080/geoserver/opengeo/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=opengeo%3Acountries&maxfeatures=50&outputformat=geopackage";
    private GeoPackageHelper myDbHelper;
    SimpleAlertDialogFragment sfrag;
    UpdateDBDialogFragment ufrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        /*final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog);
        dialog.setTitle("Dialog box");

        Button button = (Button) dialog.findViewById(R.id.Button01);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();*/

        myDbHelper = new GeoPackageHelper(getApplicationContext(),getApplicationContext().getString(R.string.gpkgname));
        //Does the database exist on the SDCard?
        boolean gpkgon = myDbHelper.checkDataBase();
        //is the device connected to a network?
        boolean amonline = amonline();
        //if not on device and online, download it
        if(!gpkgon&&amonline) {

            launchDownloadGPKGDialog();

        }else if(!gpkgon&&!amonline){//no database and offline
            sfrag = SimpleAlertDialogFragment.newInstance(R.string.nodboffline);
            sfrag.show(getSupportFragmentManager(),"Offline");
        }else if(gpkgon&&amonline){//database and online
            ufrag = UpdateDBDialogFragment.newInstance(R.string.update);
            ufrag.show(getSupportFragmentManager(),"Update");
        }else if(gpkgon&&!amonline){

            launchPickLayerDialog();
        }




    }

    private void launchPickLayerDialog() {
        //layer selector
        PickLayersAlert pickLayersAlert = new PickLayersAlert();
        pickLayersAlert.setPathToDB(Environment.getExternalStorageDirectory().toString() + "/"+R.string.gpkgname);
        pickLayersAlert.setGhelper(myDbHelper);
        pickLayersAlert.show(getSupportFragmentManager(),"picklayersalert");
    }

    private void launchDownloadGPKGDialog() {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Downloading file. Please wait...");
        pDialog.setIndeterminate(false);
        pDialog.setMax(100);
        pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pDialog.setCancelable(true);
        pDialog.show();
        new DownloadFileFromURL().execute(file_url);

    }

    private boolean amonline() {
        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
/*        GeoPackageHelper myDbHelper = new GeoPackageHelper(this);

        try {

            myDbHelper.createDataBase();

        } catch (IOException ioe) {

            throw new Error("Unable to create database");

        }

        try {

            myDbHelper.openDataBase();

        }catch(SQLException sqle){

            throw sqle;

        }*/
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }


    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // User touched the dialog's positive button
        PickLayersAlert pla = (PickLayersAlert)dialog;
        System.out.println(pla.getmSelectedItems());
        //open geopackage
        myDbHelper.openDataBase();
        GeoPkgWorkspace wspace =  myDbHelper.getMyDataBase();
        try {
            //jm placeholder
            Dataset firstdataset = wspace.get(pla.getLayers()[(Integer) pla.getmSelectedItems().get(0)]);
            System.out.println(firstdataset.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // User touched the dialog's negative button

    }

    public void doPositiveUpdateClick() {

        ufrag.dismiss();
        launchDownloadGPKGDialog();
    }

    public void doNegativeUpdateClick() {
        ufrag.dismiss();
        launchPickLayerDialog();
    }


    /**
     * Background Async Task to download file
     * */
    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //showDialog(progress_bar_type);
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {

            downloadGPKG(f_url);
            return null;
        }


        protected void downloadGPKG(String... f_url){
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.setConnectTimeout(120000);
                conection.connect();
                // this will be useful so that you can show a tipical 0-100% progress bar
                int lenghtOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream
                OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory().getPath()+"/"+getApplicationContext().getString(R.string.gpkgname));

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress(""+(int)((total*100)/lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

        }
        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        /**
         * After completing background task
         * Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            try {
                //create the internal db if necessary, and then copy it over with the downloaded file if it is newer
                myDbHelper.createDataBase();
            } catch (IOException e) {
                e.printStackTrace();
            }
            pDialog.dismiss();
            launchPickLayerDialog();

        }

    }
}
