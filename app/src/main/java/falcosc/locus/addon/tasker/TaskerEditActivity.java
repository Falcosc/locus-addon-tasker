package falcosc.locus.addon.tasker;

import android.content.Intent;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import falcosc.locus.addon.tasker.R.id;
import falcosc.locus.addon.tasker.R.layout;
import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.intent.edit.AbstractDialogFragment;
import falcosc.locus.addon.tasker.utils.Const;

public final class TaskerEditActivity extends AppCompatActivity implements AbstractDialogFragment.EditTaskFinish {

    @Override
    public void onFinish(Intent resultIntent) {
        setResult(RESULT_OK, resultIntent);
        finish();
    }


    private void addActionButtons(ViewGroup viewGroup) {

        LayoutInflater inflater = LayoutInflater.from(this);

        for (LocusActionType type : LocusActionType.values()) {
            View view = inflater.inflate(layout.list_btn, viewGroup);
            Button button = view.findViewById(id.listBtn);
            button.setText(type.getLabelStringId());
            button.setOnClickListener(v -> type.createFragment().show(getSupportFragmentManager(), type.name()));
            viewGroup.addView(view);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO try to hide the dialog if we did open 2nd level dialog
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
                    //TODO log
                }
            }
        }
    }
}
