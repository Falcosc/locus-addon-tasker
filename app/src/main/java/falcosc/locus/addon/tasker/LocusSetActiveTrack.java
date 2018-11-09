package falcosc.locus.addon.tasker;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Track;

public class LocusSetActiveTrack extends ProjectActivity {

    private static final String ACTIVITY_ALIAS = "LocusSetActiveTrack"; //NON-NLS

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            LocusCache locusCache = LocusCache.getInstance(getApplication());
            Track track = LocusUtils.handleIntentTrackTools(this, getIntent());
            locusCache.setLastSelectedTrack(track);
            Toast.makeText(getApplication(), track.getName() + " " + getResources().getString(R.string.is_active), Toast.LENGTH_LONG).show();
        } catch (RequiredVersionMissingException e) {
            Toast.makeText(getApplication(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
        finish();
    }

    public static void setVisibility(PackageManager packageManager, Boolean isVisible) {
        ComponentName setActiveComponent = new ComponentName(BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + "." + ACTIVITY_ALIAS);
        if (isVisible) {
            packageManager.setComponentEnabledSetting(setActiveComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
        } else {
            //TODO this destroys my singelton
            //packageManager.setComponentEnabledSetting(setActiveComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }
}
