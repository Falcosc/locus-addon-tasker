package falcosc.locus.addon.tasker.utils;

import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.annotation.NonNull;

import locus.api.android.features.periodicUpdates.UpdateContainer;

public class UpdateContainerField extends TaskerField implements ExtUpdateContainerGetter {

    private final Function<UpdateContainer, Object> mUpdateContainerGetter;

    UpdateContainerField(@NonNull String taskerName, @Nullable String label, @NonNull Function<UpdateContainer, Object> updateContainerGetter) {
        super(taskerName, label);
        mUpdateContainerGetter = updateContainerGetter;
    }

    public String apply(ExtUpdateContainer u) {
        return String.valueOf(mUpdateContainerGetter.apply(u.mUpdateContainer));
    }
}
