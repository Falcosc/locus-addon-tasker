package falcosc.locus.addon.tasker.utils;

import android.arch.core.util.Function;
import android.util.Log;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Track;

import java.util.List;

class CalculateElevationToTarget implements Function<UpdateContainer, String> {
    private static final String NO_TRK = "noTRK";
    private static final String RESET = "RESET";
    private static final String TAG = "CalcElevationToTarget";

    @Override
    public String apply(UpdateContainer updateContainer) {

        LocusCache locusCache = LocusCache.getInstanceNullable();

        try {
            Track track = locusCache.getLastSelectedTrack();

            if (track == null) {
                //Don't search for the current track, this is done at the entry point of a new update request
                return NO_TRK;
            }

            Location nextPoint = updateContainer.getGuideTypeTrack().getTargetLoc();
            int currentIndex = findMatchingPointIndex(track.getPoints(), nextPoint, locusCache.lastIndexOnRemainingTrack);
            if (currentIndex < 0) {
                //point not found on track, try to find the nav point because this may align because the selected track has less points
                Location nextNavPoint = updateContainer.getGuideTypeTrack().getNavPoint1Loc();
                currentIndex = findMatchingPointIndex(track.getPoints(), nextNavPoint, locusCache.lastIndexOnRemainingTrack);
            }

            if (currentIndex >= 0) {
                locusCache.lastIndexOnRemainingTrack = currentIndex;
                return String.valueOf(locusCache.remainingTrackElevation[currentIndex]);
            }

        } catch (Exception e) {
            Log.e(TAG, "Can not get remaining elevation", e); //NON-NLS
        }

        locusCache.setLastSelectedTrack(null);
        //Don't search for the current track, this is done at the entry point of a new update request

        //tracking is off on exception or we are not on track
        return RESET;
    }

    private int findMatchingPointIndex(List<Location> points, Location current, int previousIndex) {
        //go back 5 points in case of wrong position
        previousIndex -= 5;

        int lastIndex = points.size() - 1;

        if (previousIndex < 0) {
            previousIndex = 0;
        }
        if (previousIndex > lastIndex) {
            previousIndex = lastIndex;
        }

        for (int i = previousIndex; i <= lastIndex; i++) {
            Location loc = points.get(i);
            if (current.longitude == loc.longitude && current.latitude == loc.latitude) {
                return i;
            }
        }

        //not found ahead, go backwards
        for (int i = previousIndex; i >= 0; i--) {
            Location loc = points.get(i);
            if (current.longitude == loc.longitude && current.latitude == loc.latitude) {
                return i;
            }
        }

        return -1;
    }

    public static int[] calculateRemainingElevation(Track track) {
        if (track == null) {
            return new int[0];
        }

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
