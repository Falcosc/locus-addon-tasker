package falcosc.locus.addon.tasker.intent.handler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.RequiredDataMissingException;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.ReportingHelper;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;

abstract class AbstractTaskerAction implements TaskerAction {

    private static final String TAG = "AbstractTaskerAction"; //NON-NLS

    protected abstract void doHandle(@NonNull Bundle apiExtraBundle) throws RequiredVersionMissingException, LocusCache.MissingAppContextException, RequiredDataMissingException;

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
        } catch (LocusCache.MissingAppContextException | RequiredVersionMissingException | RequiredDataMissingException e) {
            if (mReceiver.isOrderedBroadcast()) {
                Bundle varsBundle = new Bundle();
                varsBundle.putString(TaskerPlugin.Setting.VARNAME_ERROR_MESSAGE, e.getMessage());
                TaskerPlugin.addVariableBundle(mReceiver.getResultExtras(true), varsBundle);
                mReceiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_FAILED);
            } else {
                //can't return anything, write it to log
                new ReportingHelper(mContext).sendErrorNotification(TAG, "Can't handle not ordered broadcast", e); //NON-NLS
            }
        }
    }

    void requireSupportingVariables() throws RequiredDataMissingException {
        if (!TaskerPlugin.Condition.hostSupportsVariableReturn(mIntent.getExtras())) {
            throw new RequiredDataMissingException(getString(R.string.err_no_support_return_variables));
        }

        if (!mReceiver.isOrderedBroadcast()) {
            throw new RequiredDataMissingException(getString(R.string.err_not_set_sync_exec));
        }
    }

    @NonNull
    Set<String> requireSelectedFields(@NonNull Bundle apiExtraBundle) throws RequiredDataMissingException {

        String[] selectedFieldsArray = apiExtraBundle.getStringArray(Const.INTENT_EXTRA_FIELD_LIST);

        if ((selectedFieldsArray == null) || (selectedFieldsArray.length < 1)) {
            throw new RequiredDataMissingException(getString(R.string.err_field_selection_missing));
        }

        return new HashSet<>(Arrays.asList(selectedFieldsArray));
    }

    private String getString(int field) {
        return mContext.getResources().getString(field);
    }
}
