package falcosc.locus.addon.tasker.intent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.Toast;
import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;

public final class TrackToolReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {

        try {

            LocusCache.getInstance(context).lastSelectedTrack = LocusUtils.handleIntentTrackTools(context, intent);

        } catch (RequiredVersionMissingException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
