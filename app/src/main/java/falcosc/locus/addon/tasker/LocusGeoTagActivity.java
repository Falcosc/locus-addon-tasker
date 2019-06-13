package falcosc.locus.addon.tasker;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.print.PrintDocumentInfo;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.provider.DocumentsContract.Document;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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
    private ArrayList<Uri> mDocumentUris;
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

    public class DocumentInfo{
        public DocumentInfo(String documentId,String mimeType,long lastModified){
            mDocumentId = documentId;
            mMimeType = mimeType;
            mLastModified = lastModified;
        }

        public String getMimeType() {
            return mMimeType;
        }

        public String getDocumentId() {
            return mDocumentId;
        }

        public long getLastModified() {
            return mLastModified;
        }

        private String mMimeType;
        private String mDocumentId;
        private long mLastModified;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void handleOpenDir(int resultCode, @Nullable Intent resultData) {
        if (resultCode == RESULT_OK) {

            long testTime = System.currentTimeMillis();
            Log.i(TAG,"1 " + (System.currentTimeMillis() - testTime) );

            try {
                mFolderUri = Objects.requireNonNull(resultData).getData();
                mDocumentUris = new ArrayList<>();
                Log.i(TAG, "mFolderUri: " + mFolderUri); //NON-NLS

                Uri treeUrl = DocumentsContract.buildChildDocumentsUriUsingTree(mFolderUri,
                        DocumentsContract.getTreeDocumentId(mFolderUri));
                Log.i(TAG, "treeUrl: " + treeUrl); //NON-NLS
                Log.i(TAG,"2 " + (System.currentTimeMillis() - testTime) );

                List<DocumentInfo> info = new ArrayList<>();
                //selection and order is ignored for content resolver
                try (Cursor c = getContentResolver().query(treeUrl, new String[] { Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_LAST_MODIFIED },
                        null, null,null, null)){ //NON-NLS
                    if (c != null) {
                        int i = 0;
                        while (c.moveToNext()) {
                            info.add(new DocumentInfo(c.getString(0), c.getString(1), c.getLong(2)));
                        }
                    }
                }
                Log.i(TAG,"3 " + (System.currentTimeMillis() - testTime) );
                mDocumentUris = info.stream()
                        .filter(documentInfo -> MIME_TYPE_JPEG.equals(documentInfo.mMimeType))
                        .sorted(Comparator.comparing(DocumentInfo::getLastModified).reversed())
                        .map(documentInfo -> DocumentsContract.buildDocumentUriUsingTree(mFolderUri,documentInfo.mDocumentId))
                        .collect(Collectors.toCollection(ArrayList::new));
                Log.i(TAG,"4 " + (System.currentTimeMillis() - testTime) );

                mDocumentUris.stream().forEachOrdered(uri -> Log.i(TAG, uri.toString()));


                //DocumentsContract.buildDocumentUriUsingTree(mFolderUri,c.getString(0))

                Log.i(TAG,"5 " + (System.currentTimeMillis() - testTime) );

            } catch (Exception e) {
                Log.e(TAG, "Can't read folder", e); //NON-NLS
                mDocumentUris = new ArrayList<>();
            }

            if (mDocumentUris.isEmpty()) {
                Toast.makeText(this, R.string.err_geotag_no_images_found, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
                return;
            }
            Log.i(TAG,"5 " + (System.currentTimeMillis() - testTime) );
            setExample(mDocumentUris.get(0));
            setTitle(getResources().getQuantityString(R.plurals.geotag_x_photos, mDocumentUris.size(), mDocumentUris.size()));
            Log.i(TAG,"6 " + (System.currentTimeMillis() - testTime) );
        } else {
            finish();
        }
    }

    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
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
        long testTime = System.currentTimeMillis();
        Log.i(TAG,"t1 " + (System.currentTimeMillis() - testTime) );
        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r")) { //NON-NLS
            assert pfd != null;
            //TODO Name
            TextView photoName = findViewById(R.id.textPhotoName);
            photoName.setText(new File(uri.getPath()).getName());
            ExifInterface exifInterface = new ExifInterface(pfd.getFileDescriptor());

            String exifTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
            if(exifTime != null){
                mExampleDateTime.setTime(Const.EXIF_DATE_FORMAT.parse(exifTime));
                handleExampleTimeOffset(mEditOffset.getText());
            } else {
                Toast.makeText(this, "example has no exif date time, please select another or abort", Toast.LENGTH_LONG).show();
                pickExampleFile();
            }
        } catch (Exception e) {
            Log.e(TAG, "can't set example", e); //NON-NLS
        }
        Log.i(TAG,"t2 " + (System.currentTimeMillis() - testTime) );
    }

    void handleExampleTimeOffset(@Nullable CharSequence offset) {
        try {
            if (StringUtils.isBlank(offset)) {
                mPhotoTime.setText(mLocalDateTimeFormat.format(mExampleDateTime.getTime()));
                mTimeOffset = 0;
            } else {
                mTimeOffset = Integer.parseInt(offset.toString());
                Calendar newTime = (Calendar) mExampleDateTime.clone();
                newTime.add(Calendar.HOUR, mTimeOffset);
                mPhotoTime.setText(mLocalDateTimeFormat.format(newTime.getTime()));
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
        Uri[] fileUris = mDocumentUris.toArray(new Uri[]{});
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

    /*// Innere Klasse HoleDatenTask führt den asynchronen Task auf eigenem Arbeitsthread aus
    public class FilterFolderTask extends AsyncTask<Uri, Integer, String[]> {

        private final String LOG_TAG = HoleDatenTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... strings) {

            String[] ergebnisArray = new String[20];

            for (int i=0; i < 20; i++) {

                // Den StringArray füllen wir mit Beispieldaten
                ergebnisArray[i] = strings[0] + "_" + (i+1);

                // Alle 5 Elemente geben wir den aktuellen Fortschritt bekannt
                if (i%5 == 4) {
                    publishProgress(i+1, 20);
                }

                // Mit Thread.sleep(600) simulieren wir eine Wartezeit von 600 ms
                try {
                    Thread.sleep(600);
                }
                catch (Exception e) { Log.e(LOG_TAG, "Error ", e); }
            }

            return ergebnisArray;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

            // Auf dem Bildschirm geben wir eine Statusmeldung aus, immer wenn
            // publishProgress(int...) in doInBackground(String...) aufgerufen wird
            Toast.makeText(getActivity(), values[0] + " von " + values[1] + " geladen",
                    Toast.LENGTH_SHORT).show();

        }

        @Override
        protected void onPostExecute(String[] strings) {

            // Wir löschen den Inhalt des ArrayAdapters und fügen den neuen Inhalt ein
            // Der neue Inhalt ist der Rückgabewert von doInBackground(String...) also
            // der StringArray gefüllt mit Beispieldaten
            if (strings != null) {
                mAktienlisteAdapter.clear();
                for (String aktienString : strings) {
                    mAktienlisteAdapter.add(aktienString);
                }
            }

            // Hintergrundberechnungen sind jetzt beendet, darüber informieren wir den Benutzer
            Toast.makeText(getActivity(), "Aktiendaten vollständig geladen!",
                    Toast.LENGTH_SHORT).show();
        }
    }*/

}
