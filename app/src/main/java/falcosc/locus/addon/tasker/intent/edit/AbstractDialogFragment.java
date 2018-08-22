package falcosc.locus.addon.tasker.intent.edit;

import android.R.layout;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;

public abstract class AbstractDialogFragment extends DialogFragment {

    private static final String TAG = "AbstractDialogFragment"; //NON-NLS

    public interface EditTaskFinish {

        void onFinish(Intent resultIntent, Dialog hints);

    }

    void finish(Intent resultIntent, Dialog hints) {
        EditTaskFinish activity = (EditTaskFinish) requireActivity();
        activity.onFinish(resultIntent, hints);
    }

    private ArrayAdapter<String> varSelectAdapter;

    Dialog createVarSelectDialog() {
        try {
            Activity activity = requireActivity();
            Bundle hostExtras = activity.getIntent().getExtras();

            if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(hostExtras)) {
                String[] variables = TaskerPlugin.getRelevantVariableList(activity.getIntent().getExtras());

                if (variables.length > 0) {
                    varSelectAdapter = new ArrayAdapter<>(activity, layout.simple_list_item_1, variables);

                    Builder builder = new Builder(requireActivity());
                    builder.setTitle(R.string.variable_select);
                    builder.setAdapter(varSelectAdapter, ((dialog, which) -> insertTextAtFocus(activity, varSelectAdapter.getItem(which))));
                    return builder.create();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Can't create var select dialog", e); //NON-NLS
        }

        return null;
    }

    private void insertTextAtFocus(Activity activity, CharSequence text) {
        View v = activity.getCurrentFocus();
        if ((v == null) && (getDialog() != null)) {
            v = getDialog().getCurrentFocus();
        }
        if (v instanceof EditText) {
            EditText editText = (EditText) v;
            editText.getText().insert(Math.max(editText.getSelectionStart(), 0), text);
        }
    }
}
