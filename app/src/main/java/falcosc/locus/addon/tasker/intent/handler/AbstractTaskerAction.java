package falcosc.locus.addon.tasker.intent.handler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.widget.Toast;

import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;

abstract class AbstractTaskerAction implements TaskerAction {

    protected abstract void doHandle(@NonNull Bundle apiExtraBundle) throws RequiredVersionMissingException, LocusCache.MissingAppContextException;

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
    public void setContext(@NonNull Context context, @NonNull BroadcastReceiver receiver) {
        mContext = context;
        mReceiver = receiver;
    }

    @Override
    public void handle(@NonNull Intent intent, @NonNull Bundle apiExtraBundle) {
        mIntent = intent;

        try {
            doHandle(apiExtraBundle);
        } catch (LocusCache.MissingAppContextException | RequiredVersionMissingException e) {
            Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
