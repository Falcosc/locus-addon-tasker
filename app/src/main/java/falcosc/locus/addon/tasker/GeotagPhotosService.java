package falcosc.locus.addon.tasker;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.stream.Stream;

import androidx.annotation.RequiresApi;
import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.utils.IntentHelper;
import locus.api.objects.extra.Track;

public class GeotagPhotosService extends IntentService {

    public static final SimpleDateFormat exifDateFormat = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss");
    private static final String TAG = "GeotagPhotosService"; //NON-NLS
    private int notificationID = 10;
    private Notification.Builder notificationBuilder;
    private int fileCount;
    private int fileProgress;


    public GeotagPhotosService() {
        super("GeotagPhotosService");
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        String[] fileUries = workIntent.getStringArrayExtra("files");
        Log.e(TAG, "fileUries: " + fileUries);
        fileCount = fileUries.length;
        fileProgress = 0;

        if (Build.VERSION.SDK_INT < 24) {
            //TODO
            return;
        }

//Set notification information:
        notificationBuilder = new Notification.Builder(getApplicationContext());
        notificationBuilder.setOngoing(true)
                .setContentTitle("Notification Content Title")
                .setContentText("Notification Content Text")
                .setSmallIcon(R.drawable.ic_camera_alt)
                .setProgress(fileCount, fileProgress, false);

//Send the notification:
        Notification notification = notificationBuilder.build();
        startForeground(notificationID, notification);

        //loadTrack(workIntent);

        Stream.of(fileUries).forEach(fileUri -> writeEXIFWithFileDescriptor(Uri.parse(fileUri)));

        stopForeground(true);
    }

    private void loadTrack(Intent locusIntent) {
        try {
            LocusCache locusCache = LocusCache.getInstanceUnsafe(this);
            Track t = IntentHelper.INSTANCE.getTrackFromIntent(this, locusIntent);
            Log.e(TAG, "points: " + t.getPoints().size());
        } catch (Exception e) {
            Log.e(TAG, "Can't get intent details", e); //NON-NLS
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void writeEXIFWithFileDescriptor(Uri uri) {

        ParcelFileDescriptor parcelFileDescriptor = null;
        try {

            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "rw");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Log.i(TAG,"writeEXIFWithFileDescriptor(): " + fileDescriptor.toString());
            ExifInterface exifInterface = new ExifInterface(fileDescriptor);
            Log.i(TAG,"GPS Ref: " +  exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF));
            // TODO Create  Exif Tags class to save Exif data
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
            exifInterface.saveAttributes();

        } catch (FileNotFoundException e) {
            Log.i(TAG,"File Not Found " + e.getMessage());

        } catch (IOException e) {
            // Handle any errors
            e.printStackTrace();
            Log.i(TAG,"IOEXception " + e.getMessage());
        } finally {
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }
            }
            notificationBuilder.setProgress(fileCount, ++fileProgress, false);
            Notification notification = notificationBuilder.build();
            startForeground(notificationID, notification);
        }
    }

}
