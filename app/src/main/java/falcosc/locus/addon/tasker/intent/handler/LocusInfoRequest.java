package falcosc.locus.addon.tasker.intent.handler;

import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.LocusInfoField;
import locus.api.android.ActionBasics;
import locus.api.android.utils.LocusInfo;

public class LocusInfoRequest extends AbstractTaskerAction {

    public static List<LocusInfoField> FIELDS = new ArrayList<>(Arrays.asList(
            new LocusInfoField("dir_backup", "Backup Folder", LocusInfo::getRootDirBackup),
            new LocusInfoField("dir_root", "Main Folder", LocusInfo::getRootDirectory),
            new LocusInfoField("dir_export", "Export Folder", LocusInfo::getRootDirExport),
            new LocusInfoField("dir_geocaching", "Geocaching Folder", LocusInfo::getRootDirGeocaching),
            new LocusInfoField("dir_mapitems", "Map Items Folder", LocusInfo::getRootDirMapItems),
            new LocusInfoField("dir_maps", "Maps Folder", LocusInfo::getRootDirMapsPersonal),
            new LocusInfoField("dir_mapsvector", "Maps Vector Folder", LocusInfo::getRootDirMapsVector),
            new LocusInfoField("dir_mapsonline", "Maps Online Folder", LocusInfo::getRootDirMapsOnline),
            new LocusInfoField("dir_srtm", "SRTM Folder", LocusInfo::getRootDirSrtm),
            new LocusInfoField("locus_package", "Locus Package Name", LocusInfo::getPackageName),
            new LocusInfoField("locus_isrunning", "Is Locus Running?", LocusInfo::isRunning),
            new LocusInfoField("last_active", "Last Active Timestamp", LocusInfo::getLastActive)
    ));

    private Set<String> getSelectedFields(@NonNull Bundle apiExtraBundle) {

        String[] selectedFieldsArray = apiExtraBundle.getStringArray(Const.INTENT_EXTRA_FIELD_LIST);

        if (selectedFieldsArray == null) {
            Toast.makeText(mContext, R.string.err_field_selection_missing, Toast.LENGTH_LONG).show();
            return null;
        }

        Set<String> selectedFields = new HashSet<>(Arrays.asList(selectedFieldsArray));

        if (selectedFields.isEmpty()) {
            Toast.makeText(mContext, R.string.err_field_selection_missing, Toast.LENGTH_LONG).show();
            return null;
        }

        return selectedFields;
    }

    @Override
    protected void doHandle(@NonNull Bundle apiExtraBundle) throws LocusCache.MissingAppContextException {

        LocusCache locusCache = LocusCache.getInstanceUnsafe(mContext);

        Set<String> selectedFields = getSelectedFields(apiExtraBundle);

        if (isSupportingVariables() && (locusCache.mLocusVersion != null) && (selectedFields != null)) {

            LocusInfo info = ActionBasics.INSTANCE.getLocusInfo(locusCache.getApplicationContext(), locusCache.mLocusVersion);

            Bundle varsBundle = new Bundle();

            for (LocusInfoField locusInfoField : FIELDS) {
                if(selectedFields.contains(locusInfoField.mTaskerName)) {
                    varsBundle.putString("%" + locusInfoField.mTaskerName, String.valueOf(locusInfoField.apply(info)));
                    TaskerPlugin.addVariableBundle(mReceiver.getResultExtras(true), varsBundle);
                }
            }

            TaskerPlugin.addVariableBundle(mReceiver.getResultExtras(true), varsBundle);
            mReceiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
        }

        //TODO error handling
        //mReceiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_FAILED);
    }
}
