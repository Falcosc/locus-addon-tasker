package falcosc.locus.addon.tasker.intent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.intent.handler.TaskerAction;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.TaskerPlugin;



public final class TaskerActionFireReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
        Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        if (bundle == null) {
            //TODO log
            return;
        }

        String actionType = bundle.getString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE);
        if (actionType == null) {
            //TODO ignore
            return;
        }

        try {
            TaskerAction action = LocusActionType.valueOf(actionType).createHandler();

            action.handle(context, intent, bundle, this);
        } catch (Exception e) {
            //TODO log
            setResultCode(TaskerPlugin.Setting.RESULT_CODE_FAILED);
        }


    }
}
