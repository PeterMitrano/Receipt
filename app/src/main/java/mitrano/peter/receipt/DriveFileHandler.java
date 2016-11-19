package mitrano.peter.receipt;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.DriveFolder;

/**
 * Created by peter on 11/18/16.
 */
class DriveFileHandler implements ResultCallback<DriveFolder.DriveFileResult> {

    static final String TAG = "DriveFileHandler";
    private GoogleApiClient mGoogleApiClient;

    public DriveFileHandler(GoogleApiClient googleApiClient) {
        this.mGoogleApiClient = googleApiClient;
    }

    @Override
    public void onResult(@NonNull DriveFolder.DriveFileResult result) {
        if (!result.getStatus().isSuccess()) {
            Log.e(TAG, "Error while trying to create the file");
            return;
        }
        Log.e(TAG, "success: " + result.getDriveFile().getDriveId().toString());
    }
}
