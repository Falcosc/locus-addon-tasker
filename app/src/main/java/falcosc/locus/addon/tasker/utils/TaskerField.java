package falcosc.locus.addon.tasker.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;

public class TaskerField {
    public final String mTaskerName;
    public final String mLabel;

    TaskerField(@NonNull String taskerName, @Nullable String label) {
        if (!TaskerPlugin.variableNameValid("%" + taskerName)) {
            throw new IllegalArgumentException("mTaskerName: " + taskerName + " is not valid");
        }
        mTaskerName = taskerName;
        if (StringUtils.isBlank(label)) {
            mLabel = WordUtils.capitalize(taskerName.replace('_', ' '));
        } else {
            mLabel = label;
        }
    }

    public TaskerField(TaskerField field) {
        mTaskerName = field.mTaskerName;
        mLabel = field.mLabel;
    }
}
