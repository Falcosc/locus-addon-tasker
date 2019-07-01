package falcosc.locus.addon.tasker.intent.handler;

import android.app.Notification;
import android.os.Bundle;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import falcosc.locus.addon.tasker.RequiredDataMissingException;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.ActionBasics;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Point;

public class NearestPointRequest extends AbstractTaskerAction {
    private String mNameFilter;
    private int mRadius;
    private int mExtendedRadius;
    private int mLimit;
    private long[] mKnownPIDs;
    private RemoveMode mRemoveMode;
    private long[] mPointIdsFromNameFilter;
    private long[] mPointIdsRadius;

    public enum RemoveMode {
        NEVER,
        OUTSIDE_RADIUS,
        OUTSIDE_EXTENDED_RADIUS
    }

    @Override
    protected void doHandle(@NonNull Bundle apiExtraBundle) throws RequiredVersionMissingException, LocusCache.MissingAppContextException, RequiredDataMissingException {

        getFields(apiExtraBundle);

        LocusCache locusCache = LocusCache.getInstanceUnsafe(mContext);
        LocusUtils.LocusVersion locusVersion = locusCache.requireLocusVersion();

        if (StringUtils.isNotBlank(mNameFilter)) {
            mPointIdsFromNameFilter = ActionBasics.INSTANCE.getPointsId(mContext, locusVersion, mNameFilter);
        } else {
            mPointIdsFromNameFilter = null;
        }

        mPointIdsRadius = getNearPointIds(locusVersion, mRadius);
        //don't alter mPointIdsRadius because we need to return it as known points
        long[] pointIdsResult = ArrayUtils.clone(mPointIdsRadius);
        for (long knownPID : mKnownPIDs) {
            ArrayUtils.removeElement(pointIdsResult, knownPID);
        }

        long[] knownPIDsResult = getKnownPIDs(locusVersion);

        List<Point> points = getPoints(locusVersion, pointIdsResult);
        String[] pointNames = new String[points.size()];
        for (int i = 0; i < pointNames.length; i++) {
            pointNames[i] = points.get(i).getName();
        }
        //TODO remove overwrite
        pointNames = new String[]{"Test1", "Test2"};

        Bundle varsBundle = new Bundle();
        varsBundle.putLongArray("%" + Const.INTENT_EXTRA_POINTS_KNOWN, knownPIDsResult);
        varsBundle.putStringArray("%points", pointNames);

        //TODO get Points json

        TaskerPlugin.addVariableBundle(mReceiver.getResultExtras(true), varsBundle);
        mReceiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
    }

    private List<Point> getPoints(LocusUtils.LocusVersion locusVersion, long[] pointIds) {
        List<Point> points = new ArrayList<>();
        for (long pointId : pointIds) {
            points.add(ActionBasics.INSTANCE.getPoint(mContext, locusVersion, pointId));
        }
        return points;
    }

    private void getFields(@NonNull Bundle bundle) {

        mNameFilter = bundle.getString(Const.INTENT_EXTRA_POINTS_NAME_FILTER);
        mRadius = Integer.parseInt(bundle.getString(Const.INTENT_EXTRA_POINTS_RADIUS, "10"));
        String extendedRadius = bundle.getString(Const.INTENT_EXTRA_POINTS_RADIUS_EXTENDED);
        if (StringUtils.isNotBlank(extendedRadius)) {
            mExtendedRadius = Integer.parseInt(extendedRadius);
        }
        mLimit = Integer.parseInt(bundle.getString(Const.INTENT_EXTRA_POINTS_LIMIT, "100"));
        //TODO try long array
        String[] knownPIDs = StringUtils.split(bundle.getString(Const.INTENT_EXTRA_POINTS_KNOWN, ""), ',');
        mKnownPIDs = new long[knownPIDs.length];
        for (int i = 0; i < knownPIDs.length; i++) {
            mKnownPIDs[i] = Long.valueOf(knownPIDs[i]);
        }

        mRemoveMode = RemoveMode.valueOf(bundle.getString(Const.INTENT_EXTRA_POINTS_REMOVE_MODE, RemoveMode.NEVER.toString()));
    }

    private long[] getNearPointIds(LocusUtils.LocusVersion locusVersion, int radius) throws RequiredVersionMissingException {

        //TODO get and check field
        Location loc = new Location();
        loc.latitude = 51.05089;
        loc.longitude = 13.73832;

        //long[] pointIds = ActionBasics.INSTANCE.getPointsId(locusCache.getApplicationContext(), locusCache.requireLocusVersion(),loc, 100, 100000);
        //TODO Location loc = locusCache.getUpdateContainer().mUpdateContainer.getLocMyLocation();
        long[] pointIdsRadius = ActionBasics.INSTANCE.getPointsId(mContext, locusVersion, loc, mLimit, radius);
        if (mPointIdsFromNameFilter != null) {
            long[] pointIdsRadiusFiltered = new long[0];
            for (long pointId : mPointIdsFromNameFilter) {
                if (ArrayUtils.contains(pointIdsRadius, pointId)) {
                    ArrayUtils.add(pointIdsRadiusFiltered, pointId);
                }
            }
            pointIdsRadius = pointIdsRadiusFiltered;
        }

        return pointIdsRadius;
    }

    @Nullable
    private long[] getKnownPIDs(@NonNull LocusUtils.LocusVersion locusVersion) throws RequiredVersionMissingException {
        switch (mRemoveMode) {
            case NEVER:
                return ArrayUtils.addAll(mKnownPIDs, mPointIdsRadius);
            case OUTSIDE_RADIUS:
                return mPointIdsRadius;
            case OUTSIDE_EXTENDED_RADIUS:
                return getNearPointIds(locusVersion, mExtendedRadius);
        }
        return null;
    }
}
