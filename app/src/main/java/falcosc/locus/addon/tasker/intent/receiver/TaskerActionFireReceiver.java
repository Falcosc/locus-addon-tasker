package falcosc.locus.addon.tasker.intent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.util.Log;

import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.intent.handler.TaskerAction;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.ReportingHelper;


public class TaskerActionFireReceiver extends BroadcastReceiver {

    private static final String TAG = "TaskerActionFire"; //NON-NLS

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        Bundle apiExtraBundle = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        if (apiExtraBundle == null) {
            Log.i(TAG, "onReceive EXTRA_BUNDLE missing"); //NON-NLS
            return;
        }

        String actionType = apiExtraBundle.getString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE);
        if (actionType == null) {
            Log.i(TAG, "onReceive INTEND_EXTRA_ADDON_ACTION_TYPE missing"); //NON-NLS
            return;
        }

        try {
            TaskerAction action = LocusActionType.valueOf(actionType).createHandler();
            action.setContext(context, this);
            action.handle(intent, apiExtraBundle);
        } catch (Exception e) {
            new ReportingHelper(context).sendErrorNotification(TAG, "Can't execute action " + actionType, e); //NON-NLS
        }
    }
}
