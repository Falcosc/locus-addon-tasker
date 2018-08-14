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

public class LocusSetGuideTrackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocusCache locusCache = LocusCache.getInstance(this);

        try {
            locusCache.lastSelectedTrack = LocusUtils.handleIntentTrackTools(this, getIntent());
        } catch (RequiredVersionMissingException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        setContentView(R.layout.calc_remain_elevation);

        String trackName = "not found"; //NON-NLS it's just an workaround
        if (locusCache.lastSelectedTrack != null) {
            trackName = locusCache.lastSelectedTrack.getName();
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
}
