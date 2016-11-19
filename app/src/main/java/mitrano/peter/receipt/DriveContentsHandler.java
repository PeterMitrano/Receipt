package mitrano.peter.receipt;

import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by peter on 11/18/16.
 */

public class DriveContentsHandler {
    static class DriveContentHandler implements ResultCallback<DriveApi.DriveContentsResult> {

        private static final String TAG = "DriveFileHandler";
        private final java.io.File mPhotoFile;
        private GoogleApiClient mGoogleApiClient;
        private DriveFolder mReceiptsFolder;

        public DriveContentHandler(java.io.File photoFile, DriveFolder receiptsFolder, GoogleApiClient googleApiClient) {
            this.mPhotoFile = photoFile;
            this.mGoogleApiClient = googleApiClient;
            this.mReceiptsFolder = receiptsFolder;
        }

        @Override
        public void onResult(@NonNull DriveApi.DriveContentsResult result) {
            if (!result.getStatus().isSuccess()) {
                Log.e(TAG, "Error while trying to create the file");
                return;
            }
            else if (mPhotoFile == null) {
                Log.e(TAG, "null photo file");
            }

            try {
                OutputStream outputStream = result.getDriveContents().getOutputStream();
               FileInputStream inputStream = new FileInputStream(mPhotoFile);
                int size = inputStream.available();
                byte b[] = new byte[size];
                inputStream.read(b);
                outputStream.write(b);
            } catch (FileNotFoundException fnfe) {
                Log.w(TAG, "Unable to open FileInputStream");
                fnfe.printStackTrace();
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to write file contents.");
                ioe.printStackTrace();
            }

            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(mPhotoFile.getName())
                    .setMimeType("image/jpeg").build();

            mReceiptsFolder.createFile(mGoogleApiClient, changeSet, result.getDriveContents())
                    .setResultCallback(new DriveFileHandler(mGoogleApiClient));

        }
    }
}
