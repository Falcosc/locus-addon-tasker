package falcosc.locus.addon.tasker;

import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import locus.api.android.utils.IntentHelper;
import locus.api.objects.extra.Track;

public class LocusGeoTagActivity extends ProjectActivity {

    private static final String TAG = "LocusGeoTagActivity"; //NON-NLS
    private static final Pattern NON_WORD_CHAR_PATTERN = Pattern.compile("[^\\w]");  //NON-NLS
    private static final int REQUEST_CODE_OPEN_DIRECTORY = 1;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;
    private LinkedList<DocumentFile> mDocumentFiles;
    private Uri mFolderUri;
    private java.text.DateFormat mLocalDateTimeFormat = DateFormat.getDateFormat(this);


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.geotag_start);

        Button cancelButton = findViewById(R.id.btnCancel);
        cancelButton.setOnClickListener(v -> finish());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            //TODO
            return;
        }

        Button okButton = findViewById(R.id.btnOk);
        okButton.setOnClickListener(v -> startGeoTag());

        TextView photoDetails = findViewById(R.id.textPhotoDetails);
        photoDetails.setOnClickListener(v -> pickExampleFile());

        //don't need to check for track intent because this is bound only to track intents

        try {
            setTrack(IntentHelper.INSTANCE.getTrackFromIntent(this, getIntent()));
        } catch (Exception e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG);
            finish();
        }
        
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.i(TAG, "requestCode: " + requestCode);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            //todo
            return;
        }

        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY) {
            handleOpenDir(resultCode, resultData);
        } else if (requestCode == REQUEST_CODE_OPEN_DOCUMENT) {
            handleOpenFile(resultCode, resultData);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void handleOpenDir(int resultCode, Intent resultData){
        if (resultCode == RESULT_OK) {

            Uri uriTree = resultData.getData();
            Log.i(TAG, "Uri: " + uriTree.toString());
            mFolderUri = uriTree;

            mDocumentFiles = Stream.of(DocumentFile.fromTreeUri(this, uriTree).listFiles())
                    .filter(file -> "image/jpeg".equals(file.getType()))
                    .collect(Collectors.toCollection(LinkedList::new));

            if (mDocumentFiles.isEmpty()) {
                Toast.makeText(this, "Can't find any jpeg's, please select an other folder or abort", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
                return;
            }

            mDocumentFiles.stream()
                    .max(Comparator.comparing(DocumentFile::lastModified))
                    .ifPresent(file -> setExmple(file));
            this.setTitle(String.format("Geotag %1$i photos", mDocumentFiles.size()));

        } else {
            finish();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void handleOpenFile(int resultCode, Intent resultData){
        if (resultCode == RESULT_OK && resultData != null) {
            Uri uri = resultData.getData();
            if (uri != null) {
                setExmple(DocumentFile.fromSingleUri(this, uri));
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setExmple(DocumentFile file) {
        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(file.getUri(), "r")) {
            ExifInterface exifInterface = new ExifInterface(pfd.getFileDescriptor());
            Date dateTime = GeotagPhotosService.exifDateFormat.parse(exifInterface.getAttribute(ExifInterface.TAG_DATETIME));
            TextView photoDetails = findViewById(R.id.textPhotoDetails);
            String dateTimeString = mLocalDateTimeFormat.format(dateTime);
            photoDetails.setText(file.getName() + '\n' + dateTimeString);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
    }

    private void setTrack(Track track){
        if(track == null){
            throw new NoSuchElementException("Can't find Locus Track");
        }
        TextView trackText = findViewById(R.id.textTrack);
        trackText.setText(track.getName());

        String starTime = mLocalDateTimeFormat.format(new Date( track.getStats().getStartTime()));
        String stopTime = mLocalDateTimeFormat.format(new Date( track.getStats().getStopTime()));

        TextView trackDetails = findViewById(R.id.textTrackDetails);
        trackDetails.setText(String.format("Start %1$s \n End %2$s", starTime, stopTime));

    }

    private void pickExampleFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/jpeg");
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, mFolderUri);
        intent.putExtra(DocumentsContract.EXTRA_PROMPT, "select example");
        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startGeoTag() {
        Intent serviceIntent = new Intent(this,
                GeotagPhotosService.class);
        //set locus intent data
        serviceIntent.putExtras(getIntent());
        String[] fileUries = (String[]) mDocumentFiles.stream()
                .map(file -> file.getUri().toString())
                .toArray(String[]::new);
        serviceIntent.putExtra("files", fileUries);
        ContextCompat.startForegroundService(this, serviceIntent);
        setResult(RESULT_OK, getIntent());
        finish();
    }


}
