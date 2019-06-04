package falcosc.locus.addon.tasker.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import locus.api.android.ActionBasics;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Track;

import java.util.List;

final class NavigationProgress {
    private int mRemainingUphill;
    private int mRemainingDownhill;
    int pointIndex = -1;
    String trackName;
    private final String mError;

    private static final String NO_TRK = "noTRK"; //NON-NLS
    private static final String NO_NAV = "noNAV"; //NON-NLS
    private static final String NO_ELE = "noELE"; //NON-NLS
    private static final String OFF_TRK = "offTRK"; //NON-NLS
    private static final String RESET = "RESET"; //NON-NLS
    private static final String TAG = "CalcElevationToTarget"; //NON-NLS

    private NavigationProgress() {
        mError = null;
    }

    private NavigationProgress(@NonNull String error) {
        mError = error;
    }

    String getRemainingUphill() {
        if (mError != null) {
            return mError;
        }
        return Integer.toString(mRemainingUphill);
    }

    String getRemainingDownhill() {
        if (mError != null) {
            return mError;
        }
        return Integer.toString(mRemainingDownhill);
    }

    static NavigationProgress calculate(@NonNull UpdateContainer updateContainer) {

        LocusCache locusCache = LocusCache.getInstanceNullable();

        if (locusCache == null) {
            Log.e(TAG, "locus cache missing"); //NON-NLS
            return new NavigationProgress();
        }

        try {

            Track track = getActiveTrack(locusCache, updateContainer);

            if ((updateContainer.getGuideType() != UpdateContainer.GUIDE_TYPE_TRACK_GUIDE)
                    && (updateContainer.getGuideType() != UpdateContainer.GUIDE_TYPE_TRACK_NAVIGATION)) {
                return new NavigationProgress(NO_NAV);
            }

            if (track == null) {
                return new NavigationProgress(NO_TRK);
            }

            Location nextPoint = updateContainer.getGuideWptLoc();
            int currentIndex = findMatchingPointIndex(track.getPoints(), nextPoint, locusCache.mLastIndexOnRemainingTrack);
            if (currentIndex < 0) {
                //point not found on track, try to find the nav point because this may align because the selected track has less points
                Location nextNavPoint = updateContainer.getGuideNavPoint1Loc();
                currentIndex = findMatchingPointIndex(track.getPoints(), nextNavPoint, locusCache.mLastIndexOnRemainingTrack);
            }

            return setCalculationResult(locusCache, track, currentIndex);


        } catch (Exception e) {
            Log.e(TAG, "Can not get remaining elevation", e); //NON-NLS
        }

        //Error case
        //Don't search for the current track, this is done at the entry point of a new update request
        //reset track on error
        locusCache.setLastSelectedTrack(null);
        return new NavigationProgress(RESET);
    }

    private static NavigationProgress setCalculationResult(@NonNull LocusCache cache, @NonNull Track track, int currentIndex) {
        if (currentIndex < 0) {
            return new NavigationProgress(OFF_TRK);
        }

        NavigationProgress progress;
        progress = (cache.mRemainingTrackElevation[0].remainingUphill == 0) ? new NavigationProgress(NO_ELE) : new NavigationProgress();

        cache.mLastIndexOnRemainingTrack = currentIndex;
        progress.pointIndex = currentIndex;
        progress.mRemainingUphill = cache.mRemainingTrackElevation[currentIndex].remainingUphill;
        progress.mRemainingDownhill = cache.mRemainingTrackElevation[currentIndex].remainingDownhill;
        progress.trackName = track.getName();
        return progress;
    }

    private static Track getActiveTrack(@NonNull LocusCache locusCache, UpdateContainer updateContainer) {
        try {
            Track newTrack = null;
            long guideTargetId = updateContainer.getGuideTargetId();
            if (guideTargetId != -1L) {
                //TODO why is getTrack not static?
                newTrack = ActionBasics.INSTANCE.getTrack(locusCache.getApplicationContext(), locusCache.mLocusVersion, guideTargetId);
            }

            Track lastSelectedTrack = locusCache.getLastSelectedTrack();

            if (!isSameTrack(lastSelectedTrack, newTrack)) {
                //recalculate or clear if null
                locusCache.setLastSelectedTrack(newTrack);
            }
        } catch (RequiredVersionMissingException e) {
            return null;
        }

        return locusCache.getLastSelectedTrack();
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

        if (track1.getPoint(0).hasAltitude() != track1.getPoint(0).hasAltitude()) {
            Log.d(TAG, "is not same track because altitude miss match"); //NON-NLS
            return false;
        }
        return true;
    }

    @SuppressWarnings("FloatingPointEquality")
    private static int findMatchingPointIndex(@NonNull List<Location> points, @Nullable Location current, int previousIndex) {
        if(current == null){
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
            if ((current.longitude == loc.longitude) && (current.latitude == loc.latitude)) {
                return i;
            }
        }

        //not found ahead, go backwards
        for (int i = prevIndex; i >= 0; i--) {
            Location loc = points.get(i);
            if ((current.longitude == loc.longitude) && (current.latitude == loc.latitude)) {
                return i;
            }
        }

        return -1;
    }

    static class Point {
        int remainingUphill;
        int remainingDownhill;
    }

    static Point[] calculateRemainingElevation(@Nullable Track track) {
        if (track == null) {
            return new Point[0];
        }

        Log.i(TAG, "recalculate track elevation of: " + track.getName()); //NON-NLS

        List<Location> points = track.getPoints();
        int size = points.size();
        //make array one point larger because we assign remaining elevation to point+1 because remain is current target point -1
        Point[] remainingElevation = new Point[size + 1];
        Double uphillElevation = 0.0;
        Double downhillElevation = 0.0;
        double nextAltitude = points.get(size - 1).getAltitude();
        for (int i = size - 1; i >= 0; i--) {
            double currentAltitude = points.get(i).getAltitude();
            if (nextAltitude > currentAltitude) {
                uphillElevation += nextAltitude - currentAltitude;
            } else {
                downhillElevation += currentAltitude - nextAltitude;
            }

            Point p = new Point();
            p.remainingUphill = uphillElevation.intValue();
            p.remainingDownhill = downhillElevation.intValue();
            remainingElevation[i + 1] = p;

            nextAltitude = currentAltitude;
        }
        //assign remaining altitude of point 0 because we have no values at 0 because we read 1 point ahead.
        remainingElevation[0] = remainingElevation[1];

        Log.i(TAG, "Points calculated: " + size); //NON-NLS

        return remainingElevation;
    }


}
