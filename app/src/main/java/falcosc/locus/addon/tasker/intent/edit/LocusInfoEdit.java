package falcosc.locus.addon.tasker.intent.edit;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.intent.handler.LocusInfoRequest;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.LocusField;
import falcosc.locus.addon.tasker.utils.LocusInfoField;

public class LocusInfoEdit extends TaskerEditActivity {

    private static final int DEFAULT_REQUEST_TIMEOUT_MS = 10000;
    private Set<String> mStoredFieldSelection;
    private SparseBooleanArray mCheckState;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice);

        setContentView(R.layout.edit_list_view);

        ListView listView = findViewById(R.id.listView);

        listView.setAdapter(arrayAdapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

        mCheckState = listView.getCheckedItemPositions();

        for (int i = 0; i < LocusInfoRequest.FIELDS.size(); i++) {
            LocusInfoField field = LocusInfoRequest.FIELDS.get(i);
            arrayAdapter.add(field.mLabel);
            mCheckState.put(i, mStoredFieldSelection.contains(field.mTaskerName));
        }
    }

    @Nullable
    private Intent createResultIntent(ArrayList<LocusInfoField> locusFields) {

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
            LocusInfoField field = locusFields.get(i);
            fieldDesc[i] = "%" + field.mTaskerName + "\n" + field.mLabel + "\n";
            fieldKeys[i] = field.mTaskerName;
        }
        Arrays.sort(fieldKeys);

        Bundle extraBundle = new Bundle();
        extraBundle.putString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE, LocusActionType.LOCUS_INFO_REQUEST.name());
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
        mStoredFieldSelection = new LinkedHashSet<>();

        ArrayList<LocusInfoField> selectedFields = new ArrayList<>();

        int checkedItemsCount = mCheckState.size();
        for (int i = 0; i < checkedItemsCount; ++i) {
            int position = mCheckState.keyAt(i);
            if (mCheckState.valueAt(i)) {
                LocusInfoField field = LocusInfoRequest.FIELDS.get(position);
                selectedFields.add(field);
                mStoredFieldSelection.add(field.mTaskerName);
            }
        }
        finish(createResultIntent(selectedFields),null);
    }
}
