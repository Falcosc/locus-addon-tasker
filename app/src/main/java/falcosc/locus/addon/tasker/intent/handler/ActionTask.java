package falcosc.locus.addon.tasker.intent.handler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;
import falcosc.locus.addon.tasker.R;

public class ActionTask implements TaskerAction {
    @Override
    public void handle(@NonNull Context context, @NonNull Intent intent, @NonNull Bundle apiExtraBundle, @NonNull BroadcastReceiver receiver) {
        //TODO implement it
        Toast.makeText(context, R.string.not_implemented, Toast.LENGTH_LONG).show();
    }
}
