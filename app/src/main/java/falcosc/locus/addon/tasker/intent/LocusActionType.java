package falcosc.locus.addon.tasker.intent;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import java.util.concurrent.Callable;

import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.edit.*;
import falcosc.locus.addon.tasker.intent.handler.*;

public enum LocusActionType {
    UPDATE_CONTAINER_REQUEST(R.string.act_request_stats_sensors, UpdateContainerRequest::new, UpdateContainerDialog::new),
    ACTION_TASK(R.string.act_exec_task, ActionTask::new, ActionTaskDialog::new),
    IMPORT_POINTS(R.string.act_import_points, null, null),
    IMPORT_GPX(R.string.act_import_gpx, null, null),
    DISPLAY_POINTS(R.string.act_display_points, null, null),
    DISPLAY_GEOCACHES(R.string.act_display_geocaches, null, null),
    GET_FIELD_NOTES(R.string.act_get_geocache_field_notes, null, null),
    LOG_FIELD_NOTES(R.string.act_log_field_notes, null, null),
    SEARCH_POINT_BY_NAME(R.string.act_search_point_by_name, null, null),
    DISPLAY_POINT_SCREEN(R.string.act_display_point_screen, null, null),
    DISPLAY_TRACKS(R.string.act_display_tracks, null, null),
    START_NAV_TO(R.string.act_start_nav_to, null, null),
    PICK_LOCATION(R.string.act_pick_location, null, null),
    GET_ROOT_DIR(R.string.act_get_root_dir, null, null),
    ADD_WMS_MAP(R.string.act_add_wms_map, null, null),
    DISPLAY_CIRCLES(R.string.act_display_circles, null, null);

    LocusActionType(int labelStringId, @Nullable Callable<TaskerAction> handler, @Nullable Callable<DialogFragment> editFragment) {
        mLabelStringId = labelStringId;
        mHandler = handler;
        mEditFragment = editFragment;
    }

    private final int mLabelStringId;
    private final Callable<TaskerAction> mHandler;
    private final Callable<DialogFragment> mEditFragment;

    public int getLabelStringId() {
        return mLabelStringId;
    }

    public boolean isNotImplemented() {
        return (mHandler == null) || (mEditFragment == null);
    }

    @NonNull
    public TaskerAction createHandler() {

        TaskerAction handlerInstance = null;
        try {
            if (mHandler != null) {
                handlerInstance = mHandler.call();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Handler required for " + name(), e);
        }

        if (handlerInstance == null) {
            throw new IllegalArgumentException("Handler required for " + name());
        }
        return handlerInstance;

    }

    @NonNull
    public DialogFragment createFragment() {
        try {
            if (mEditFragment == null) {
                return NotImplementedDialog.newInstance(mLabelStringId);
            }

            return mEditFragment.call();
        } catch (Exception e) {
            throw new IllegalArgumentException("Dialog Fragment required for " + name(), e);
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.NotSerializableException {
        throw new java.io.NotSerializableException("");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.NotSerializableException {
        throw new java.io.NotSerializableException("");
    }
}
