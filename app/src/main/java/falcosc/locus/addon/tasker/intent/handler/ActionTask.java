package falcosc.locus.addon.tasker.intent.handler;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;

public class ActionTask extends AbstractTaskerAction {

    private static final String COM_ASAMM_LOCUS_ACTION_TASK = "com.asamm.locus.ACTION_TASK"; //NON-NLS

    @Override
    protected void doHandle(@NonNull Bundle apiExtraBundle) throws LocusCache.MissingAppContextException {

        LocusCache locusCache = LocusCache.getInstanceUnsafe(mContext);
        String json = apiExtraBundle.getString(Const.INTENT_EXTRA_FIELD_JSON);

        Intent intent = new Intent(COM_ASAMM_LOCUS_ACTION_TASK);
        intent.putExtra(Const.INTENT_ACTION_TASK_EXTRA_KEY, json);
        if (locusCache.mLocusVersion != null) {
            intent.setPackage(locusCache.mLocusVersion.getPackageName());
        }
        mContext.sendBroadcast(intent);

        if (mReceiver.isOrderedBroadcast()) {
            mReceiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
        }

    }
}