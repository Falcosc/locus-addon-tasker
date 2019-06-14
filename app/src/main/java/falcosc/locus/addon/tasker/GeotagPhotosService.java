package falcosc.locus.addon.tasker;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.exifinterface.media.ExifInterface;
import falcosc.locus.addon.tasker.utils.Const;
import locus.api.android.utils.IntentHelper;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Track;

public final class GeotagPhotosService extends IntentService {

    private static final String TAG = "GeotagPhotosService"; //NON-NLS
    private NotificationCompat.Builder mNotificationBuilder;
    private int fileCount;
    private List<String> fileErrors;
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
        mNotificationBuilder = createNotificationBuilder().setOngoing(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            stopWithError(getString(R.string.err_geotag_required_android_version));
            return;
        }

        ArrayList<Parcelable> fileUris = Optional.ofNullable(workIntent)
                .map(intent -> intent.getParcelableArrayListExtra(Const.INTENT_EXTRA_GEOTAG_FILES))
                .orElse(new ArrayList<>());

        if (fileUris.isEmpty()) {
            stopWithError(getString(R.string.err_geotag_no_images_found));
            return;
        }

        fileCount = fileUris.size();
        fileProgress = 0;
        fileErrors = new ArrayList<>();
        mTimeOffset = Objects.requireNonNull(workIntent).getIntExtra(Const.INTENT_EXTRA_GEOTAG_OFFSET, 0) * Const.ONE_HOUR;

        mNotificationBuilder.setProgress(fileCount, fileProgress, false);
        mNotificationBuilder.setContentText(getString(R.string.start_process));
        startForeground(Const.NOTIFICATION_ID_GEOTAG, mNotificationBuilder.build());

        try {
            loadTrack(workIntent);

            mNotificationBuilder.setContentText(getString(R.string.geotag_process_photos));
            startForeground(Const.NOTIFICATION_ID_GEOTAG, mNotificationBuilder.build());

            fileUris.parallelStream().forEach(fileUri -> exifFindAndWriteLocation((Uri) fileUri));

            boolean hasResultNotification = sendResultNotification();

            //remove notification if everything is fine, otherwise keep it with detach from service
            stopForeground(hasResultNotification ? STOP_FOREGROUND_DETACH : STOP_FOREGROUND_REMOVE);
        } catch (RequiredDataMissingException e) {
            stopWithError(e.getMessage());
        }
    }

    private boolean sendResultNotification() {
        if (fileErrors.isEmpty()) {
            return false;
        }

        Notification notification = createNotificationBuilder()
                .setContentTitle(getString(R.string.geotag_title) + " " + getString(R.string.done_with_errors))
                .setContentText(getResources().getQuantityString(R.plurals.err_geotag_x_photos_no_match, fileErrors.size(), fileErrors.size()))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(StringUtils.join(fileErrors, '\n'))
                        .setSummaryText(String.format(getString(R.string.err_geotag_x_photos_got_not_tagged), fileErrors.size(), fileCount))
                ).build();
        //TODO onclick display dialog or share onclick
        startForeground(Const.NOTIFICATION_ID_GEOTAG, notification);
        return true;
    }

    private NotificationCompat.Builder createNotificationBuilder() {
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //TODO do we need advanced channel management?
            builder = new NotificationCompat.Builder(getApplicationContext(), NotificationChannel.DEFAULT_CHANNEL_ID);
        } else {
            //noinspection deprecation
            builder = new NotificationCompat.Builder(getApplicationContext());
        }

        return builder
                .setSmallIcon(R.drawable.ic_camera_alt)
                .setContentTitle(getString(R.string.geotag_title));

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void stopWithError(String errMsg) {
        Log.e(TAG, errMsg);
        NotificationCompat.Builder builder = createNotificationBuilder()
                .setOngoing(false)
                .setContentText(errMsg);
        startForeground(Const.NOTIFICATION_ID_GEOTAG, builder.build());
        //detach notification to keep
        stopForeground(STOP_FOREGROUND_DETACH);
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

    private void exifFindAndWriteLocation(@NonNull Uri uri) {

        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "rw")) { //NON-NLS
            assert pfd != null;
            ExifInterface exifInterface = new ExifInterface(pfd.getFileDescriptor());


            String exifTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
            if (exifTime == null) {
                incrementProgressWithError(uri, getString(R.string.err_geotag_no_date));
                return;
            }

            long time = Const.EXIF_DATE_FORMAT.parse(exifTime).getTime() + mTimeOffset;

            Location loc = findNearestLocation(time);
            long timeDiff = Math.abs(loc.getTime() - time);
            if (timeDiff > Const.ONE_HOUR) {
                //noinspection NumericCastThatLosesPrecision
                int hoursAway = (int) (timeDiff / Const.ONE_HOUR);
                incrementProgressWithError(uri, getResources().getQuantityString(R.plurals.err_geotag_x_hours_away, hoursAway, hoursAway));
                return;
            }

            exifInterface.setLatLong(loc.latitude, loc.longitude);
            exifInterface.saveAttributes();
            incrementNotificationProgress();
        } catch (IOException e) {
            incrementProgressWithError(uri, getString(R.string.err_geotag_write_exif) + " " + e.getLocalizedMessage());
            Log.e(TAG, "exif IOException", e); //NON-NLS
        } catch (ParseException e) {
            incrementProgressWithError(uri, getString(R.string.err_geotag_date_invalid));
        }


    }

    private synchronized void incrementProgressWithError(@NonNull Uri uri, @NonNull String error) {
        String uriPath = uri.getPath();
        if (uriPath == null) {
            uriPath = uri.toString();
        }
        String fileName = new File(uriPath).getName();
        String errLine = fileName + ": " + error;

        fileErrors.add(errLine);
        Log.e(TAG, errLine);

        int fileErrorCount = fileErrors.size();

        mNotificationBuilder
                .setContentText(getResources().getQuantityString(R.plurals.err_geotag_x_photos_no_match, fileErrorCount, fileErrorCount))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(StringUtils.join(fileErrors, '\n'))
                        .setSummaryText(String.format(getString(R.string.err_geotag_x_photos_got_not_tagged), fileErrorCount, fileCount))
                );
        incrementNotificationProgress();
    }

    private synchronized void incrementNotificationProgress() {
        mNotificationBuilder.setProgress(fileCount, ++fileProgress, false);
        Notification notification = mNotificationBuilder.build();
        startForeground(Const.NOTIFICATION_ID_GEOTAG, notification);
    }

}
