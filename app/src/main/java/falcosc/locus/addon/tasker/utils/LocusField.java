package falcosc.locus.addon.tasker.utils;

import android.arch.core.util.Function;
import android.support.annotation.NonNull;

import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import locus.api.android.features.periodicUpdates.UpdateContainer;

public class LocusField {
    public final String mTaskerName;
    public final String mLabel;
    public final Function<UpdateContainer, Object> mUpdateContainerGetter;

    LocusField(@NonNull String taskerName, @NonNull String label, @NonNull Function<UpdateContainer, Object> updateContainerGetter) {
        if (!TaskerPlugin.variableNameValid("%" + taskerName)) {
            throw new IllegalArgumentException("mTaskerName: " + taskerName + " is not valid");
        }
        mTaskerName = taskerName;
        mLabel = label;
        mUpdateContainerGetter = updateContainerGetter;
    }
}
