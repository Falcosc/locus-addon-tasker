package falcosc.locus.addon.tasker.receivers;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.Toast;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Track;

public final class TrackToolReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {

        try {
            final Track track = LocusUtils.handleIntentTrackTools(context, intent);

            if (track == null) {
                Toast.makeText(context, "Wrong INTENT - no track!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Received intent with track:\n\n" + track.getName() + "\n\ndesc:" + track.getParameterDescription(), Toast.LENGTH_SHORT).show();
            }
        } catch (RequiredVersionMissingException e) {
            e.printStackTrace();
        }
    }
}
