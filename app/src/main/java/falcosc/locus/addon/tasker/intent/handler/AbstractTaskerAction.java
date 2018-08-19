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
        if (!TaskerPlugin.Condition.hostSupportsVariableReturn(intent.getExtras())) {
            Toast.makeText(context, R.string.err_no_support_return_variables, Toast.LENGTH_LONG).show();
            return false;
        }

        if (!receiver.isOrderedBroadcast()) {
            Toast.makeText(context, R.string.err_not_set_sync_exec, Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    Context context;
    private Intent intent;
    BroadcastReceiver receiver;

    @Override
    public void handle(@NonNull Context context, @NonNull Intent intent, @NonNull Bundle apiExtraBundle, @NonNull BroadcastReceiver receiver) {
        this.context = context;
        this.intent = intent;
        this.receiver = receiver;

        try {
            doHandle(apiExtraBundle);
        } catch (RequiredVersionMissingException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
