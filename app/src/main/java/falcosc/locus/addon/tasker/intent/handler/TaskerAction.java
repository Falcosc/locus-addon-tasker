package falcosc.locus.addon.tasker.intent.handler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

public interface TaskerAction {
    void setContext(@NonNull Context context, @NonNull BroadcastReceiver receiver);
    void handle(@NonNull Intent intent, @NonNull Bundle apiExtraBundle);
}
