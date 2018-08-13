package falcosc.locus.addon.tasker.intent.handler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import falcosc.locus.addon.tasker.utils.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.LocusField;
import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UpdateContainerRequest implements TaskerAction {
    @Override
    public void handle(@NonNull Context context, Intent intent, @NonNull Bundle bundle, @NonNull BroadcastReceiver receiver) throws RequiredVersionMissingException {
        if (!TaskerPlugin.Condition.hostSupportsVariableReturn(intent.getExtras())) {
            //TODO add Error
        }

        if (!receiver.isOrderedBroadcast()) {
            //TODO add error
        }

        String[] selectedFieldsArray = bundle.getStringArray(Const.INTENT_EXTRA_FIELD_LIST);

        if (selectedFieldsArray == null) {
            //TODO log
            return;
        }

        Set<String> selectedFields = new HashSet<>(Arrays.asList(selectedFieldsArray));

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
