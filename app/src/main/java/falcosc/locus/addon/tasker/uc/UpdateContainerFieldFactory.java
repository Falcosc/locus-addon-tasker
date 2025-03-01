package falcosc.locus.addon.tasker.uc;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.text.TextUtils;

import com.asamm.logger.Logger;

import org.apache.commons.text.WordUtils;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import falcosc.locus.addon.tasker.RequiredDataMissingException;
import falcosc.locus.addon.tasker.utils.TaskerField;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.objects.LocusVersion;

@SuppressWarnings({"OverlyLongMethod", "HardCodedStringLiteral"}) //because it is a factory for API methods
public class UpdateContainerFieldFactory {

    private static final int TASKER_FIELD_LABEL_SIZE = 80;
    private final Resources mLocusResources;
    private final LocusVersion mLocusVersion;

    public UpdateContainerFieldFactory(@Nullable Resources locusResources, @Nullable LocusVersion locusVersion) {
        mLocusResources = locusResources;
        mLocusVersion = locusVersion;
    }

    @Nullable
    private String getLocusLabelByName(@Nullable String locusResName) {
        if ((mLocusResources == null) || (mLocusVersion == null) || TextUtils.isEmpty(locusResName)) {
            return null;
        }

        @SuppressLint("DiscouragedApi")  //relfection is ok because we don't know if the other app has this translation
        int id = mLocusResources.getIdentifier(locusResName, "string", mLocusVersion.getPackageName()); //NON-NLS
        if (id != 0) {
            return mLocusResources.getString(id);
        }

        return locusResName;
    }

    @NonNull
    private UpdateContainerField cField(@NonNull String taskerVar, @Nullable String locusResName, @NonNull Function<UpdateContainer, Object> updateContainerGetter) {
        String label = getLocusLabelByName(locusResName);
        return new UpdateContainerField(taskerVar, label, updateContainerGetter);
    }

    @NonNull
    private UpdateContainerField cField(@NonNull String taskerVar, @NonNull String[] locusResNames, @NonNull Function<UpdateContainer, Object> updateContainerGetter) {
        StringBuilder label = new StringBuilder(TASKER_FIELD_LABEL_SIZE);
        for (String locusResName : locusResNames) {
            String resolvedLabel = getLocusLabelByName(locusResName);
            if (resolvedLabel != null) {
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

    @NonNull
    public ArrayList<TaskerField> createUpdateContainerFields() {
        ArrayList<TaskerField> list = new ArrayList<>();
        //most locus resource keys are documented in https://crowdin.com/editor/menion/3292/en-de
        //this is a custom order
        list.add(cField("my_latitude", "latitude", u -> u.getLocMyLocation().getLatitude()));
        list.add(cField("my_longitude", "longitude", u -> u.getLocMyLocation().getLongitude()));
        list.add(cField("my_altitude", "altitude", u -> u.getLocMyLocation().getAltitude()));
        list.add(cField("my_latitude_original", new String[]{"latitude", "original"}, u -> u.getLocMyLocation().getLatitudeOriginal()));
        list.add(cField("my_longitude_original", new String[]{"longitude", "original"}, u -> u.getLocMyLocation().getLongitudeOriginal()));
        list.add(cField("my_altitude_original", new String[]{"altitude", "original"}, u -> u.getLocMyLocation().getAltitudeOriginal()));
        list.add(cField("my_accuracy_hor", "gnss_accuracy_hor", u -> u.getLocMyLocation().getAccuracyHor()));
        list.add(cField("my_accuracy_ver", "gnss_accuracy_ver", u -> u.getLocMyLocation().getAccuracyVer()));
        list.add(cField("my_gps_fix", "gps_fix", u -> u.getLocMyLocation().getTime()));
        list.add(cField("my_speed", "speed", u -> u.getLocMyLocation().getSpeed()));
        list.add(cField("my_bearing", "bearing", u -> u.getLocMyLocation().getBearing()));
        list.add(cField("my_loc_provider", "", u -> u.getLocMyLocation().getProvider()));
        list.add(cField("sensor_hrm", "heart_rate", u -> u.getLocMyLocation().getSensorHeartRate()));
        list.add(cField("sensor_cadence", "cadence", u -> u.getLocMyLocation().getSensorCadence()));
        list.add(cField("sensor_power", "power", u -> u.getLocMyLocation().getSensorPower()));
        list.add(cField("sensor_speed", "bicycle_speed", u -> u.getLocMyLocation().getSensorSpeed()));
        list.add(cField("sensor_strides", "strides_label", u -> u.getLocMyLocation().getSensorStrides()));
        list.add(cField("sensor_temperature", "temperature", u -> u.getLocMyLocation().getSensorTemperature()));
        list.add(cField("sensor_battery_temperature", "battery_temp", UpdateContainer::getDeviceBatteryTemperature));
        list.add(cField("pace", "pace", UpdateContainer::getPace));
        list.add(cField("speed_vertical", new String[]{"speed", "vertical"}, UpdateContainer::getSpeedVertical));
        list.add(cField("slope", "slope", UpdateContainer::getSlope));
        list.add(cField("is_gps_valid", "", UpdateContainer::isGpsLocValid));
        list.add(cField("gps_sat_used", "satellites_used", UpdateContainer::getGpsSatsUsed));
        list.add(cField("gps_sat_all", "satellites_all", UpdateContainer::getGpsSatsAll));
        list.add(cField("declination", "declination", UpdateContainer::getDeclination));
        list.add(cField("heading", "heading", UpdateContainer::getOrientHeading));
        list.add(cField("gps_angle", "", UpdateContainer::getOrientGpsAngle));
        list.add(cField("course", "course", UpdateContainer::getOrientCourse));
        list.add(cField("roll", "roll", UpdateContainer::getOrientRoll));
        list.add(cField("pitch", "orientation_pitch", UpdateContainer::getOrientPitch));
        list.add(cField("is_user_touching", "", UpdateContainer::isUserTouching));
        list.add(cField("is_guide_enabled", "guide_on", UpdateContainer::isGuideEnabled));
        list.add(cField("is_track_rec_recording", "recording", UpdateContainer::isTrackRecRecording));
        list.add(cField("is_track_rec_paused", new String[]{"track_record", "paused"}, UpdateContainer::isTrackRecPaused));
        list.add(cField("track_rec_profile", "track_rec_profile", UpdateContainer::getTrackRecProfileName));
        list.add(cField("is_enabled_my_location", "gps_on", UpdateContainer::isEnabledMyLocation));
        list.add(cField("is_map_visible", new String[]{"map", "visible"}, UpdateContainer::isMapVisible));
        list.add(cField("active_live_track_id", "", UpdateContainer::getActiveLiveTrackId));
        list.add(cField("active_dashboard_id", "", UpdateContainer::getActiveDashboardId));
        list.add(cField("gnss_hdop", "gnss_hdop", u -> u.getLocMyLocation().getGnssHdop()));
        list.add(cField("gnss_pdop", "gnss_pdop", u -> u.getLocMyLocation().getGnssPdop()));
        list.add(cField("gnss_vdop", "gnss_vdop", u -> u.getLocMyLocation().getGnssVdop()));
        list.add(cField("gnss_quality", "gnss_quality", u -> u.getLocMyLocation().getGnssQuality()));
        list.add(cField("gnss_sats_used", new String[]{"GNSS", "satellites_used"}, u -> u.getLocMyLocation().getGnssSatsUsed()));
        list.add(cField("gnss_sats_visible", new String[]{"GNSS", "satellites_visible"}, u -> u.getLocMyLocation().getGnssSatsVisible()));
        list.add(cField("gnss_diff_msg_age", "", u -> u.getLocMyLocation().getGnssDiffMessageAge()));
        list.add(cField("gnss_ntrip_mount_point", "", u -> u.getLocMyLocation().getGnssNtripMountPoint()));
        list.add(cField("gnss_observ_time_start", "", u -> u.getLocMyLocation().getGnssObservationTimeStart()));
        list.add(cField("gnss_observ_time_end", "", u -> u.getLocMyLocation().getGnssObservationTimeEnd()));
        list.add(cField("extra_gssn_signal_strength", "gsm_signal", u -> u.getLocMyLocation().getExtraGsmSignalStrength()));
        list.add(cField("extra_ant_phase_center_offset", "", u -> u.getLocMyLocation().getExtraAntennaPhaseCenterOffset()));
        list.add(cField("extra_pole_height", "", u -> u.getLocMyLocation().getExtraPoleHeight()));

        list.add(cField("simulate_error_hidden", "", u -> {
            Logger.e(new RequiredDataMissingException("Simulate Hidden Error"), "Simulation", "Simulate Hidden Error");
            return "";
        }));
        list.add(cField("simulate_error_visible", "", u -> {
            //noinspection ProhibitedExceptionThrown
            throw new RuntimeException("Simulated Error");
        }));

        return list;
    }

    @SuppressWarnings("ConstantConditions") //don't make null because if map data is missing locus isn't active
    @NonNull
    public ArrayList<TaskerField> createMapFields() {
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
        list.add(cField("map_center_altitude", "", u -> u.getLocMapCenter().getAltitude()));

        return list;
    }

    @SuppressWarnings("ConstantConditions") //don't make null checks here, we do it based on key
    @NonNull
    public ArrayList<TaskerField> createTrackRecStatsFields() {
        ArrayList<TaskerField> list = new ArrayList<>();
        //this is a custom order
        list.add(cField("rec_total_length", "distance", u -> u.getTrackRecStats().getTotalLength()));
        list.add(cField("rec_total_length_move", "", u -> u.getTrackRecStats().getTotalLengthMove()));
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
        list.add(cField("rec_speed_max", "max_speed", u -> u.getTrackRecStats().getSpeedMax()));
        list.add(cField("rec_average_speed_total", "average_speed", u -> u.getTrackRecStats().getSpeedAverage(false)));
        list.add(cField("rec_average_speed_move", "average_moving_speed", u -> u.getTrackRecStats().getSpeedAverage(true)));
        list.add(cField("rec_point_count", "points_count", u -> u.getTrackRecStats().getNumOfPoints()));
        list.add(cField("rec_cadence_avg", "cadence_avg", u -> u.getTrackRecStats().getCadenceAverage()));
        list.add(cField("rec_cadence_max", "cadence_max", u -> u.getTrackRecStats().getCadenceMax()));
        list.add(cField("rec_energy_burned", "energy_burned", u -> u.getTrackRecStats().getEnergy()));
        list.add(cField("rec_hrm_avg", "heart_rate_avg", u -> u.getTrackRecStats().getHeartRateAverage()));
        list.add(cField("rec_hrm_max", "heart_rate_max", u -> u.getTrackRecStats().getHeartRateMax()));
        list.add(cField("rec_strides_count", "strides_label", u -> u.getTrackRecStats().getNumOfStrides()));
        list.add(cField("rec_power_avg", "power_avg", u -> u.getTrackRecStats().getPowerAverage()));
        list.add(cField("rec_power_max", "power_max", u -> u.getTrackRecStats().getPowerMax()));
        list.add(cField("rec_temp_min", "temperature_min", u -> u.getTrackRecStats().getTemperatureMin()));
        list.add(cField("rec_temp_max", "temperature_max", u -> u.getTrackRecStats().getTemperatureMax()));
        list.add(cField("rec_has_temp", "has_temperature", u -> u.getTrackRecStats().getHasTemperature()));

        return list;
    }

    @SuppressWarnings("ConstantConditions") //don't make null checks here, we do it based on key
    @NonNull
    public ArrayList<TaskerField> createGuideFields() {
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
        list.add(cField("guide_navpoint2_lon", "",
                u -> (u.getGuideNavPoint2Loc() == null) ? null : u.getGuideNavPoint2Loc().getLongitude()));
        list.add(cField("guide_navpoint2_lat", "",
                u -> (u.getGuideNavPoint2Loc() == null) ? null : u.getGuideNavPoint2Loc().getLatitude()));
        list.add(cField("guide_navpoint2_name", "", UpdateContainer::getGuideNavPoint2Name));
        list.add(cField("guide_navpoint2_time", "", UpdateContainer::getGuideNavPoint2Time));
        list.add(cField("guide_nextvia_dist", "", UpdateContainer::getGuideNextViaDist));
        list.add(cField("guide_nextvia_name", "", UpdateContainer::getGuideNextViaName));
        list.add(cField("guide_nextvia_time", "", UpdateContainer::getGuideNextViaTime));

        return list;
    }

    @NonNull
    public static ArrayList<TaskerField> createNavigationProgressFields() {
        ArrayList<TaskerField> list = new ArrayList<>();
        list.add(extField("calc_remain_uphill_elevation", u -> u.getNavigationProgress().getRemainingUphill()));
        list.add(extField("calc_remain_downhill_elevation", u -> u.getNavigationProgress().getRemainingDownhill()));
        list.add(extField("navigation_point_index", u -> u.getNavigationProgress().pointIndex));
        list.add(extField("navigation_track_name", u -> u.getNavigationProgress().trackName));

        return list;
    }
}
