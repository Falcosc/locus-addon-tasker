package falcosc.locus.addon.tasker.utils;

import android.arch.core.util.Function;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Track;

import java.util.List;

public class CalculateElevationToTarget implements Function<UpdateContainer, String> {
    @Override
    public String apply(UpdateContainer updateContainer) {

        try {
            Track track = LocusCache.getInstanceNullable().lastSelectedTrack;

            if (track == null) {
                return "noTRK";
            }

            //TODO reverse track calculation

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

            Location nextGuidLocation = updateContainer.getGuideTypeTrack().getNavPoint1Loc();
            Location targetLocation = updateContainer.getGuideTypeTrack().getTargetLoc();

            int direction = getTrackDirection(targetLocation, track);

            if (direction == 0) {
                //wrong track
                return "wrong";
            }

            int currentIndex = findMatchingPointIntex(points, nextGuidLocation);
            if (currentIndex >= 0) {
                return String.valueOf(remainingUphill[currentIndex]);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        //tracking is off on exception or we are not on track
        return "offTRK";
    }

    private int getTrackDirection(Location targetLocation, Track track) {
        List<Location> points = track.getPoints();
        Location firstLoc = points.get(0);
        Location lastLoc = points.get(points.size() - 1);

        if (lastLoc.latitude == targetLocation.latitude && lastLoc.longitude == targetLocation.longitude) {
            return 1;
        }

        if (firstLoc.latitude == targetLocation.latitude && firstLoc.longitude == targetLocation.longitude) {
            return -1;
        }

        //is not the guiding track
        return 0;
    }

    private int findMatchingPointIntex(List<Location> points, Location current) {
        for (int i = points.size() - 1; i >= 0; i--) {
            Location loc = points.get(i);
            if (current.longitude == loc.longitude && current.latitude == loc.latitude) {
                return i;
            }
        }

        return -1;
    }
}
