package falcosc.locus.addon.tasker.intent.handler;

import android.os.Bundle;

import androidx.annotation.NonNull;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.RequiredDataMissingException;
import falcosc.locus.addon.tasker.intent.edit.TrackPointsEdit;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.uc.NavigationProgress;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.TrackPointCache;
import locus.api.android.ActionBasics;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.geoData.Track;

public class TrackPointsRequest extends AbstractTaskerAction {


    public enum Type {
        WAYPOINTS, POINTS, POINTS_AND_WAYPOINTS
    }


    public enum TrackSource {
        LAST_LOADED, LOAD_SHARE, LOAD_GUIDE
    }

    @Override
    protected void doHandle(@NonNull Bundle apiExtraBundle) throws LocusCache.MissingAppContextException, RequiredDataMissingException, RequiredVersionMissingException {
        requireSupportingVariables();

        TrackSource source = TrackSource.valueOf(requireField(apiExtraBundle, Const.INTENT_EXTRA_TRK_SOURCE));
        Type pointType = Type.valueOf(requireField(apiExtraBundle, Const.INTENT_EXTRA_TRK_POINTS_TYPE));
        String locationFields = apiExtraBundle.getString(Const.INTENT_EXTRA_LOCATION_FIELDS);
        String waypointFields = apiExtraBundle.getString(Const.INTENT_EXTRA_WAYPOINT_FIELDS);
        int count = Math.max(Integer.parseInt(apiExtraBundle.getString(Const.INTENT_EXTRA_COUNT, "100")), 0);
        int offset = Math.max(Integer.parseInt(apiExtraBundle.getString(Const.INTENT_EXTRA_OFFSET, "0")), 0);

        LocusCache locusCache = LocusCache.getInstanceUnsafe(mContext);

        TrackPointCache track = getTrack(source, locusCache);
        track.setSelectFields(locationFields, waypointFields);
        track.setSelectAmount(count, offset);

        Bundle vars = new Bundle();
        vars.putString(TrackPointsEdit.TRACK_SEGMENT_VAR.getVar(), track.getSelectedSegment(pointType).toString());
        vars.putString(TrackPointsEdit.TRACK_POINT_COUNT.getVar(), String.valueOf(track.mTrack.getPoints().size()));
        vars.putString(TrackPointsEdit.TRACK_WAYPOINT_COUNT.getVar(), String.valueOf(track.mTrack.getWaypoints().size()));
        TaskerPlugin.addVariableBundle(mReceiver.getResultExtras(true), vars);

        mReceiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
    }

    @NonNull
    private static TrackPointCache getTrack(@NonNull TrackSource source, @NonNull LocusCache lCache) throws RequiredDataMissingException, RequiredVersionMissingException {
        //noinspection SwitchStatement we don't want to register handler, it's only used here.
        switch (source) {
            case LAST_LOADED:
                if (lCache.mTrackPointCache == null) {
                    throw new RequiredDataMissingException(lCache.getApplicationContext().getString(R.string.err_trk_points_no_loaded_track));
                }
                return lCache.mTrackPointCache;
            case LOAD_SHARE:
                if (lCache.mLastSharedTrack == null) {
                    throw new RequiredDataMissingException(lCache.getApplicationContext().getString(R.string.err_trk_points_no_track_selected));
                }
                lCache.mTrackPointCache = new TrackPointCache(lCache.mLastSharedTrack);
                return lCache.mTrackPointCache;
            case LOAD_GUIDE:
                return getFreshGuideTrack(lCache);
            default:
                throw new IllegalStateException("Type is not implemented: " + source);
        }
    }

    @NonNull
    private static TrackPointCache getFreshGuideTrack(@NonNull LocusCache lCache) throws RequiredVersionMissingException, RequiredDataMissingException {
        UpdateContainer uc = lCache.getUpdateContainer().mUpdateContainer;
        long guideTargetId = uc.getGuideTargetId();
        if (((uc.getGuideType() != UpdateContainer.GUIDE_TYPE_TRACK_GUIDE)
                && (uc.getGuideType() != UpdateContainer.GUIDE_TYPE_TRACK_NAVIGATION))) {
            throw new RequiredDataMissingException(lCache.getApplicationContext().getString(R.string.err_trk_points_no_guide));
        }
        Track track = ActionBasics.INSTANCE.getTrack(lCache.getApplicationContext(), lCache.requireLocusVersion(), guideTargetId);
        if (track == null) {
            throw new RequiredDataMissingException(lCache.getApplicationContext().getString(R.string.err_trk_points_no_guide_by_id, guideTargetId));
        }
        TrackPointCache oldTrackPoints = lCache.mTrackPointCache;
        if ((oldTrackPoints != null) && NavigationProgress.isSameTrack(track, oldTrackPoints.mTrack)) {
            return oldTrackPoints;
        }
        lCache.mTrackPointCache = new TrackPointCache(track);
        return lCache.mTrackPointCache;
    }
}
