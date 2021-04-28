package falcosc.locus.addon.tasker;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.ReportingHelper;
import locus.api.android.utils.IntentHelper;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Track;

public final class GeotagPhotosService extends JobIntentService {

    private static final String TAG = "GeotagPhotosService"; //NON-NLS
    private NotificationCompat.Builder mNotificationBuilder;
    private int progressEnd;
    private List<String> fileErrors;
    private int fileProgress;
    private List<Location> mPoints;
    private long[] mPointTimestamps;
    private long mTimeOffset;
    private AtomicInteger mOpenFiles;

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private final long openFilesThreshold = (long) (Os.sysconf(OsConstants._SC_OPEN_MAX) * 0.9);
    private boolean mNoMediaStoreAccess;
    private long mLastNotificationTime;

    private NotificationCompat.Builder createNotificationBuilder() {
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(getApplicationContext(), Const.NOTIFICATION_CHANNEL_ID);
        } else {
            //noinspection deprecation
            builder = new NotificationCompat.Builder(getApplicationContext());
        }

        return builder
                .setSmallIcon(R.drawable.ic_camera_alt)
                .setContentTitle(getString(R.string.geotag_title));

    }

    @Override
    protected void onHandleWork(@Nullable Intent workIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            new ReportingHelper(this).createDefaultNotificationChannel();
        }

        mNoMediaStoreAccess = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
        mNotificationBuilder = createNotificationBuilder();

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

        //progressbar read exif could reach up to 33% because we have 3 IO operations (read, copy, write)
        progressEnd = fileUris.size() * 3;
        fileProgress = 0;
        fileErrors = new ArrayList<>();
        mOpenFiles = new AtomicInteger(0);
        mTimeOffset = Objects.requireNonNull(workIntent).getIntExtra(Const.INTENT_EXTRA_GEOTAG_OFFSET, 0) * Const.ONE_HOUR;

        mNotificationBuilder.setProgress(progressEnd, fileProgress, false);
        mNotificationBuilder.setContentText(getString(R.string.start_process));
        startForeground(Const.NOTIFICATION_ID_GEOTAG, mNotificationBuilder.build());

        try {
            loadTrack(workIntent);
            processPhotos(fileUris);

            //keep notification
            stopForeground(STOP_FOREGROUND_DETACH);
        } catch (RequiredDataMissingException e) {
            stopWithError(ReportingHelper.getUserFriendlyName(e));
        }
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

    private void stopWithError(@NonNull String errMsg) {
        Log.e(TAG, errMsg);
        NotificationCompat.Builder builder = createNotificationBuilder().setContentText(errMsg);
        SystemClock.sleep(Const.NOTIFICATION_REPEAT_AFTER); //detach does sometimes not work if notifications fire close to each other
        startForeground(Const.NOTIFICATION_ID_GEOTAG, builder.build());
        //detach notification to keep
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH);
        } else {
            stopForeground(false);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void processPhotos(@NonNull ArrayList<Parcelable> fileUris) {
        Log.i(TAG, "search matching photos"); //NON-NLS
        mNotificationBuilder.setContentText(getString(R.string.geotag_search_matching_photos));
        startForeground(Const.NOTIFICATION_ID_GEOTAG, mNotificationBuilder.build());

        ArrayList<PendingExifChange> pendingExifChanges = fileUris.parallelStream()
                .map(fileUri -> findAndSetLocation((Uri) fileUri))
                .filter(Objects::nonNull)
                .sorted(Comparator.nullsLast(Comparator.comparing(PendingExifChange::getTime)))
                .collect(Collectors.toCollection(ArrayList::new));

        Log.i(TAG, "write exif"); //NON-NLS
        //reset progress to 33% because 2 of 3 IO ops are missing
        fileProgress = pendingExifChanges.size() / 2; //66% / 2
        progressEnd = fileProgress + pendingExifChanges.size(); //33% + 66%
        mNotificationBuilder.setContentText(getString(R.string.geotag_write_exif));
        mNotificationBuilder.setProgress(progressEnd, fileProgress, false);
        startForeground(Const.NOTIFICATION_ID_GEOTAG, mNotificationBuilder.build());

        pendingExifChanges.forEach(this::write);

        //mLastNotificationTime does prevent that this important notification got dismissed because of notification overflow
        sendResultNotification(pendingExifChanges.stream()
                .map(PendingExifChange::getUri)
                .collect(Collectors.toCollection(ArrayList::new)));
        Log.i(TAG, "done"); //NON-NLS
    }

    private void sendResultNotification(@NonNull ArrayList<Uri> imageUris) {
        NotificationCompat.Builder builder = createNotificationBuilder();

        Intent filesIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        filesIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
        filesIntent.setType(Const.MIME_TYPE_IMAGES);
        filesIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        String title = getResources().getQuantityString(R.plurals.share_x_photos, imageUris.size(), imageUris.size());
        PendingIntent pendingSendFiles = PendingIntent.getActivity(this, 2,
                Intent.createChooser(filesIntent, title), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(android.R.drawable.ic_menu_share, title, pendingSendFiles);
        builder.setContentIntent(pendingSendFiles);

        if (fileErrors.isEmpty()) {
            builder.setContentTitle(getString(R.string.geotag_title_done));
            builder.setContentText(getResources().getQuantityString(R.plurals.geotag_x_photos_successful, imageUris.size(), imageUris.size()));
        } else {
            String errorLongText = StringUtils.join(fileErrors, '\n');
            builder.setContentTitle(getResources().getQuantityString(R.plurals.geotag_x_photos_successful, imageUris.size(), imageUris.size())
                    + ", " + getResources().getQuantityString(R.plurals.err_geotag_x_skipped, fileErrors.size(), fileErrors.size()))
                    .setContentText(fileErrors.get(0))
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(StringUtils.join(errorLongText, '\n'))
                            .setSummaryText(getString(R.string.geotag_title))
                    );

            Intent getTextIntent = new Intent(Intent.ACTION_SEND);
            getTextIntent.putExtra(Intent.EXTRA_TEXT, errorLongText);
            getTextIntent.setType(Const.MIME_TYPE_TEXT);
            getTextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingGetText = PendingIntent.getActivity(this, 1,
                    Intent.createChooser(getTextIntent, getString(R.string.error_details)), PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_description, getString(R.string.error_details), pendingGetText);
        }

        startForeground(Const.NOTIFICATION_ID_GEOTAG, builder.build());
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
    @Nullable
    private PendingExifChange findAndSetLocation(@NonNull Uri uri) {
        ParcelFileDescriptor pfd = null;
        PendingExifChange pendingChange = null;
        try {
            //don't need setRequireOriginal because we are just writing
            pfd = getContentResolver().openFileDescriptor(uri, "rw"); //NON-NLS
            if (pfd == null) {
                incrementProgressWithError(uri, getString(R.string.err_geotag_open_file));
                return null;
            }

            //createPendingExifChange does increment progress
            pendingChange = createPendingExifChange(pfd, uri);
        } catch (IOException e) {
            incrementProgressWithError(uri, getString(R.string.err_geotag_read_file)
                    + " " + ReportingHelper.getUserFriendlyName(e));
            Log.e(TAG, "exif IOException", e); //NON-NLS
        } catch (ParseException e) {
            incrementProgressWithError(uri, getString(R.string.err_geotag_date_invalid));
        } catch (Exception e) {
            incrementProgressWithError(uri, ReportingHelper.getUserFriendlyName(e));
        } finally {
            //only close if we don't have a pending change
            if (pendingChange == null) {
                closeQuietly(pfd);
            }
        }

        return pendingChange;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    private static Optional<String> getAttribute(@NonNull ExifInterface exif, @NonNull String tag) {
        String value = exif.getAttribute(tag);
        if (StringUtils.isBlank(value)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    private static void setAttributeIfBlank(@NonNull ExifInterface exif, @NonNull String tag, @NonNull String newValue) {
        String oldValue = exif.getAttribute(tag);
        if (StringUtils.isBlank(oldValue)) {
            exif.setAttribute(tag, newValue);
        }
    }

    private static android.location.Location convertLocation(Location loc) {
        android.location.Location result = new android.location.Location(loc.getProvider());
        result.setLatitude(loc.getLatitude());
        result.setLongitude(loc.getLongitude());
        result.setAltitude(loc.getAltitude());
        result.setSpeed(loc.getSpeed());
        result.setTime(loc.getTime());
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    public static String getOriginalDateTime(@NonNull ExifInterface exif) {
        return getAttribute(exif, ExifInterface.TAG_DATETIME_ORIGINAL)
                .orElseGet(() -> getAttribute(exif, ExifInterface.TAG_DATETIME)
                        .orElseGet(() -> getAttribute(exif, ExifInterface.TAG_DATETIME_DIGITIZED)
                                .orElse(null)));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private PendingExifChange createPendingExifChange(@NonNull ParcelFileDescriptor pfd, @NonNull Uri uri) throws IOException, ParseException {
        FileDescriptor fd = pfd.getFileDescriptor();
        ExifInterface exif = new ExifInterface(fd);

        String exifTime = getOriginalDateTime(exif);
        if (exifTime == null) {
            incrementProgressWithError(uri, getString(R.string.err_geotag_no_date));
            return null;
        }
        setAttributeIfBlank(exif, ExifInterface.TAG_DATETIME_ORIGINAL, exifTime);
        setAttributeIfBlank(exif, ExifInterface.TAG_DATETIME_DIGITIZED, exifTime);

        long time = Const.EXIF_DATE_FORMAT.parse(exifTime).getTime() + mTimeOffset;
        exif.setAttribute(ExifInterface.TAG_DATETIME, Const.EXIF_DATE_FORMAT.format(time));

        Location loc = findNearestLocation(time);
        long timeDiff = Math.abs(loc.getTime() - time);
        if (timeDiff > Const.ONE_HOUR) {
            //noinspection NumericCastThatLosesPrecision
            int hoursAway = (int) (timeDiff / Const.ONE_HOUR);
            incrementProgressWithError(uri, getResources().getQuantityString(R.plurals.err_geotag_x_hours_away, hoursAway, hoursAway));
            return null;
        }

        android.location.Location androidLoc = convertLocation(loc);
        exif.setGpsInfo(androidLoc);

        //it is ok to increment at this point because ForkJoinPool is very small
        if (mOpenFiles.get() >= openFilesThreshold) {
            //write exif in parallel mode if we have to many open files
            write(new PendingExifChange(exif, pfd, uri, time, androidLoc));
            return null;
        }

        mOpenFiles.incrementAndGet();
        incrementNotificationProgress();
        return new PendingExifChange(exif, pfd, uri, time, androidLoc);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private synchronized void updateMediaStore(Uri uri, android.location.Location loc, long time) {
        if (mNoMediaStoreAccess || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)) {
            return;
        }

        Uri mediaStore;

        //noinspection CallToSuspiciousStringMethod
        if (Const.AUTHORITY_EXTERNAL_STORAGE.equals(uri.getAuthority())) {
            mediaStore = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else {
            Log.w(TAG, "skip update media store");  //NON-NLS
            return;
        }

        String path = Optional.ofNullable(uri.getPath()).orElseGet(uri::toString);
        path = path.substring(path.lastIndexOf(':') + 1);

        ContentValues values = new ContentValues();
        //noinspection StaticFieldReferencedViaSubclass because this is an api 29 refactoring
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, time);
        values.put(MediaStore.Images.ImageColumns.LATITUDE, loc.getLatitude());
        values.put(MediaStore.Images.ImageColumns.LONGITUDE, loc.getLongitude());
        int updatedRows = getContentResolver().update(mediaStore, values,
                MediaStore.MediaColumns.DATA + " LIKE ?", new String[]{"%" + path}); //NON-NLS
        Log.i(TAG, "Updated MediaStore Rows: " + updatedRows + " for " + path);  //NON-NLS
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void write(@NonNull PendingExifChange pendingChange) {
        Log.i(TAG, "write exif " + pendingChange.getUri()); //NON-NLS
        try {
            pendingChange.mExif.saveAttributes();
            //manually updating media store to get result in realtime
            updateMediaStore(pendingChange.getUri(), pendingChange.mLoc, pendingChange.getTime());
            incrementNotificationProgress();
        } catch (IOException e) {
            incrementProgressWithError(pendingChange.getUri(), getString(R.string.err_geotag_write_exif)
                    + " " + ReportingHelper.getUserFriendlyName(e));
            Log.e(TAG, "exif IOException", e); //NON-NLS
        } finally {
            closeQuietly(pendingChange.mPfd);
        }
    }

    private static void closeQuietly(@Nullable Closeable closable) {
        try {
            if (closable != null) {
                closable.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "close Exception", e); //NON-NLS
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
        mNotificationBuilder.setContentTitle(getText(R.string.geotag_title) + ", " +
                getResources().getQuantityString(R.plurals.err_geotag_x_skipped, fileErrorCount, fileErrorCount));

        incrementNotificationProgress();
    }

    @SuppressWarnings("CallToNativeMethodWhileLocked")
    private synchronized void incrementNotificationProgress() {
        mNotificationBuilder.setProgress(progressEnd, ++fileProgress, false);
        long newTime = SystemClock.elapsedRealtime();
        if ((mLastNotificationTime + Const.NOTIFICATION_REPEAT_AFTER) < newTime) {
            Notification notification = mNotificationBuilder.build();
            startForeground(Const.NOTIFICATION_ID_GEOTAG, notification);
            mLastNotificationTime = newTime;
        }
    }

    static class PendingExifChange {
        final ExifInterface mExif;
        final ParcelFileDescriptor mPfd;
        final android.location.Location mLoc;

        Uri getUri() {
            return mUri;
        }

        private final Uri mUri;
        private final long mTime;

        long getTime() {
            return mTime;
        }

        PendingExifChange(@NonNull ExifInterface exif, @NonNull ParcelFileDescriptor pfd, @NonNull Uri uri, long time, android.location.Location loc) {
            mExif = exif;
            mPfd = pfd;
            mUri = uri;
            mTime = time;
            mLoc = loc;
        }

    }

}
