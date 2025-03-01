package falcosc.locus.addon.tasker.intent.handler;

import android.os.Bundle;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import falcosc.locus.addon.tasker.RequiredDataMissingException;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.ExecutionTimes;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.LocusInfoField;
import falcosc.locus.addon.tasker.utils.TaskerField;
import locus.api.android.ActionBasics;
import locus.api.android.objects.LocusInfo;

public class LocusInfoRequest extends AbstractTaskerAction {

    @NonNls
    public static final String DURATION_FIELD_SUFFIX = "_durations";
    private static final ArrayList<LocusInfoField> FIELDS = new ArrayList<>(Arrays.asList(
            new LocusInfoField("dir_backup", "Backup Folder", LocusInfo::getRootDirBackup),
            new LocusInfoField("dir_root", "Main Folder", LocusInfo::getRootDir),
            new LocusInfoField("dir_export", "Export Folder", LocusInfo::getRootDirExport),
            new LocusInfoField("dir_geocaching", "Geocaching Folder", LocusInfo::getRootDirGeocaching),
            new LocusInfoField("dir_mapitems", "Map Items Folder", LocusInfo::getRootDirMapItems),
            new LocusInfoField("dir_maps", "Maps Folder", LocusInfo::getRootDirMapsPersonal),
            new LocusInfoField("dir_mapsvector", "Maps Vector Folder", LocusInfo::getRootDirMapsVector),
            new LocusInfoField("dir_mapsonline", "Maps Online Folder", LocusInfo::getRootDirMapsOnline),
            new LocusInfoField("dir_srtm", "SRTM Folder", LocusInfo::getRootDirSrtm),
            new LocusInfoField("locus_package", "Locus Package Name", LocusInfo::getPackageName),
            new LocusInfoField("locus_isrunning", "Is Locus Running?", LocusInfo::isRunning),
            new LocusInfoField("last_active", "Last Active Timestamp", LocusInfo::getLastActive),
            new LocusInfoField("unit_weight", "Unit Format Weight", LocusInfo::getUnitsFormatWeight),
            new LocusInfoField("unit_altitude", "Unit Format Altitude", LocusInfo::getUnitsFormatAltitude),
            new LocusInfoField("unit_temperature", "Unit Format Temperature", LocusInfo::getUnitsFormatTemperature),
            new LocusInfoField("unit_angle", "Unit Format Angle", LocusInfo::getUnitsFormatAngle),
            new LocusInfoField("unit_speed", "Unit Format Weight", LocusInfo::getUnitsFormatSpeed),
            new LocusInfoField("unit_length", "Unit Format Weight", LocusInfo::getUnitsFormatLength),
            new LocusInfoField("unit_area", "Unit Format Weight", LocusInfo::getUnitsFormatArea),
            new LocusInfoField("unit_energy", "Unit Format Energy", LocusInfo::getUnitsFormatEnergy),
            new LocusInfoField("unit_slope", "Unit Format Slope", LocusInfo::getUnitsFormatSlope),
            new LocusInfoField("gc_owner_name", "GeoCache Owner Name", LocusInfo::getGcOwnerName),
            new LocusInfoField("select_version_req" + DURATION_FIELD_SUFFIX, "Runtimes: Select Version",
                    info -> ExecutionTimes.INSTANCE.extractDurations(LocusActionType.SELECT_VERSION)),
            new LocusInfoField("locus_info_req" + DURATION_FIELD_SUFFIX, "Runtimes: Locus Info",
                    info -> ExecutionTimes.INSTANCE.extractDurations(LocusActionType.LOCUS_INFO_REQUEST)),
            new LocusInfoField("action_task_req" + DURATION_FIELD_SUFFIX, "Runtimes: Execute Action Task",
                    info -> ExecutionTimes.INSTANCE.extractDurations(LocusActionType.ACTION_TASK)),
            new LocusInfoField("track_points_req" + DURATION_FIELD_SUFFIX, "Runtimes: Get Track Points",
                    info -> ExecutionTimes.INSTANCE.extractDurations(LocusActionType.TRACK_POINTS_REQUEST)),
            new LocusInfoField("update_container_req" + DURATION_FIELD_SUFFIX, "Runtimes: Request sensors and stats",
                    info -> ExecutionTimes.INSTANCE.extractDurations(LocusActionType.UPDATE_CONTAINER_REQUEST))
    ));

    public static List<TaskerField> getFieldNames() {
        return new ArrayList<>(FIELDS);
    }

    @Override
    protected void doHandle(@NonNull Bundle apiExtraBundle) throws LocusCache.MissingAppContextException, RequiredDataMissingException {


        LocusCache locusCache = LocusCache.getInstanceUnsafe(mContext);

        requireSupportingVariables();

        Set<String> selectedFields = requireSelectedFields(apiExtraBundle);

        LocusInfo info = ActionBasics.INSTANCE.getLocusInfo(locusCache.getApplicationContext(), locusCache.requireLocusVersion());

        Bundle varsBundle = new Bundle();

        for (LocusInfoField locusInfoField : FIELDS) {
            if (selectedFields.contains(locusInfoField.mTaskerName)) {
                varsBundle.putString(locusInfoField.getVar(), String.valueOf(locusInfoField.apply(info)));
            }
        }

        TaskerPlugin.addVariableBundle(mReceiver.getResultExtras(true), varsBundle);
        mReceiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
    }
}
