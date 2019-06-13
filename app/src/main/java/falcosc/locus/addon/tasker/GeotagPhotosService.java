package falcosc.locus.addon.tasker;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.exifinterface.media.ExifInterface;
import falcosc.locus.addon.tasker.utils.Const;
import locus.api.android.utils.IntentHelper;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Track;

public final class GeotagPhotosService extends IntentService {

    private static final String TAG = "GeotagPhotosService"; //NON-NLS
    private Notification.Builder mNotificationBuilder;
    private int fileCount;
    private AtomicInteger fileErrorCount;
    private int fileProgress;
    private List<Location> mPoints;
    private long[] mPointTimestamps;
    private long mTimeOffset;


    public GeotagPhotosService() {
        super(TAG);
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent workIntent) {
        mNotificationBuilder = createNotificationBuilder();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            stopWithError(getString(R.string.err_geotag_required_android_version));
            return;
        }

        ArrayList<String> fileUris = Optional.ofNullable(workIntent)
                .map(intent -> intent.getStringArrayListExtra(Const.INTENT_EXTRA_GEOTAG_FILES))
                .orElse(new ArrayList<>());

        if (fileUris.isEmpty()) {
            stopWithError(getString(R.string.err_geotag_no_images_found));
            return;
        }

        fileCount = fileUris.size();
        fileProgress = 0;
        fileErrorCount = new AtomicInteger(0);
        //noinspection ConstantConditions is checked by fileUries
        mTimeOffset = workIntent.getLongExtra(Const.INTENT_EXTRA_GEOTAG_OFFSET,0L) * Const.ONE_HOUR;

        mNotificationBuilder.setProgress(fileCount, fileProgress, false);
        mNotificationBuilder.setOngoing(true).setContentText(getString(R.string.start_process));
        startForeground(Const.NOTIFICATION_ID_GEOTAG, mNotificationBuilder.build());

        try {
            loadTrack(workIntent);
            fileUris.parallelStream().forEach(fileUri -> writeEXIFWithFileDescriptor(Uri.parse(fileUri)));
            stopForeground(fileErrorCount.get() == 0);
        } catch (RequiredDataMissingException e) {
            stopWithError(e.getMessage());
        }
    }

    private Notification.Builder createNotificationBuilder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //TODO do we need advanced channel management?
            mNotificationBuilder = new Notification.Builder(getApplicationContext(), NotificationChannel.DEFAULT_CHANNEL_ID);
        } else {
            mNotificationBuilder = new Notification.Builder(getApplicationContext());
        }
        mNotificationBuilder.setSmallIcon(R.drawable.ic_camera_alt)
                .setContentTitle(getString(R.string.geotag_title));
        return mNotificationBuilder;
    }

    private void stopWithError(String errMsg) {
        mNotificationBuilder.setOngoing(false).setContentText(errMsg);
        startForeground(Const.NOTIFICATION_ID_GEOTAG, mNotificationBuilder.build());
        stopForeground(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadTrack(@Nullable Intent locusIntent) throws RequiredDataMissingException {
        try {
            Track t = IntentHelper.INSTANCE.getTrackFromIntent(this, Objects.requireNonNull(locusIntent));
            mPoints = Objects.requireNonNull(t).getPoints();
            mPoints.sort(Comparator.comparing(Location::getTime));
            mPointTimestamps = mPoints.stream().mapToLong(Location::getTime).toArray();
            Log.i(TAG, "points: " + t.getPoints().size()); //NON-NLS
        } catch (Exception e) {
            Log.e(TAG, "Can't load track details", e); //NON-NLS
            throw new RequiredDataMissingException("Can't load track details", e);
        }
    }

    @NonNull
    private Location findNearestLocation(long time) {
        int index = Arrays.binarySearch(mPointTimestamps, time);
        if (index >= 0) {
            //direct match
            return mPoints.get(index);
        }

        index = -index - 1;
        if (index == 0) {
            // smaller than any
            return mPoints.get(0);
        } else if (index >= mPointTimestamps.length) {
            // larger than any
            return mPoints.get(mPointTimestamps.length - 1);
        }

        //index larger then time and index-1 is smaller then time
        return ((mPointTimestamps[index] - time) < (time - mPointTimestamps[index - 1]))
                ? mPoints.get(index)  //index is closer then index-1
                : mPoints.get(index - 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void writeEXIFWithFileDescriptor(@NonNull Uri uri) {

        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "rw")) { //NON-NLS
            assert pfd != null;
            ExifInterface exifInterface = new ExifInterface(pfd.getFileDescriptor());
            Date dateTime = Const.EXIF_DATE_FORMAT.parse(exifInterface.getAttribute(ExifInterface.TAG_DATETIME));
            long time = dateTime.getTime() + mTimeOffset;
            Location loc = findNearestLocation(time);
            long timeDiff = Math.abs(loc.getTime() - time);
            if (timeDiff < Const.ONE_HOUR) {
                exifInterface.setLatLong(loc.latitude, loc.longitude);
                exifInterface.saveAttributes();
            } else {
                Log.e(TAG, uri + " is " + timeDiff / Const.ONE_HOUR + " hours away from closest match"); //NON-NLS
                fileErrorCount.incrementAndGet();
            }

        } catch (ParseException | IOException e) {
            Log.e(TAG, "can't write exif for " + uri, e); //NON-NLS
            fileErrorCount.incrementAndGet();
        }

        incrementNotificationProgress();
    }

    private synchronized void incrementNotificationProgress() {
        mNotificationBuilder.setProgress(fileCount, ++fileProgress, false);
        int errorCount = fileErrorCount.get();
        if (errorCount > 0) {
            mNotificationBuilder.setContentText(getResources().getQuantityString(R.plurals.err_geotag_x_photos_no_match, errorCount));
            //TODO expandable notification
        } else {
            mNotificationBuilder.setContentText(getString(R.string.geotag_process_photos));
        }
        Notification notification = mNotificationBuilder.build();
        startForeground(Const.NOTIFICATION_ID_GEOTAG, notification);
    }

}
