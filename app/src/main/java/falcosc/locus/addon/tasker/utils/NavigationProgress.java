package falcosc.locus.addon.tasker.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Track;

import java.util.List;

final class NavigationProgress {
    private int mRemainingUphill;
    private int mRemainingDownhill;
    public int pointIndex = -1;
    public String trackName;
    private String mError;

    private static final String NO_TRK = "noTRK"; //NON-NLS
    private static final String NO_NAV = "noNAV"; //NON-NLS
    private static final String OFF_TRK = "offTRK"; //NON-NLS
    private static final String RESET = "RESET"; //NON-NLS
    private static final String TAG = "CalcElevationToTarget"; //NON-NLS

    private NavigationProgress() {

    }

    private NavigationProgress(String error) {
        mError = error;
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

    public static NavigationProgress calculate(@NonNull UpdateContainer updateContainer) {

        LocusCache locusCache = LocusCache.getInstanceNullable();

        if (locusCache == null) {
            Log.e(TAG, "locus cache missing"); //NON-NLS
            return new NavigationProgress();
        }

        try {

            Track track = getActiveTrack(locusCache, updateContainer);

            if (updateContainer.getGuideTypeTrack() == null) {
                return new NavigationProgress(NO_NAV);
            }

            if (track == null) {
                return new NavigationProgress(NO_TRK);
            }

            Location nextPoint = updateContainer.getGuideTypeTrack().getTargetLoc();
            int currentIndex = findMatchingPointIndex(track.getPoints(), nextPoint, locusCache.mLastIndexOnRemainingTrack);
            if (currentIndex < 0) {
                //point not found on track, try to find the nav point because this may align because the selected track has less points
                Location nextNavPoint = updateContainer.getGuideTypeTrack().getNavPoint1Loc();
                currentIndex = findMatchingPointIndex(track.getPoints(), nextNavPoint, locusCache.mLastIndexOnRemainingTrack);
            }

            if (currentIndex < 0) {
                return new NavigationProgress(OFF_TRK);
            } else {
                locusCache.mLastIndexOnRemainingTrack = currentIndex;
                NavigationProgress progress = new NavigationProgress("");
                progress.pointIndex = currentIndex;
                progress.mRemainingUphill = locusCache.mRemainingTrackElevation[currentIndex];
                //TODO downhill
                progress.mRemainingDownhill = locusCache.mRemainingTrackElevation[currentIndex];
                progress.trackName = track.getName();
                return progress;
            }

        } catch (Exception e) {
            Log.e(TAG, "Can not get remaining elevation", e); //NON-NLS
        }

        //Error case
        //Don't search for the current track, this is done at the entry point of a new update request
        //reset track on error
        locusCache.setLastSelectedTrack(null);
        return new NavigationProgress(RESET);
    }

    private static Track getActiveTrack(@NonNull LocusCache locusCache, UpdateContainer updateContainer) {
        try {
            Track newTrack = null;
            UpdateContainer.GuideTypeTrack guideTrack = updateContainer.getGuideTypeTrack();
            if (guideTrack != null) {
                newTrack = ActionTools.getLocusTrack(locusCache.getApplicationContext(), locusCache.mLocusVersion, guideTrack.getTargetId());
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
        if (track1 == null) {
            //is same if both null or can't be same if track2 is not null
            return track2 == null;
        }

        if (track1.getId() != track2.getId()) {
            return false;
        }

        //everything is equal, check track length float as last step
        return Float.compare(track1.getStats().getTotalLength(), track2.getStats().getTotalLength()) != 0;
    }

    @SuppressWarnings("FloatingPointEquality")
    private static int findMatchingPointIndex(@NonNull List<Location> points, @NonNull Location current, int previousIndex) {
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

    public static int[] calculateRemainingElevation(@Nullable Track track) {
        if (track == null) {
            return new int[0];
        }

        Log.i(TAG, "recalculate track elevation of: " + track.getName()); //NON-NLS

        List<Location> points = track.getPoints();
        int size = points.size();
        //make array one point larger because we assign remaining elevation to point+1 because remain is current target point -1
        int[] remainingUphill = new int[size + 1];
        Double uphillElevation = 0.0;
        double nextAltitude = points.get(size - 1).getAltitude();
        for (int i = size - 1; i >= 0; i--) {
            double currentAltitude = points.get(i).getAltitude();
            if (nextAltitude > currentAltitude) {
                uphillElevation += nextAltitude - currentAltitude;
            }
            remainingUphill[i + 1] = uphillElevation.intValue();
            nextAltitude = currentAltitude;
        }
        //assign remaining altitude of point 0 because we have no values at 0 because we read 1 point ahead.
        remainingUphill[0] = remainingUphill[1];

        return remainingUphill;
    }


}
