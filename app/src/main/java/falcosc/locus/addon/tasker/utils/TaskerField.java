package falcosc.locus.addon.tasker.utils;

import androidx.annotation.NonNull;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;

public class TaskerField {
    public final String mTaskerName;
    public final String mLabel;

    TaskerField(@NonNull String taskerName, @NonNull String label) {
        if (!TaskerPlugin.variableNameValid("%" + taskerName)) {
            throw new IllegalArgumentException("mTaskerName: " + taskerName + " is not valid");
        }
        mTaskerName = taskerName;
        mLabel = label;
    }

    public TaskerField(TaskerField field) {
        mTaskerName = field.mTaskerName;
        mLabel = field.mLabel;
    }
}
