package falcosc.locus.addon.tasker.utils;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import locus.api.android.objects.LocusInfo;


public class LocusInfoField extends TaskerField {

    private final Function<LocusInfo, Object> mLocusInfoGetter;

    public LocusInfoField(@NonNull String taskerName, @NonNull String label, @NonNull Function<LocusInfo, Object> locusInfoGetter) {
        super(taskerName, label);
        mLocusInfoGetter = locusInfoGetter;
    }

    public String apply(LocusInfo locusInfo) {
        return String.valueOf(mLocusInfoGetter.apply(locusInfo));
    }
}
