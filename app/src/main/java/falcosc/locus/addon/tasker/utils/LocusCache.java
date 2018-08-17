package falcosc.locus.addon.tasker.utils;

import android.arch.core.util.Function;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusUtils;
import locus.api.objects.extra.Track;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import java.util.*;

public class LocusCache {
    private static final String TAG = "LocusCache";
    public static final String CALC_REMAIN_UPHILL_ELEVATION = "calc_remain_uphill_elevation";
    private static final Object syncObj = new Object();
    private static LocusCache instance;

    public final HashSet<String> trackRecordingKeys;
    public final Map<String, LocusField> updateContainerFieldMap;
    public final ArrayList<LocusField> updateContainerFields;
    public final LocusUtils.LocusVersion locusVersion;
    private final Resources locusResources;
    public final String navigationTrackName;

    //selected track fields
    private Track lastSelectedTrack;
    public int[] remainingTrackElevation;
    public int lastIndexOnRemainingTrack;

    @SuppressWarnings("HardCodedStringLiteral")
    private LocusCache(Context context) {
        Log.d(TAG, "init Locus cache");

        locusVersion = LocusUtils.getActiveVersion(context);
        Log.d(TAG, "Locus version: " + locusVersion);

        Resources locusRes = null;
        try {
            locusRes = context.getPackageManager().getResourcesForApplication(locusVersion.getPackageName());
            Log.d(TAG, "Found Locus resources");
        } catch (Exception e) {
            Log.d(TAG, "Missing Locus resources", e);
        }
        locusResources = locusRes;

        updateContainerFields = createUpdateContainerFields();
        Log.d(TAG, "Locus fields created: " + updateContainerFields.size());
        updateContainerFieldMap = createUpdateContainerFieldMap();
        trackRecordingKeys = createUpdateContainerTrackRecKeys();
        Log.d(TAG, "Locus field keys mapped - recording keys: " + trackRecordingKeys.size());


        navigationTrackName = getLocusLabelByName("navigation");
        Log.d(TAG, "Locus navigation track name: " + navigationTrackName);
    }

    public static LocusCache getInstance(Context context) {

        if (instance == null) {
            synchronized (syncObj) {
                if (instance == null) {
                    instance = new LocusCache(context);
                }
            }
        }
        return instance;
    }

    public static void initAsync(Context context) {
        if (instance == null) {
            Thread thread = new Thread(() -> getInstance(context));
            thread.start();
        }
    }

    public static LocusCache getInstanceNullable() {
        return instance;
    }

    /**
     * used for debugging without process termination
     */
    @SuppressWarnings("unused")
    public static void reset() {
        instance = null;
    }

    private LocusField cLocusField(String taskerVar, String locusResName, Function<UpdateContainer, String> updateContainerGetter) throws IllegalArgumentException {
        String label = getLocusLabelByName(locusResName);

        if (StringUtils.isBlank(label)) {
            label = taskerVar.replace('_', ' ');
            label = WordUtils.capitalize(label);
        }
        return new LocusField(taskerVar, label, updateContainerGetter);

    }

    private String getLocusLabelByName(String locusResName) {
        if (locusResources != null && locusResName != null) {
            int id = locusResources.getIdentifier(locusResName, "string", locusVersion.getPackageName()); //NON-NLS
            if (id != 0) {
                return locusResources.getString(id);
            }
        }
        return null;
    }


    @SuppressWarnings("HardCodedStringLiteral")
    private ArrayList<LocusField> createUpdateContainerFields() throws IllegalArgumentException {
        ArrayList<LocusField> f = new ArrayList<>();
        //this is a custom order
        f.add(cLocusField("my_latitude", "latitude", u -> String.valueOf(u.getLocMyLocation().latitude)));
        f.add(cLocusField("my_longitude", "longitude", u -> String.valueOf(u.getLocMyLocation().longitude)));
        f.add(cLocusField("my_altitude", "altitude", u -> String.valueOf(u.getLocMyLocation().getAltitude())));
        f.add(cLocusField("my_accuracy", "accuracy", u -> String.valueOf(u.getLocMyLocation().getAccuracy())));
        f.add(cLocusField("my_gps_fix", "gps_fix", u -> String.valueOf(u.getLocMyLocation().getTime())));
        f.add(cLocusField("my_speed", "speed", u -> String.valueOf(u.getLocMyLocation().getSpeedOptimal())));
        f.add(cLocusField("sensor_hrm", "heart_rate", u -> String.valueOf(u.getLocMyLocation().getSensorHeartRate())));
        f.add(cLocusField("sensor_cadence", "cadence", u -> String.valueOf(u.getLocMyLocation().getSensorCadence())));
        f.add(cLocusField("sensor_speed", null, u -> String.valueOf(u.getLocMyLocation().getSensorSpeed())));
        f.add(cLocusField("sensor_strides", "strides_label", u -> String.valueOf(u.getLocMyLocation().getSensorStrides())));
        f.add(cLocusField("sensor_temperature", "temperature", u -> String.valueOf(u.getLocMyLocation().getSensorTemperature())));
        f.add(cLocusField("speed_vertical", null, u -> String.valueOf(u.getSpeedVertical())));
        f.add(cLocusField("slope", "slope", u -> String.valueOf(u.getSlope())));
        f.add(cLocusField("gps_sat_used", null, u -> String.valueOf(u.getGpsSatsUsed())));
        f.add(cLocusField("gps_sat_all", null, u -> String.valueOf(u.getGpsSatsAll())));
        f.add(cLocusField("declination", "declination", u -> String.valueOf(u.getDeclination())));
        f.add(cLocusField("heading", "heading", u -> String.valueOf(u.getOrientHeading())));
        f.add(cLocusField("course", "course", u -> String.valueOf(u.getOrientCourse())));
        f.add(cLocusField("roll", "roll", u -> String.valueOf(u.getOrientRoll())));
        f.add(cLocusField("pitch", "orientation_pitch", u -> String.valueOf(u.getOrientPitch())));
        f.add(cLocusField("rec_total_length", null, u -> String.valueOf(u.getTrackRecStats().getTotalLength())));
        f.add(cLocusField("rec_eleva_neg_length", null, u -> String.valueOf(u.getTrackRecStats().getEleNegativeDistance())));
        f.add(cLocusField("rec_eleva_pos_length", null, u -> String.valueOf(u.getTrackRecStats().getElePositiveDistance())));
        f.add(cLocusField("rec_eleva_neutral_length", null, u -> String.valueOf(u.getTrackRecStats().getEleNeutralDistance())));
        f.add(cLocusField("rec_eleva_neutral_height", null, u -> String.valueOf(u.getTrackRecStats().getEleNeutralHeight())));
        f.add(cLocusField("rec_eleva_downhill", "var_elevation_downhill", u -> String.valueOf(u.getTrackRecStats().getEleNegativeHeight())));
        f.add(cLocusField("rec_eleva_uphill", "var_elevation_uphill", u -> String.valueOf(u.getTrackRecStats().getElePositiveHeight())));
        f.add(cLocusField("rec_altitude_min", "min_altitude", u -> String.valueOf(u.getTrackRecStats().getAltitudeMin())));
        f.add(cLocusField("rec_altitude_max", "max_altitude", u -> String.valueOf(u.getTrackRecStats().getAltitudeMax())));
        f.add(cLocusField("rec_start_time", null, u -> String.valueOf(u.getTrackRecStats().getStartTime())));
        f.add(cLocusField("rec_stop_time", null, u -> String.valueOf(u.getTrackRecStats().getStopTime())));
        f.add(cLocusField("rec_time", null, u -> String.valueOf(u.getTrackRecStats().getTotalTime())));
        f.add(cLocusField("rec_time_move", null, u -> String.valueOf(u.getTrackRecStats().getTotalTimeMove())));
        f.add(cLocusField("rec_average_speed_total", "average_speed", u -> String.valueOf(u.getTrackRecStats().getSpeedAverage(false))));
        f.add(cLocusField("rec_average_speed_move", "average_moving_speed", u -> String.valueOf(u.getTrackRecStats().getSpeedAverage(true))));
        f.add(cLocusField("rec_point_count", "points_count", u -> String.valueOf(u.getTrackRecStats().getNumOfPoints())));
        f.add(cLocusField("rec_cadence_avg", "cadence_avg", u -> String.valueOf(u.getTrackRecStats().getCadenceAverage())));
        f.add(cLocusField("rec_cadence_max", "cadence_max", u -> String.valueOf(u.getTrackRecStats().getCadenceMax())));
        f.add(cLocusField("rec_energy_burned", "energy_burned", u -> String.valueOf(u.getTrackRecStats().getEnergy())));
        f.add(cLocusField("rec_hrm_avg", "heart_rate_avg", u -> String.valueOf(u.getTrackRecStats().getHrmAverage())));
        f.add(cLocusField("rec_hrm_max", "heart_rate_max", u -> String.valueOf(u.getTrackRecStats().getHrmMax())));
        f.add(cLocusField("rec_strides_count", null, u -> String.valueOf(u.getTrackRecStats().getNumOfStrides())));
        f.add(cLocusField("is_guide_enabled", null, u -> String.valueOf(u.isGuideEnabled())));
        f.add(cLocusField("is_new_zoom_level", null, u -> String.valueOf(u.isNewZoomLevel())));
        f.add(cLocusField("is_new_map_center", null, u -> String.valueOf(u.isNewMapCenter())));
        f.add(cLocusField("is_track_rec_recording", null, u -> String.valueOf(u.isTrackRecRecording())));
        f.add(cLocusField("is_track_rec_paused", null, u -> String.valueOf(u.isTrackRecPaused())));
        f.add(cLocusField("is_enabled_my_location", null, u -> String.valueOf(u.isEnabledMyLocation())));
        f.add(cLocusField("is_map_visible", null, u -> String.valueOf(u.isMapVisible())));
        f.add(cLocusField("map_zoom_level", null, u -> String.valueOf(u.getMapZoomLevel())));
        f.add(cLocusField("map_distance_to_gps", "distance_to_gps", u -> String.valueOf(u.getLocMapCenter().distanceTo(u.getLocMyLocation()))));
        f.add(cLocusField("map_rotate_angle", null, u -> String.valueOf(u.getMapRotate())));
        f.add(cLocusField("map_bottom_right_lon", null, u -> String.valueOf(u.getMapBottomRight().longitude)));
        f.add(cLocusField("map_bottom_right_lat", null, u -> String.valueOf(u.getMapBottomRight().latitude)));
        f.add(cLocusField("map_top_left_lon", null, u -> String.valueOf(u.getMapTopLeft().longitude)));
        f.add(cLocusField("map_top_left_lat", null, u -> String.valueOf(u.getMapTopLeft().latitude)));
        f.add(cLocusField("map_center_lon", null, u -> String.valueOf(u.getLocMapCenter().longitude)));
        f.add(cLocusField("map_center_lat", null, u -> String.valueOf(u.getLocMapCenter().latitude)));
        f.add(cLocusField("active_live_track_id", null, u -> String.valueOf(u.getActiveLiveTrackId())));
        f.add(cLocusField("active_dashboard_id", null, u -> String.valueOf(u.getActiveDashboardId())));
        f.add(cLocusField(CALC_REMAIN_UPHILL_ELEVATION, null, new CalculateElevationToTarget()));

        //TODO Navigation points

        return f;
    }

    private Map<String, LocusField> createUpdateContainerFieldMap() {
        Map<String, LocusField> updateContainerFieldMap = new HashMap<>();
        for (LocusField field : updateContainerFields) {
            updateContainerFieldMap.put(field.taskerName, field);
        }

        return updateContainerFieldMap;
    }

    private HashSet<String> createUpdateContainerTrackRecKeys() {
        HashSet<String> trackRecordingKeys = new HashSet<>();
        for (String key : updateContainerFieldMap.keySet()) {
            if (key.startsWith("rec")) trackRecordingKeys.add(key); //NON-NLS
        }
        return trackRecordingKeys;
    }

    public Track getLastSelectedTrack() {
        return lastSelectedTrack;
    }

    public void setLastSelectedTrack(Track lastSelectedTrack) {
        this.lastSelectedTrack = lastSelectedTrack;
        remainingTrackElevation = CalculateElevationToTarget.calculateRemainingElevation(lastSelectedTrack);
    }
}
