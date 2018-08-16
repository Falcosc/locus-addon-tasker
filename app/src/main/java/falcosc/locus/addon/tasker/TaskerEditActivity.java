package falcosc.locus.addon.tasker;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import falcosc.locus.addon.tasker.R.id;
import falcosc.locus.addon.tasker.R.layout;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.intent.edit.AbstractDialogFragment;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;

public final class TaskerEditActivity extends AppCompatActivity implements AbstractDialogFragment.EditTaskFinish {

    private static final String TAG = "TaskerEditActivity";

    @Override
    public void onFinish(Intent resultIntent, Dialog hints) {
        setResult(RESULT_OK, resultIntent);

        if (hints != null) {
            hints.setOnDismissListener(dialog -> finish());
            hints.show();
        } else {
            finish();
        }
    }


    private void addActionButtons(ViewGroup viewGroup) {

        LayoutInflater inflater = LayoutInflater.from(this);

        for (LocusActionType type : LocusActionType.values()) {
            View view = inflater.inflate(layout.list_btn_launcher, viewGroup, false);
            Button button = view.findViewById(id.listBtn);
            button.setText(type.getLabelStringId());
            if (type.isNotImplemented()) {
                TextViewCompat.setTextAppearance(button, R.style.textRed);
            }
            button.setOnClickListener(v -> type.createFragment().show(getSupportFragmentManager(), type.name()));
            viewGroup.addView(view);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //start reading locus resources files in background to speed up UI
        LocusCache.initAsync(this);

        setContentView(layout.task_selection);

        addActionButtons(findViewById(R.id.linearContent));

        setTitle(R.string.select_locus_action);

        if (savedInstanceState == null) {
            final Bundle taskerBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);

            if (taskerBundle != null) {
                try {
                    LocusActionType type = LocusActionType.valueOf(taskerBundle.getString(Const.INTEND_EXTRA_ADDON_ACTION_TYPE));
                    type.createFragment().show(getSupportFragmentManager(), type.name());
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Can't open action type", e); //NON-NLS
                }
            }
        }
    }
}
