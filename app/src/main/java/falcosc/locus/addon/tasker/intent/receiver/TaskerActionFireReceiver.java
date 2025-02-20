package falcosc.locus.addon.tasker.intent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.asamm.logger.Logger;

import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.intent.handler.TaskerAction;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.ExecutionTimes;
import falcosc.locus.addon.tasker.utils.ReportingHelper;


public class TaskerActionFireReceiver extends BroadcastReceiver {

    private static final String TAG = "TaskerActionFire"; //NON-NLS

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        long startTime = System.nanoTime();
        Logger.d(TAG, "onReceive start"); //NON-NLS
        Bundle apiExtraBundle = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        if (apiExtraBundle == null) {
            Logger.i(TAG, "onReceive EXTRA_BUNDLE missing"); //NON-NLS
            return;
        }

        String actionType = apiExtraBundle.getString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE);
        if (actionType == null) {
            Logger.i(TAG, "onReceive INTEND_EXTRA_ADDON_ACTION_TYPE missing"); //NON-NLS
            return;
        }

        try {
            LocusActionType type = LocusActionType.valueOf(actionType);
            TaskerAction action = type.createHandler();
            action.setContext(context, this);
            Logger.i(TAG, "onReceive: " + apiExtraBundle); //NON-NLS
            action.handle(intent, apiExtraBundle);
            ExecutionTimes.INSTANCE.addDuration(type, System.nanoTime() - startTime);
        } catch (Exception e) {
            new ReportingHelper(context).sendErrorNotification(TAG, "Can't execute action " + actionType, e); //NON-NLS
        }

        Logger.i(TAG, "finish " + actionType + " after " + ExecutionTimes.formatNanoToMilli(System.nanoTime() - startTime)); //NON-NLS
    }
}
