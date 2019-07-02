package falcosc.locus.addon.tasker.intent.handler;

import android.app.Notification;
import android.os.Bundle;
import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import falcosc.locus.addon.tasker.LocusRunTaskerActivity;
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
    private static final String TAG = "NearestPointRequest";
    public static final String OUT_POINTS = "%points";
    public static final String OUT_POINTS_JSON = "%points_json";
    public static final String OUT_P1_NAME = "%p1_name";
    public static final String OUT_P1_DISTANCE = "%p1_distance";
    public static final String OUT_P1_BEARING = "%p1_bearing";
    public static final String OUT_KNOWN_PIDS = "%known_pids";
    private String mNameFilter;
    private int mRadius;
    private int mExtendedRadius;
    private int mLimit;
    private long[] mKnownPIDs;
    private RemoveMode mRemoveMode;
    private long[] mPointIdsFromNameFilter;
    private long[] mPointIdsRadius;
    private Location mLoc;

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
        mLoc = locusCache.getUpdateContainer().mUpdateContainer.getLocMyLocation();

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
        varsBundle.putLongArray(OUT_KNOWN_PIDS, knownPIDsResult);
        varsBundle.putStringArray(OUT_POINTS, pointNames);

        //TODO json checkbox
        if(true){
            JSONArray jsonArray = new JSONArray();

            for(Point p : points){
                JSONObject json = new JSONObject(LocusRunTaskerActivity.mapPointFields(p, ""));
                float[] distanceAndBearing = mLoc.distanceAndBearingTo(p.getLocation());
                try {
                    json.put("distance", distanceAndBearing[0]);
                    json.put("bearing", distanceAndBearing[1]);

                } catch (JSONException e) {
                    Log.e(TAG, "Can not set distance result", e); //NON-NLS
                }
                jsonArray.put(json);
            }
            varsBundle.putString(OUT_POINTS_JSON, jsonArray.toString());
        }

        if(!points.isEmpty()){
            float[] distanceAndBearing = mLoc.distanceAndBearingTo(points.get(0).getLocation());
            varsBundle.putString(OUT_P1_NAME, points.get(0).getName());
            varsBundle.putFloat(OUT_P1_DISTANCE, distanceAndBearing[0]);
            varsBundle.putFloat(OUT_P1_BEARING, distanceAndBearing[1]);
        }

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

        long[] pointIdsRadius = ActionBasics.INSTANCE.getPointsId(mContext, locusVersion, mLoc, mLimit, radius);
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
