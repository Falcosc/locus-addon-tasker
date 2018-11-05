package falcosc.locus.addon.tasker.intent.edit;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog.Builder;
import android.util.SparseBooleanArray;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.LocusField;

public class UpdateContainerEdit extends TaskerEditActivity {

    private static final int DEFAULT_REQUEST_TIMEOUT_MS = 10000;
    private Set<String> mStoredFieldSelection;
    private SparseBooleanArray mCheckState;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocusCache locusCache = LocusCache.getInstance(getApplication());

        mStoredFieldSelection = new HashSet<>();

        if (savedInstanceState == null) {

            Bundle taskerBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
            if (taskerBundle != null) {
                String[] savedSelectedFieldsArray = taskerBundle.getStringArray(Const.INTENT_EXTRA_FIELD_LIST);
                if (savedSelectedFieldsArray != null) {
                    mStoredFieldSelection.addAll(Arrays.asList(savedSelectedFieldsArray));
                }
            }
        }


        ArrayList<LocusField> locusFields = locusCache.mUpdateContainerFields;

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice);

        setContentView(R.layout.edit_list_view);

        ListView listView = findViewById(R.id.listView);

        listView.setAdapter(arrayAdapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

        mCheckState = listView.getCheckedItemPositions();

        for (int i = 0; i < locusFields.size(); i++) {
            LocusField field = locusFields.get(i);
            arrayAdapter.add(field.mLabel);
            mCheckState.put(i, mStoredFieldSelection.contains(field.mTaskerName));
        }

        //todo check content view
    }

    @Nullable
    private Dialog createHintsDialog(@NonNull Set<String> previousFieldSelection) {
        Dialog hintsDialog = null;
        if (!previousFieldSelection.contains(LocusCache.CALC_REMAIN_UPHILL_ELEVATION)
                && mStoredFieldSelection.contains(LocusCache.CALC_REMAIN_UPHILL_ELEVATION)) {
            //track required was not selected and got selected this time, create hint:
            Builder builder = new Builder(this);
            builder.setView(R.layout.calc_remain_elevation);
            builder.setPositiveButton(R.string.ok, null);
            hintsDialog = builder.create();
        }
        return hintsDialog;
    }


    @Nullable
    private Intent createResultIntent(ArrayList<LocusField> locusFields) {

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
        String[] fieldDesc = new String[locusFields.size()];
        for (int i = 0; i < locusFields.size(); i++) {
            LocusField field = locusFields.get(i);
            fieldDesc[i] = "%" + field.mTaskerName + "\n" + field.mLabel + "\n";
            fieldKeys[i] = field.mTaskerName;
        }
        Arrays.sort(fieldKeys);

        Bundle extraBundle = new Bundle();
        extraBundle.putString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE, LocusActionType.UPDATE_CONTAINER_REQUEST.name());
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

    @Override
    void onApply() {
        Set<String> previousFieldSelection = mStoredFieldSelection;
        mStoredFieldSelection = new LinkedHashSet<>();

        ArrayList<LocusField> locusFields = LocusCache.getInstance(getApplication()).mUpdateContainerFields;
        ArrayList<LocusField> selectedFields = new ArrayList<>();

        int checkedItemsCount = mCheckState.size();
        for (int i = 0; i < checkedItemsCount; ++i) {
            int position = mCheckState.keyAt(i);
            if (mCheckState.valueAt(i)) {
                LocusField field = locusFields.get(position);
                selectedFields.add(field);
                mStoredFieldSelection.add(field.mTaskerName);
            }
        }
        finish(createResultIntent(selectedFields), createHintsDialog(previousFieldSelection));
    }
}
