package falcosc.locus.addon.tasker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.ReportingHelper;
import locus.api.android.utils.IntentHelper;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.geoData.Track;

public class LocusGeoTagActivity extends ProjectActivity {

    private static class TrackDetails {
        TrackDetails() {
        }

        String startTime;
        String endTime;
    }

    private static final String TAG = "LocusGeoTagActivity"; //NON-NLS
    private static final int REQUEST_CODE_OPEN_DIRECTORY = 1;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;
    private final DateFormat mLocalDateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    private long mExampleDateTime;
    private ArrayList<Uri> mDocumentUris;
    private Uri mFolderUri;
    private EditText mEditOffset;
    private int mTimeOffset;
    private TextView mPhotoTime;
    private boolean isMessageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            createMessageView(getString(R.string.err_geotag_required_android_version));
            return;
        }

        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        //don't need to check for track intent because this is bound only to track intents
        try {
            TrackDetails trackDetails = getAndValidateTrackDetails();
            pickFolder();
            createGeotagView(trackDetails);
        } catch (Exception e) {
            createMessageView(ReportingHelper.getUserFriendlyName(e));
            Log.e(TAG, ReportingHelper.getUserFriendlyName(e), e);
        }

    }

    private void createMessageView(@NonNull String text) {
        isMessageView = true;
        setContentView(R.layout.text_dialog);

        TextView textMsg = findViewById(R.id.textMsg);
        textMsg.setText(text);

        Button okButton = findViewById(R.id.btnOk);
        okButton.setOnClickListener(v -> finish());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void createGeotagView(@NonNull TrackDetails trackDetails) {
        setContentView(R.layout.geotag_start);
        Button cancelButton = findViewById(R.id.btnCancel);
        cancelButton.setOnClickListener(v -> finish());

        Button okButton = findViewById(R.id.btnOk);
        okButton.setOnClickListener(v -> startGeoTag());

        findViewById(R.id.viewExamplePhoto).setOnClickListener(v -> pickExampleFile());

        mPhotoTime = findViewById(R.id.textPhotoTime);
        mEditOffset = findViewById(R.id.editOffset);
        mEditOffset.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                handleExampleTimeOffset(charSequence);
            }

            public void afterTextChanged(Editable charSequence) {
            }
        });

        TextView textStartTime = findViewById(R.id.textStartTime);
        textStartTime.setText(trackDetails.startTime);
        TextView textEndTime = findViewById(R.id.textEndTime);
        textEndTime.setText(trackDetails.endTime);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY) {
            handleOpenDir(resultCode, resultData);
        } else if (requestCode == REQUEST_CODE_OPEN_DOCUMENT) {
            handleOpenFile(resultCode, resultData);
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void handleOpenDir(int resultCode, @Nullable Intent resultData) {
        if (isMessageView) {
            //do nothing during message view
            return;
        }

        if ((resultCode == RESULT_OK) && (resultData != null)) {

            mFolderUri = resultData.getData();

            Uri treeUrl = DocumentsContract.buildChildDocumentsUriUsingTree(mFolderUri, DocumentsContract.getTreeDocumentId(mFolderUri));
            Log.i(TAG, "mFolderUri: " + mFolderUri + " \ntreeUrl: " + treeUrl); //NON-NLS

            int fileCount = 0;
            //selection and order is ignored by content resolver, use null and order manually
            try (Cursor cursor = getContentResolver().query(treeUrl, new String[]{Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE},
                    null, null, null, null)) { //NON-NLS
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        if(GeotagPhotosService.supportedMimeTypes.contains(cursor.getString(1))){
                            fileCount++;
                        }
                    }
                    if(cursor.moveToFirst()){
                        setExample(DocumentsContract.buildDocumentUriUsingTree(mFolderUri, cursor.getString(0)));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Can't read folder", e); //NON-NLS
            }

            if (fileCount == 0) {
                Toast.makeText(this, R.string.err_geotag_no_images_found, Toast.LENGTH_LONG).show();
                pickFolder();
                return;
            }

            setTitle(getResources().getQuantityString(R.plurals.geotag_x_photos, fileCount, fileCount));
        } else {
            finish();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void handleOpenFile(int resultCode, @Nullable Intent resultData) {
        if (resultCode == RESULT_OK) {
            try {
                //noinspection ConstantConditions use try catch because this would need 3 null checks
                setExample(resultData.getData());
            } catch (Exception e) {
                Log.e(TAG, "can't open file", e); //NON-NLS
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setExample(@NonNull Uri uri) {
        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r")) { //NON-NLS
            assert pfd != null;
            String fileName = new File(Objects.requireNonNull(uri.getPath())).getName();
            TextView photoName = findViewById(R.id.textPhotoName);
            photoName.setText(fileName);
            ExifInterface exifInterface = new ExifInterface(pfd.getFileDescriptor());

            String exifTime = GeotagPhotosService.getOriginalDateTime(exifInterface);
            if (exifTime != null) {
                mExampleDateTime = Const.EXIF_DATE_FORMAT.parse(exifTime).getTime();
                handleExampleTimeOffset(mEditOffset.getText());
            } else {
                Toast.makeText(this, fileName + ": " + getString(R.string.err_geotag_example_no_date), Toast.LENGTH_LONG).show();
                pickExampleFile();
            }
        } catch (Exception e) {
            Log.e(TAG, "can't set example", e); //NON-NLS
            Toast.makeText(this, R.string.err_geotag_example_load_exif, Toast.LENGTH_LONG).show();
            pickExampleFile();
        }
    }

    @SuppressWarnings("WeakerAccess")
        //because of inner class access
    void handleExampleTimeOffset(@Nullable CharSequence offset) {
        try {
            if (StringUtils.isBlank(offset)) {
                mPhotoTime.setText(mLocalDateTimeFormat.format(mExampleDateTime));
                mTimeOffset = 0;
            } else {
                mTimeOffset = Integer.parseInt(offset.toString());
                long newTime = mExampleDateTime;
                newTime += mTimeOffset * Const.ONE_HOUR;
                mPhotoTime.setText(mLocalDateTimeFormat.format(newTime));
            }
        } catch (NumberFormatException e) {
            mEditOffset.setError(getString(R.string.err_invalid_number));
        }
    }

    /**
     * @return Error Message
     */
    @SuppressWarnings("UseOfObsoleteDateTimeApi")
    @NonNull
    private TrackDetails getAndValidateTrackDetails() throws RequiredDataMissingException, RequiredVersionMissingException {
        Track track = IntentHelper.INSTANCE.getTrackFromIntent(this, getIntent());

        if (track == null) {
            throw new RequiredDataMissingException("Can't find Locus Track");
        }

        if (track.getPointsCount() == 0) {
            throw new RequiredDataMissingException("Track has no points");
        }

        long firstPointTime = track.getPoint(0).getTime();
        long lastPointTime = track.getPoint(track.getPointsCount() - 1).getTime();

        if ((firstPointTime == 0L) || (lastPointTime == 0L)) {
            throw new RequiredDataMissingException("Can't use this track, because first or last point doesn't have a time");
        }

        TrackDetails details = new TrackDetails();
        details.startTime = mLocalDateTimeFormat.format(new Date(firstPointTime));
        details.endTime = mLocalDateTimeFormat.format(new Date(lastPointTime));
        return details;
    }

    private void pickExampleFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(Const.MIME_TYPE_JPEG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, mFolderUri);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.putExtra(DocumentsContract.EXTRA_PROMPT, R.string.geotag_select_example);
        }
        startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT);
    }

    private void pickFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra(DocumentsContract.EXTRA_PROMPT, R.string.geotag_select_folder);
        //workaround to force displaying "use this folder" button
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/document/secondary:"));  //NON-NLS
        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startGeoTag() {
        Intent serviceIntent = new Intent(this,
                GeotagPhotosService.class);
        serviceIntent.setData(mFolderUri);
        serviceIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        );
        serviceIntent.setPackage(getPackageName());
        //set locus intent data
        serviceIntent.putExtras(getIntent());
        serviceIntent.putExtra(Const.INTENT_EXTRA_GEOTAG_OFFSET, mTimeOffset);

        JobIntentService.enqueueWork(this, GeotagPhotosService.class, GeotagPhotosService.JOB_ID, serviceIntent);
        setResult(RESULT_OK, getIntent());
        finish();
    }

}
