package falcosc.locus.addon.tasker;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import falcosc.locus.addon.tasker.utils.ExecutionTimes;

public class CustomCallback implements Handler.Callback {
    private Handler originalHandler;

    public CustomCallback(Handler originalHandler) {
        this.originalHandler = originalHandler;
    }

    @Override
    public boolean handleMessage(Message msg) {
        // Custom handling logic here
        if(msg.what == 113) {
            Log.d("CustomCallback", "RECEIVER message");
            ExecutionTimes.INSTANCE.addDurationSinceLastAdd(ExecutionTimes.Type.FINISH_TO_NEXT_MESSAGE);
        }

        // Delegate to the original handler
        originalHandler.handleMessage(msg);
        return true;
    }
}
