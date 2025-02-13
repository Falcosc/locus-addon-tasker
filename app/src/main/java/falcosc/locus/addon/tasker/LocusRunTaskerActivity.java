package falcosc.locus.addon.tasker;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.asamm.logger.Logger;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import falcosc.locus.addon.tasker.settings.SettingsActivity;
import falcosc.locus.addon.tasker.thridparty.TaskerIntent;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.ReportingHelper;
import locus.api.android.utils.IntentHelper;
import locus.api.android.utils.LocusConst;
import locus.api.objects.extra.GeoDataExtra;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.TrackStats;
import locus.api.objects.geoData.GeoData;
import locus.api.objects.geoData.GeoDataHelperKt;
import locus.api.objects.geoData.Point;
import locus.api.objects.geoData.Track;
import locus.api.utils.Utils;

public class LocusRunTaskerActivity extends ProjectActivity {

    private static final String TAG = "LocusRunTaskerActivity"; //NON-NLS
    private static final Pattern NON_WORD_CHAR_PATTERN = Pattern.compile("[^\\w]");  //NON-NLS
    private static final String KEY_SUFFIX_FILTER = "_filter"; //NON-NLS
    private TextView mMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.run_task);
        mMessage = findViewById(R.id.message);

        Set<String> implementedActions = new HashSet<>();
        implementedActions.add(LocusConst.INTENT_ITEM_TRACK_TOOLS);
        implementedActions.add(LocusConst.INTENT_ITEM_POINT_TOOLS);

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("RunTasker_showHelp", true)) { //NON-NLS
            if (!implementedActions.contains(getIntent().getAction())) {
                mMessage.setText(R.string.run_task_not_implemented);
            }
        } else {
            //hide text before checking for errors because errors my want to make it visible again
            mMessage.setVisibility(View.GONE);
        }


        List<String> taskNames = queryTaskNames(getTaskFilter());
        if (!taskNames.isEmpty()) {
            if (taskNames.size() == 1) {
                startTask(taskNames.get(0));
            } else {
                addTaskButtons(findViewById(R.id.linearContent), taskNames);
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_runtask, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        if (id == R.id.share) {
            openWebPage(getString(R.string.issues_link));
            return true;
        }
        if (id == R.id.close) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    Pattern getTaskFilter() {
        String key = getIntent().getComponent().getShortClassName() + KEY_SUFFIX_FILTER;
        key = key.substring(1);
        return Pattern.compile(PreferenceManager.getDefaultSharedPreferences(this).getString(key, ".*"));
    }

    List<String> queryTaskNames(Pattern pattern) {
        List<String> taskNames = new ArrayList<>();
        List<String> excludedTasks = new ArrayList<>();
        try (Cursor cursor = getContentResolver().query(Uri.parse("content://net.dinglisch.android.tasker/tasks"), //NON-NLS
                null, null, null, null)) {
            if (cursor != null) {
                int nameCol = cursor.getColumnIndex("name"); //NON-NLS
                int projNameCol = cursor.getColumnIndex("project_name"); //NON-NLS

                while (cursor.moveToNext()) {
                    String task = cursor.getString(nameCol);
                    String prjName = cursor.getString(projNameCol);
                    String combinedName = prjName + "/" + task;
                    if (pattern.matcher(combinedName).matches()) {
                        taskNames.add(task);
                    } else {
                        excludedTasks.add(combinedName);
                    }
                }
                if (taskNames.isEmpty()) {
                    String message = getString(R.string.run_task_no_match, pattern.pattern());
                    if (!excludedTasks.isEmpty()) {
                        Collections.sort(excludedTasks);
                        message += "\n" + getString(R.string.run_task_excluded_tasks)
                                + "\n\t• " + StringUtils.join(excludedTasks, "\n\t• ");
                    }
                    mMessage.setText(message);
                    mMessage.setVisibility(View.VISIBLE);

                }
            } else {
                mMessage.setText(R.string.err_tasker_external_access);
                mMessage.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Logger.e(e, TAG, ReportingHelper.getUserFriendlyName(e));
            new ReportingHelper(this).sendErrorNotification(TAG, "Can't create Buttons for Tasks", e); //NON-NLS
        }
        return taskNames;
    }

    private void addTaskButtons(@NonNull ViewGroup viewGroup, List<String> taskNames) {

        LayoutInflater inflater = LayoutInflater.from(this);

        for (String taskName : taskNames) {
            View view = inflater.inflate(R.layout.list_btn, viewGroup, false);
            Button taskBtn = view.findViewById(R.id.listBtn);
            taskBtn.setText(taskName);
            taskBtn.setOnClickListener(v -> startTask(taskName));
            viewGroup.addView(view);
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

            LinkedHashMap<String, Object> allIntentFields = getAllIntentFields(locusIntent);

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
                    Logger.d(TAG, "Map: " + allIntentFields); //NON-NLS
                } catch (Exception e) {
                    new ReportingHelper(this).sendErrorNotification(TAG, "Can't get intent details", e); //NON-NLS
                }

                taskerIntent.addLocalVariable("%data", new JSONObject(allIntentFields).toString()); //NON-NLS
                taskerIntent.addLocalVariable("%fields", StringUtils.join(allIntentFields.keySet(), ',')); //NON-NLS
                for (Map.Entry<String, Object> e : allIntentFields.entrySet()) {
                    taskerIntent.addLocalVariable("%" + e.getKey(), e.getValue().toString());
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
        map.put(prefix + "altitude", String.valueOf(loc.getAltitude()));
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

    private static JSONObject getExtraDataAsJSON(GeoDataExtra extraData) {
        JSONObject jsonObject = new JSONObject();
        try {
            for (int key : EXTRA_DATA_PAR_KEYS) {
                byte[] data = extraData.getParameterRaw(key);
                if (data != null) {
                    jsonObject.put(String.valueOf(key), Utils.INSTANCE.doBytesToString(data));
                }
            }
        } catch (JSONException e) {
            Logger.e(e, TAG, "can not create extra data json"); //NON-NLS
        }

        return jsonObject;
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private static LinkedHashMap<String, Object> mapGeoDataFields(String prefix, GeoData g) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        map.put(prefix + "name", g.getName());
        map.put(prefix + "param_desc", GeoDataHelperKt.getParameterDescription(g));
        map.put(prefix + "param_style", GeoDataHelperKt.getParameterStyleName(g));
        map.put(prefix + "id", Long.toString(g.getId()));
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

    public static LinkedHashMap<String, Object> mapPointFields(Point p, String prefix) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>(mapGeoDataFields(prefix, p));
        map.putAll(mapLocationFields(prefix, p.getLocation()));
        return map;
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private static LinkedHashMap<String, Object> mapTrackFields(Track t) {
        String prefix = "t_";
        LinkedHashMap<String, Object> map = new LinkedHashMap<>(mapGeoDataFields(prefix, t));
        int pointCount = t.getPointsCount();
        map.put(prefix + "point_count", Integer.toString(pointCount));
        if (pointCount > 0) {
            map.putAll(mapLocationFields(prefix + "first_point_", t.getPoint(0)));
            map.putAll(mapLocationFields(prefix + "last_point_", t.getPoint(pointCount - 1)));
        }

        map.put(prefix + "break_count", Integer.toString(t.getBreaks().size()));
        map.put(prefix + "is_use_folder_style", Boolean.toString(t.getUseParentLineStyle()));
        map.put(prefix + "activity_type", Integer.toString(t.getActivityType()));
        map.putAll(mapTrackStatsFields(prefix, t.getStats()));
        return map;
    }


    @NonNull
    private static LinkedHashMap<String, Object> getAllIntentFields(@NonNull Intent intent) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key); //wait until we get an alternative
                if (value != null) {
                    String taskerKey = key + "_" + value.getClass().getSimpleName();
                    map.put(NON_WORD_CHAR_PATTERN.matcher(taskerKey).replaceAll(""), value.toString());
                }
            }
        }

        return map;

    }
}
