package falcosc.locus.addon.tasker.intent.handler;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.LocusField;
import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Track;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UpdateContainerRequest extends AbstractTaskerAction {

    private static final String TAG = "UpdateContainerRequest";

    private Set<String> getSelectedFields(@NonNull Bundle apiExtraBundle) {

        String[] selectedFieldsArray = apiExtraBundle.getStringArray(Const.INTENT_EXTRA_FIELD_LIST);

        if (selectedFieldsArray == null) {
            Toast.makeText(context, R.string.err_field_selection_missing, Toast.LENGTH_LONG).show();
            return null;
        }

        Set<String> selectedFields = new HashSet<>(Arrays.asList(selectedFieldsArray));

        if (selectedFields.isEmpty()) {
            Toast.makeText(context, R.string.err_field_selection_missing, Toast.LENGTH_LONG).show();
            return null;
        }

        return selectedFields;
    }

    @Override
    protected void doHandle(@NonNull Bundle apiExtraBundle) throws RequiredVersionMissingException {

        Set<String> selectedFields = getSelectedFields(apiExtraBundle);

        if (isSupportingVariables() && selectedFields != null) {

            LocusCache locusCache = LocusCache.getInstance(context);

            UpdateContainer update = ActionTools.getDataUpdateContainer(context, locusCache.locusVersion);

            if (!update.isTrackRecRecording()) {
                //remove track recording fields
                selectedFields.removeAll(locusCache.trackRecordingKeys);
            }

            if (isLiveTrackNeeded(locusCache, selectedFields, update)) {
                setLiveTrack(locusCache, context, update.getActiveLiveTrackId());
            }

            Bundle varsBundle = new Bundle();
            for (String field : selectedFields) {
                //Don't need to check updateContainerMethodMap, illegal intents creates exceptions
                LocusField lf = locusCache.updateContainerFieldMap.get(field);
                varsBundle.putString("%" + field, lf.updateContainerGetter.apply(update));
                TaskerPlugin.addVariableBundle(receiver.getResultExtras(true), varsBundle);
            }

            receiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
        }
    }

    private static boolean isLiveTrackNeeded(LocusCache locusCache, Set<String> selectedFields, UpdateContainer update) {
        if (locusCache.getLastSelectedTrack() == null) {
            if (selectedFields.contains(LocusCache.CALC_REMAIN_UPHILL_ELEVATION)) {
                if (update.isGuideEnabled()) {
                    UpdateContainer.GuideTypeTrack guideTypeTrack = update.getGuideTypeTrack();
                    //don't check guideTypeTrack.isValid because it is false if we are guiding to track
                    return guideTypeTrack != null;
                }
            }
        }
        return false;
    }

    private static void setLiveTrack(LocusCache locusCache, Context context, String id) {
        try {
            int liveTrackId = Integer.valueOf(id);
            Track liveTrack = ActionTools.getLocusTrack(context, locusCache.locusVersion, liveTrackId);
            locusCache.setLastSelectedTrack(liveTrack);
        } catch (Exception e) {
            Log.w(TAG, "can't get live track", e); //NON-NLS logging message
        }
    }
}
