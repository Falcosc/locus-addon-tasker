package falcosc.locus.addon.tasker.intent.edit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.asamm.logger.Logger;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;

import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.TaskerField;

public abstract class TaskerEditActivity extends AppCompatActivity {

    private static final String TAG = "TaskerEditActivity"; //NON-NLS
    static final int DEFAULT_REQUEST_TIMEOUT_MS = 10000;

    abstract void onApply();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        setButton(android.R.id.button2, R.string.cancel, (v) -> finish());
        setButton(android.R.id.button1, R.string.ok, (v) -> onApply());
        findViewById(android.R.id.button3).setVisibility(View.GONE);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setButton(android.R.id.button2, R.string.cancel, (v) -> finish());
        setButton(android.R.id.button1, R.string.ok, (v) -> onApply());
        findViewById(android.R.id.button3).setVisibility(View.GONE);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        setButton(android.R.id.button2, R.string.cancel, (v) -> finish());
        setButton(android.R.id.button1, R.string.ok, (v) -> onApply());
        findViewById(android.R.id.button3).setVisibility(View.GONE);
    }

    public void setOptionalButton(@StringRes int textId, View.OnClickListener listener) {
        Button btn = findViewById(android.R.id.button3);
        btn.setVisibility(View.VISIBLE);
        btn.setText(textId);
        btn.setOnClickListener(listener);
    }

    private void setButton(@IdRes int btnId, @StringRes int textId, View.OnClickListener listener) {
        Button btn = findViewById(btnId);
        btn.setText(textId);
        btn.setOnClickListener(listener);
    }

    void finish(@Nullable Intent resultIntent, @Nullable Dialog hints) {
        if (resultIntent == null) {
            setResult(TaskerPlugin.Setting.RESULT_CODE_FAILED, null);
        } else {
            setResult(RESULT_OK, resultIntent);
        }

        if (hints != null) {
            hints.setOnDismissListener(dialog -> finish());
            hints.show();
        } else {
            finish();
        }
    }

    private ArrayAdapter<String> varSelectAdapter;

    @Nullable
    Dialog createVarSelectDialog() {
        try {
            Bundle hostExtras = getIntent().getExtras();

            if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(hostExtras)) {
                String[] variables = TaskerPlugin.getRelevantVariableList(getIntent().getExtras());

                if (variables.length > 0) {
                    varSelectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, variables);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.variable_select);
                    builder.setAdapter(varSelectAdapter, ((dialog, which) -> insertTextAtFocus(this, varSelectAdapter.getItem(which))));
                    return builder.create();
                }
            }
        } catch (Exception e) {
            Logger.w(TAG, "Can't create var select dialog", e); //NON-NLS
        }

        return null;
    }

    static void setVarSelectDialog(@Nullable Dialog varSelectDialog, EditText text, View varSelectBtn) {
        if (varSelectDialog != null) {
            text.setOnFocusChangeListener((v, hasFocus) -> varSelectBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE));
            varSelectBtn.setOnClickListener(v -> varSelectDialog.show());
        }
    }

    @Nullable
    Intent createResultIntent(LocusActionType actionType, ArrayList<TaskerField> locusFields) {

        Bundle hostExtras = getIntent().getExtras();

        if (!TaskerPlugin.hostSupportsRelevantVariables(hostExtras)) {
            Toast.makeText(this, R.string.err_no_support_relevant_variables, Toast.LENGTH_LONG).show();
            return null;
        }

        if (!TaskerPlugin.Setting.hostSupportsSynchronousExecution(hostExtras)) {
            Toast.makeText(this, R.string.err_no_support_sync_exec, Toast.LENGTH_LONG).show();
            return null;
        }

        String[] fieldKeys = new String[locusFields.size()];
        String[] fieldDesc = new String[locusFields.size() + 1];
        for (int i = 0; i < locusFields.size(); i++) {
            TaskerField field = locusFields.get(i);
            fieldDesc[i] = field.getVarDesc();
            fieldKeys[i] = field.mTaskerName;
        }
        Arrays.sort(fieldKeys);

        String[] errorMessages = {
                getString(R.string.err_missing_field, "CONFIG_FIELD"), //NON-NLS
                getString(R.string.err_field_selection_missing),
                getString(R.string.err_not_set_sync_exec),
                getString(R.string.err_no_support_return_variables)
        };
        String errMsgHtml = getErrMsgHtmlDesc(getString(R.string.possible_configuration_errors), errorMessages);
        fieldDesc[locusFields.size()] = Const.ERROR_MSG_VAR.getVarDesc(errMsgHtml);

        Bundle extraBundle = new Bundle();
        extraBundle.putString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE, actionType.name());
        extraBundle.putStringArray(Const.INTENT_EXTRA_FIELD_LIST, fieldKeys);
        String blurb = StringUtils.join(fieldKeys, ",\n");

        Intent resultIntent = new Intent();
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, extraBundle);
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, blurb);

        TaskerPlugin.addRelevantVariableList(resultIntent, fieldDesc);

        //force synchronous execution by set a timeout to handle variables
        TaskerPlugin.Setting.requestTimeoutMS(resultIntent, DEFAULT_REQUEST_TIMEOUT_MS);

        return resultIntent;
    }

    private static void insertTextAtFocus(@NonNull Activity activity, CharSequence text) {
        View v = activity.getCurrentFocus();
        if (v instanceof EditText) {
            EditText editText = (EditText) v;
            editText.getText().insert(Math.max(editText.getSelectionStart(), 0), text);
        }
    }

    @NonNull
    static String getErrMsgHtmlDesc(String headLine, String[] errorMessages) {
        StringBuilder errMsgHtml = new StringBuilder(headLine + "<br/>");
        for(String errorMsg : errorMessages){
            errMsgHtml.append(" • ").append(errorMsg).append("<br/>");
        }
        return errMsgHtml.toString();
    }

    static void setSpinnerValue(Spinner spinner, Object value) {
        if (value != null) {
            if (spinner.getAdapter() instanceof ArrayAdapter) {
                //noinspection unchecked because it is a generic Object Array
                spinner.setSelection(((ArrayAdapter<Object>) spinner.getAdapter()).getPosition(value));
            }
        }
    }
}
