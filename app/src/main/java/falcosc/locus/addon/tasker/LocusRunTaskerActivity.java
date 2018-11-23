package falcosc.locus.addon.tasker;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import falcosc.locus.addon.tasker.thridparty.TaskerIntent;
import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.utils.LocusConst;
import locus.api.android.utils.LocusUtils;
import locus.api.objects.GeoData;
import locus.api.objects.extra.GeoDataExtra;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Point;
import locus.api.objects.extra.Track;
import locus.api.objects.extra.TrackStats;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class LocusRunTaskerActivity extends ProjectActivity {

    private static final String TAG = "LocusRunTaskerActivity"; //NON-NLS
    private static final Pattern NON_WORD_CHAR_PATTERN = Pattern.compile("[^\\w]");  //NON-NLS

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.run_task);

        addTaskButtons(findViewById(R.id.linearContent));

        Button closeButton = findViewById(R.id.btnClose);
        closeButton.setOnClickListener(v -> finish());

        Button shareButton = findViewById(R.id.btnShare);
        shareButton.setOnClickListener(v -> openWebPage(getString(R.string.issues_link)));

        Set<String> implementedActions = new HashSet<>();
        implementedActions.add(LocusConst.INTENT_ITEM_TRACK_TOOLS);
        implementedActions.add(LocusConst.INTENT_ITEM_POINT_TOOLS);

        if(implementedActions.contains(getIntent().getAction())){
            findViewById(R.id.not_implemented).setVisibility(View.GONE);
        }
    }

    private void addTaskButtons(@NonNull ViewGroup viewGroup) {

        LayoutInflater inflater = LayoutInflater.from(this);

        try (Cursor cursor = getContentResolver().query(Uri.parse("content://net.dinglisch.android.tasker/tasks"), //NON-NLS
                null, null, null, null)) {
            assert cursor != null;
            int nameCol = cursor.getColumnIndex("name"); //NON-NLS

            while (cursor.moveToNext()) {
                String task = cursor.getString(nameCol);

                View view = inflater.inflate(R.layout.list_btn, viewGroup, false);
                Button taskBtn = view.findViewById(R.id.listBtn);
                taskBtn.setText(task);
                taskBtn.setOnClickListener(v -> startTask(task));
                viewGroup.addView(view);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void openWebPage(@NonNull String url) {
        Uri webPage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webPage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void startTask(@NonNull String taskName) {
        TaskerIntent.Status taskerStatus = TaskerIntent.testStatus(this);
        if (taskerStatus == TaskerIntent.Status.OK) {
            TaskerIntent taskerIntent = new TaskerIntent(taskName);
            Intent locusIntent = getIntent();

            LinkedHashMap<String, String> allIntentFields = getAllIntentFields(locusIntent);

            String action = locusIntent.getAction();
            allIntentFields.put("action", action); //NON-NLS

            LocusCache locusCache = LocusCache.getInstanceNullable();
            if (locusCache != null) {

                try {
                    if (LocusUtils.isIntentPointTools(locusIntent)) {
                        Point p = LocusUtils.handleIntentPointTools(locusCache.getApplicationContext(), locusIntent);
                        allIntentFields.putAll(mapPointFields(p));
                    } else if (LocusUtils.isIntentTrackTools(locusIntent)) {
                        Track t = LocusUtils.handleIntentTrackTools(locusCache.getApplicationContext(), locusIntent);
                        allIntentFields.putAll(mapTrackFields(t));
                    }
                    allIntentFields.values().removeAll(Arrays.asList(null, ""));
                    Log.d(TAG, "Map: " + allIntentFields); //NON-NLS
                } catch (Exception e) {
                    Log.e(TAG, "Can't get intent details", e); //NON-NLS
                }

                taskerIntent.addLocalVariable("%data", new JSONObject(allIntentFields).toString()); //NON-NLS
                taskerIntent.addLocalVariable("%fields", StringUtils.join(allIntentFields.keySet(), ',')); //NON-NLS
                for (Map.Entry<String, String> e : allIntentFields.entrySet()) {
                    taskerIntent.addLocalVariable("%" + e.getKey(), e.getValue());
                }
                sendBroadcast(taskerIntent);
            }
        } else {
            Toast.makeText(this, getString(R.string.err_cant_start_task) + " " + taskerStatus.name(), Toast.LENGTH_LONG).show();
        }
        finish();
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private static LinkedHashMap<String, String> mapLocationFields(String prefix, Location loc) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(prefix + "lon", Double.toString(loc.getLongitude()));
        map.put(prefix + "lat", Double.toString(loc.getLatitude()));
        map.put(prefix + "time", Long.toString(loc.getTime()));
        map.put(prefix + "altitude", Double.toString(loc.getAltitude()));
        return map;
    }

    private static List<String> convertToTexts(List<GeoDataExtra.LabelTextContainer> data) {
        List<String> result = new ArrayList<>();
        for (int i = 0, m = data.size(); i < m; i++) {
            result.add(data.get(i).text);
        }
        return result;
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private static LinkedHashMap<String, String> mapGeoDataFields(String prefix, GeoData g) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(prefix + "name", g.getName());
        map.put(prefix + "param_desc", g.getParameterDescription());
        map.put(prefix + "param_style", g.getParameterStyleName());
        map.put(prefix + "id", Long.toString(g.getId()));
        map.put(prefix + "param_rte_action", g.getParameterRteAction().name());
        map.put(prefix + "param_rte_index", Integer.toString(g.getParamRteIndex()));
        map.put(prefix + "time_created", Long.toString(g.getTimeCreated()));
        map.put(prefix + "is_enabled", Boolean.toString(g.isEnabled()));
        map.put(prefix + "is_visible", Boolean.toString(g.isVisible()));
        map.put(prefix + "is_selected", Boolean.toString(g.isSelected()));
        int extraCount = g.extraData.getCount();
        map.put(prefix + "extra_count", Integer.toString(extraCount));
        if (extraCount > 0) {
            map.put(prefix + "extra_emails", StringUtils.join(convertToTexts(g.extraData.getEmails()), ','));
            map.put(prefix + "extra_phones", StringUtils.join(convertToTexts(g.extraData.getPhones()), ','));
            map.put(prefix + "extra_urls", StringUtils.join(convertToTexts(g.extraData.getUrls()), ','));
            map.put(prefix + "extra_attachments", StringUtils.join(g.extraData.getAllAttachments(), ','));
        }
        map.put(prefix + "extra_emails", g.getName());
        map.put(prefix + "name", g.getName());
        map.put(prefix + "name", g.getName());

        return map;
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private static LinkedHashMap<String, String> mapTrackStatsFields(String prefix, TrackStats stats) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(prefix + "total_length", Float.toString(stats.getTotalLength()));
        map.put(prefix + "eleva_neg_length", Float.toString(stats.getEleNegativeDistance()));
        map.put(prefix + "eleva_pos_length", Float.toString(stats.getElePositiveDistance()));
        map.put(prefix + "eleva_neutral_length", Float.toString(stats.getEleNeutralDistance()));
        map.put(prefix + "eleva_neutral_height", Float.toString(stats.getEleNeutralHeight()));
        map.put(prefix + "eleva_downhill", Float.toString(stats.getEleNegativeHeight()));
        map.put(prefix + "eleva_uphill", Float.toString(stats.getElePositiveHeight()));
        map.put(prefix + "altitude_min", Float.toString(stats.getAltitudeMin()));
        map.put(prefix + "altitude_max", Float.toString(stats.getAltitudeMax()));
        map.put(prefix + "start_time", Long.toString(stats.getStartTime()));
        map.put(prefix + "stop_time", Long.toString(stats.getStopTime()));
        map.put(prefix + "time", Long.toString(stats.getTotalTime()));
        map.put(prefix + "time_move", Long.toString(stats.getTotalTimeMove()));
        map.put(prefix + "average_speed_total", Float.toString(stats.getSpeedAverage(false)));
        map.put(prefix + "average_speed_move", Float.toString(stats.getSpeedAverage(true)));
        map.put(prefix + "point_count", Integer.toString(stats.getNumOfPoints()));
        map.put(prefix + "cadence_avg", Integer.toString(stats.getCadenceAverage()));
        map.put(prefix + "cadence_max", Integer.toString(stats.getCadenceMax()));
        map.put(prefix + "energy_burned", Integer.toString(stats.getEnergy()));
        map.put(prefix + "hrm_avg", Integer.toString(stats.getHrmAverage()));
        map.put(prefix + "hrm_max", Integer.toString(stats.getHrmMax()));
        map.put(prefix + "strides_count", Integer.toString(stats.getNumOfStrides()));
        return map;
    }

    private static LinkedHashMap<String, String> mapPointFields(Point p) {
        String prefix = "p_"; //NON-NLS
        LinkedHashMap<String, String> map = new LinkedHashMap<>(mapGeoDataFields(prefix, p));
        map.putAll(mapLocationFields(prefix, p.getLocation()));
        return map;
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private static LinkedHashMap<String, String> mapTrackFields(Track t) {
        String prefix = "t_";
        LinkedHashMap<String, String> map = new LinkedHashMap<>(mapGeoDataFields(prefix, t));
        int pointCount = t.getPointsCount();
        map.put(prefix + "point_count", Integer.toString(pointCount));
        if (pointCount > 0) {
            map.putAll(mapLocationFields(prefix + "first_point_", t.getPoint(0)));
            map.putAll(mapLocationFields(prefix + "last_point_", t.getPoint(pointCount - 1)));
        }

        map.put(prefix + "break_count", Integer.toString(t.getBreaks().size()));
        map.put(prefix + "is_use_folder_style", Boolean.toString(t.isUseFolderStyle()));
        map.put(prefix + "activity_type", Integer.toString(t.getActivityType()));
        map.putAll(mapTrackStatsFields(prefix, t.getStats()));
        return map;
    }


    @NonNull
    private static LinkedHashMap<String, String> getAllIntentFields(@NonNull Intent intent) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value != null) {
                    String taskerKey = key + "_" + value.getClass().getSimpleName();
                    map.put(NON_WORD_CHAR_PATTERN.matcher(taskerKey).replaceAll(""), value.toString());
                }
            }
        }

        return map;

    }
}
