package falcosc.locus.addon.tasker.intent.edit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.intent.handler.SelectVersionRequest;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.objects.LocusVersion;
import locus.api.android.utils.LocusConst;
import locus.api.android.utils.LocusUtils;

public class SelectVersion extends TaskerEditActivity {

    private Spinner mPackageSelection;
    private ArrayAdapter<Option> mSpinnerArrayAdapter;

    static class Option {
        final String key;
        final String label;

        Option(@NonNull String optionKey, @NonNull String displayLabel) {
            key = optionKey;
            label = displayLabel;
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_version);

        ArrayList<Option> availablePackageNames = new ArrayList<>();
        for(LocusVersion lv : LocusUtils.INSTANCE.getAvailableVersions(this)){
            availablePackageNames.add(new Option(lv.getPackageName(), lv.toString()));
        }
        if(availablePackageNames.size() > 1){
            //only add autoselection if we have multiple versions
            availablePackageNames.add(new Option(SelectVersionRequest.LAST_ACTIVE, getString(R.string.find_recent_version)));
        }

        mSpinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availablePackageNames);
        mSpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPackageSelection = findViewById(R.id.package_select);
        mPackageSelection.setAdapter(mSpinnerArrayAdapter);

        if(!mSpinnerArrayAdapter.isEmpty()) {
            mPackageSelection.setSelection(0);
        }

        Bundle taskerBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        if (taskerBundle != null) {
            String oldPackage = taskerBundle.getString(LocusConst.INTENT_EXTRA_PACKAGE_NAME);
            for(int i = 0; i < availablePackageNames.size(); i++){
                if(availablePackageNames.get(i).key.equals(oldPackage)){
                    mPackageSelection.setSelection(i);
                }
            }
        }
    }

    @Nullable
    private Intent createResultIntent() {

        Bundle extraBundle = new Bundle();
        extraBundle.putString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE, LocusActionType.SELECT_VERSION.name());

        String packageName = mSpinnerArrayAdapter.getItem(mPackageSelection.getSelectedItemPosition()).key;

        extraBundle.putString(LocusConst.INTENT_EXTRA_PACKAGE_NAME, packageName);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, extraBundle);
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, packageName);

        LocusCache.getInstance(getApplication()).versionSelectReminder.setVersionSelectLastUsage();

        if (!TaskerPlugin.Setting.hostSupportsSynchronousExecution(getIntent().getExtras())) {
            Toast.makeText(this, R.string.err_no_support_sync_exec, Toast.LENGTH_LONG).show();
            return null;
        }

        //force synchronous execution by set a timeout to handle variables
        TaskerPlugin.Setting.requestTimeoutMS(resultIntent, DEFAULT_REQUEST_TIMEOUT_MS);

        return resultIntent;
    }

    @Override
    void onApply() {
        finish(createResultIntent(), null);
    }
}
