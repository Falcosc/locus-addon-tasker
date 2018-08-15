package falcosc.locus.addon.tasker.utils;

import android.arch.core.util.Function;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Track;

import java.util.List;

class CalculateElevationToTarget implements Function<UpdateContainer, String> {
    private static final String NO_TRK = "noTRK";
    private static final String OFF_TRK = "offTRK";

    @Override
    public String apply(UpdateContainer updateContainer) {

        try {
            LocusCache locusCache = LocusCache.getInstanceNullable();
            Track track = locusCache.lastSelectedTrack;

            if (track == null) {
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
            e.printStackTrace();
        }

        //tracking is off on exception or we are not on track
        return OFF_TRK;
    }

    //TODO need to be able to reverse but track with name "navigation" is never reverse
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
}
