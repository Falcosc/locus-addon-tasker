package falcosc.locus.addon.tasker;

import android.app.Activity;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.stream.Stream;

import androidx.annotation.Nullable;

import androidx.documentfile.provider.DocumentFile;
import falcosc.locus.addon.tasker.intent.edit.ActionTaskEdit;
import falcosc.locus.addon.tasker.intent.edit.LocusInfoEdit;
import falcosc.locus.addon.tasker.intent.edit.NotImplementedActions;
import falcosc.locus.addon.tasker.intent.edit.UpdateContainerEdit;

@SuppressWarnings("ClassWithTooManyTransitiveDependencies") //because of mock tasker start
public class MainActivity extends ProjectActivity {

    private int i;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.imageView).setOnLongClickListener((v) -> mockTaskerEditStart());
    }

    @SuppressWarnings({"HardCodedStringLiteral", "MagicNumber"}) //because it is just a mock
    private boolean mockTaskerEditStart() {
        Class<?>[] editClasses = {
                UpdateContainerEdit.class,
                LocusInfoEdit.class,
                ActionTaskEdit.class,
                NotImplementedActions.class
        };

        performFileSearch();
        return false;
    }

    private static final int REQUEST_CODE_OPEN_FILE = 44;
    private static final int REQUEST_CODE_OPEN_DIRECTORY = 45;

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        //Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        //intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        //intent.setType("image/*");

        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        Log.i("Test", "requestCode: " + requestCode);


        if (requestCode == REQUEST_CODE_OPEN_FILE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i("Test", "Uri: " + uri.toString());
                writeEXIFWithFileDescriptor(uri);
            }
        }
        if(resultCode == RESULT_OK && requestCode == REQUEST_CODE_OPEN_DIRECTORY){
            if (Build.VERSION.SDK_INT < 24) {
                //TODO
                return;
            }
            Uri uriTree = resultData.getData();
            Log.i("Test", "Uri: " + uriTree.toString());

            DocumentFile documentFile = DocumentFile.fromTreeUri(this, uriTree);
            for (DocumentFile file : documentFile.listFiles()) {
                Log.i("Test", file.getName());

                if(file.isDirectory()){
                    Log.i("Test","is a Directory");
                }else{
                    Log.i("Test", file.getType());
                }
            }
            Intent serviceIntent = new Intent(this,
                    GeotagPhotosService.class);
            String[] fileUries = (String[]) Stream.of(documentFile.listFiles())
                    .filter(file -> "image/jpeg".equals(file.getType()))
                    .map(file -> file.getUri().toString())
                    .toArray( String[]::new );
            serviceIntent.putExtra("files" , fileUries);
            startService(serviceIntent);
        }
    }

    private void writeEXIFWithFileDescriptor(Uri uri) {

        if (Build.VERSION.SDK_INT < 24) {
            //TODO
            return;
        }

        ParcelFileDescriptor parcelFileDescriptor = null;
        try {

            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "rw");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Log.i("Test","writeEXIFWithFileDescriptor(): " + fileDescriptor.toString());
            ExifInterface exifInterface = new ExifInterface(fileDescriptor);
            Log.i("Test","GPS Ref: " +  exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF));
            // TODO Create  Exif Tags class to save Exif data
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
            exifInterface.saveAttributes();

        } catch (FileNotFoundException e) {
            Log.i("Test","File Not Found " + e.getMessage());

        } catch (IOException e) {
            // Handle any errors
            e.printStackTrace();
            Log.i("Test","IOEXception " + e.getMessage());
        } finally {
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }
            }
        }
    }


}
