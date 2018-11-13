package falcosc.locus.addon.tasker.utils;

import androidx.arch.core.util.Function;
import androidx.annotation.NonNull;

import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import locus.api.android.features.periodicUpdates.UpdateContainer;

public class LocusField {
    public final String mTaskerName;
    public final String mLabel;

    private Function<UpdateContainer, Object> mUpdateContainerGetter;

    LocusField(@NonNull String taskerName, @NonNull String label) {
        if (!TaskerPlugin.variableNameValid("%" + taskerName)) {
            throw new IllegalArgumentException("mTaskerName: " + taskerName + " is not valid");
        }
        mTaskerName = taskerName;
        mLabel = label;
    }

    LocusField(@NonNull String taskerName, @NonNull String label, @NonNull Function<UpdateContainer, Object> updateContainerGetter) {
        this(taskerName, label);
        mUpdateContainerGetter = updateContainerGetter;
    }

    public String apply(ExtUpdateContainer u) {
        return String.valueOf(mUpdateContainerGetter.apply(u.mUpdateContainer));
    }
}
