package falcosc.locus.addon.tasker.intent;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;

import falcosc.locus.addon.tasker.intent.handler.*;

public enum LocusActionType {
    UPDATE_CONTAINER_REQUEST(UpdateContainerRequest::new),
    LOCUS_INFO_REQUEST(LocusInfoRequest::new),
    ACTION_TASK(ActionTask::new);

    LocusActionType(@NonNull Callable<TaskerAction> handler) {
        mHandler = handler;
    }

    private final Callable<TaskerAction> mHandler;

    @NonNull
    public TaskerAction createHandler() {
        try {
            return mHandler.call();
        } catch (Exception e) {
            throw new IllegalArgumentException("Handler required for " + name(), e);
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.NotSerializableException {
        throw new java.io.NotSerializableException("");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.NotSerializableException {
        throw new java.io.NotSerializableException("");
    }
}
