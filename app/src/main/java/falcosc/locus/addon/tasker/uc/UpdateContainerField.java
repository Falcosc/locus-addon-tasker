package falcosc.locus.addon.tasker.uc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import falcosc.locus.addon.tasker.utils.TaskerField;
import locus.api.android.features.periodicUpdates.UpdateContainer;

public class UpdateContainerField extends TaskerField implements ExtUpdateContainerGetter {

    private final Function<UpdateContainer, Object> mUpdateContainerGetter;

    public UpdateContainerField(@NonNull String taskerName, @Nullable String label, @NonNull Function<UpdateContainer, Object> updateContainerGetter) {
        super(taskerName, label);
        mUpdateContainerGetter = updateContainerGetter;
    }

    public String apply(ExtUpdateContainer u) {
        return String.valueOf(mUpdateContainerGetter.apply(u.mUpdateContainer));
    }
}
