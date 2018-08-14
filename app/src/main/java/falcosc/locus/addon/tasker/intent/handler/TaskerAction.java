package falcosc.locus.addon.tasker.intent.handler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

public interface TaskerAction {
    void handle(@NonNull Context context, @NonNull Intent intent, @NonNull Bundle apiExtraBundle, @NonNull BroadcastReceiver receiver);

}
