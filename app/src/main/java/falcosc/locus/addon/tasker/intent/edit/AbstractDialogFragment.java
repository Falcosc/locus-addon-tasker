package falcosc.locus.addon.tasker.intent.edit;

import android.app.Dialog;
import android.content.Intent;
import android.support.v4.app.DialogFragment;

public abstract class AbstractDialogFragment extends DialogFragment {

    public interface EditTaskFinish {

        void onFinish(Intent resultIntent, Dialog hints);

    }

    void finish(Intent resultIntent, Dialog hints) {
        EditTaskFinish activity = (EditTaskFinish) requireActivity();
        activity.onFinish(resultIntent, hints);
    }
}
