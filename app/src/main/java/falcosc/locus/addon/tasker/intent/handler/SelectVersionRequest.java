package falcosc.locus.addon.tasker.intent.handler;

import android.os.Bundle;

import androidx.annotation.NonNull;
import falcosc.locus.addon.tasker.RequiredDataMissingException;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.utils.LocusConst;
import locus.api.android.utils.LocusUtils;

public class SelectVersionRequest extends AbstractTaskerAction {

    @Override
    protected void doHandle(@NonNull Bundle apiExtraBundle) throws LocusCache.MissingAppContextException, RequiredDataMissingException {

        LocusCache locusCache = LocusCache.getInstanceUnsafe(mContext);

        String packageName = apiExtraBundle.getString(LocusConst.INTENT_EXTRA_PACKAGE_NAME);

        LocusUtils.LocusVersion lv = LocusUtils.createLocusVersion(mContext, packageName);
        if(lv == null) {
            throw new RequiredDataMissingException("Could not found version for package: " + packageName);
        }
        locusCache.mLocusVersion = lv;

        mReceiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
    }
}
