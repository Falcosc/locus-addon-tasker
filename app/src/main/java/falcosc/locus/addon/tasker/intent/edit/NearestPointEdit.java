package falcosc.locus.addon.tasker.intent.edit;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.intent.handler.NearestPointRequest;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;

public class NearestPointEdit extends TaskerEditActivity {

    private ArrayAdapter<NearestPointRequest.RemoveMode> mRemoveModeAdapter;

    private EditText mNameEdit;
    private EditText mRadiusEdit;
    private EditText mLimitEdit;
    private EditText mKnownPIDsEdit;
    private Spinner mRemoveMode;
    private EditText mExtendedRadiusEdit;
    private boolean isNewConfig = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.nearest_point);

        mNameEdit = findViewById(R.id.name);
        mRadiusEdit = findViewById(R.id.radius);
        mExtendedRadiusEdit = findViewById(R.id.ext_radius);
        mLimitEdit = findViewById(R.id.limit);
        mKnownPIDsEdit = findViewById(R.id.known_pids);
        mRemoveMode = findViewById(R.id.remove_points_mode);
        mRemoveModeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, NearestPointRequest.RemoveMode.values());
        mRemoveMode.setAdapter(mRemoveModeAdapter);


        mNameEdit.setText(null);
        if (savedInstanceState == null) {

            Bundle taskerBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
            if (taskerBundle != null) {
                parseConfiguration(taskerBundle);
            }
        }
    }

    @NonNull
    private Bundle createConfiguration() {
        Bundle config = new Bundle();
        config.putString(Const.INTENT_EXTRA_POINTS_NAME_FILTER, mNameEdit.getText().toString());
        config.putString(Const.INTENT_EXTRA_POINTS_RADIUS, mRadiusEdit.getText().toString());
        config.putString(Const.INTENT_EXTRA_POINTS_RADIUS_EXTENDED, mExtendedRadiusEdit.getText().toString());
        config.putString(Const.INTENT_EXTRA_POINTS_LIMIT, mLimitEdit.getText().toString());
        config.putString(Const.INTENT_EXTRA_POINTS_KNOWN, mKnownPIDsEdit.getText().toString());
        NearestPointRequest.RemoveMode mode = mRemoveModeAdapter.getItem(mRemoveMode.getSelectedItemPosition());
        if (mode != null) {
            config.putString(Const.INTENT_EXTRA_POINTS_REMOVE_MODE, mode.name());
        }

        return config;
    }

    private void parseConfiguration(@NonNull Bundle bundle) {
        isNewConfig = false;
        mNameEdit.setText(bundle.getString(Const.INTENT_EXTRA_POINTS_NAME_FILTER));
        mRadiusEdit.setText(bundle.getString(Const.INTENT_EXTRA_POINTS_RADIUS));
        mExtendedRadiusEdit.setText(bundle.getString(Const.INTENT_EXTRA_POINTS_RADIUS_EXTENDED));
        mLimitEdit.setText(bundle.getString(Const.INTENT_EXTRA_POINTS_LIMIT));
        mKnownPIDsEdit.setText(bundle.getString(Const.INTENT_EXTRA_POINTS_KNOWN));
        String removeMode = bundle.getString(Const.INTENT_EXTRA_POINTS_REMOVE_MODE);
        if (removeMode != null) {
            mRemoveMode.setSelection(mRemoveModeAdapter.getPosition(NearestPointRequest.RemoveMode.valueOf(removeMode)));
        }
    }

    private static String getBundleBlurb(Bundle bundle) {
        StringBuilder builder = new StringBuilder(0);
        for (String key : bundle.keySet()) {
            String value = bundle.getString(key);
            if (StringUtils.isNotBlank(value)) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(key);
                builder.append(": ");
                builder.append(value);
            }
        }
        return builder.toString();
    }

    @Nullable
    private Dialog createHintsDialog() {
        Dialog hintsDialog = null;
        if (isNewConfig) {
            TextView textView = new TextView(this);
            textView.setText(R.string.nearest_point_before_use);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            Builder builder = new Builder(this);
            builder.setPositiveButton(R.string.ok, null);
            builder.setView(textView);
            hintsDialog = builder.create();
        }
        return hintsDialog;
    }

    @Override
    void onApply() {
        Bundle hostExtras = getIntent().getExtras();

        if (!TaskerPlugin.hostSupportsRelevantVariables(hostExtras)) {
            Toast.makeText(this, R.string.err_no_support_relevant_variables, Toast.LENGTH_LONG).show();
            return;
        }

        if (!TaskerPlugin.Setting.hostSupportsSynchronousExecution(hostExtras)) {
            Toast.makeText(this, R.string.err_no_support_sync_exec, Toast.LENGTH_LONG).show();
            return;
        }

        Bundle config = createConfiguration();

        Bundle extraBundle = new Bundle();
        extraBundle.putString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE, LocusActionType.NEAREST_POINT_REQUEST.name());
        extraBundle.putAll(config);

        if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(hostExtras)) {

            ArrayList<String> keysWithReplacement = new ArrayList<>();
            for (String key : config.keySet()) {
                if (StringUtils.contains(config.getString(key), "%")) {
                    keysWithReplacement.add(key);
                }
            }

            TaskerPlugin.Setting.setVariableReplaceKeys(extraBundle, keysWithReplacement.toArray(new String[0]));
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, extraBundle);
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, getBundleBlurb(config));
        String[] variables = {
                NearestPointRequest.OUT_POINTS + '\n' + getString(R.string.var_desc_points) + '\n',
                NearestPointRequest.OUT_KNOWN_PIDS + '\n' + getString(R.string.var_desc_known_pids) + '\n',
                NearestPointRequest.OUT_P1_BEARING + '\n' + "bearing to first point" + '\n',
                NearestPointRequest.OUT_P1_DISTANCE + '\n' + "distance to first point" + '\n'};
        if(true){
            ArrayUtils.add(variables, NearestPointRequest.OUT_POINTS_JSON + '\n' + "contains all point contents" + '\n');
        }
        TaskerPlugin.addRelevantVariableList(resultIntent, variables);


        //force synchronous execution by set a timeout to handle variables
        TaskerPlugin.Setting.requestTimeoutMS(resultIntent, DEFAULT_REQUEST_TIMEOUT_MS);

        finish(resultIntent, createHintsDialog());
    }
}
