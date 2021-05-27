package falcosc.locus.addon.tasker.uc;

import androidx.annotation.NonNull;
import locus.api.android.features.periodicUpdates.UpdateContainer;

public class ExtUpdateContainer {

    @SuppressWarnings("InstanceVariableOfConcreteClass")
    @NonNull
    public final UpdateContainer mUpdateContainer;

    public ExtUpdateContainer(@NonNull UpdateContainer updateContainer) {
        mUpdateContainer = updateContainer;
    }

    @SuppressWarnings("InstanceVariableOfConcreteClass")
    private NavigationProgress mNavigationProgress;

    public NavigationProgress getNavigationProgress() {
        if (mNavigationProgress == null) {
            mNavigationProgress = new NavigationProgress(mUpdateContainer);
        }
        return mNavigationProgress;
    }

}
