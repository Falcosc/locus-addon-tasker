package falcosc.locus.addon.tasker.intent.handler;

import android.os.Bundle;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import androidx.annotation.NonNull;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.RequiredDataMissingException;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.ActionBasics;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;

public class NearestPointRequest extends AbstractTaskerAction {
    private String mNameFilter;
    private int mRadius;
    private int mExtendedRadius;
    private int mLimit;
    private ArrayList<Long> mKnownPIDs;
    private RemoveMode mRemoveMode;

    public enum RemoveMode {
        NEVER,
        OUTSIDE_RADIUS,
        OUTSIDE_EXTENDED_RADIUS
    }

    @Override
    protected void doHandle(@NonNull Bundle apiExtraBundle) throws RequiredVersionMissingException, LocusCache.MissingAppContextException, RequiredDataMissingException {

        getFields(apiExtraBundle);

        getPoints();

        Bundle varsBundle = new Bundle();


        long[] knowPIDs = new long[mKnownPIDs.size()];
        for (int i = 0; i < knowPIDs.length; i++) {
            knowPIDs[i] = mKnownPIDs.get(i);
        }
        varsBundle.putLongArray("%" + Const.INTENT_EXTRA_POINTS_KNOWN, knowPIDs);
        //TODO set result
        varsBundle.putStringArray("%points", new String[]{"Test1", "Test2"});

        TaskerPlugin.addVariableBundle(mReceiver.getResultExtras(true), varsBundle);
        mReceiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
    }

    private void getFields(@NonNull Bundle bundle) {

        mNameFilter = bundle.getString(Const.INTENT_EXTRA_POINTS_NAME_FILTER);
        mRadius = Integer.parseInt(bundle.getString(Const.INTENT_EXTRA_POINTS_RADIUS, "10"));
        String extendedRadius = bundle.getString(Const.INTENT_EXTRA_POINTS_RADIUS_EXTENDED);
        if(StringUtils.isNotBlank(extendedRadius)) {
            mExtendedRadius = Integer.parseInt(extendedRadius);
        }
        mLimit = Integer.parseInt(bundle.getString(Const.INTENT_EXTRA_POINTS_LIMIT,"100"));
        String[] knownPIDs = StringUtils.split(bundle.getString(Const.INTENT_EXTRA_POINTS_KNOWN, ""), ',');
        mKnownPIDs = new ArrayList<>();
        for(String knownPID : knownPIDs){
            mKnownPIDs.add(Long.valueOf(knownPID));
        }
        mRemoveMode = RemoveMode.valueOf(bundle.getString(Const.INTENT_EXTRA_POINTS_REMOVE_MODE, RemoveMode.NEVER.toString()));
    }

    public void getPoints() throws LocusCache.MissingAppContextException, RequiredDataMissingException, RequiredVersionMissingException {
        LocusCache locusCache = LocusCache.getInstanceUnsafe(mContext);

        //TODO get and check field
        Location loc = new Location();
        loc.latitude = 51.05089;
        loc.longitude = 13.73832;

        //long[] pointIds = ActionBasics.INSTANCE.getPointsId(locusCache.getApplicationContext(), locusCache.requireLocusVersion(),loc, 100, 100000);
        ActionBasics.INSTANCE.getPointsId(locusCache.getApplicationContext(), locusCache.requireLocusVersion(), "Name");
    }
}
