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
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.annotation.RequiresApi;
import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.utils.IntentHelper;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Track;

public class GeotagPhotosService extends IntentService {

    public static final SimpleDateFormat exifDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
    private static final String TAG = "GeotagPhotosService"; //NON-NLS
    public static final long ONE_HOUR = 3600000L;
    private int notificationID = 10;
    private Notification.Builder notificationBuilder;
    private int fileCount;
    private AtomicInteger fileErrorCount;
    private int fileProgress;
    private List<Location> mPoints;
    private long[] mPointTimestamps;


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
        fileErrorCount = new AtomicInteger(0);

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

        loadTrack(workIntent);

        Stream.of(fileUries).parallel().forEach(fileUri -> writeEXIFWithFileDescriptor(Uri.parse(fileUri)));

        stopForeground(fileErrorCount.get() == 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadTrack(Intent locusIntent) {
        try {
            Track t = IntentHelper.INSTANCE.getTrackFromIntent(this, locusIntent);
            mPoints = t.getPoints();
            mPoints.sort(Comparator.comparing(Location::getTime));
            mPointTimestamps = mPoints.stream().mapToLong(Location::getTime).toArray();

            Log.e(TAG, "points: " + t.getPoints().size());
        } catch (Exception e) {
            Log.e(TAG, "Can't get intent details", e); //NON-NLS
        }
    }

    private Location findNearestLocation(long time){
        int index = Arrays.binarySearch(mPointTimestamps, time);
        if ( index >= 0 ) {
            //direct match
            return mPoints.get(index);
        }

        index = -index - 1;
        if ( index == 0 ) {
            // smaller than any
            return mPoints.get(index);
        } else if (index >= mPointTimestamps.length){
            // larger than any
            return mPoints.get(mPointTimestamps.length - 1);
        }

        //index larger then time and index-1 is smaller then time
        return ((mPointTimestamps[index] - time) < (time - mPointTimestamps[index - 1]))
                ? mPoints.get(index)  //index is closer then index-1
                : mPoints.get(index - 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void writeEXIFWithFileDescriptor(Uri uri) {

        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "rw")) {
            ExifInterface exifInterface = new ExifInterface(pfd.getFileDescriptor());
            Date dateTime = exifDateFormat.parse(exifInterface.getAttribute(ExifInterface.TAG_DATETIME));
            //TODO use offset
            //TODO apply timezone
            long time = dateTime.getTime();
            Location loc = findNearestLocation(time);
            long timeDiff = Math.abs(loc.getTime()-time);
            if(timeDiff < ONE_HOUR){
                // TODO save location
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
                exifInterface.saveAttributes();
            } else {
                Log.e(TAG, uri + " is "  + timeDiff / ONE_HOUR + " hours away from closest match");
                fileErrorCount.incrementAndGet();
            }


        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        incrementNotificationProgress();

    }

    private synchronized void incrementNotificationProgress(){
        notificationBuilder.setProgress(fileCount, ++fileProgress, false);
        int errorCount = fileErrorCount.get();
        if(errorCount > 0) {
            notificationBuilder.setContentText(String.format("%1$d Photos are more then 1 hour away from Track", errorCount));
        }
        Notification notification = notificationBuilder.build();
        startForeground(notificationID, notification);
    }

}
