package falcosc.locus.addon.tasker.intent.handler;

import android.os.Bundle;

import androidx.annotation.NonNull;

import falcosc.locus.addon.tasker.RequiredDataMissingException;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.ExtUpdateContainer;
import falcosc.locus.addon.tasker.utils.ExtUpdateContainerGetter;
import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;

import java.util.Set;

public class UpdateContainerRequest extends AbstractTaskerAction {

    @Override
    protected void doHandle(@NonNull Bundle apiExtraBundle) throws RequiredVersionMissingException, LocusCache.MissingAppContextException, RequiredDataMissingException {
        requireSupportingVariables();

        Set<String> selectedFields = requireSelectedFields(apiExtraBundle);

        LocusCache locusCache = LocusCache.getInstanceUnsafe(mContext);

        ExtUpdateContainer extUpdate = locusCache.getUpdateContainer();
        UpdateContainer update = extUpdate.mUpdateContainer;

        if (!update.isTrackRecRecording()) {
            //remove track recording fields to skip null checks
            selectedFields.removeAll(locusCache.mTrackRecordingKeys);
        }

        if (!update.isGuideEnabled()) {
            //remove guide fields to skip null checks
            selectedFields.removeAll(locusCache.mTrackGuideKeys);
        }

        Bundle varsBundle = new Bundle();

        for (String field : selectedFields) {
            //Don't need to check updateContainerMethodMap, illegal intents creates exceptions
            ExtUpdateContainerGetter lf = locusCache.mExtUpdateContainerFieldMap.get(field);
            //noinspection ConstantConditions if field is null we got an illegal intent
            varsBundle.putString("%" + field, String.valueOf(lf.apply(extUpdate)));
            TaskerPlugin.addVariableBundle(mReceiver.getResultExtras(true), varsBundle);
        }

    }
}
