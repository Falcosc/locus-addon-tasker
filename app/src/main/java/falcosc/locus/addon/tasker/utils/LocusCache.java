package falcosc.locus.addon.tasker.utils;

import android.arch.core.util.Function;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.widget.Toast;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import java.util.*;

public class LocusCache {
    private static LocusCache instance;

    public final HashSet<String> trackRecordingKeys;
    public final Map<String, LocusField> updateContainerFieldMap;
    public final ArrayList<LocusField> updateContainerFields;
    public final LocusUtils.LocusVersion locusVersion;
    private final Resources locusResources;

    private LocusCache(Context context) {

        Toast.makeText(context, "Locus Addon Tasker Plugin Init", Toast.LENGTH_LONG).show();
        locusVersion = LocusUtils.getActiveVersion(context);

        Resources locusRes = null;
        try {
            locusRes = context.getPackageManager().getResourcesForApplication(locusVersion.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            //TODO
            e.printStackTrace();
        }
        locusResources = locusRes;
        updateContainerFields = createUpdateContainerFields();
        updateContainerFieldMap = createUpdateConfainerFieldMap();
        trackRecordingKeys = createUpdateContainerTrackRecKeys();
    }

    public static LocusCache getInstance(Context context) {
        if (instance == null) {
            instance = new LocusCache(context);
        }
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
        String label = null;
        if (locusResources != null) {
            int id = locusResources.getIdentifier(locusResName, "string", locusVersion.getPackageName());
            if (id != 0) {
                label = locusResources.getString(id);
            }
        }

        if (StringUtils.isBlank(label)) {
            label = taskerVar.replace('_', ' ');
            label = WordUtils.capitalize(label);
        }
        return new LocusField(taskerVar, label, updateContainerGetter);

    }

    private ArrayList<LocusField> createUpdateContainerFields() throws IllegalArgumentException {
        ArrayList<LocusField> f = new ArrayList<>();
        //this is a custom order
        f.add(cLocusField("my_latitude", "latitude", u -> String.valueOf(u.getLocMyLocation().latitude)));
        f.add(cLocusField("my_longitude", "longitude", u -> String.valueOf(u.getLocMyLocation().longitude)));
        f.add(cLocusField("my_altitude", "altitude", u -> String.valueOf(u.getLocMyLocation().getAltitude())));
        f.add(cLocusField("my_accuracy", "accuracy", u -> String.valueOf(u.getLocMyLocation().getAccuracy())));
        //TODO is this the last GPS fix?
        f.add(cLocusField("my_gps_fix", "gps_fix", u -> String.valueOf(u.getLocMyLocation().getTime())));
        f.add(cLocusField("my_speed", "speed", u -> String.valueOf(u.getLocMyLocation().getSpeedOptimal())));
        //TODO how to get real time sensors?
        f.add(cLocusField("sensor_hrm", "heart_rate", u -> String.valueOf(u.getLocMyLocation().getSensorHeartRate())));
        f.add(cLocusField("sensor_cadence", "cadence", u -> String.valueOf(u.getLocMyLocation().getSensorCadence())));
        f.add(cLocusField("sensor_speed", "", u -> String.valueOf(u.getLocMyLocation().getSensorSpeed())));
        f.add(cLocusField("sensor_strides", "strides_label", u -> String.valueOf(u.getLocMyLocation().getSensorStrides())));
        f.add(cLocusField("sensor_temperature", "temperature", u -> String.valueOf(u.getLocMyLocation().getSensorTemperature())));
        f.add(cLocusField("speed_vertical", "", u -> String.valueOf(u.getSpeedVertical())));
        f.add(cLocusField("slope", "slope", u -> String.valueOf(u.getSlope())));
        f.add(cLocusField("gps_sat_used", "", u -> String.valueOf(u.getGpsSatsUsed())));
        f.add(cLocusField("gps_sat_all", "", u -> String.valueOf(u.getGpsSatsAll())));
        f.add(cLocusField("declination", "declination", u -> String.valueOf(u.getDeclination())));
        f.add(cLocusField("heading", "heading", u -> String.valueOf(u.getOrientHeading())));
        f.add(cLocusField("course", "course", u -> String.valueOf(u.getOrientCourse())));
        f.add(cLocusField("roll", "roll", u -> String.valueOf(u.getOrientRoll())));
        f.add(cLocusField("pitch", "pitch", u -> String.valueOf(u.getOrientPitch())));
        f.add(cLocusField("rec_total_length", "", u -> String.valueOf(u.getTrackRecStats().getTotalLength())));
        f.add(cLocusField("rec_eleva_neg_length", "", u -> String.valueOf(u.getTrackRecStats().getEleNegativeDistance())));
        f.add(cLocusField("rec_eleva_pos_length", "", u -> String.valueOf(u.getTrackRecStats().getElePositiveDistance())));
        //what is getEleNeutralHeight?
        f.add(cLocusField("rec_eleva_downhill", "var_elevation_downhill", u -> String.valueOf(u.getTrackRecStats().getEleNegativeHeight())));
        f.add(cLocusField("rec_eleva_uphill", "var_elevation_uphill", u -> String.valueOf(u.getTrackRecStats().getElePositiveHeight())));
        f.add(cLocusField("rec_altitude_min", "min_altitude", u -> String.valueOf(u.getTrackRecStats().getAltitudeMin())));
        f.add(cLocusField("rec_altitude_max", "max_altitude", u -> String.valueOf(u.getTrackRecStats().getAltitudeMax())));
        f.add(cLocusField("rec_start_time", "", u -> String.valueOf(u.getTrackRecStats().getStartTime())));
        f.add(cLocusField("rec_stop_time", "", u -> String.valueOf(u.getTrackRecStats().getStopTime())));
        f.add(cLocusField("rec_time", "", u -> String.valueOf(u.getTrackRecStats().getTotalTime())));
        f.add(cLocusField("rec_time_move", "", u -> String.valueOf(u.getTrackRecStats().getTotalTimeMove())));
        f.add(cLocusField("rec_average_speed_total", "average_speed", u -> String.valueOf(u.getTrackRecStats().getSpeedAverage(false))));
        //Is wrong in locus dashboard element selection "Tempodurchschn.(Bewegung) (Bewegung)"
        f.add(cLocusField("rec_average_speed_move", "average_moving_speed", u -> String.valueOf(u.getTrackRecStats().getSpeedAverage(true))));
        f.add(cLocusField("rec_point_count", "points_count", u -> String.valueOf(u.getTrackRecStats().getNumOfPoints())));
        //TODO where is pace?
        f.add(cLocusField("rec_cadence_avg", "cadence_avg", u -> String.valueOf(u.getTrackRecStats().getCadenceAverage())));
        f.add(cLocusField("rec_cadence_max", "cadence_max", u -> String.valueOf(u.getTrackRecStats().getCadenceMax())));
        f.add(cLocusField("rec_energy_burned", "energy_burned", u -> String.valueOf(u.getTrackRecStats().getEnergy())));
        f.add(cLocusField("rec_hrm_avg", "heart_rate_avg", u -> String.valueOf(u.getTrackRecStats().getHrmAverage())));
        f.add(cLocusField("rec_hrm_max", "heart_rate_max", u -> String.valueOf(u.getTrackRecStats().getHrmMax())));
        f.add(cLocusField("rec_strides_count", "", u -> String.valueOf(u.getTrackRecStats().getNumOfStrides())));
        f.add(cLocusField("is_guide_enabled", "", u -> String.valueOf(u.isGuideEnabled())));
        //how does that work?
        f.add(cLocusField("is_new_zoom_level", "", u -> String.valueOf(u.isNewZoomLevel())));
        //how does that work?
        f.add(cLocusField("is_new_map_center", "", u -> String.valueOf(u.isNewMapCenter())));
        f.add(cLocusField("is_track_rec_recording", "", u -> String.valueOf(u.isTrackRecRecording())));
        f.add(cLocusField("is_track_rec_paused", "", u -> String.valueOf(u.isTrackRecPaused())));
        f.add(cLocusField("is_enabled_my_location", "", u -> String.valueOf(u.isEnabledMyLocation())));
        //how does that work? would be cool to know
        f.add(cLocusField("is_map_visible", "", u -> String.valueOf(u.isMapVisible())));
        f.add(cLocusField("map_zoom_level", "", u -> String.valueOf(u.getMapZoomLevel())));
        f.add(cLocusField("map_distance_to_gps", "distance_to_gps", u -> String.valueOf(u.getLocMapCenter().distanceTo(u.getLocMyLocation()))));
        f.add(cLocusField("map_rotate_angle", "", u -> String.valueOf(u.getMapRotate())));
        f.add(cLocusField("map_bottom_right_lon", "", u -> String.valueOf(u.getMapBottomRight().longitude)));
        f.add(cLocusField("map_bottom_right_lat", "", u -> String.valueOf(u.getMapBottomRight().latitude)));
        f.add(cLocusField("map_top_left_lon", "", u -> String.valueOf(u.getMapTopLeft().longitude)));
        f.add(cLocusField("map_top_left_lat", "", u -> String.valueOf(u.getMapTopLeft().latitude)));
        f.add(cLocusField("map_center_lon", "", u -> String.valueOf(u.getLocMapCenter().longitude)));
        f.add(cLocusField("map_center_lat", "", u -> String.valueOf(u.getLocMapCenter().latitude)));

        //TODO Navigration points

        return f;
    }

    private Map<String, LocusField> createUpdateConfainerFieldMap() {
        Map<String, LocusField> updateContainerFieldMap = new HashMap<>();
        for (LocusField field : updateContainerFields) {
            updateContainerFieldMap.put(field.taskerName, field);
        }

        return updateContainerFieldMap;
    }

    private HashSet<String> createUpdateContainerTrackRecKeys() {
        HashSet<String> trackRecordingKeys = new HashSet<>();
        for (String key : updateContainerFieldMap.keySet()) {
            if (key.startsWith("rec")) trackRecordingKeys.add(key);
        }
        return trackRecordingKeys;
    }
}
