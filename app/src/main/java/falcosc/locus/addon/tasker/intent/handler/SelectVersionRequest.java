package falcosc.locus.addon.tasker.intent.handler;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.RequiredDataMissingException;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.ExecutionTimes;
import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.ActionBasics;
import locus.api.android.objects.LocusInfo;
import locus.api.android.objects.LocusVersion;
import locus.api.android.utils.LocusConst;
import locus.api.android.utils.LocusUtils;

public class SelectVersionRequest extends AbstractTaskerAction {

    public static final String LAST_ACTIVE = "LAST_ACTIVE"; //NON-NLS

    @Override
    protected void doHandle(@NonNull Bundle apiExtraBundle) throws LocusCache.MissingAppContextException, RequiredDataMissingException {
        for (ExecutionTimes.Type t : ExecutionTimes.Type.values()) {
            Log.d("TEST",t.toString() + ": " + ExecutionTimes.INSTANCE.extractDurations(t));
        }
        LocusCache locusCache = LocusCache.getInstanceUnsafe(mContext);

        String packageName = apiExtraBundle.getString(LocusConst.INTENT_EXTRA_PACKAGE_NAME);

        LocusVersion lv = null;
        if (LAST_ACTIVE.equals(packageName)) {
            LocusInfo activeLocusInfo = new LocusInfo();
            for (LocusVersion availableVersion : LocusUtils.INSTANCE.getAvailableVersions(mContext)) {
                LocusInfo info = ActionBasics.INSTANCE.getLocusInfo(mContext, availableVersion);
                if (info != null) {
                    if (info.getLastActive() > activeLocusInfo.getLastActive()) {
                        activeLocusInfo = info;
                        lv = availableVersion;
                    }
                }
            }
        } else {
            lv = LocusUtils.INSTANCE.createLocusVersion(mContext, packageName);
        }
        if (lv == null) {
            throw new RequiredDataMissingException(mContext.getString(R.string.err_select_version_no_package, packageName));
        }
        locusCache.mLocusVersion = lv;

        mReceiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
    }
}
