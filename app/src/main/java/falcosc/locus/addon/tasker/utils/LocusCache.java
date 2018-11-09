package falcosc.locus.addon.tasker.utils;

import android.app.Application;
import android.arch.core.util.Function;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import falcosc.locus.addon.tasker.LocusSetActiveTrack;
import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.LocusUtils.LocusVersion;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Track;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import java.util.*;

public final class LocusCache {
    private static final String TAG = "LocusCache"; //NON-NLS
    public static final String CALC_REMAIN_UPHILL_ELEVATION = "calc_remain_uphill_elevation"; //NON-NLS
    private static final Object mSyncObj = new Object();
    private static final long UPDATE_CONTAINER_EXPIRATION = 950L;

    private static LocusCache mInstance;

    private final Application mApplicationContext;

    @NonNull
    public Application getApplicationContext() {
        return mApplicationContext;
    }

    public final HashSet<String> mTrackRecordingKeys;
    public final HashSet<String> mTrackGuideKeys;
    public final Map<String, LocusField> mUpdateContainerFieldMap;
    public final ArrayList<LocusField> mUpdateContainerFields;
    public final LocusVersion mLocusVersion;
    private final Resources mLocusResources;
    public final String mNavigationTrackName;

    //selected track fields
    private Track mLastSelectedTrack;
    public int[] mRemainingTrackElevation;
    public int mLastIndexOnRemainingTrack;

    //update container
    private UpdateContainer mUpdateContainer;
    private long mUpdateContainerExpiration;

    @SuppressWarnings("HardCodedStringLiteral")
    private LocusCache(Application context) {
        Log.d(TAG, "init Locus cache");

        mApplicationContext = context;

        mLocusVersion = LocusUtils.getActiveVersion(context);
        Log.d(TAG, "Locus version: " + mLocusVersion);

        Resources locusRes = null;
        try {
            locusRes = context.getPackageManager().getResourcesForApplication(mLocusVersion.getPackageName());
            Log.d(TAG, "Found Locus resources");
        } catch (Exception e) {
            Log.d(TAG, "Missing Locus resources", e);
        }
        mLocusResources = locusRes;

        mUpdateContainerFields = createUpdateContainerFields();
        Log.d(TAG, "Locus fields created: " + mUpdateContainerFields.size());
        mUpdateContainerFieldMap = createUpdateContainerFieldMap();
        mTrackRecordingKeys = createUpdateContainerTrackRecKeys();
        Log.d(TAG, "Locus field keys mapped - recording keys: " + mTrackRecordingKeys.size());
        mTrackGuideKeys = createUpdateContainerTrackGuideKeys();
        Log.d(TAG, "Locus field keys mapped - guiding keys: " + mTrackGuideKeys.size());


        mNavigationTrackName = getLocusLabelByName("navigation");
        Log.d(TAG, "Locus navigation track name: " + mNavigationTrackName);
    }

    @NonNull
    public static LocusCache getInstance(@NonNull Application context) {

        if (mInstance == null) {
            synchronized (mSyncObj) {
                if (mInstance == null) {
                    mInstance = new LocusCache(context);
                }
            }
        }
        return mInstance;
    }

    @NonNull
    public static LocusCache getInstanceUnsafe(@NonNull Context context) throws MissingAppContextException {
        if (mInstance == null) {
            Context appContext = context.getApplicationContext();
            //TODO test this with broadcast receiver
            if (appContext instanceof Application) {
                return getInstance((Application) appContext);
            } else {
                throw new MissingAppContextException();
            }
        }
        return mInstance;
    }

    @Nullable
    public static LocusCache getInstanceNullable() {
        return mInstance;
    }

    public static void initAsync(@NonNull Application context) {
        if (mInstance == null) {
            Thread thread = new Thread(() -> getInstance(context));
            thread.start();
        }
    }

    /**
     * used for debugging without process termination
     */
    @SuppressWarnings("unused")
    public static void reset() {
        mInstance = null;
    }

    @NonNull
    private LocusField cField(@NonNull String taskerVar, @Nullable String locusResName, @NonNull Function<UpdateContainer, Object> updateContainerGetter) {
        String label = getLocusLabelByName(locusResName);

        if (StringUtils.isBlank(label)) {
            label = taskerVar.replace('_', ' ');
            label = WordUtils.capitalize(label);
        }
        return new LocusField(taskerVar, label, updateContainerGetter);

    }

    @Nullable
    private String getLocusLabelByName(@Nullable String locusResName) {
        if ((mLocusResources != null) && (locusResName != null)) {
            int id = mLocusResources.getIdentifier(locusResName, "string", mLocusVersion.getPackageName()); //NON-NLS
            if (id != 0) {
                return mLocusResources.getString(id);
            }
        }
        return null;
    }


    @SuppressWarnings({"HardCodedStringLiteral", "OverlyLongMethod"})
    @NonNull
    private ArrayList<LocusField> createUpdateContainerFields() {
        ArrayList<LocusField> list = new ArrayList<>();
        //this is a custom order
        list.add(cField("my_latitude", "latitude", u -> u.getLocMyLocation().latitude));
        list.add(cField("my_longitude", "longitude", u -> u.getLocMyLocation().longitude));
        list.add(cField("my_altitude", "altitude", u -> u.getLocMyLocation().getAltitude()));
        list.add(cField("my_accuracy", "accuracy", u -> u.getLocMyLocation().getAccuracy()));
        list.add(cField("my_gps_fix", "gps_fix", u -> u.getLocMyLocation().getTime()));
        list.add(cField("my_speed", "speed", u -> u.getLocMyLocation().getSpeedOptimal()));
        list.add(cField("sensor_hrm", "heart_rate", u -> u.getLocMyLocation().getSensorHeartRate()));
        list.add(cField("sensor_cadence", "cadence", u -> u.getLocMyLocation().getSensorCadence()));
        list.add(cField("sensor_speed", null, u -> u.getLocMyLocation().getSensorSpeed()));
        list.add(cField("sensor_strides", "strides_label", u -> u.getLocMyLocation().getSensorStrides()));
        list.add(cField("sensor_temperature", "temperature", u -> u.getLocMyLocation().getSensorTemperature()));
        list.add(cField("speed_vertical", null, UpdateContainer::getSpeedVertical));
        list.add(cField("slope", "slope", UpdateContainer::getSlope));
        list.add(cField("gps_sat_used", null, UpdateContainer::getGpsSatsUsed));
        list.add(cField("gps_sat_all", null, UpdateContainer::getGpsSatsAll));
        list.add(cField("declination", "declination", UpdateContainer::getDeclination));
        list.add(cField("heading", "heading", UpdateContainer::getOrientHeading));
        list.add(cField("course", "course", UpdateContainer::getOrientCourse));
        list.add(cField("roll", "roll", UpdateContainer::getOrientRoll));
        list.add(cField("pitch", "orientation_pitch", UpdateContainer::getOrientPitch));
        list.add(cField("rec_total_length", null, u -> u.getTrackRecStats().getTotalLength()));
        list.add(cField("rec_eleva_neg_length", null, u -> u.getTrackRecStats().getEleNegativeDistance()));
        list.add(cField("rec_eleva_pos_length", null, u -> u.getTrackRecStats().getElePositiveDistance()));
        list.add(cField("rec_eleva_neutral_length", null, u -> u.getTrackRecStats().getEleNeutralDistance()));
        list.add(cField("rec_eleva_neutral_height", null, u -> u.getTrackRecStats().getEleNeutralHeight()));
        list.add(cField("rec_eleva_downhill", "var_elevation_downhill", u -> u.getTrackRecStats().getEleNegativeHeight()));
        list.add(cField("rec_eleva_uphill", "var_elevation_uphill", u -> u.getTrackRecStats().getElePositiveHeight()));
        list.add(cField("rec_altitude_min", "min_altitude", u -> u.getTrackRecStats().getAltitudeMin()));
        list.add(cField("rec_altitude_max", "max_altitude", u -> u.getTrackRecStats().getAltitudeMax()));
        list.add(cField("rec_start_time", null, u -> u.getTrackRecStats().getStartTime()));
        list.add(cField("rec_stop_time", null, u -> u.getTrackRecStats().getStopTime()));
        list.add(cField("rec_time", null, u -> u.getTrackRecStats().getTotalTime()));
        list.add(cField("rec_time_move", null, u -> u.getTrackRecStats().getTotalTimeMove()));
        list.add(cField("rec_average_speed_total", "average_speed", u -> u.getTrackRecStats().getSpeedAverage(false)));
        list.add(cField("rec_average_speed_move", "average_moving_speed", u -> u.getTrackRecStats().getSpeedAverage(true)));
        list.add(cField("rec_point_count", "points_count", u -> u.getTrackRecStats().getNumOfPoints()));
        list.add(cField("rec_cadence_avg", "cadence_avg", u -> u.getTrackRecStats().getCadenceAverage()));
        list.add(cField("rec_cadence_max", "cadence_max", u -> u.getTrackRecStats().getCadenceMax()));
        list.add(cField("rec_energy_burned", "energy_burned", u -> u.getTrackRecStats().getEnergy()));
        list.add(cField("rec_hrm_avg", "heart_rate_avg", u -> u.getTrackRecStats().getHrmAverage()));
        list.add(cField("rec_hrm_max", "heart_rate_max", u -> u.getTrackRecStats().getHrmMax()));
        list.add(cField("rec_strides_count", null, u -> u.getTrackRecStats().getNumOfStrides()));
        list.add(cField("is_guide_enabled", null, UpdateContainer::isGuideEnabled));
        list.add(cField("is_new_zoom_level", null, UpdateContainer::isNewZoomLevel));
        list.add(cField("is_new_map_center", null, UpdateContainer::isNewMapCenter));
        list.add(cField("is_track_rec_recording", null, UpdateContainer::isTrackRecRecording));
        list.add(cField("is_track_rec_paused", null, UpdateContainer::isTrackRecPaused));
        list.add(cField("is_enabled_my_location", null, UpdateContainer::isEnabledMyLocation));
        list.add(cField("is_map_visible", null, UpdateContainer::isMapVisible));
        list.add(cField("map_zoom_level", null, UpdateContainer::getMapZoomLevel));
        list.add(cField("map_distance_to_gps", "distance_to_gps", u -> u.getLocMapCenter().distanceTo(u.getLocMyLocation())));
        list.add(cField("map_rotate_angle", null, UpdateContainer::getMapRotate));
        list.add(cField("map_bottom_right_lon", null, u -> u.getMapBottomRight().longitude));
        list.add(cField("map_bottom_right_lat", null, u -> u.getMapBottomRight().latitude));
        list.add(cField("map_top_left_lon", null, u -> u.getMapTopLeft().longitude));
        list.add(cField("map_top_left_lat", null, u -> u.getMapTopLeft().latitude));
        list.add(cField("map_center_lon", null, u -> u.getLocMapCenter().longitude));
        list.add(cField("map_center_lat", null, u -> u.getLocMapCenter().latitude));
        list.add(cField("active_live_track_id", null, UpdateContainer::getActiveLiveTrackId));
        list.add(cField("active_dashboard_id", null, UpdateContainer::getActiveDashboardId));
        list.add(cField("guide_target_lon", null, u -> u.getGuideTypeWaypoint().getTargetLoc().longitude));
        list.add(cField("guide_target_lat", null, u -> u.getGuideTypeWaypoint().getTargetLoc().latitude));
        list.add(cField(CALC_REMAIN_UPHILL_ELEVATION, null, new CalculateElevationToTarget()));

        //TODO Navigation points

        return list;
    }

    @NonNull
    private Map<String, LocusField> createUpdateContainerFieldMap() {
        Map<String, LocusField> updateContainerFieldMap = new HashMap<>();
        for (LocusField field : mUpdateContainerFields) {
            updateContainerFieldMap.put(field.mTaskerName, field);
        }

        return updateContainerFieldMap;
    }


    @NonNull
    private HashSet<String> createUpdateContainerTrackGuideKeys() {
        HashSet<String> mTrackGuideKeys = new HashSet<>();
        for (String key : mUpdateContainerFieldMap.keySet()) {
            if (key.startsWith("guide")) mTrackGuideKeys.add(key); //NON-NLS
        }
        return mTrackGuideKeys;
    }

    @NonNull
    private HashSet<String> createUpdateContainerTrackRecKeys() {
        HashSet<String> trackRecordingKeys = new HashSet<>();
        for (String key : mUpdateContainerFieldMap.keySet()) {
            if (key.startsWith("rec")) trackRecordingKeys.add(key); //NON-NLS
        }
        return trackRecordingKeys;
    }

    @Nullable
    public Track getLastSelectedTrack() {
        return mLastSelectedTrack;
    }

    public void setLastSelectedTrack(@Nullable Track lastSelectedTrack) {
        //enable button on null track and disable button otherwise
        LocusSetActiveTrack.setVisibility(mApplicationContext.getPackageManager(), lastSelectedTrack == null);

        mLastSelectedTrack = lastSelectedTrack;
        mRemainingTrackElevation = CalculateElevationToTarget.calculateRemainingElevation(mLastSelectedTrack);
    }

    @NonNull
    public UpdateContainer getUpdateContainer() throws RequiredVersionMissingException {
        long requestTime = System.currentTimeMillis();
        if (requestTime > mUpdateContainerExpiration) {
            mUpdateContainer = ActionTools.getDataUpdateContainer(mApplicationContext, mLocusVersion);
            mUpdateContainerExpiration = requestTime + UPDATE_CONTAINER_EXPIRATION; //don't care about 1 second offset for manual update requests
        } else {
            Log.d(TAG, "getUpdateContainer cache hit, time to expiration: " //NON-NLS
                    + (mUpdateContainerExpiration - requestTime));
        }

        return mUpdateContainer;
    }

    public static class MissingAppContextException extends Exception {

        private static final long serialVersionUID = -1817542570335055712L;

        MissingAppContextException() {
            super("Please start the Locus Tasker Plugin before you use it.");
        }
    }
}
