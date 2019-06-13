package falcosc.locus.addon.tasker;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.exifinterface.media.ExifInterface;
import falcosc.locus.addon.tasker.utils.Const;
import locus.api.android.utils.IntentHelper;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Track;

public class LocusGeoTagActivity extends ProjectActivity {

    private static final String MIME_TYPE_JPEG = "image/jpeg"; //NON-NLS
    private static final String TAG = "LocusGeoTagActivity"; //NON-NLS
    private static final int REQUEST_CODE_OPEN_DIRECTORY = 1;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;
    private final DateFormat mLocalDateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    private final Calendar mExampleDateTime = Calendar.getInstance();
    private LinkedList<DocumentFile> mDocumentFiles;
    private Uri mFolderUri;
    private EditText mEditOffset;
    private int mTimeOffset;
    private TextView mPhotoTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            createMessageView(getString(R.string.err_geotag_required_android_version));
            return;
        }

        //don't need to check for track intent because this is bound only to track intents
        try {

            TrackDetails trackDetails = getAndValidateTrackDetails();

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.putExtra(DocumentsContract.EXTRA_PROMPT, R.string.geotag_select_folder);
            startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);

            createGeotagView(trackDetails);
        } catch (Exception e) {
            //TODO check if setContentView was called
            if (findViewById(android.R.id.content) == null) {
                createMessageView(e.getLocalizedMessage());
            } else {
                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        }

    }

    private void createMessageView(@NonNull String text) {
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
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void handleOpenDir(int resultCode, @Nullable Intent resultData) {
        if (resultCode == RESULT_OK) {

            try {
                mFolderUri = Objects.requireNonNull(resultData).getData();
                Log.i(TAG, "mFolderUri: " + mFolderUri); //NON-NLS

                DocumentFile folder = DocumentFile.fromTreeUri(this, Objects.requireNonNull(mFolderUri));
                //noinspection CallToSuspiciousStringMethod
                mDocumentFiles = Stream.of(Objects.requireNonNull(folder).listFiles())
                        .filter(file -> MIME_TYPE_JPEG.equals(file.getType()))
                        .collect(Collectors.toCollection(LinkedList::new));
            } catch (Exception e) {
                Log.e(TAG, "Can't read folder", e); //NON-NLS
                mDocumentFiles = new LinkedList<>();
            }

            if (mDocumentFiles.isEmpty()) {
                Toast.makeText(this, R.string.err_geotag_no_images_found, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
                return;
            }

            mDocumentFiles.stream()
                    .max(Comparator.comparing(DocumentFile::lastModified))
                    .ifPresent(this::setExample);
            setTitle(getResources().getQuantityString(R.plurals.geotag_x_photos, mDocumentFiles.size()));

        } else {
            finish();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void handleOpenFile(int resultCode, @Nullable Intent resultData) {
        if (resultCode == RESULT_OK) {
            try {
                //noinspection ConstantConditions use try catch because this would need 3 null checks
                setExample(DocumentFile.fromSingleUri(this, resultData.getData()));
            } catch (Exception e) {
                Log.e(TAG, "can't open file", e); //NON-NLS
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setExample(@NonNull DocumentFile file) {
        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(file.getUri(), "r")) { //NON-NLS
            assert pfd != null;
            ExifInterface exifInterface = new ExifInterface(pfd.getFileDescriptor());

            mExampleDateTime.setTime(Const.EXIF_DATE_FORMAT.parse(exifInterface.getAttribute(ExifInterface.TAG_DATETIME)));
            Log.i(TAG, "Exif Time: " + exifInterface.getAttribute(ExifInterface.TAG_DATETIME)); //NON-NLS

            TextView photoName = findViewById(R.id.textPhotoName);
            photoName.setText(file.getName());

            handleExampleTimeOffset(mEditOffset.getText());

        } catch (Exception e) {
            Log.e(TAG, "can't set example", e); //NON-NLS
        }
    }

    void handleExampleTimeOffset(@Nullable CharSequence offset) {
        try {
            if (StringUtils.isBlank(offset)) {
                mPhotoTime.setText(mLocalDateTimeFormat.format(mExampleDateTime));
                mTimeOffset = 0;
            } else {
                mTimeOffset = Integer.parseInt(offset.toString());
                Calendar newTime = (Calendar) mExampleDateTime.clone();
                newTime.add(Calendar.HOUR, mTimeOffset);
                mPhotoTime.setText(mLocalDateTimeFormat.format(newTime));
            }
        } catch (NumberFormatException e) {
            mEditOffset.setError(getString(R.string.err_invalid_number));
        }
    }

    /**
     * @return Error Message
     */
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void pickExampleFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(MIME_TYPE_JPEG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, mFolderUri);
        }
        intent.putExtra(DocumentsContract.EXTRA_PROMPT, R.string.geotag_select_example);
        startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startGeoTag() {
        Intent serviceIntent = new Intent(this,
                GeotagPhotosService.class);
        //set locus intent data
        serviceIntent.putExtras(getIntent());
        String[] fileUris = mDocumentFiles.stream()
                .map(file -> file.getUri().toString())
                .toArray(String[]::new);
        serviceIntent.putExtra(Const.INTENT_EXTRA_GEOTAG_FILES, fileUris);
        serviceIntent.putExtra(Const.INTENT_EXTRA_GEOTAG_OFFSET, mTimeOffset);
        ContextCompat.startForegroundService(this, serviceIntent);
        setResult(RESULT_OK, getIntent());
        finish();
    }

    private static class TrackDetails {
        String startTime;
        String endTime;
    }
}
