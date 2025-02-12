package falcosc.locus.addon.tasker.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NonNls;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;

public class TaskerField {
    public final String mTaskerName;
    public final String mLabel;

    public TaskerField(@NonNls @NonNull String taskerName, @Nullable String label) {
        if (!TaskerPlugin.variableNameValid(TaskerPlugin.VARIABLE_PREFIX + taskerName)) {
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

    public String getVar() {
        return TaskerPlugin.VARIABLE_PREFIX + mTaskerName;
    }

    public String getVarDesc() {
        return getVar() + "\n" + mLabel + "\n";
    }

    public String getVarDesc(String htmlDescription) {
        return getVar() + "\n" + mLabel + "\n" + htmlDescription;
    }

    public String getVarDesc(Map<String, Object> jsonDesc, String jsonDescPrefix) throws JSONException {
        String jsonString = new JSONObject(jsonDesc).toString(2);
        //noinspection DynamicRegexReplaceableByCompiledPattern
        jsonString = jsonString.replace("\n", "<br/>").replace("  ", "&nbsp;&nbsp;"); //NON-NLS
        return getVar() + "\n"
                + mLabel + " " + String.join(", ", jsonDesc.keySet()) + "\n"
                + jsonDescPrefix + " " + jsonString;
    }
}
