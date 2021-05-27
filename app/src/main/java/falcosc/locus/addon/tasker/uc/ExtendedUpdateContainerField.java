package falcosc.locus.addon.tasker.uc;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import falcosc.locus.addon.tasker.utils.TaskerField;

public class ExtendedUpdateContainerField extends TaskerField implements ExtUpdateContainerGetter {
    private final Function<ExtUpdateContainer, Object> mExtUpdateContainerGetter;

    public ExtendedUpdateContainerField(@NonNull String taskerName, @NonNull String label, Function<ExtUpdateContainer, Object> extUpdateContainerGetter) {
        super(taskerName, label);
        mExtUpdateContainerGetter = extUpdateContainerGetter;
    }

    public String apply(ExtUpdateContainer u) {
        return String.valueOf(mExtUpdateContainerGetter.apply(u));
    }
}
