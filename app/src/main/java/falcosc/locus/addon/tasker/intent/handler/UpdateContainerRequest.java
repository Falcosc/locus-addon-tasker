package falcosc.locus.addon.tasker.intent.handler;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.receiver.AbstractTaskerAction;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.LocusField;
import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UpdateContainerRequest extends AbstractTaskerAction {

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
}
