package falcosc.locus.addon.tasker;

import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
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
import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.utils.IntentHelper;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Track;
import locus.api.objects.extra.TrackStats;

public class LocusGeoTagActivity extends ProjectActivity {

    private static final String TAG = "LocusGeoTagActivity"; //NON-NLS
    private static final Pattern NON_WORD_CHAR_PATTERN = Pattern.compile("[^\\w]");  //NON-NLS
    private static final int REQUEST_CODE_OPEN_DIRECTORY = 1;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;
    private LinkedList<DocumentFile> mDocumentFiles;
    private Uri mFolderUri;
    private DateFormat mLocalDateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    private Calendar mExampleDateTime = Calendar.getInstance();;
    private EditText mEditOffset;


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

        findViewById(R.id.viewExamplePhoto).setOnClickListener(v -> pickExampleFile());
        
        mEditOffset = findViewById(R.id.editOffset);
        //TODO add mEditOffset change listener

        //don't need to check for track intent because this is bound only to track intents

        try {
            if(!validateAndSetTrackDetails(IntentHelper.INSTANCE.getTrackFromIntent(this, getIntent()))){
                //TODO create error dialog instead of toast
                finish();
                return;
            }


            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
        } catch (Exception e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
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
            this.setTitle(String.format("Geotag %1$d photos", mDocumentFiles.size()));

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
            
            
            mExampleDateTime.setTime(GeotagPhotosService.exifDateFormat.parse(exifInterface.getAttribute(ExifInterface.TAG_DATETIME)));
            Log.i(TAG, "Exif Time: " + exifInterface.getAttribute(ExifInterface.TAG_DATETIME));

            TextView photoName = findViewById(R.id.textPhotoName);
            photoName.setText(file.getName());
            
            mExampleDateTime.add(Calendar.HOUR, Integer.parseInt(mEditOffset.getText().toString()));

            TextView photoTime = findViewById(R.id.textPhotoTime);
            String dateTimeString = mLocalDateTimeFormat.format(mExampleDateTime);
            photoTime.setText(dateTimeString);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
    }

    private boolean validateAndSetTrackDetails(Track track) throws RequiredDataMissingException {
        if(track == null || track.getPointsCount() == 0){
            throw new RequiredDataMissingException("Can't find Locus Track");
        }

        long firstPointTime = track.getPoint(0).getTime();
        long lastPointTime = track.getPoint(track.getPointsCount()-1).getTime();

        if( firstPointTime == 0 ||  lastPointTime == 0 ){
            Toast.makeText(this, "Can't use this track, because first or last point doesn't have a time", Toast.LENGTH_LONG).show();
            return false;
        }

        TextView startTime = findViewById(R.id.textStartTime);
        startTime.setText(mLocalDateTimeFormat.format(new Date( firstPointTime )));
        TextView endTime = findViewById(R.id.textEndTime);
        endTime.setText(mLocalDateTimeFormat.format(new Date( lastPointTime )));

        return true;
    }

    private void pickExampleFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/jpeg");
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, mFolderUri);
        intent.putExtra(DocumentsContract.EXTRA_PROMPT, "select example");
        startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT);
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
