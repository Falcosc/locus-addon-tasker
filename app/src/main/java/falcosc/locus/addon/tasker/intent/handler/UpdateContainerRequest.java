package falcosc.locus.addon.tasker.intent.handler;

import android.os.Bundle;
import androidx.annotation.NonNull;
import android.widget.Toast;

import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.ExtUpdateContainer;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.LocusField;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UpdateContainerRequest extends AbstractTaskerAction {

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
    protected void doHandle(@NonNull Bundle apiExtraBundle) throws RequiredVersionMissingException, LocusCache.MissingAppContextException {

        Set<String> selectedFields = getSelectedFields(apiExtraBundle);

        if (isSupportingVariables() && (selectedFields != null)) {

            LocusCache locusCache = LocusCache.getInstanceUnsafe(mContext);

            ExtUpdateContainer extUpdate = locusCache.getUpdateContainer();
            UpdateContainer update = extUpdate.mUpdateContainer;

            if (!update.isTrackRecRecording()) {
                //remove track recording fields to skip null checks
                selectedFields.removeAll(locusCache.mTrackRecordingKeys);
            }

            if(!update.isGuideEnabled()){
                //remove guide fields to skip null checks
                selectedFields.removeAll(locusCache.mTrackGuideKeys);
            }

            Bundle varsBundle = new Bundle();

            for (String field : selectedFields) {
                //Don't need to check updateContainerMethodMap, illegal intents creates exceptions
                LocusField lf = locusCache.mUpdateContainerFieldMap.get(field);
                varsBundle.putString("%" + field, String.valueOf(lf.apply(extUpdate)));
                TaskerPlugin.addVariableBundle(mReceiver.getResultExtras(true), varsBundle);
            }

            mReceiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
        }
    }
}
