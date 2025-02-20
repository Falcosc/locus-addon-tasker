package falcosc.locus.addon.tasker.intent.edit;

import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.Nullable;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.intent.handler.LocusInfoRequest;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.TaskerField;

public class LocusInfoEdit extends TaskerEditActivity {

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

        List<TaskerField> fields = LocusInfoRequest.getFieldNames();
        for (int i = 0; i < fields.size(); i++) {
            TaskerField field = fields.get(i);
            arrayAdapter.add(field.mLabel);
            mCheckState.put(i, mStoredFieldSelection.contains(field.mTaskerName));
        }
    }

    @Override
    void onApply() {
        mStoredFieldSelection = new LinkedHashSet<>();

        ArrayList<TaskerField> selectedFields = new ArrayList<>();

        List<TaskerField> fields = LocusInfoRequest.getFieldNames();

        int checkedItemsCount = mCheckState.size();
        for (int i = 0; i < checkedItemsCount; ++i) {
            int position = mCheckState.keyAt(i);
            if (mCheckState.valueAt(i)) {
                TaskerField field = fields.get(position);
                if(field.mTaskerName.endsWith(LocusInfoRequest.DURATION_FIELD_SUFFIX)) {
                    field.mHTMLDesc = getString(R.string.exec_duration_html_desc);
                }
                selectedFields.add(field);
                mStoredFieldSelection.add(field.mTaskerName);
            }
        }
        finish(createResultIntent(LocusActionType.LOCUS_INFO_REQUEST, selectedFields), null);
    }
}
