package falcosc.locus.addon.tasker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Objects;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.ReportingHelper;
import locus.api.android.utils.IntentHelper;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.geoData.Track;

public class LocusGeoTagActivity extends ProjectActivity {

    //workaround to force displaying "use this folder" button
    private static final Uri LAST_FOLDER_URI = Uri.parse("content://com.android.externalstorage.documents/document/secondary:"); //NON-NLS
    private final ArrayList<Long> photoTimestamps = new ArrayList<>();
    private long mTrackStartTime;
    private long mTrackEndTime;

    private static final String TAG = "LocusGeoTagActivity"; //NON-NLS
    private final DateFormat mLocalDateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    private long mExampleDateTime;
    private Uri mFolderUri;
    private EditText mEditOffset;
    private int mTimeOffset;
    private TextView mPhotoTime;
    private boolean isMessageView;

    final ActivityResultLauncher<Uri> pickExampleFile = registerForActivityResult(new PickExampleFile(), this::setExample);
    final ActivityResultLauncher<Uri> pickFolder = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), this::setFolder);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            createMessageView(getString(R.string.err_geotag_required_android_version));
            return;
        }

        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        //don't need to check for track intent because this is bound only to track intents
        try {
            getAndValidateTrackDetails();
            pickFolder.launch(LAST_FOLDER_URI);
            createGeotagView();
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

    private void createGeotagView() {
        setContentView(R.layout.geotag_start);
        Button cancelButton = findViewById(R.id.btnCancel);
        cancelButton.setOnClickListener(v -> finish());

        Button okButton = findViewById(R.id.btnOk);
        okButton.setOnClickListener(v -> startGeoTag());

        //findViewById(R.id.viewExamplePhoto).setOnClickListener(v -> pickExampleFile());

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
        textStartTime.setText(mLocalDateTimeFormat.format(mTrackStartTime));
        TextView textEndTime = findViewById(R.id.textEndTime);
        textEndTime.setText(mLocalDateTimeFormat.format(mTrackEndTime));
    }

    private void setFolder(@Nullable Uri uri) {
        if (isMessageView) {
            //do nothing during message view
            return;
        }
        if (uri == null) {
            //user did abort
            finish();
            return;
        }

        mFolderUri = uri;
        findViewById(R.id.viewExamplePhoto).setOnClickListener(v -> pickExampleFile.launch(mFolderUri));

        Uri treeUrl = DocumentsContract.buildChildDocumentsUriUsingTree(mFolderUri, DocumentsContract.getTreeDocumentId(mFolderUri));
        Log.i(TAG, "mFolderUri: " + mFolderUri + " \ntreeUrl: " + treeUrl); //NON-NLS

        long timeToTrackStart = mTrackStartTime;
        String exampleUri = null;
        photoTimestamps.clear();
        //selection and order is ignored by content resolver, use null and order manually
        try (Cursor cursor = getContentResolver().query(treeUrl, new String[]{
                        Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, MediaStore.MediaColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_MODIFIED, DocumentsContract.Document.COLUMN_LAST_MODIFIED},
                null, null, null, null)) { //NON-NLS
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (GeotagPhotosService.supportedMimeTypes.contains(cursor.getString(1))) {
                        long date = getFileDateFromCursor(cursor);
                        photoTimestamps.add(date);
                        long newTimeToTrackStart = date - mTrackStartTime;
                        if (newTimeToTrackStart < timeToTrackStart) {
                            timeToTrackStart = newTimeToTrackStart;
                            exampleUri = cursor.getString(0);
                        }
                    }
                }
                if (exampleUri != null) {
                    setExample(DocumentsContract.buildDocumentUriUsingTree(mFolderUri, exampleUri));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't read folder", e); //NON-NLS
        }

        if (photoTimestamps.isEmpty()) {
            Toast.makeText(this, R.string.err_geotag_no_images_found, Toast.LENGTH_LONG).show();
            pickFolder.launch(LAST_FOLDER_URI);
            return;
        }
        updatePhotoCount();
    }

    private void updatePhotoCount() {
        long startTime = mTrackStartTime - (DateUtils.HOUR_IN_MILLIS * -mTimeOffset);
        long endTime = mTrackEndTime - (DateUtils.HOUR_IN_MILLIS * -mTimeOffset);
        int photoCount = (int) photoTimestamps.stream().filter(date -> ((startTime < date) && (date < endTime))).count();
        if (photoCount == 0) {
            //no matching photos found, display photos which don't have any time
            photoCount = (int) photoTimestamps.stream().filter(date -> (date == 0)).count();
        }
        setTitle(getResources().getQuantityString(R.plurals.geotag_x_photos, photoTimestamps.size(), photoCount, photoTimestamps.size()));
    }

    private static Long getFileDateFromCursor(Cursor cursor) {
        long datetime = 0;
        for (int i = 2; i < 5; i++) {
            datetime = cursor.getLong(i);
            if (datetime > 0) {
                return datetime;
            }
        }
        return datetime;
    }

    public static class PickExampleFile extends ActivityResultContract<Uri, Uri> {

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull Uri folderUri) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .putExtra(Intent.EXTRA_MIME_TYPES, GeotagPhotosService.supportedMimeTypes.toArray())
                    .setType("*/*");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri);
            }
            intent.putExtra(DocumentsContract.EXTRA_PROMPT, R.string.geotag_select_example);
            return intent;
        }

        @Nullable
        @Override
        public Uri parseResult(int resultCode, @Nullable Intent intent) {
            if ((resultCode != RESULT_OK) || (intent == null)) return null;
            return intent.getData();
        }
    }

    private void setExample(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
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
                pickExampleFile.launch(mFolderUri);
            }
        } catch (Exception e) {
            Log.e(TAG, "can't set example", e); //NON-NLS
            Toast.makeText(this, R.string.err_geotag_example_load_exif, Toast.LENGTH_LONG).show();
            pickExampleFile.launch(mFolderUri);
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
                newTime += mTimeOffset * DateUtils.HOUR_IN_MILLIS;
                mPhotoTime.setText(mLocalDateTimeFormat.format(newTime));
            }
            updatePhotoCount();
        } catch (NumberFormatException e) {
            mEditOffset.setError(getString(R.string.err_invalid_number));
        }
    }

    private void getAndValidateTrackDetails() throws RequiredDataMissingException, RequiredVersionMissingException {
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

        mTrackStartTime = firstPointTime;
        mTrackEndTime = lastPointTime;
    }

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
        serviceIntent.putExtra(Const.INTENT_EXTRA_GEOTAG_REPORT_NON_MATCH, ((Checkable) findViewById(R.id.reportNonMatch)).isChecked());

        JobIntentService.enqueueWork(this, GeotagPhotosService.class, GeotagPhotosService.JOB_ID, serviceIntent);
        setResult(RESULT_OK, getIntent());
        finish();
    }

}
