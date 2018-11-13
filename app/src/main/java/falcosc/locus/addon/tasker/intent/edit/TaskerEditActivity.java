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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;

public abstract class TaskerEditActivity extends AppCompatActivity {

    private static final String TAG = "TaskerEditActivity"; //NON-NLS

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
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setButton(android.R.id.button2, R.string.cancel, (v) -> finish());
        setButton(android.R.id.button1, R.string.ok, (v) -> onApply());
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        setButton(android.R.id.button2, R.string.cancel, (v) -> finish());
        setButton(android.R.id.button1, R.string.ok, (v) -> onApply());
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
            Log.w(TAG, "Can't create var select dialog", e); //NON-NLS
        }

        return null;
    }

    private static void insertTextAtFocus(@NonNull Activity activity, CharSequence text) {
        View v = activity.getCurrentFocus();
        if (v instanceof EditText) {
            EditText editText = (EditText) v;
            editText.getText().insert(Math.max(editText.getSelectionStart(), 0), text);
        }
    }
}
