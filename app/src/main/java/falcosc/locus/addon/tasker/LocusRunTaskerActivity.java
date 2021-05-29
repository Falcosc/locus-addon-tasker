package falcosc.locus.addon.tasker;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import falcosc.locus.addon.tasker.thridparty.TaskerIntent;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.ReportingHelper;
import locus.api.android.utils.IntentHelper;
import locus.api.android.utils.LocusConst;
import locus.api.objects.extra.GeoDataExtra;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.TrackStats;
import locus.api.objects.geoData.GeoData;
import locus.api.objects.geoData.Point;
import locus.api.objects.geoData.Track;
import locus.api.utils.Utils;

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

        if (implementedActions.contains(getIntent().getAction())) {
            findViewById(R.id.not_implemented).setVisibility(View.GONE);
        }
    }

    private void addTaskButtons(@NonNull ViewGroup viewGroup) {

        LayoutInflater inflater = LayoutInflater.from(this);

        try (Cursor cursor = getContentResolver().query(Uri.parse("content://net.dinglisch.android.tasker/tasks"), //NON-NLS
                null, null, null, null)) {
            if (cursor != null) {
                int nameCol = cursor.getColumnIndex("name"); //NON-NLS

                while (cursor.moveToNext()) {
                    String task = cursor.getString(nameCol);

                    View view = inflater.inflate(R.layout.list_btn, viewGroup, false);
                    Button taskBtn = view.findViewById(R.id.listBtn);
                    taskBtn.setText(task);
                    taskBtn.setOnClickListener(v -> startTask(task));
                    viewGroup.addView(view);
                }
            } else {
                findViewById(R.id.not_implemented).setVisibility(View.GONE);
                TextView text = findViewById(R.id.execute_tasks);
                text.setText(R.string.err_tasker_external_access);
            }
        } catch (Exception e) {
            Log.e(TAG, ReportingHelper.getUserFriendlyName(e), e);
            new ReportingHelper(this).sendErrorNotification(TAG, "Can't create Buttons for Tasks", e); //NON-NLS
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
                    if (IntentHelper.INSTANCE.isIntentPointTools(locusIntent)) {
                        Point p = IntentHelper.INSTANCE.getPointFromIntent(locusCache.getApplicationContext(), locusIntent);
                        allIntentFields.putAll(mapPointFields(p, "p_")); //NON-NLS
                    } else if (IntentHelper.INSTANCE.isIntentTrackTools(locusIntent)) {
                        Track t = IntentHelper.INSTANCE.getTrackFromIntent(locusCache.getApplicationContext(), locusIntent);
                        allIntentFields.putAll(mapTrackFields(t));
                    }
                    allIntentFields.values().removeAll(Arrays.asList(null, ""));
                    Log.d(TAG, "Map: " + allIntentFields); //NON-NLS
                } catch (Exception e) {
                    new ReportingHelper(this).sendErrorNotification(TAG, "Can't get intent details", e); //NON-NLS
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
            result.add(data.get(i).getText());
        }
        return result;
    }

    private static final int[] EXTRA_DATA_PAR_KEYS = {
            GeoDataExtra.PAR_SOURCE,
            GeoDataExtra.PAR_STYLE_NAME,
            GeoDataExtra.PAR_AREA_SIZE,
            GeoDataExtra.PAR_DB_POI_EXTRA_DATA,
            GeoDataExtra.PAR_KML_TRIP_ID,
            GeoDataExtra.PAR_GOOGLE_PLACES_REFERENCE,
            GeoDataExtra.PAR_GOOGLE_PLACES_RATING,
            GeoDataExtra.PAR_GOOGLE_PLACES_DETAILS,
            GeoDataExtra.PAR_INTENT_EXTRA_CALLBACK,
            GeoDataExtra.PAR_INTENT_EXTRA_ON_DISPLAY,
            GeoDataExtra.PAR_DESCRIPTION,
            GeoDataExtra.PAR_COMMENT,
            GeoDataExtra.PAR_RELATIVE_WORKING_DIR,
            GeoDataExtra.PAR_TYPE,
            GeoDataExtra.PAR_GEOCACHE_CODE,
            GeoDataExtra.PAR_POI_ALERT_INCLUDE,
            GeoDataExtra.PAR_ADDRESS_STREET,
            GeoDataExtra.PAR_ADDRESS_CITY,
            GeoDataExtra.PAR_ADDRESS_REGION,
            GeoDataExtra.PAR_ADDRESS_POST_CODE,
            GeoDataExtra.PAR_ADDRESS_COUNTRY,
            GeoDataExtra.PAR_RTE_INDEX,
            GeoDataExtra.PAR_RTE_DISTANCE_F,
            GeoDataExtra.PAR_RTE_TIME_I,
            GeoDataExtra.PAR_RTE_SPEED_F,
            GeoDataExtra.PAR_RTE_TURN_COST,
            GeoDataExtra.PAR_RTE_STREET,
            GeoDataExtra.PAR_RTE_POINT_ACTION,
            GeoDataExtra.PAR_RTE_COMPUTE_TYPE,
            GeoDataExtra.PAR_RTE_SIMPLE_ROUNDABOUTS,
            GeoDataExtra.PAR_RTE_PLAN_DEFINITION,
            GeoDataExtra.PAR_OSM_NOTES_ID,
            GeoDataExtra.PAR_OSM_NOTES_CLOSED
    };

    private static String getExtraDataAsJSON(GeoDataExtra extraData) {
        JSONStringer stringer = new JSONStringer();
        try {
            stringer.object();
            for (int key : EXTRA_DATA_PAR_KEYS) {
                byte[] data = extraData.getParameterRaw(key);
                if (data != null) {
                    stringer.key(String.valueOf(key));
                    stringer.value(Utils.INSTANCE.doBytesToString(data));
                }
            }
            stringer.endObject();
        } catch (JSONException e) {
            Log.e(TAG, "can not create extra data json", e); //NON-NLS
        }

        return stringer.toString();
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


        GeoDataExtra extraData = g.getExtraData();
        if (extraData != null) {
            int extraCount = extraData.getCount();
            map.put(prefix + "extra_count", Integer.toString(extraCount));
            if (extraCount > 0) {
                map.put(prefix + "extra_data", getExtraDataAsJSON(extraData));
                map.put(prefix + "extra_emails", StringUtils.join(convertToTexts(extraData.getAttachments(GeoDataExtra.AttachType.EMAIL)), ','));
                map.put(prefix + "extra_phones", StringUtils.join(convertToTexts(extraData.getAttachments(GeoDataExtra.AttachType.PHONE)), ','));
                map.put(prefix + "extra_urls", StringUtils.join(convertToTexts(extraData.getAttachments(GeoDataExtra.AttachType.URL)), ','));
                map.put(prefix + "extra_attachments", StringUtils.join(extraData.getAllAttachments(), ','));
            }
        }
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
        map.put(prefix + "hrm_avg", Integer.toString(stats.getHeartRateAverage()));
        map.put(prefix + "hrm_max", Integer.toString(stats.getHeartRateMax()));
        map.put(prefix + "strides_count", Integer.toString(stats.getNumOfStrides()));
        return map;
    }

    public static LinkedHashMap<String, String> mapPointFields(Point p, String prefix) {
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
