package falcosc.locus.addon.tasker.uc;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import falcosc.locus.addon.tasker.RequiredDataMissingException;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.ReportingHelper;
import locus.api.android.ActionBasics;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.objects.geoData.Track;

public final class NavigationProgress {
    private int mRemainingUphill;
    private int mRemainingDownhill;
    public int pointIndex = -1;
    public String trackName;
    private String mError;

    private static final String NO_TRK = "noTRK"; //NON-NLS
    private static final String NO_NAV = "noNAV"; //NON-NLS
    private static final String NO_ELE = "noELE"; //NON-NLS
    private static final String OFF_TRK = "offTRK"; //NON-NLS
    private static final String RESET = "RESET"; //NON-NLS
    private static final String TAG = "CalcElevationToTarget"; //NON-NLS

    public static class Point {
        int remainingUphill;
        int remainingDownhill;
    }

    public static class TrackData {
        public final Track mTrack;
        public int mPreviousFoundIndex;
        public final Point[] mRemainingTrackElevation;

        public TrackData(Track track) {
            mTrack = track;
            mRemainingTrackElevation = calculateRemainingElevation(track);
        }

        @SuppressWarnings("NumericCastThatLosesPrecision")
        public static Point[] calculateRemainingElevation(@Nullable Track track) {
            if (track == null) {
                return new Point[0];
            }

            Log.i(TAG, "recalculate track elevation of: " + track.getName()); //NON-NLS

            List<Location> points = track.getPoints();
            int size = points.size();
            //make array one point larger because we assign remaining elevation to point+1 because remain is current target point -1
            Point[] remainingElevation = new Point[size + 1];
            double uphillElevation = 0.0;
            double downhillElevation = 0.0;
            double nextAltitude = points.get(size - 1).getAltitude();
            for (int i = size - 1; i >= 0; i--) {
                double currentAltitude = points.get(i).getAltitude();
                if (nextAltitude > currentAltitude) {
                    uphillElevation += nextAltitude - currentAltitude;
                } else {
                    downhillElevation += currentAltitude - nextAltitude;
                }

                Point p = new Point();
                p.remainingUphill = (int) uphillElevation;
                p.remainingDownhill = (int) downhillElevation;
                remainingElevation[i + 1] = p;

                nextAltitude = currentAltitude;
            }
            //assign remaining altitude of point 0 because we have no values at 0 because we read 1 point ahead.
            remainingElevation[0] = remainingElevation[1];

            Log.i(TAG, "Points calculated: " + size); //NON-NLS

            return remainingElevation;
        }
    }

    public String getRemainingUphill() {
        if (mError != null) {
            return mError;
        }
        return Integer.toString(mRemainingUphill);
    }

    public String getRemainingDownhill() {
        if (mError != null) {
            return mError;
        }
        return Integer.toString(mRemainingDownhill);
    }

    public NavigationProgress(@NonNull UpdateContainer updateContainer) {

        LocusCache locusCache = LocusCache.getInstanceNullable();

        if (locusCache == null) {
            Log.e(TAG, "locus cache missing"); //NON-NLS
            return;
        }

        try {

            locusCache.mLastSelectedTrack = getActiveTrack(locusCache, updateContainer);

            mError = validateNavigationProgress(locusCache.mLastSelectedTrack, updateContainer);
            if (StringUtils.isNotBlank(mError)) {
                return;
            }

            locusCache.mLastSelectedTrack.mPreviousFoundIndex = pointIndex;
            mRemainingUphill = locusCache.mLastSelectedTrack.mRemainingTrackElevation[pointIndex].remainingUphill;
            mRemainingDownhill = locusCache.mLastSelectedTrack.mRemainingTrackElevation[pointIndex].remainingDownhill;
            trackName = locusCache.mLastSelectedTrack.mTrack.getName();


        } catch (Exception e) {
            new ReportingHelper(locusCache.getApplicationContext())
                    .sendErrorNotification(TAG, "Can not get remaining elevation", e); //NON-NLS

            //Error case
            //Don't search for the current track, this is done at the entry point of a new update request
            //reset track on error
            locusCache.mLastSelectedTrack = null;
            mError = RESET;
        }
    }

    private String validateNavigationProgress(TrackData track, UpdateContainer updateContainer) {
        if ((updateContainer.getGuideType() != UpdateContainer.GUIDE_TYPE_TRACK_GUIDE)
                && (updateContainer.getGuideType() != UpdateContainer.GUIDE_TYPE_TRACK_NAVIGATION)) {
            return NO_NAV;
        }

        if (track == null) {
            return NO_TRK;
        }

        if (track.mRemainingTrackElevation[0].remainingUphill == 0) {
            return NO_ELE;
        }

        Location nextPoint = updateContainer.getGuideWptLoc();
        pointIndex = findMatchingPointIndex(track.mTrack.getPoints(), nextPoint, track.mPreviousFoundIndex);

        if (pointIndex < 0) {
            //point not found on track, try to find the nav point because this may align because the selected track has less points
            Location nextNavPoint = updateContainer.getGuideNavPoint1Loc();
            pointIndex = findMatchingPointIndex(track.mTrack.getPoints(), nextNavPoint, track.mPreviousFoundIndex);
        }

        if (pointIndex < 0) {
            return OFF_TRK;
        }
        return null;
    }

    private static TrackData getActiveTrack(@NonNull LocusCache locusCache, UpdateContainer updateContainer) throws RequiredDataMissingException {
        try {
            Track newTrack = null;
            long guideTargetId = updateContainer.getGuideTargetId();
            if (guideTargetId != -1L) {
                newTrack = ActionBasics.INSTANCE.getTrack(locusCache.getApplicationContext(), locusCache.requireLocusVersion(), guideTargetId);
            }

            if (!isSameTrack(locusCache.mLastSelectedTrack.mTrack, newTrack)) {
                //recalculate or clear if null
                return new TrackData(newTrack);
            }
        } catch (RequiredVersionMissingException ignored) {
        }

        return null;
    }

    private static boolean isSameTrack(Track track1, Track track2) {
        if ((track1 == null) || (track2 == null)) {
            Log.d(TAG, "is not same track because one is null"); //NON-NLS
            //is same if both null or can't be same if track2 is not null
            return track2 == null;
        }

        if (track1.getId() != track2.getId()) {
            Log.d(TAG, "is not same track because id miss match"); //NON-NLS
            return false;
        }

        if (track1.getPointsCount() != track2.getPointsCount()) {
            Log.d(TAG, "is not same track because point count miss match, " //NON-NLS
                    + track1.getPointsCount() + " != " + track2.getPointsCount());
            return false;
        }

        if (track1.getPoint(0).getHasAltitude() != track1.getPoint(0).getHasAltitude()) {
            Log.d(TAG, "is not same track because altitude miss match"); //NON-NLS
            return false;
        }
        return true;
    }

    @SuppressWarnings("FloatingPointEquality") //float equal is ok because current location uses memory references to track point location
    private static int findMatchingPointIndex(@NonNull List<Location> points, @Nullable Location current, int previousIndex) {
        if (current == null) {
            return -1;
        }

        //go back 5 points in case of wrong position
        int prevIndex = previousIndex - 5;

        int lastIndex = points.size() - 1;

        if (prevIndex < 0) {
            prevIndex = 0;
        }
        if (prevIndex > lastIndex) {
            prevIndex = lastIndex;
        }

        for (int i = prevIndex; i <= lastIndex; i++) {
            Location loc = points.get(i);
            if ((current.getLongitude() == loc.getLongitude()) && (current.getLatitude() == loc.getLatitude())) {
                return i;
            }
        }

        //not found ahead, go backwards
        for (int i = prevIndex; i >= 0; i--) {
            Location loc = points.get(i);
            if ((current.getLongitude() == loc.getLongitude()) && (current.getLatitude() == loc.getLatitude())) {
                return i;
            }
        }

        return -1;
    }

}
