package falcosc.locus.addon.tasker.intent.edit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import falcosc.locus.addon.tasker.utils.Const;
import locus.api.android.utils.LocusConst;
import locus.api.android.utils.LocusUtils;

public class SelectVersion extends TaskerEditActivity {

    Spinner mPackageSelection;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_version);

        ArrayList<String> availablePackageNames = new ArrayList<>();
        for(LocusUtils.LocusVersion lv : LocusUtils.getAvailableVersions(this)){
            availablePackageNames.add(lv.getPackageName());
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availablePackageNames);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPackageSelection = findViewById(R.id.package_select);
        mPackageSelection.setAdapter(spinnerArrayAdapter);

        if(!spinnerArrayAdapter.isEmpty()) {
            mPackageSelection.setSelection(0);
        }

        Bundle taskerBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        if (taskerBundle != null) {
            int savedPosition = spinnerArrayAdapter.getPosition(taskerBundle.getString(LocusConst.INTENT_EXTRA_PACKAGE_NAME));
            if(savedPosition > 0){
                mPackageSelection.setSelection(savedPosition);
            }
        }
    }

    @Nullable
    private Intent createResultIntent() {

        Bundle extraBundle = new Bundle();
        extraBundle.putString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE, LocusActionType.SELECT_VERSION.name());

        String packageName = mPackageSelection.getSelectedItem().toString();

        extraBundle.putString(LocusConst.INTENT_EXTRA_PACKAGE_NAME, packageName);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, extraBundle);
        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, packageName);

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
