package falcosc.locus.addon.tasker.intent.edit;

import android.content.Intent;
import android.support.v4.app.DialogFragment;

public abstract class AbstractDialogFragment extends DialogFragment {

    public interface EditTaskFinish {

        void onFinish(Intent resultIntent);

    }

    public void finish(Intent resultIntent) {
        EditTaskFinish activity = (EditTaskFinish) getActivity();
        activity.onFinish(resultIntent);
    }
}
