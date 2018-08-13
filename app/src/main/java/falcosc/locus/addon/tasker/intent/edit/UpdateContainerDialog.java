package falcosc.locus.addon.tasker.intent.edit;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.utils.TaskerPlugin;
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

        Activity activity = getActivity();

        LocusCache locusCache = LocusCache.getInstance(activity);

        storedFieldSelection = new LinkedHashMap<>();

        if (savedInstanceState == null) {

            final Bundle taskerBundle = getActivity().getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
            if (taskerBundle != null) {

                String[] savedSelectedFieldsArray = taskerBundle.getStringArray(Const.INTENT_EXTRA_FIELD_LIST);
                if (savedSelectedFieldsArray != null) {

                    List<String> savedSelectedFields = Arrays.asList(savedSelectedFieldsArray);
                    for (String fieldKey : savedSelectedFields) {

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

        //TODO label
        builder.setTitle(R.string.act_request_stats_sensors);
        builder.setNegativeButton("Cancel", (dialog, which) -> getActivity().finish());
        builder.setNeutralButton("Back", null);
        builder.setPositiveButton("OK", (dialog, which) -> {
            storedFieldSelection = new LinkedHashMap<>();
            for (int i = 0; i < fieldChecks.length; i++) {
                if (fieldChecks[i]) {
                    LocusField field = locusFields.get(i);
                    storedFieldSelection.put(field.taskerName, field);
                }
            }
            setGetUpdateContainerResult();
        });

        return builder.create();
    }

    private void setGetUpdateContainerResult() {

        Bundle hostExtras = getActivity().getIntent().getExtras();

        if (!TaskerPlugin.hostSupportsRelevantVariables(hostExtras)) {
            //TODO add Error
        }

        if (!TaskerPlugin.Setting.hostSupportsSynchronousExecution(hostExtras)) {
            //TODO add error
        }

        String[] fieldKeyList = storedFieldSelection.keySet().toArray(new String[0]);
        Arrays.sort(fieldKeyList);

        Bundle extraBundle = new Bundle();
        extraBundle.putString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE, LocusActionType.UPDATE_CONTAINER_REQUEST.name());
        extraBundle.putStringArray(Const.INTENT_EXTRA_FIELD_LIST, fieldKeyList);
        String blurb = "Get Sensor/Stats: " + StringUtils.join(fieldKeyList, ", ");

        Intent resultIntent = new Intent();
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, extraBundle);
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, blurb);

        List<String> fieldDesc = new ArrayList<>();
        for (Map.Entry<String, LocusField> entry : storedFieldSelection.entrySet()) {
            LocusField field = entry.getValue();
            fieldDesc.add("%" + field.taskerName + "\n" + field.label + "\n");
        }
        TaskerPlugin.addRelevantVariableList(resultIntent, fieldDesc.toArray(new String[0]));

        //TODO remove static value
        TaskerPlugin.Setting.requestTimeoutMS(resultIntent, 3000);

        finish(resultIntent);
    }

}
