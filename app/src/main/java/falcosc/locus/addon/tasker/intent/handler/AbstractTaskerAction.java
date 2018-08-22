package falcosc.locus.addon.tasker.intent.handler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;

abstract class AbstractTaskerAction implements TaskerAction {

    @SuppressWarnings("RedundantThrows")
    protected abstract void doHandle(@NonNull Bundle apiExtraBundle) throws RequiredVersionMissingException;

    boolean isSupportingVariables() {
        if (!TaskerPlugin.Condition.hostSupportsVariableReturn(mIntent.getExtras())) {
            Toast.makeText(mContext, R.string.err_no_support_return_variables, Toast.LENGTH_LONG).show();
            return false;
        }

        if (!mReceiver.isOrderedBroadcast()) {
            Toast.makeText(mContext, R.string.err_not_set_sync_exec, Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    Context mContext;
    private Intent mIntent;
    BroadcastReceiver mReceiver;

    @Override
    public void handle(@NonNull Context context, @NonNull Intent intent,
                       @NonNull Bundle apiExtraBundle, @NonNull BroadcastReceiver receiver) {
        mContext = context;
        mIntent = intent;
        mReceiver = receiver;

        try {
            doHandle(apiExtraBundle);
        } catch (RequiredVersionMissingException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
