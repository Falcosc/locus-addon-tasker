package falcosc.locus.addon.tasker.utils;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import falcosc.locus.addon.tasker.thridparty.TaskerPlugin;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusInfo;

public class LocusInfoField {
    public final String mTaskerName;
    public final String mLabel;

    private Function<LocusInfo, Object> mLocusInfoGetter;

    LocusInfoField(@NonNull String taskerName, @NonNull String label) {
        if (!TaskerPlugin.variableNameValid("%" + taskerName)) {
            throw new IllegalArgumentException("mTaskerName: " + taskerName + " is not valid");
        }
        mTaskerName = taskerName;
        mLabel = label;
    }

    public LocusInfoField(@NonNull String taskerName, @NonNull String label, @NonNull Function<LocusInfo, Object> locusInfoGetter) {
        this(taskerName, label);
        mLocusInfoGetter = locusInfoGetter;
    }

    public String apply(LocusInfo locusInfo) {
        return String.valueOf(mLocusInfoGetter.apply(locusInfo));
    }
}
