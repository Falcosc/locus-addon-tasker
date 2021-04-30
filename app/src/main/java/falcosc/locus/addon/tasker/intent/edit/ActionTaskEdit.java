package falcosc.locus.addon.tasker.intent.edit;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.Map.Entry;

import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.ReportingHelper;


public class ActionTaskEdit extends TaskerEditActivity {

    private static final String TAG = "ActionTaskDialog"; //NON-NLS
    private static final String ACTION = "action"; //NON-NLS
    private static final String ACTION_AFTER = "action_after"; //NON-NLS
    private static final String AUTO_SAVE = "auto_save"; //NON-NLS
    private static final String ALLOW_START = "allow_start"; //NON-NLS
    private static final String NAME = "name"; //NON-NLS
    private static final String START = "start"; //NON-NLS
    private static final String VALUE = "value"; //NON-NLS
    private static final String UNIT = "unit"; //NON-NLS
    private static final String LON = "lon"; //NON-NLS
    private static final String LAT = "lat"; //NON-NLS


    static class Dashboard extends ActionTypeJSON {
        Dashboard(@NonNull View view, @Nullable Dialog varSelectDialog) {
            super(view.findViewById(R.id.dashboard), view.findViewById(R.id.dashboard_content));
            Spinner spinner = view.findViewById(R.id.dashboard_spinner);
            spinner.setOnItemSelectedListener(onItemSelected);
            EditText text = view.findViewById(R.id.dashboard_text);
            setVarSelectDialog(varSelectDialog, text, view.findViewById(R.id.dashboard_var));

            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
            bindKey(NAME, (v) -> text.setText((CharSequence) v), text::getText);
        }
    }

    static class OpenFunction extends ActionTypeJSON {
        OpenFunction(@NonNull View view) {
            super(view.findViewById(R.id.function), view.findViewById(R.id.function_content));
            Spinner spinner = (Spinner) mContent;
            spinner.setOnItemSelectedListener(onItemSelected);
            bindKey(VALUE, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
        }
    }

    static class LiveTrackingAsamm extends ActionTypeJSON {
        LiveTrackingAsamm(@NonNull View view, @Nullable Dialog varSelectDialog) {
            super(view.findViewById(R.id.live_tracking_asamm), view.findViewById(R.id.live_tracking_asamm_content));
            Spinner spinner = view.findViewById(R.id.live_tracking_asamm_spinner);
            spinner.setOnItemSelectedListener(onItemSelected);
            EditText text = view.findViewById(R.id.live_tracking_asamm_text);
            setVarSelectDialog(varSelectDialog, text, view.findViewById(R.id.live_tracking_asamm_var));

            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
            bindKey(NAME, (v) -> text.setText((CharSequence) v), text::getText);
        }

    }

    static class LiveTrackingCustom extends ActionTypeJSON {
        LiveTrackingCustom(@NonNull View view, @Nullable Dialog varSelectDialog) {
            super(view.findViewById(R.id.live_tracking_custom), view.findViewById(R.id.live_tracking_custom_content));
            Spinner spinner = view.findViewById(R.id.live_tracking_custom_spinner);
            spinner.setOnItemSelectedListener(onItemSelected);
            EditText text = view.findViewById(R.id.live_tracking_custom_text);
            setVarSelectDialog(varSelectDialog, text, view.findViewById(R.id.live_tracking_custom_var));

            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
            bindKey(NAME, (v) -> text.setText((CharSequence) v), text::getText);
        }
    }

    static class MapCenter extends ActionTypeJSON {
        MapCenter(@NonNull View view) {
            super(view.findViewById(R.id.map_center), view.findViewById(R.id.map_center_content));
            Spinner spinner = (Spinner) mContent;
            spinner.setOnItemSelectedListener(onItemSelected);
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
        }
    }

    static class MapMoveX extends ActionTypeJSON {
        MapMoveX(@NonNull View view) {
            super(view.findViewById(R.id.map_move_x), view.findViewById(R.id.map_move_x_content));
            Spinner spinner = view.findViewById(R.id.map_move_x_spinner);
            spinner.setOnItemSelectedListener(onItemSelected);
            EditText text = view.findViewById(R.id.map_move_x_text);
            bindKey(UNIT, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
            bindKey(VALUE, (v) -> text.setText(String.valueOf((int) v)), () -> Integer.valueOf(text.getText().toString()));
        }
    }

    static class MapMoveY extends ActionTypeJSON {
        MapMoveY(@NonNull View view) {
            super(view.findViewById(R.id.map_move_y), view.findViewById(R.id.map_move_y_content));
            Spinner spinner = view.findViewById(R.id.map_move_y_spinner);
            spinner.setOnItemSelectedListener(onItemSelected);
            EditText text = view.findViewById(R.id.map_move_y_text);
            bindKey(UNIT, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
            bindKey(VALUE, (v) -> text.setText(String.valueOf((int) v)), () -> Integer.valueOf(text.getText().toString()));
        }
    }

    static class MapRotate extends ActionTypeJSON {
        MapRotate(@NonNull View view) {
            super(view.findViewById(R.id.map_rotate), view.findViewById(R.id.map_rotate_content));
            Spinner spinner = (Spinner) mContent;
            spinner.setOnItemSelectedListener(onItemSelected);
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
        }
    }

    static class MapZoom extends ActionTypeJSON {
        MapZoom(@NonNull View view, @Nullable Dialog varSelectDialog) {
            super(view.findViewById(R.id.map_zoom), view.findViewById(R.id.map_zoom_content));
            Spinner spinner = view.findViewById(R.id.map_zoom_spinner);
            spinner.setOnItemSelectedListener(onItemSelected);
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
            EditText text = view.findViewById(R.id.map_zoom_value_text);
            setVarSelectDialog(varSelectDialog, text, view.findViewById(R.id.map_zoom_value_var));
            bindKey(VALUE, (v) -> text.setText((CharSequence) v), text::getText);
        }
    }

    static class GuideTo extends ActionTypeJSON {
        GuideTo(@NonNull View view, @Nullable Dialog varSelectDialog) {
            super(view.findViewById(R.id.guide_to), view.findViewById(R.id.guide_to_content));
            EditText text = view.findViewById(R.id.guide_to_name);
            bindKey(NAME, (v) -> text.setText((CharSequence) v), text::getText);
            EditText lon = view.findViewById(R.id.guide_to_lon_text);
            setVarSelectDialog(varSelectDialog, lon, view.findViewById(R.id.guide_to_lon_var));
            bindKey(LON, (v) -> lon.setText((CharSequence) v), lon::getText);
            EditText lat = view.findViewById(R.id.guide_to_lat_text);
            setVarSelectDialog(varSelectDialog, lat, view.findViewById(R.id.guide_to_lat_var));
            bindKey(LAT, (v) -> lat.setText((CharSequence) v), lat::getText);
        }
    }

    static class NavigateTo extends ActionTypeJSON {
        NavigateTo(@NonNull View view, @Nullable Dialog varSelectDialog) {
            super(view.findViewById(R.id.navigate_to), view.findViewById(R.id.navigate_to_content));
            EditText text = view.findViewById(R.id.navigate_to_name);
            bindKey(NAME, (v) -> text.setText((CharSequence) v), text::getText);
            EditText lon = view.findViewById(R.id.navigate_to_lon_text);
            setVarSelectDialog(varSelectDialog, lon, view.findViewById(R.id.navigate_to_lon_var));
            bindKey(LON, (v) -> lon.setText((CharSequence) v), lon::getText);
            EditText lat = view.findViewById(R.id.navigate_to_lat_text);
            setVarSelectDialog(varSelectDialog, lat, view.findViewById(R.id.navigate_to_lat_var));
            bindKey(LAT, (v) -> lat.setText((CharSequence) v), lat::getText);
        }
    }
    
    static class Navigation extends ActionTypeJSON {
        Navigation(@NonNull View view) {
            super(view.findViewById(R.id.navigation), view.findViewById(R.id.navigation_content));
            Spinner spinner = (Spinner) mContent;
            spinner.setOnItemSelectedListener(onItemSelected);
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
        }
    }

    static class Open extends ActionTypeJSON {
        Open(@NonNull View view) {
            super(view.findViewById(R.id.open), view.findViewById(R.id.open_content));
            Spinner spinner = (Spinner) mContent;
            spinner.setOnItemSelectedListener(onItemSelected);
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
        }
    }

    static class Preset extends ActionTypeJSON {
        Preset(@NonNull View view, @Nullable Dialog varSelectDialog) {
            super(view.findViewById(R.id.preset), view.findViewById(R.id.preset_content));
            EditText text = view.findViewById(R.id.preset_text);
            setVarSelectDialog(varSelectDialog, text, view.findViewById(R.id.preset_var));

            bindKey(ACTION, null, () -> START);
            bindKey(NAME, (v) -> text.setText((CharSequence) v), text::getText);
        }
    }

    static class ScreenLock extends ActionTypeJSON {
        ScreenLock(@NonNull View view) {
            super(view.findViewById(R.id.screen_lock), view.findViewById(R.id.screen_lock_content));
            Spinner spinner = (Spinner) mContent;
            spinner.setOnItemSelectedListener(onItemSelected);
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
        }
    }

    static class ScreenOnOff extends ActionTypeJSON {
        ScreenOnOff(@NonNull View view) {
            super(view.findViewById(R.id.screen), view.findViewById(R.id.screen_content));
            Spinner spinner = (Spinner) mContent;
            spinner.setOnItemSelectedListener(onItemSelected);
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
        }
    }

    static class TrackRecord extends ActionTypeJSON {
        TrackRecord(@NonNull View view, @Nullable Dialog varSelectDialog) {
            super(view.findViewById(R.id.track_record), view.findViewById(R.id.track_record_content));
            Spinner spinner = view.findViewById(R.id.track_record_spinner);
            spinner.setOnItemSelectedListener(onItemSelected);
            EditText text = view.findViewById(R.id.track_record_text);
            CheckBox saveCheckbox = view.findViewById(R.id.track_record_save_checkbox);
            CheckBox allowStartCheckbox = view.findViewById(R.id.track_record_allow_start_checkbox);
            Spinner wptSpinner = view.findViewById(R.id.track_record_wpt_spinner);
            setVarSelectDialog(varSelectDialog, text, view.findViewById(R.id.track_record_var));

            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
            bindKey(NAME, (v) -> text.setText((CharSequence) v), text::getText);
            bindKey(AUTO_SAVE, (v) -> saveCheckbox.setChecked((boolean) v), saveCheckbox::isChecked);
            bindKey(ALLOW_START, (v) -> allowStartCheckbox.setChecked((boolean) v), allowStartCheckbox::isChecked);
            bindKey(ACTION_AFTER, (v) -> setSpinnerValue(wptSpinner, v), wptSpinner::getSelectedItem);
        }
    }

    static class QuickBookmark extends ActionTypeJSON {
        QuickBookmark(@NonNull View view, @Nullable Dialog varSelectDialog) {
            super(view.findViewById(R.id.quick_bookmark), view.findViewById(R.id.quick_bookmark_content));
            EditText text = view.findViewById(R.id.quick_bookmark_text);
            setVarSelectDialog(varSelectDialog, text, view.findViewById(R.id.quick_bookmark_var));

            bindKey(ACTION, null, () -> START);
            bindKey(NAME, (v) -> text.setText((CharSequence) v), text::getText);
        }
    }

    private Map<String, ActionTypeJSON> actionMap;

    @SuppressWarnings("HardCodedStringLiteral")
    private void initActionMapping(@Nullable Dialog varSelectDialog) {
        View view = findViewById(R.id.content);
        actionMap = new HashMap<>();
        actionMap.put("dashboard", new Dashboard(view, varSelectDialog));
        actionMap.put("function", new OpenFunction(view));
        actionMap.put("live_tracking_asamm", new LiveTrackingAsamm(view, varSelectDialog));
        actionMap.put("live_tracking_custom", new LiveTrackingCustom(view, varSelectDialog));
        actionMap.put("map_center", new MapCenter(view));
        actionMap.put("map_move_x", new MapMoveX(view));
        actionMap.put("map_move_y", new MapMoveY(view));
        actionMap.put("map_rotate", new MapRotate(view));
        actionMap.put("map_zoom", new MapZoom(view, varSelectDialog));
        actionMap.put("guide_to", new GuideTo(view, varSelectDialog));
        actionMap.put("navigate_to", new NavigateTo(view, varSelectDialog));
        actionMap.put("navigation", new Navigation(view));
        actionMap.put("open", new Open(view));
        actionMap.put("preset", new Preset(view, varSelectDialog));
        actionMap.put("screen_lock", new ScreenLock(view));
        actionMap.put("screen_on_off", new ScreenOnOff(view));
        actionMap.put("track_record", new TrackRecord(view, varSelectDialog));
        actionMap.put("quick_bookmark", new QuickBookmark(view, varSelectDialog));
    }

    private void applyJSONToView(@Nullable String json) {
        if (json == null) {
            return;
        }

        try {
            JSONObject locusJSON = new JSONObject(json);

            for (Iterator<String> it = locusJSON.keys(); it.hasNext(); ) {
                String key = it.next();
                ActionTypeJSON actionType = actionMap.get(key);
                if (actionType != null) {
                    actionType.setJSON(locusJSON.getJSONObject(key));
                }
            }
        } catch (JSONException e) {
            Toast.makeText(this, ReportingHelper.getUserFriendlyName(e), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_action_task);
        initActionMapping(createVarSelectDialog());

        if (savedInstanceState == null) {
            Bundle taskerBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
            if (taskerBundle != null) {
                applyJSONToView(taskerBundle.getString(Const.INTENT_EXTRA_FIELD_JSON));
            }
        }
    }

    private void handleVariableReplacements(@NonNull Bundle extraBundle, @NonNull String jsonString) {

        //it is ok to waste some cpu time on false positive actions like "map move x 20%"
        if (jsonString.contains("%")) {
            Bundle hostExtras = getIntent().getExtras();
            if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(hostExtras)) {
                TaskerPlugin.Setting.setVariableReplaceKeys(extraBundle, new String[]{Const.INTENT_EXTRA_FIELD_JSON});

                if (TaskerPlugin.hostSupportsKeyEncoding(hostExtras, TaskerPlugin.Encoding.JSON)) {
                    TaskerPlugin.setKeyEncoding(extraBundle, new String[]{Const.INTENT_EXTRA_FIELD_JSON}, TaskerPlugin.Encoding.JSON);
                }
            }
        }
    }

    @NonNull
    private Intent createResultIntent() {

        Bundle extraBundle = new Bundle();
        extraBundle.putString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE, LocusActionType.ACTION_TASK.name());

        JSONObject locusJSON = new JSONObject();

        for (Entry<String, ActionTypeJSON> stringActionTypeJSONEntry : actionMap.entrySet()) {
            ActionTypeJSON actionType = stringActionTypeJSONEntry.getValue();
            if (actionType.isChecked()) {
                try {
                    locusJSON.put(stringActionTypeJSONEntry.getKey(), actionType.getJSON());
                } catch (JSONException e) {
                    Toast.makeText(this, ReportingHelper.getUserFriendlyName(e), Toast.LENGTH_LONG).show();
                }
            }
        }
        String jsonString = locusJSON.toString();
        Log.d(TAG, jsonString);

        extraBundle.putString(Const.INTENT_EXTRA_FIELD_JSON, jsonString);
        String blurb = Const.INTENT_ACTION_TASK_EXTRA_KEY + ": " + jsonString;

        handleVariableReplacements(extraBundle, jsonString);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, extraBundle);
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, blurb);

        return resultIntent;
    }

    @Override
    void onApply() {
        finish(createResultIntent(), null);

    }
}
