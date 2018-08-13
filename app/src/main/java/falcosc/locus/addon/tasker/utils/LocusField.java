package falcosc.locus.addon.tasker.utils;

import android.arch.core.util.Function;
import locus.api.android.features.periodicUpdates.UpdateContainer;

public class LocusField {
    public final String taskerName;
    public final String label;
    public final Function<UpdateContainer, String> updateContainerGetter;

    LocusField(String taskerName, String label, Function<UpdateContainer, String> updateContainerGetter) throws IllegalArgumentException {
        if (!TaskerPlugin.variableNameValid("%" + taskerName)) {
            throw new IllegalArgumentException("taskerName: " + taskerName + " is not valid");
        }
        this.taskerName = taskerName;
        this.label = label;
        this.updateContainerGetter = updateContainerGetter;
    }
}
