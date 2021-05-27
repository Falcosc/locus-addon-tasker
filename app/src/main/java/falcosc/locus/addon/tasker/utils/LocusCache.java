package falcosc.locus.addon.tasker.utils;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import org.apache.commons.text.WordUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import falcosc.locus.addon.tasker.BuildConfig;
import falcosc.locus.addon.tasker.RequiredDataMissingException;
import falcosc.locus.addon.tasker.reminder.VersionSelectReminder;
import locus.api.android.ActionBasics;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.objects.LocusVersion;
import locus.api.android.objects.VersionCode;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.geoData.Track;

public final class LocusCache {
    private static final String TAG = "LocusCache"; //NON-NLS
    private static final Object mSyncObj = new Object();
    private static final long UPDATE_CONTAINER_EXPIRATION = 950L;
    private static final int TASKER_FIELD_LABEL_SIZE = 80;

    private static LocusCache mInstance;

    private final Application mApplicationContext;

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Application getApplicationContext() {
        return mApplicationContext;
    }

    public final Set<String> mTrackRecordingKeys;
    public final Set<String> mTrackGuideKeys;
    public final Set<String> mLocationProgressKeys;
    public final Map<String, ExtUpdateContainerGetter> mExtUpdateContainerFieldMap;
    public final ArrayList<TaskerField> mUpdateContainerFields;
    public LocusVersion mLocusVersion;
    private final Resources mLocusResources;

    //selected track fields
    private Track mLastSelectedTrack;
    @SuppressWarnings("InstanceVariableOfConcreteClass")
    NavigationProgress.Point[] mRemainingTrackElevation;
    int mLastIndexOnRemainingTrack;

    //empty update container to avoid null checks because result is normally never null
    @SuppressWarnings("InstanceVariableOfConcreteClass")
    private ExtUpdateContainer mExtUpdateContainer = new ExtUpdateContainer(new UpdateContainer());
    private long mUpdateContainerExpiration;

    //reminders
    public final VersionSelectReminder versionSelectReminder;

    @SuppressWarnings("HardCodedStringLiteral")
    private LocusCache(Application context) {
        Log.d(TAG, "init Locus cache");
        Log.d(TAG, BuildConfig.VERSION_NAME);

        mApplicationContext = context;

        mLocusVersion = LocusUtils.INSTANCE.getActiveVersion(context, VersionCode.UPDATE_01);
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
        mUpdateContainerFields.addAll(createMapFields());

        ArrayList<TaskerField> trackRecStatsFields = createTrackRecStatsFields();
        mTrackRecordingKeys = getLocusFieldKeys(trackRecStatsFields);
        mUpdateContainerFields.addAll(trackRecStatsFields);

        ArrayList<TaskerField> guideFields = createGuideFields();
        mTrackGuideKeys = getLocusFieldKeys(guideFields);
        mUpdateContainerFields.addAll(guideFields);

        ArrayList<TaskerField> navigationProgressFields = createNavigationProgressFields();
        mLocationProgressKeys = getLocusFieldKeys(navigationProgressFields);
        mUpdateContainerFields.addAll(navigationProgressFields);

        mExtUpdateContainerFieldMap = createExtUpdateContainerFieldMap();

        Log.d(TAG, "Locus fields created: " + mUpdateContainerFields.size());
        Log.d(TAG, "Locus Field keys mapped - recording keys: " + mTrackRecordingKeys.size());
        Log.d(TAG, "Locus Field keys mapped - guiding keys: " + mTrackGuideKeys.size());

        versionSelectReminder = new VersionSelectReminder(mApplicationContext);
    }

    @NonNull
    public LocusVersion requireLocusVersion() throws RequiredDataMissingException {
        if (mLocusVersion == null) {
            throw new RequiredDataMissingException("Locus Maps need to be installed");
        }
        return mLocusVersion;
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
    private UpdateContainerField cField(@NonNull String taskerVar, @Nullable String locusResName, @NonNull Function<UpdateContainer, Object> updateContainerGetter) {
        String label = getLocusLabelByName(locusResName);
        return new UpdateContainerField(taskerVar, label, updateContainerGetter);
    }

    @NonNull
    private UpdateContainerField cField(@NonNull String taskerVar, @NonNull String[] locusResNames, @NonNull Function<UpdateContainer, Object> updateContainerGetter) {
        StringBuilder label = new StringBuilder(TASKER_FIELD_LABEL_SIZE);
        for(String locusResName : locusResNames) {
            String resolvedLabel = getLocusLabelByName(locusResName);
            if(resolvedLabel != null) {
               label.append(resolvedLabel).append(" ");
            }
        }
        label = new StringBuilder(label.toString().trim());
        return new UpdateContainerField(taskerVar, label.toString(), updateContainerGetter);
    }

    @NonNull
    private static ExtendedUpdateContainerField extField(@NonNull String taskerVar, @NonNull Function<ExtUpdateContainer, Object> extUpdateContainerGetter) {
        String label = taskerVar.replace('_', ' ');
        label = WordUtils.capitalize(label);
        return new ExtendedUpdateContainerField(taskerVar, label, extUpdateContainerGetter);

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


    @SuppressWarnings("HardCodedStringLiteral")
    @NonNull
    private ArrayList<TaskerField> createUpdateContainerFields() {
        ArrayList<TaskerField> list = new ArrayList<>();
        //this is a custom order
        list.add(cField("my_latitude", "latitude", u -> u.getLocMyLocation().getLatitude()));
        list.add(cField("my_longitude", "longitude", u -> u.getLocMyLocation().getLongitude()));
        list.add(cField("my_altitude", "altitude", u -> u.getLocMyLocation().getAltitude()));
        list.add(cField("my_accuracy", "accuracy", u -> u.getLocMyLocation().getAccuracy()));
        list.add(cField("my_gps_fix", "gps_fix", u -> u.getLocMyLocation().getTime()));
        list.add(cField("my_speed", "speed", u -> u.getLocMyLocation().getSpeedOptimal()));
        list.add(cField("sensor_hrm", "heart_rate", u -> u.getLocMyLocation().getSensorHeartRate()));
        list.add(cField("sensor_cadence", "cadence", u -> u.getLocMyLocation().getSensorCadence()));
        list.add(cField("sensor_speed", "bicycle_speed", u -> u.getLocMyLocation().getSensorSpeed()));
        list.add(cField("sensor_strides", "strides_label", u -> u.getLocMyLocation().getSensorStrides()));
        list.add(cField("sensor_temperature", "temperature", u -> u.getLocMyLocation().getSensorTemperature()));
        list.add(cField("speed_vertical", new String[]{"speed", "vertical"}, UpdateContainer::getSpeedVertical));
        list.add(cField("slope", "slope", UpdateContainer::getSlope));
        list.add(cField("gps_sat_used", "satellites_used", UpdateContainer::getGpsSatsUsed));
        list.add(cField("gps_sat_all", "satellites_all", UpdateContainer::getGpsSatsAll));
        list.add(cField("declination", "declination", UpdateContainer::getDeclination));
        list.add(cField("heading", "heading", UpdateContainer::getOrientHeading));
        list.add(cField("course", "course", UpdateContainer::getOrientCourse));
        list.add(cField("roll", "roll", UpdateContainer::getOrientRoll));
        list.add(cField("pitch", "orientation_pitch", UpdateContainer::getOrientPitch));
        list.add(cField("is_guide_enabled", "guide_on", UpdateContainer::isGuideEnabled));
        list.add(cField("is_track_rec_recording", "recording", UpdateContainer::isTrackRecRecording));
        list.add(cField("is_track_rec_paused", new String[]{"track_record","paused"}, UpdateContainer::isTrackRecPaused));
        list.add(cField("is_enabled_my_location", "gps_on", UpdateContainer::isEnabledMyLocation));
        list.add(cField("is_map_visible", new String[]{"map", "visible"}, UpdateContainer::isMapVisible));
        list.add(cField("active_live_track_id", "", UpdateContainer::getActiveLiveTrackId));
        list.add(cField("active_dashboard_id", "", UpdateContainer::getActiveDashboardId));

        return list;
    }

    @SuppressWarnings({"HardCodedStringLiteral", "ConstantConditions"}) //don't make null because if map data is missing locus isn't active
    @NonNull
    private ArrayList<TaskerField> createMapFields() {
        ArrayList<TaskerField> list = new ArrayList<>();
        list.add(cField("map_zoom_level", "", UpdateContainer::getMapZoomLevel));
        list.add(cField("map_distance_to_gps", "distance_to_gps", u -> u.getLocMapCenter().distanceTo(u.getLocMyLocation())));
        list.add(cField("map_rotate_angle", "rotate_map_angle", UpdateContainer::getMapRotate));
        //no null checks needed, map locations are always available
        list.add(cField("map_bottom_right_lon", "", u -> u.getMapBottomRight().getLongitude()));
        list.add(cField("map_bottom_right_lat", "", u -> u.getMapBottomRight().getLatitude()));
        list.add(cField("map_top_left_lon", "", u -> u.getMapTopLeft().getLongitude()));
        list.add(cField("map_top_left_lat", "", u -> u.getMapTopLeft().getLatitude()));
        list.add(cField("map_center_lon", "", u -> u.getLocMapCenter().getLongitude()));
        list.add(cField("map_center_lat", "", u -> u.getLocMapCenter().getLatitude()));

        return list;
    }

    @SuppressWarnings({"HardCodedStringLiteral", "ConstantConditions"}) //don't make null checks here, we do it based on key
    @NonNull
    private ArrayList<TaskerField> createTrackRecStatsFields() {
        ArrayList<TaskerField> list = new ArrayList<>();
        //this is a custom order
        list.add(cField("rec_total_length", "distance", u -> u.getTrackRecStats().getTotalLength()));
        list.add(cField("rec_eleva_neg_length", new String[]{"distance", "downhill"}, u -> u.getTrackRecStats().getEleNegativeDistance()));
        list.add(cField("rec_eleva_pos_length", new String[]{"distance", "uphill"}, u -> u.getTrackRecStats().getElePositiveDistance()));
        list.add(cField("rec_eleva_neutral_length", "", u -> u.getTrackRecStats().getEleNeutralDistance()));
        list.add(cField("rec_eleva_neutral_height", "", u -> u.getTrackRecStats().getEleNeutralHeight()));
        list.add(cField("rec_eleva_downhill", "var_elevation_downhill", u -> u.getTrackRecStats().getEleNegativeHeight()));
        list.add(cField("rec_eleva_uphill", "var_elevation_uphill", u -> u.getTrackRecStats().getElePositiveHeight()));
        list.add(cField("rec_altitude_min", "min_altitude", u -> u.getTrackRecStats().getAltitudeMin()));
        list.add(cField("rec_altitude_max", "max_altitude", u -> u.getTrackRecStats().getAltitudeMax()));
        list.add(cField("rec_start_time", "", u -> u.getTrackRecStats().getStartTime()));
        list.add(cField("rec_stop_time", "", u -> u.getTrackRecStats().getStopTime()));
        list.add(cField("rec_time", "track_time", u -> u.getTrackRecStats().getTotalTime()));
        list.add(cField("rec_time_move", "moving_time", u -> u.getTrackRecStats().getTotalTimeMove()));
        list.add(cField("rec_average_speed_total", "average_speed", u -> u.getTrackRecStats().getSpeedAverage(false)));
        list.add(cField("rec_average_speed_move", "average_moving_speed", u -> u.getTrackRecStats().getSpeedAverage(true)));
        list.add(cField("rec_point_count", "points_count", u -> u.getTrackRecStats().getNumOfPoints()));
        list.add(cField("rec_cadence_avg", "cadence_avg", u -> u.getTrackRecStats().getCadenceAverage()));
        list.add(cField("rec_cadence_max", "cadence_max", u -> u.getTrackRecStats().getCadenceMax()));
        list.add(cField("rec_energy_burned", "energy_burned", u -> u.getTrackRecStats().getEnergy()));
        list.add(cField("rec_hrm_avg", "heart_rate_avg", u -> u.getTrackRecStats().getHeartRateAverage()));
        list.add(cField("rec_hrm_max", "heart_rate_max", u -> u.getTrackRecStats().getHeartRateMax()));
        list.add(cField("rec_strides_count", "strides_label", u -> u.getTrackRecStats().getNumOfStrides()));

        return list;
    }

    @SuppressWarnings({"HardCodedStringLiteral", "ConstantConditions"}) //don't make null checks here, we do it based on key
    @NonNull
    private ArrayList<TaskerField> createGuideFields() {
        ArrayList<TaskerField> list = new ArrayList<>();
        //this is a custom order
        list.add(cField("guide_target_lon", "", u -> u.getGuideWptLoc().getLongitude()));
        list.add(cField("guide_target_lat", "", u -> u.getGuideWptLoc().getLatitude()));
        list.add(cField("guide_target_angle", "", UpdateContainer::getGuideWptAngle));
        list.add(cField("guide_target_azimuth", "", UpdateContainer::getGuideWptAzim));
        list.add(cField("guide_target_dist", "", UpdateContainer::getGuideWptDist));
        list.add(cField("guide_target_name", "", UpdateContainer::getGuideWptName));
        list.add(cField("guide_target_time", "", UpdateContainer::getGuideWptTime));
        list.add(cField("guide_dist_from_start", "distance_from_start", UpdateContainer::getGuideDistFromStart));
        list.add(cField("guide_dist_to_finish", "distance_to_target", UpdateContainer::getGuideDistToFinish));
        list.add(cField("guide_target_id", "", UpdateContainer::getGuideTargetId));
        list.add(cField("guide_time_to_finish", "time_to_target", UpdateContainer::getGuideTimeToFinish));
        list.add(cField("guide_type", "", UpdateContainer::getGuideType));
        list.add(cField("guide_valid", "", UpdateContainer::getGuideValid));
        list.add(cField("guide_navpoint1_action", "", UpdateContainer::getGuideNavPoint1Action));
        list.add(cField("guide_navpoint1_dist", "", UpdateContainer::getGuideNavPoint1Dist));
        list.add(cField("guide_navpoint1_extra", "", UpdateContainer::getGuideNavPoint1Extra));
        list.add(cField("guide_navpoint1_lon", "", u -> u.getGuideNavPoint1Loc().getLongitude()));
        list.add(cField("guide_navpoint1_lat", "", u -> u.getGuideNavPoint1Loc().getLatitude()));
        list.add(cField("guide_navpoint1_name", "", UpdateContainer::getGuideNavPoint1Name));
        list.add(cField("guide_navpoint1_time", "", UpdateContainer::getGuideNavPoint1Time));
        list.add(cField("guide_navpoint2_action", "", UpdateContainer::getGuideNavPoint2Action));
        list.add(cField("guide_navpoint2_dist", "", UpdateContainer::getGuideNavPoint2Dist));
        list.add(cField("guide_navpoint2_extra", "", UpdateContainer::getGuideNavPoint2Extra));
        list.add(cField("guide_navpoint2_lon", "", u -> u.getGuideNavPoint2Loc().getLongitude()));
        list.add(cField("guide_navpoint2_lat", "", u -> u.getGuideNavPoint2Loc().getLatitude()));
        list.add(cField("guide_navpoint2_name", "", UpdateContainer::getGuideNavPoint2Name));
        list.add(cField("guide_navpoint2_time", "", UpdateContainer::getGuideNavPoint2Time));

        return list;
    }

    @SuppressWarnings("HardCodedStringLiteral")
    @NonNull
    private static ArrayList<TaskerField> createNavigationProgressFields() {
        ArrayList<TaskerField> list = new ArrayList<>();
        list.add(extField("calc_remain_uphill_elevation", u -> u.getNavigationProgress().getRemainingUphill()));
        list.add(extField("calc_remain_downhill_elevation", u -> u.getNavigationProgress().getRemainingDownhill()));
        list.add(extField("navigation_point_index", u -> u.getNavigationProgress().pointIndex));
        list.add(extField("navigation_track_name", u -> u.getNavigationProgress().trackName));

        return list;
    }

    @NonNull
    private Map<String, ExtUpdateContainerGetter> createExtUpdateContainerFieldMap() {
        Map<String, ExtUpdateContainerGetter> updateContainerFieldMap = new HashMap<>();
        for (TaskerField field : mUpdateContainerFields) {
            //do cast only once
            updateContainerFieldMap.put(field.mTaskerName, (ExtUpdateContainerGetter) field);
        }

        return updateContainerFieldMap;
    }

    @NonNull
    private static Set<String> getLocusFieldKeys(List<TaskerField> fields) {
        Set<String> keys = new HashSet<>();
        for (TaskerField field : fields) {
            keys.add(field.mTaskerName);
        }
        return keys;
    }

    @Nullable
    Track getLastSelectedTrack() {
        return mLastSelectedTrack;
    }

    void setLastSelectedTrack(@Nullable Track lastSelectedTrack) {
        mLastSelectedTrack = lastSelectedTrack;
        mRemainingTrackElevation = NavigationProgress.calculateRemainingElevation(mLastSelectedTrack);
    }

    @NonNull
    public ExtUpdateContainer getUpdateContainer() throws RequiredVersionMissingException, RequiredDataMissingException {
        long requestTime = System.currentTimeMillis();
        if (requestTime > mUpdateContainerExpiration) {
            UpdateContainer container = ActionBasics.INSTANCE.getUpdateContainer(mApplicationContext, requireLocusVersion());
            if (container != null) {
                mExtUpdateContainer = new ExtUpdateContainer(container);
                mUpdateContainerExpiration = requestTime + UPDATE_CONTAINER_EXPIRATION; //don't care about 1 second offset for manual update requests
            }
        } else {
            Log.d(TAG, "getUpdateContainer cache hit, time to expiration: " //NON-NLS
                    + (mUpdateContainerExpiration - requestTime));
        }

        return mExtUpdateContainer;
    }

    public static class MissingAppContextException extends Exception {

        private static final long serialVersionUID = -1817542570335055712L;

        MissingAppContextException() {
            super("Please start the Locus Tasker Plugin before you use it.");
        }
    }
}
