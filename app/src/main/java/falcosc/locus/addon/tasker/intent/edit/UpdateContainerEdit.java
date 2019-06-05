package falcosc.locus.addon.tasker.intent.edit;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;

import android.util.SparseBooleanArray;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.*;

import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.TaskerField;

public class UpdateContainerEdit extends TaskerEditActivity {

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

        ArrayList<TaskerField> updateContainerFields = locusCache.mUpdateContainerFields;

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice);

        setContentView(R.layout.edit_list_view);

        ListView listView = findViewById(R.id.listView);

        listView.setAdapter(arrayAdapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

        mCheckState = listView.getCheckedItemPositions();

        for (int i = 0; i < updateContainerFields.size(); i++) {
            TaskerField field = updateContainerFields.get(i);
            arrayAdapter.add(field.mLabel);
            mCheckState.put(i, mStoredFieldSelection.contains(field.mTaskerName));
        }
    }

    private boolean isCalcNavigationProgressNew(@NonNull Set<String> previousFieldSelection) {
        LocusCache locusCache = LocusCache.getInstance(getApplication());
        boolean isPrevWithoutNavigation = Collections.disjoint(previousFieldSelection, locusCache.mLocationProgressKeys);
        boolean isNewNavigation = !Collections.disjoint(mStoredFieldSelection, locusCache.mLocationProgressKeys);

        return isPrevWithoutNavigation && isNewNavigation;
    }

    @Nullable
    private Dialog createHintsDialog(@NonNull Set<String> previousFieldSelection) {
        Dialog hintsDialog = null;
        if (isCalcNavigationProgressNew(previousFieldSelection)) {
            //track required was not selected and got selected this time, create hint:
            Builder builder = new Builder(this);
            builder.setView(R.layout.calc_remain_elevation);
            builder.setPositiveButton(R.string.ok, null);
            hintsDialog = builder.create();
        }
        return hintsDialog;
    }

    @Override
    void onApply() {
        Set<String> previousFieldSelection = mStoredFieldSelection;
        mStoredFieldSelection = new LinkedHashSet<>();

        ArrayList<TaskerField> updateContainerFields = LocusCache.getInstance(getApplication()).mUpdateContainerFields;
        ArrayList<TaskerField> selectedFields = new ArrayList<>();

        int checkedItemsCount = mCheckState.size();
        for (int i = 0; i < checkedItemsCount; ++i) {
            int position = mCheckState.keyAt(i);
            if (mCheckState.valueAt(i)) {
                TaskerField field = updateContainerFields.get(position);
                selectedFields.add(field);
                mStoredFieldSelection.add(field.mTaskerName);
            }
        }
        finish(createResultIntent(LocusActionType.UPDATE_CONTAINER_REQUEST, selectedFields), createHintsDialog(previousFieldSelection));
    }
}
