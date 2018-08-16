package falcosc.locus.addon.tasker.intent.edit;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.LocusField;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class UpdateContainerDialog extends AbstractDialogFragment {

    private LinkedHashMap<String, LocusField> storedFieldSelection;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = requireActivity();

        LocusCache locusCache = LocusCache.getInstance(activity);

        storedFieldSelection = new LinkedHashMap<>();

        if (savedInstanceState == null) {

            final Bundle taskerBundle = activity.getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
            if (taskerBundle != null) {

                String[] savedSelectedFieldsArray = taskerBundle.getStringArray(Const.INTENT_EXTRA_FIELD_LIST);
                if (savedSelectedFieldsArray != null) {

                    for (String fieldKey : savedSelectedFieldsArray) {

                        LocusField field = locusCache.updateContainerFieldMap.get(fieldKey);
                        if (field != null) {
                            storedFieldSelection.put(field.taskerName, field);
                        }
                    }
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        ArrayList<LocusField> locusFields = locusCache.updateContainerFields;
        int fieldCount = locusFields.size();
        final String[] fieldLabels = new String[fieldCount];
        final boolean[] fieldChecks = new boolean[fieldCount];

        for (int i = 0; i < locusFields.size(); i++) {
            LocusField field = locusFields.get(i);
            fieldLabels[i] = field.label;
            fieldChecks[i] = storedFieldSelection.keySet().contains(field.taskerName);
        }

        builder.setMultiChoiceItems(fieldLabels, fieldChecks,
                (dialog, which, isChecked) -> fieldChecks[which] = isChecked);

        builder.setTitle(R.string.act_request_stats_sensors);
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> requireActivity().finish());
        builder.setNeutralButton(R.string.back, null);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> onFieldSelectionOK(fieldChecks, locusFields));

        return builder.create();
    }

    private void onFieldSelectionOK(boolean[] fieldChecks, ArrayList<LocusField> locusFields) {
        Set<String> previousFieldSelection = storedFieldSelection.keySet();
        storedFieldSelection = new LinkedHashMap<>();
        for (int i = 0; i < fieldChecks.length; i++) {
            if (fieldChecks[i]) {
                LocusField field = locusFields.get(i);
                storedFieldSelection.put(field.taskerName, field);
            }
        }

        finish(createResultIntent(), createHintsDialog(previousFieldSelection));
    }

    private Dialog createHintsDialog(Set<String> previousFieldSelection) {
        Dialog hintsDialog = null;
        if (!previousFieldSelection.contains(LocusCache.CALC_REMAIN_UPHILL_ELEVATION)
                && storedFieldSelection.containsKey(LocusCache.CALC_REMAIN_UPHILL_ELEVATION)) {
            //track required was not selected and got selected this time, create hint:
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setView(R.layout.help_set_track);
            builder.setPositiveButton(R.string.ok, null);
            hintsDialog = builder.create();
        }
        return hintsDialog;
    }

    private Intent createResultIntent() {

        Bundle hostExtras = requireActivity().getIntent().getExtras();

        if (!TaskerPlugin.hostSupportsRelevantVariables(hostExtras)) {
            Toast.makeText(requireContext(), R.string.err_no_support_relevant_variables, Toast.LENGTH_LONG).show();
            return null;
        }

        if (!TaskerPlugin.Setting.hostSupportsSynchronousExecution(hostExtras)) {
            Toast.makeText(requireContext(), R.string.err_no_support_sync_exec, Toast.LENGTH_LONG).show();
            return null;
        }

        String[] fieldKeyList = storedFieldSelection.keySet().toArray(new String[0]);
        Arrays.sort(fieldKeyList);

        Bundle extraBundle = new Bundle();
        extraBundle.putString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE, LocusActionType.UPDATE_CONTAINER_REQUEST.name());
        extraBundle.putStringArray(Const.INTENT_EXTRA_FIELD_LIST, fieldKeyList);
        String blurb = "Get Sensor/Stats: " + StringUtils.join(fieldKeyList, ", "); //NON-NLS

        Intent resultIntent = new Intent();
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, extraBundle);
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, blurb);

        List<String> fieldDesc = new ArrayList<>();
        for (Map.Entry<String, LocusField> entry : storedFieldSelection.entrySet()) {
            LocusField field = entry.getValue();
            fieldDesc.add("%" + field.taskerName + "\n" + field.label + "\n");
        }
        TaskerPlugin.addRelevantVariableList(resultIntent, fieldDesc.toArray(new String[0]));

        //force synchronous execution by set a timeout to handle variables
        if (TaskerPlugin.Setting.getHintTimeoutMS(hostExtras) == TaskerPlugin.Setting.REQUESTED_TIMEOUT_MS_NONE) {
            TaskerPlugin.Setting.requestTimeoutMS(resultIntent, 5000);
        }

        return resultIntent;
    }

}
