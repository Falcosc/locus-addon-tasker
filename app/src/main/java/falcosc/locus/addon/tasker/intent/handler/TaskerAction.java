package falcosc.locus.addon.tasker.intent.handler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

public interface TaskerAction {
    void handle(@NonNull final Context context, Intent intent, @NonNull final Bundle bundle, @NonNull final BroadcastReceiver receiver) throws Exception;

}
