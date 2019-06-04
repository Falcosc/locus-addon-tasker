package falcosc.locus.addon.tasker.utils;

import androidx.annotation.NonNull;
import locus.api.android.features.periodicUpdates.UpdateContainer;

public class ExtUpdateContainer {

    @SuppressWarnings("InstanceVariableOfConcreteClass")
    @NonNull
    public final UpdateContainer mUpdateContainer;

    ExtUpdateContainer(@NonNull UpdateContainer updateContainer){
        mUpdateContainer = updateContainer;
    }

    @SuppressWarnings("InstanceVariableOfConcreteClass")
    private NavigationProgress mNavigationProgress;

    NavigationProgress getNavigationProgress() {
        if(mNavigationProgress == null){
            mNavigationProgress = NavigationProgress.calculate(mUpdateContainer);
        }
        return mNavigationProgress;
    }

}
