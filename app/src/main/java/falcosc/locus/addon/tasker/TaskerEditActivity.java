package falcosc.locus.addon.tasker;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import falcosc.locus.addon.tasker.R.id;
import falcosc.locus.addon.tasker.R.layout;
import falcosc.locus.addon.tasker.TaskerPlugin.Setting;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.LocusField;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public final class TaskerEditActivity extends Activity {

    private Set<String> storedFieldKeySelection;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layout.main);

        storedFieldKeySelection = new HashSet<>();
        findViewById(id.btnRequestUpdateContainer).setOnClickListener(v -> createUpdateContainerFieldSelection());

        if (savedInstanceState == null) {
            final Bundle taskerBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);

            if (taskerBundle != null) {
                List<String> savedSelectedFields = Arrays.asList(taskerBundle.getStringArray(Const.INTENT_EXTRA_FIELD_LIST));
                storedFieldKeySelection.addAll(savedSelectedFields);
            }
        }
        /*
         * To help the user keep context, the title shows the host's name and the subtitle
         * shows the plug-in's name.
         */
        CharSequence callingApplicationLabel = null;
        try {
            callingApplicationLabel =
                    getPackageManager().getApplicationLabel(
                            getPackageManager().getApplicationInfo(getCallingPackage(),
                                    0));
        } catch (final NameNotFoundException e) {
            //TODO log
        }
        if (null != callingApplicationLabel) {
            setTitle(callingApplicationLabel);
        }

        //getSupportActionBar().setSubtitle(R.string.tasker_plugin_name);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void createUpdateContainerFieldSelection() {

        Builder builder = new Builder(TaskerEditActivity.this);

        LocusCache locusCache = LocusCache.getInstance(this);
        ArrayList<LocusField> locusFields = locusCache.updateContainerFields;
        int fieldCount = locusFields.size();
        final String[] fieldLabels = new String[fieldCount];
        final boolean[] fieldChecks = new boolean[fieldCount];

        for (Integer i = 0; i < locusFields.size(); i++) {
            LocusField field = locusFields.get(i);
            fieldLabels[i] = field.label;
            fieldChecks[i] = storedFieldKeySelection.contains(field.taskerName);
        }

        builder.setMultiChoiceItems(fieldLabels, fieldChecks,
                (dialog, which, isChecked) -> fieldChecks[which] = isChecked);

        builder.setCancelable(false);
        //TODO label
        builder.setTitle("Request which fields?");
        builder.setPositiveButton("OK", (dialog, which) -> {
            storedFieldKeySelection = new HashSet <>();
            ArrayList<LocusField> selectedFields = new ArrayList<>();
            for (int i = 0; i < fieldChecks.length; i++) {
                if (fieldChecks[i]) {
                    LocusField field = locusFields.get(i);
                    storedFieldKeySelection.add(field.taskerName);
                    selectedFields.add(field);
                }
            }
            setGetUpdateContainerResult(selectedFields);
        });

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();


    }

    private void setGetUpdateContainerResult(List<LocusField> selectedFields) {

        if (!TaskerPlugin.hostSupportsRelevantVariables(getIntent().getExtras())) {
            //TODO add Error
        }

        if (!Setting.hostSupportsSynchronousExecution(getIntent().getExtras())) {
            //TODO add error
        }

        String[] fieldKeyList = new String[selectedFields.size()];
        fieldKeyList = storedFieldKeySelection.toArray(fieldKeyList);
        Arrays.sort(fieldKeyList);

        Bundle extraBundle = new Bundle();
        extraBundle.putStringArray(Const.INTENT_EXTRA_FIELD_LIST, fieldKeyList);
        String blurb = "Get Sensor/Stats: " + StringUtils.join(fieldKeyList, ", ");

        Intent resultIntent = new Intent();
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, extraBundle);
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, blurb);

        List<String> fieldDesc = new ArrayList<>();
        for (LocusField field : selectedFields) {
            fieldDesc.add("%" + field.taskerName + "\n" + field.label + "\n");
        }
        TaskerPlugin.addRelevantVariableList(resultIntent, fieldDesc.toArray(new String[fieldDesc.size()]));

        //TODO remove static value
        Setting.requestTimeoutMS(resultIntent, 3000);

        setResult(RESULT_OK, resultIntent);
        finish();
    }
}