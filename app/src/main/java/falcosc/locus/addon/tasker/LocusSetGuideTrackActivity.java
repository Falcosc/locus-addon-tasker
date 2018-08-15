package falcosc.locus.addon.tasker;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.LocusField;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Track;

import java.util.List;

public class LocusSetGuideTrackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocusCache locusCache = LocusCache.getInstance(this);

        try {
            locusCache.lastSelectedTrack = LocusUtils.handleIntentTrackTools(this, getIntent());
            locusCache.remainingTrackElevation = calculateRemainingElevation(locusCache.lastSelectedTrack);
        } catch (RequiredVersionMissingException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        setContentView(R.layout.calc_remain_elevation);

        String trackName = "not found"; //NON-NLS it's just an workaround
        if (locusCache.lastSelectedTrack != null) {
            trackName = locusCache.lastSelectedTrack.getName();
            trackName += " (" + locusCache.lastSelectedTrack.getId() + ")";
        }
        TextView text = findViewById(R.id.text_desc);

        String calcFieldText = "";

        LocusField field = locusCache.updateContainerFieldMap.get(LocusCache.CALC_REMAIN_UPHILL_ELEVATION);
        if (field != null) {
            calcFieldText = field.label + "(%" + field.taskerName + ")";
        }

        text.setText(getString(R.string.calc_remain_elev_description,
                trackName, getString(R.string.act_request_stats_sensors), calcFieldText));

        Button closeButton = findViewById(R.id.btnClose);
        closeButton.setOnClickListener(v -> finish());
    }

    private static int[] calculateRemainingElevation(Track track) {
        //TODO reverse track calculation but track with name "navigation" is never reverse

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
