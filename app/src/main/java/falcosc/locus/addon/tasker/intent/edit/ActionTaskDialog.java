package falcosc.locus.addon.tasker.intent.edit;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.utils.Const;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;


public class ActionTaskDialog extends AbstractDialogFragment {

    private static final String TAG = "ActionTaskDialog";
    private static final String ACTION = "action";
    private static final String ACTION_AFTER = "action_after";
    private static final String AUTO_SAVE = "auto_save";
    private static final String NAME = "name";
    private static final String START = "start";
    private static final String VALUE = "value";
    private static final String UNIT = "unit";

    static class MapCenter extends AbstractActionTypeJSON {
        MapCenter(View view) {
            super(view.findViewById(R.id.map_center), view.findViewById(R.id.map_center_content));
            final Spinner spinner = (Spinner) content;
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
        }
    }

    static class LiveTrackingAsamm extends AbstractActionTypeJSON {
        LiveTrackingAsamm(View view) {
            super(view.findViewById(R.id.live_tracking_asamm), view.findViewById(R.id.live_tracking_asamm_content));
            final Spinner spinner = view.findViewById(R.id.live_tracking_asamm_spinner);
            final EditText text = view.findViewById(R.id.live_tracking_asamm_text);
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
            bindKey(NAME, (v) -> text.setText((String) v), text::getText);
        }
    }

    static class LiveTrackingCustom extends AbstractActionTypeJSON {
        LiveTrackingCustom(View view) {
            super(view.findViewById(R.id.live_tracking_custom), view.findViewById(R.id.live_tracking_custom_content));
            final Spinner spinner = view.findViewById(R.id.live_tracking_custom_spinner);
            final EditText text = view.findViewById(R.id.live_tracking_custom_text);
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
            bindKey(NAME, (v) -> text.setText((String) v), text::getText);
        }
    }

    static class MapMoveX extends AbstractActionTypeJSON {
        MapMoveX(View view) {
            super(view.findViewById(R.id.map_move_x), view.findViewById(R.id.map_move_x_content));
            final Spinner spinner = view.findViewById(R.id.map_move_x_spinner);
            final EditText text = view.findViewById(R.id.map_move_x_text);
            bindKey(UNIT, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
            bindKey(VALUE, (v) -> text.setText(String.valueOf((int) v)), () -> Integer.valueOf(text.getText().toString()));
        }
    }

    static class MapMoveY extends AbstractActionTypeJSON {
        MapMoveY(View view) {
            super(view.findViewById(R.id.map_move_y), view.findViewById(R.id.map_move_y_content));
            final Spinner spinner = view.findViewById(R.id.map_move_y_spinner);
            final EditText text = view.findViewById(R.id.map_move_y_text);
            bindKey(UNIT, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
            bindKey(VALUE, (v) -> text.setText(String.valueOf((int) v)), () -> Integer.valueOf(text.getText().toString()));
        }
    }

    static class MapZoom extends AbstractActionTypeJSON {
        MapZoom(View view) {
            super(view.findViewById(R.id.map_zoom), view.findViewById(R.id.map_zoom_content));
            final Spinner spinner = (Spinner) content;
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
        }
    }

    static class Open extends AbstractActionTypeJSON {
        Open(View view) {
            super(view.findViewById(R.id.open), view.findViewById(R.id.open_content));
            final Spinner spinner = (Spinner) content;
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
        }
    }

    static class Preset extends AbstractActionTypeJSON {
        Preset(View view) {
            super(view.findViewById(R.id.preset), view.findViewById(R.id.preset_content));
            final EditText text = (EditText) content;
            bindKey(ACTION, null, () -> START);
            bindKey(NAME, (v) -> text.setText((String) v), text::getText);
        }
    }

    static class ScreenOnOff extends AbstractActionTypeJSON {
        ScreenOnOff(View view) {
            super(view.findViewById(R.id.screen), view.findViewById(R.id.screen_content));
            final Spinner spinner = (Spinner) content;
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
        }
    }

    static class TrackRecord extends AbstractActionTypeJSON {
        TrackRecord(View view) {
            super(view.findViewById(R.id.track_record), view.findViewById(R.id.track_record_content));
            final Spinner spinner = view.findViewById(R.id.track_record_spinner);
            final EditText text = view.findViewById(R.id.track_record_text);
            final CheckBox checkbox = view.findViewById(R.id.track_record_checkbox);
            final Spinner wptSpinner = view.findViewById(R.id.track_record_wpt_spinner);
            bindKey(ACTION, (v) -> setSpinnerValue(spinner, v), spinner::getSelectedItem);
            bindKey(NAME, (v) -> text.setText((String) v), text::getText);
            bindKey(AUTO_SAVE, (v) -> checkbox.setChecked((boolean) v), () -> checkbox.isChecked());
            bindKey(ACTION_AFTER, (v) -> setSpinnerValue(wptSpinner, v), wptSpinner::getSelectedItem);
        }
    }

    private Map<String, AbstractActionTypeJSON> actionMap;

    @SuppressWarnings("HardCodedStringLiteral")
    private void initActionMapping(View dialogView) {
        actionMap = new HashMap<>();
        actionMap.put("map_center", new MapCenter(dialogView));
        actionMap.put("live_tracking_asamm", new LiveTrackingAsamm(dialogView));
        actionMap.put("live_tracking_custom", new LiveTrackingCustom(dialogView));
        actionMap.put("map_move_x", new MapMoveX(dialogView));
        actionMap.put("map_move_y", new MapMoveY(dialogView));
        actionMap.put("map_zoom", new MapZoom(dialogView));
        actionMap.put("open", new Open(dialogView));
        actionMap.put("preset", new Preset(dialogView));
        actionMap.put("screen_on_off", new ScreenOnOff(dialogView));
        actionMap.put("track_record", new TrackRecord(dialogView));
    }

    private void applyJSONToView(String json) {
        if (json == null) {
            return;
        }

        try {
            JSONObject locusJSON = new JSONObject(json);

            for (Iterator<String> it = locusJSON.keys(); it.hasNext(); ) {
                String key = it.next();
                AbstractActionTypeJSON actionType = actionMap.get(key);
                if (actionType != null) {
                    actionType.setJSON(locusJSON.getJSONObject(key));
                }
            }
        } catch (JSONException e) {
            Toast.makeText(requireContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private View createView(@NotNull LayoutInflater inflater, Bundle savedInstanceState) {
        @SuppressLint("InflateParams") //because we don't need to look for AlertDialog view NON-NLS
        View dialogView = inflater.inflate(R.layout.edit_action_task, null);
        initActionMapping(dialogView);

        if (savedInstanceState == null) {
            final Bundle taskerBundle = requireActivity().getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
            if (taskerBundle != null) {
                applyJSONToView(taskerBundle.getString(Const.INTENT_EXTRA_FIELD_JSON));
            }
        }

        return dialogView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(createView(requireActivity().getLayoutInflater(), savedInstanceState));
        builder.setTitle(R.string.act_request_stats_sensors);
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> requireActivity().finish());
        builder.setNeutralButton(R.string.back, null);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> finish(createResultIntent(), null));

        return builder.create();
    }

    private Intent createResultIntent() {

        Bundle extraBundle = new Bundle();
        extraBundle.putString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE, LocusActionType.ACTION_TASK.name());

        JSONObject locusJSON = new JSONObject();

        for (String key : actionMap.keySet()) {
            AbstractActionTypeJSON actionType = actionMap.get(key);
            if (actionType.isChecked()) {
                try {
                    locusJSON.put(key, actionType.getJSON());
                } catch (JSONException e) {
                    Toast.makeText(requireContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
        String jsonString = locusJSON.toString();
        Log.d(TAG, jsonString);

        extraBundle.putString(Const.INTENT_EXTRA_FIELD_JSON, jsonString);
        String blurb = Const.INTENT_ACTION_TASK_EXTRA_KEY + ": " + jsonString;

        Intent resultIntent = new Intent();
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, extraBundle);
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, blurb);

        return resultIntent;
    }
}
