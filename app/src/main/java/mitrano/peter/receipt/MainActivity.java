package mitrano.peter.receipt;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_CODE_CAPTURE_IMAGE = 1001;
    private static final int REQUEST_CODE_RESOLUTION = 1002;
    private static final int REQUEST_CODE_SAVE_FILE = 1003;
    private GoogleApiClient mGoogleApiClient;
    private ProgressDialog mProgress;
    private DriveFolder mReceiptsFolder;
    private TextView mTextView;
    private java.io.File mCurrentPhoto;
    private FloatingActionButton fab;

    static private final String TAG = "MainActivity";
    private boolean mFoundReceiptsFolder;

    /**
     * Create the main activity.
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);
        fab.setEnabled(false);

        mTextView = (TextView) findViewById(R.id.text_view);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Accessing Google Drive...");
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient == null) {
            // Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection
            // failures.
            // Since no account name is passed, the user is prompted to choose.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        // Connect the client. Once connected, the camera is launched.
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.w(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }

        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.w(TAG, "API client connected.");

        final SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String drive_id = sharedPref.getString(getString(R.string.receipts_folder_key), "");

        Log.e(TAG, "driveid: " + drive_id);
        if (!drive_id.isEmpty()) {
            mFoundReceiptsFolder = true;
        }

        if (!mFoundReceiptsFolder) {
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle("Receipts").build();

            Drive.DriveApi.getRootFolder(mGoogleApiClient)
                    .createFolder(mGoogleApiClient, changeSet)
                    .setResultCallback(new ResultCallback<DriveFolder.DriveFolderResult>() {
                        @Override
                        public void onResult(@NonNull DriveFolder.DriveFolderResult driveFolderResult) {
                            if (driveFolderResult.getStatus().isSuccess()) {
                                fab.setEnabled(true);
                                mReceiptsFolder = driveFolderResult.getDriveFolder();
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString(getString(R.string.receipts_folder_key), mReceiptsFolder.getDriveId().toString());
                                editor.commit();
                            } else {
                                Log.e(TAG, "Couldn't create Receipts folder");
                            }
                        }
                    });
        } else {
            mReceiptsFolder = DriveId.decodeFromString(drive_id).asDriveFolder();
            fab.setEnabled(true);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.w(TAG, "GoogleApiClient connection suspended");
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_CAPTURE_IMAGE:
                Log.w(TAG, "took picture: " + mCurrentPhoto.getName());
                saveFileToDrive();
            case REQUEST_CODE_SAVE_FILE:
                Log.e(TAG, "success saving file");
        }
    }

    @Override
    public void onClick(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            try {
                createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this, "Error occured while creating the file", Toast.LENGTH_SHORT).show();
            }

            // Continue only if the File was successfully created
            if (mCurrentPhoto != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "mitrano.peter.receipt.fileprovider", mCurrentPhoto);
                Log.e(TAG, photoURI.toString());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_CODE_CAPTURE_IMAGE);
            }
        } else {
            Toast.makeText(this, "You need an camera app installed.", Toast.LENGTH_LONG).show();
        }
    }

    private void saveFileToDrive() {
        // Start by creating a new contents, and setting a callback.
        Log.w(TAG, "Creating new contents.");
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new DriveContentsHandler.DriveContentHandler(mCurrentPhoto, mReceiptsFolder, mGoogleApiClient));
    }

    private java.io.File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String imageFileName = timeStamp + ".jpg";
        java.io.File storageDir = new java.io.File(getFilesDir(), "images");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        java.io.File imageFile = new java.io.File(storageDir, imageFileName);
        mCurrentPhoto = imageFile;
        return imageFile;
    }

}
