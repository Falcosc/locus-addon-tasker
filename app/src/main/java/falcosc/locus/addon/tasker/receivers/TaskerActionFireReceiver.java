package falcosc.locus.addon.tasker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.LocusField;
import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;

import java.util.*;

public final class TaskerActionFireReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {

        if (!TaskerPlugin.Condition.hostSupportsVariableReturn(intent.getExtras())) {
            //TODO add Error
        }

        if (!isOrderedBroadcast()) {
            //TODO add error
        }

        Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        if (bundle == null) {
            //TODO log
            return;
        }

        Set<String> selectedFields = new HashSet<>(Arrays.asList(bundle.getStringArray(Const.INTENT_EXTRA_FIELD_LIST)));

        LocusCache locusCache = LocusCache.getInstance(context);

        try {
            UpdateContainer update = ActionTools.getDataUpdateContainer(context, locusCache.locusVersion);

            if (!update.isTrackRecRecording()) {
                //remove track recording fields
                selectedFields.removeAll(locusCache.trackRecordingKeys);
            }

            Bundle varsBundle = new Bundle();
            for (String field : selectedFields) {
                //Don't neet to check updateContainerMethodMap, illegal intents creates exceptions
                LocusField lf = locusCache.updateContainerFieldMap.get(field);
                varsBundle.putString("%" + field, lf.updateContainerGetter.apply(update));
                TaskerPlugin.addVariableBundle(getResultExtras(true), varsBundle);
            }

            setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
        } catch (RequiredVersionMissingException e) {
            //TODO
            e.printStackTrace();
            return;
        }

    }
}
