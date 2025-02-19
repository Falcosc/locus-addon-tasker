package falcosc.locus.addon.tasker.intent.edit;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.intent.handler.TrackPointsRequest;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.TaskerField;
import falcosc.locus.addon.tasker.utils.TrackPointCache;
import falcosc.locus.addon.tasker.utils.listener.SimpleItemSelectListener;

public class TrackPointsEdit extends TaskerEditActivity {

    public static final TaskerField TRACK_SEGMENT_VAR = new TaskerField("trkseg", "JSON Keys:");
    public static final TaskerField TRACK_POINT_COUNT = new TaskerField("trk_pt_count", "Total Track Points");
    public static final TaskerField TRACK_WAYPOINT_COUNT = new TaskerField("trk_wpt_count", "Total Track Waypoints");
    public static final String WAYPOINTS_KEY = "Waypoints";
    public static final String POINTS_KEY = "Points";
    public static final String OFFSET_KEY = "Offset";

    private static final EnumSpinnerItem[] typeItems = {
            new EnumSpinnerItem(TrackPointsRequest.Type.WAYPOINTS, "Waypoints"),
            new EnumSpinnerItem(TrackPointsRequest.Type.POINTS, "Points"),
            new EnumSpinnerItem(TrackPointsRequest.Type.POINTS_AND_WAYPOINTS, "Points & Waypoints")
    };

    private static final EnumSpinnerItem[] trackItems = {
            new EnumSpinnerItem(TrackPointsRequest.TrackSource.LAST_LOADED, "last loaded", "reuse result from last load to extract more than 1MB of point details via Point Offset"),
            new EnumSpinnerItem(TrackPointsRequest.TrackSource.LOAD_SHARE, "load last shared", "load track from your last track menu run task action"),
            new EnumSpinnerItem(TrackPointsRequest.TrackSource.LOAD_GUIDE, "load guide track", "load current guide/navigation track if changed"),
    };
    private static final String NUMBER_OR_STRING = "Number or String";
    private static final String STRING = "String";
    private Spinner mTypeSelection;
    private Spinner mTrackSelection;
    private EditText mLocationFieldsEdit;
    private EditText mWaypointExtrasEdit;
    private EditText mOffsetEdit;
    private EditText mCountEdit;

    public static class EnumSpinnerItem {
        @NonNull
        public final Enum<?> mType;
        @NonNull
        private final String mLabel;
        @Nullable
        public final String mHelpText;

        public EnumSpinnerItem(@NonNull Enum<?> type, @NonNull String label) {
            this(type, label, null);
        }

        public EnumSpinnerItem(@NonNull Enum<?> type, @NonNull String label, @Nullable String helpText) {
            mType = type;
            mLabel = label;
            mHelpText = helpText;
        }

        @NonNull
        @Override
        public String toString() {
            return mLabel;
        }
    }

    public ArrayAdapter<EnumSpinnerItem> getTypeArrayAdapter(@NonNull EnumSpinnerItem[] items) {
        ArrayAdapter<EnumSpinnerItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private static int findSpinnerItemByTypeName(@NonNull EnumSpinnerItem[] items, @Nullable String typeName) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].mType.name().equals(typeName)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_track_points);
        setOptionalButton(R.string.act_documentation, (v) -> openWaypointExtraDoc());

        TextView mTrackLabel =  findViewById(R.id.track_select_label);
        TextView trackHelp = findViewById(R.id.track_select_help);
        mTrackSelection = findViewById(R.id.track_select);
        mTrackSelection.setAdapter(getTypeArrayAdapter(trackItems));
        mTrackSelection.setOnItemSelectedListener(new SimpleItemSelectListener(mTrackLabel.getText(), (selectedValue) -> {
            EnumSpinnerItem trackItem = (EnumSpinnerItem) selectedValue;
            trackHelp.setText(trackItem.mHelpText);
        }));

        mLocationFieldsEdit = findViewById(R.id.location_fields_edit);
        mWaypointExtrasEdit = findViewById(R.id.waypoint_extras_edit);
        mOffsetEdit = findViewById(R.id.offset_edit);
        mCountEdit = findViewById(R.id.count_edit);

        Dialog varSelectDialog = createVarSelectDialog();
        setVarSelectDialog(varSelectDialog, mOffsetEdit, findViewById(R.id.offset_var));

        TextView mTypeLabel = findViewById(R.id.type_select_label);
        mTypeSelection = findViewById(R.id.type_select);
        mTypeSelection.setAdapter(getTypeArrayAdapter(typeItems));
        mTypeSelection.setOnItemSelectedListener(new SimpleItemSelectListener(mTypeLabel.getText(),
                (selectedValue) -> handleTypeChange((EnumSpinnerItem) selectedValue, findViewById(R.id.waypoint_content))));

        fillEdits(getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE));
    }

    private void fillEdits(Bundle taskerBundle) {
        if (taskerBundle != null) {
            mTypeSelection.setSelection(findSpinnerItemByTypeName(typeItems, taskerBundle.getString(Const.INTENT_EXTRA_TRK_POINTS_TYPE)));
            mTrackSelection.setSelection(findSpinnerItemByTypeName(trackItems, taskerBundle.getString(Const.INTENT_EXTRA_TRK_SOURCE)));
            mLocationFieldsEdit.setText(taskerBundle.getString(Const.INTENT_EXTRA_LOCATION_FIELDS));
            mWaypointExtrasEdit.setText(taskerBundle.getString(Const.INTENT_EXTRA_WAYPOINT_FIELDS));
            mOffsetEdit.setText(taskerBundle.getString(Const.INTENT_EXTRA_OFFSET));
            mCountEdit.setText(taskerBundle.getString(Const.INTENT_EXTRA_COUNT));
        }

        if(mLocationFieldsEdit.getText().toString().trim().isEmpty()) {
            mLocationFieldsEdit.setText(String.join(", ", TrackPointCache.getValidLocationFields()));
        }
        if(mWaypointExtrasEdit.getText().toString().trim().isEmpty()) {
            mWaypointExtrasEdit.setText(String.join(", ", TrackPointCache.getValidWaypointExtras()));
        }
    }

    private void openWaypointExtraDoc() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/asamm/locus-api/blob/Locus_API_0.9.62" +
                "/locus-api-core/src/main/java/locus/api/objects/extra/GeoDataExtra.kt#L791-L1071")); //NON-NLS
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void handleTypeChange(@NonNull EnumSpinnerItem selectedValue, @NonNull View waypointContent) {
        if (selectedValue.mType == TrackPointsRequest.Type.POINTS) {
            waypointContent.setVisibility(View.GONE);
        } else {
            waypointContent.setVisibility(View.VISIBLE);
            waypointContent.getParent().requestChildFocus(mWaypointExtrasEdit, mWaypointExtrasEdit);
        }
    }

    @NonNull
    private Intent createResultIntent() {
        Bundle extraBundle = new Bundle();
        extraBundle.putString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE, LocusActionType.TRACK_POINTS_REQUEST.name());

        EnumSpinnerItem typeItem = (EnumSpinnerItem) mTypeSelection.getSelectedItem();
        extraBundle.putString(Const.INTENT_EXTRA_TRK_POINTS_TYPE, typeItem.mType.name());
        EnumSpinnerItem trackItem = (EnumSpinnerItem) mTrackSelection.getSelectedItem();
        extraBundle.putString(Const.INTENT_EXTRA_TRK_SOURCE, trackItem.mType.name());

        String locationFields = mLocationFieldsEdit.getText().toString().trim();
        extraBundle.putString(Const.INTENT_EXTRA_LOCATION_FIELDS, locationFields);
        String waypointFields = mWaypointExtrasEdit.getText().toString().trim();
        extraBundle.putString(Const.INTENT_EXTRA_WAYPOINT_FIELDS, waypointFields);
        String offset = mOffsetEdit.getText().toString().trim();
        extraBundle.putString(Const.INTENT_EXTRA_OFFSET, offset);
        String count = mCountEdit.getText().toString().trim();
        extraBundle.putString(Const.INTENT_EXTRA_COUNT, count);

        String description = trackItem.toString();
        description += "\n" + count + " " + typeItem;

        if(!"0".equals(offset)) {
            description += "\n" + ((TextView) findViewById(R.id.offset_label)).getText() + " " + offset;
        }
        description += "\n" + ((TextView) findViewById(R.id.location_fields_label)).getText() + " " + locationFields;
        description += "\n" + ((TextView) findViewById(R.id.waypoint_extras_label)).getText() + " " + waypointFields;

        Bundle hostExtras = getIntent().getExtras();
        if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(hostExtras)) {
            TaskerPlugin.Setting.setVariableReplaceKeys(extraBundle, new String[]{
                    Const.INTENT_EXTRA_LOCATION_FIELDS,
                    Const.INTENT_EXTRA_WAYPOINT_FIELDS,
                    Const.INTENT_EXTRA_COUNT,
                    Const.INTENT_EXTRA_OFFSET
            });
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, extraBundle);
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, description);

        TaskerPlugin.addRelevantVariableList(resultIntent, getVariableList((TrackPointsRequest.Type) typeItem.mType, offset));

        //force synchronous execution by set a timeout to handle variables
        TaskerPlugin.Setting.requestTimeoutMS(resultIntent, DEFAULT_REQUEST_TIMEOUT_MS);

        return resultIntent;
    }

    @NonNull
    private static JSONArray pointExample() throws JSONException {
        JSONArray arr = new JSONArray();
        arr.put(new JSONObject().put("LocationField2", NUMBER_OR_STRING));
        arr.put(new JSONObject()
                .put("LocationField1", NUMBER_OR_STRING)
                .put("LocationField2", NUMBER_OR_STRING)
        );
        return arr;
    }

    private static JSONArray waypointExample() throws JSONException {
        JSONArray arr = new JSONArray();
        arr.put(new JSONObject()
                .put("LocationField2", NUMBER_OR_STRING)
                .put("WaypointExtra1", STRING)
                .put("WaypointExtra2", STRING)
        );
        arr.put(new JSONObject().put("LocationField1", NUMBER_OR_STRING));
        return arr;
    }

    @NonNull
    private String[] getVariableList(@NonNull TrackPointsRequest.Type type, @NonNull String offset) {
        Map<String, Object> jsonDesc = new LinkedHashMap<>();
        try {
            if(!offset.isEmpty() && (offset.charAt(0) == '%')) {
                jsonDesc.put(OFFSET_KEY, getString(R.string.offset_desc));
            }
            if (type == TrackPointsRequest.Type.WAYPOINTS) {
                jsonDesc.put(WAYPOINTS_KEY, waypointExample());
            } else if (type == TrackPointsRequest.Type.POINTS) {
                jsonDesc.put(POINTS_KEY, pointExample());
            } else {
                jsonDesc.put(POINTS_KEY, pointExample());
                jsonDesc.put(WAYPOINTS_KEY, waypointExample());
            }
            String[] errorMessages = {
                    getString(R.string.err_trk_points_no_loaded_track),
                    getString(R.string.err_trk_points_no_track_selected),
                    getString(R.string.err_trk_points_no_guide),
                    getString(R.string.err_trk_points_no_guide_by_id)
            };

            return new String[]{
                    TRACK_SEGMENT_VAR.getVarDesc(jsonDesc, getString(R.string.returns_json_structure)),
                    TRACK_POINT_COUNT.getVarDesc(getString(R.string.count_ignoring_offset)),
                    TRACK_WAYPOINT_COUNT.getVarDesc(getString(R.string.count_ignoring_offset)),
                    Const.ERROR_MSG_VAR.getVarDesc(getErrMsgHtmlDesc(getString(R.string.feature_messages), errorMessages))};
        } catch (JSONException ignored) { //impossible on static values
        }
        return new String[]{};
    }

    @Override
    void onApply() {
        boolean inputIsValid = true;
        mLocationFieldsEdit.setError(null);
        mWaypointExtrasEdit.setError(null);

        Collection<String> invalidLocationFields =
                TrackPointCache.findInvalidFields(mLocationFieldsEdit.getText().toString(), TrackPointCache.getValidLocationFields());
        if(!invalidLocationFields.isEmpty()) {
            mLocationFieldsEdit.setError(getString(R.string.err_trk_points_invalid_fields, String.join(", ", invalidLocationFields)));
            inputIsValid = false;
        }
        Collection<String> invalidWaypointExtras =
                TrackPointCache.findInvalidFields(mWaypointExtrasEdit.getText().toString(), TrackPointCache.getValidWaypointExtras());
        if(!invalidWaypointExtras.isEmpty()) {
            mWaypointExtrasEdit.setError(getString(R.string.err_trk_points_invalid_fields, String.join(", ", invalidWaypointExtras)));
            inputIsValid = false;
        }

        if(inputIsValid) {
            finish(createResultIntent(), null);
        }
    }
}
