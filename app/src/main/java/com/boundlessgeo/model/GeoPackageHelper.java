package com.boundlessgeo.model;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import com.boundlessgeo.util.ISO8601;

import org.jeo.geopkg.GeoPkgWorkspace;
import org.jeo.android.geopkg.GeoPackage;
import org.jeo.util.Password;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class GeoPackageHelper extends SQLiteOpenHelper {

    //The Android's default system path of your application database.
    private static String DB_PATH = "/data/data/boundlessgeo.com.offlinegeo2/databases/";

    private String db_name;

   // private SQLiteDatabase myDataBase;
    private GeoPkgWorkspace myDataBase;

    public GeoPkgWorkspace getMyDataBase() {
        return myDataBase;
    }

    private final Context myContext;
    public GeoPackageHelper(Context context,String dbname) {
        super(context, dbname, null, 1);
        this.myContext = context;
        db_name=dbname;
    }
    @Override
    public void onCreate(SQLiteDatabase arg0) {


    }
    /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public void createDataBase() throws IOException{

       // boolean dbExist = checkDataBase();

       if(!checkDataBase()||!isLocalLatest()){

            //By calling this method and empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
            this.getReadableDatabase();

            try {

                copyDataBase();

            } catch (IOException e) {

                throw new Error("Error copying database");

            }
        }

    }
    /*
     * Check if the GPKG has ever been downloaded to the SDCard
     */
    public boolean checkSDCardDataBase(){

        SQLiteDatabase checkDB = null;

        try{
            String myPath = Environment.getExternalStorageDirectory().getPath()+"/"+db_name;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);



        }catch(SQLiteException e){

            //database does't exist yet.

        }

        if(checkDB != null){

            checkDB.close();

        }

        return checkDB != null ? true : false;
    }


    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */
    public boolean checkDataBase(){

        SQLiteDatabase checkDB = null;

        try{
            String myPath = DB_PATH + db_name;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
           // File file = new File(myPath);
          //  Map opts = new HashMap();
           // opts.put("file",file);
          //  opts.put("user","");
           // opts.put("passwd",new Password(new char[]{}));
            //GeoPkgWorkspace workspace = new GeoPackage().open(file,opts);


        }catch(SQLiteException e){

            //database does't exist yet.

        }
        //catch(IOException i){

        //}

        if(checkDB != null){

            checkDB.close();

        }

        return checkDB != null ? true : false;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     * */
    public void copyDataBase() throws IOException{

        //Open your local db as the input stream
        //InputStream myInput = myContext.getAssets().open(db_name);
        File file = new File(Environment.getExternalStorageDirectory().getPath()+"/"+db_name);
        InputStream myInput = new BufferedInputStream(new FileInputStream(file));

        // Path to the just created empty db
        String outFileName = DB_PATH + db_name;

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0){
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

    }

    public void openDataBase() throws SQLException{

        //Open the database
        String myPath = DB_PATH + db_name;
        //myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
        File file = new File(myPath);
        Map opts = new HashMap();
        opts.put("file",file);
        opts.put("passwd",new Password(new char[]{}));
        try {
            GeoPkgWorkspace workspace = new GeoPackage().open(file,opts);
            myDataBase = workspace;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void close() {

        if(myDataBase != null) {
            myDataBase.close();
        }

        super.close();

    }

    public Cursor getAllCountries(String table_name) {


        // 1. build the query
        String query = "SELECT rowid _id, * FROM " + table_name;

        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        return cursor;
    }
    public boolean isLocalLatest(){
        boolean locallatest = true;
        long localtime;
        long externaltime;
        SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        String query = "SELECT last_change from gpkg_contents";
        //local internal
        String myPath = DB_PATH + db_name;
        try {
            SQLiteDatabase localinternaldb = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
            Cursor lidbc = localinternaldb.rawQuery(query, new String[]{});
            lidbc.moveToFirst();
            String lidatestring = lidbc.getString(0);
            localtime =  ISO8601.toCalendar(lidatestring).getTime().getTime();
            lidbc.close();
            localinternaldb.close();
        }catch(SQLiteException e){

            //database does't exist yet.
            return false;

        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        //sd card
       try {
           String exPath = Environment.getExternalStorageDirectory().getPath() + "/" + db_name;
           SQLiteDatabase externaldb = SQLiteDatabase.openDatabase(exPath, null, SQLiteDatabase.OPEN_READWRITE);
           Cursor exdbc = externaldb.rawQuery(query, new String[]{});
           exdbc.moveToFirst();
           String exdatestring = exdbc.getString(0);
           externaltime = ISO8601.toCalendar(exdatestring).getTime().getTime();
           exdbc.close();
           externaldb.close();
       }catch(SQLiteException e){

           //database download issue
           return true;

       } catch (ParseException e) {
           e.printStackTrace();
           return true;
       }
        locallatest = localtime>=externaltime;
        return locallatest;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


    public String[] getLayerList() {
        String query = "SELECT table_name FROM gpkg_contents";

        List layers = new ArrayList<String>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            layers.add(cursor.getString(0));
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return (String[]) layers.toArray(new String[layers.size()]);
    }
}

