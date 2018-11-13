package falcosc.locus.addon.tasker.utils;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;

public class ExtendedLocusField extends LocusField {
    private final Function<ExtUpdateContainer, Object> mExtUpdateContainerGetter;

    ExtendedLocusField(@NonNull String taskerName, @NonNull String label, Function<ExtUpdateContainer, Object> extUpdateContainerGetter) {
        super(taskerName, label);
        mExtUpdateContainerGetter = extUpdateContainerGetter;
    }

    @Override
    public String apply(ExtUpdateContainer u) {
        return String.valueOf(mExtUpdateContainerGetter.apply(u));
    }
}
