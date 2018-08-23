package falcosc.locus.addon.tasker;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.LocusField;
import locus.api.android.ActionTools;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Track;

public class LocusSetGuideTrackActivity extends ProjectActivity {

    private static final long VIRTUAL_TRACK_ID_OFFSET = 1000000000L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.calc_remain_elevation);

        try {
            LocusCache locusCache = LocusCache.getInstance(getApplication());

            Track track = LocusUtils.handleIntentTrackTools(this, getIntent());
            Track suggestedTrack = getSuggestedTrack(track, locusCache);

            if ((track != null) && (suggestedTrack != null)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.suggest_navigation_track);
                builder.setPositiveButton(suggestedTrack.getName(), (dialog, which) -> setTrack(suggestedTrack));
                builder.setNegativeButton(track.getName(), (dialog, which) -> setTrack(track));
                builder.create().show();
            } else {
                //track is null or we don't have a suggestion
                setTrack(track);
            }
        } catch (RequiredVersionMissingException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        Button closeButton = findViewById(R.id.btnClose);
        closeButton.setOnClickListener(v -> finish());
    }

    @Nullable
    private Track getSuggestedTrack(@Nullable Track currentSelection, @NonNull LocusCache locusCache) throws RequiredVersionMissingException {
        //noinspection CallToSuspiciousStringMethod getName and mNavigationTrackName are on same language context
        if ((currentSelection != null) && !currentSelection.getName().equalsIgnoreCase(locusCache.mNavigationTrackName)) {
            return searchNavigationTrack(locusCache, this);
        }

        return null;
    }

    private static Track searchNavigationTrack(@NonNull LocusCache locusCache, @NonNull Context context) throws RequiredVersionMissingException {

        Track track = ActionTools.getLocusTrack(context, locusCache.mLocusVersion, VIRTUAL_TRACK_ID_OFFSET + 1L);
        //noinspection CallToSuspiciousStringMethod  getName and mNavigationTrackName are on same language context
        if ((track != null) && !track.getName().equalsIgnoreCase(locusCache.mNavigationTrackName)) {
            //track found but is not navigation, check if there is a better one
            Track track2 = ActionTools.getLocusTrack(context, locusCache.mLocusVersion, VIRTUAL_TRACK_ID_OFFSET + 2L);
            //noinspection CallToSuspiciousStringMethod  getName and mNavigationTrackName are on same language context
            if ((track2 != null) && track.getName().equalsIgnoreCase(locusCache.mNavigationTrackName)) {
                //use track 2 only if it is a Navigation track, if both are not, then take the first one
                track = track2;
            }
        }
        return track;
    }

    private void setTrack(@Nullable Track track) {
        LocusCache locusCache = LocusCache.getInstance(getApplication());
        locusCache.setLastSelectedTrack(track);

        String trackName = "not found"; //NON-NLS it's just an workaround
        if (track != null) {
            trackName = track.getName();
            trackName += " (" + track.getId() + ")";
        }
        TextView text = findViewById(R.id.text_desc);

        String calcFieldText = "";

        LocusField field = locusCache.mUpdateContainerFieldMap.get(LocusCache.CALC_REMAIN_UPHILL_ELEVATION);
        if (field != null) {
            calcFieldText = field.mLabel + "(%" + field.mTaskerName + ")";
        }

        text.setText(getString(R.string.calc_remain_elev_description,
                trackName, getString(R.string.act_request_stats_sensors), calcFieldText));
    }


}
