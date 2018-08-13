package falcosc.locus.addon.tasker.intent;

import android.support.v4.app.DialogFragment;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.intent.edit.NotImplementedDialog;
import falcosc.locus.addon.tasker.intent.edit.UpdateContainerDialog;
import falcosc.locus.addon.tasker.intent.handler.TaskerAction;
import falcosc.locus.addon.tasker.intent.handler.UpdateContainerRequest;

import java.util.concurrent.Callable;

public enum LocusActionType {
    UPDATE_CONTAINER_REQUEST(R.string.act_request_stats_sensors, () -> new UpdateContainerRequest(), () -> new UpdateContainerDialog()),
    ACTION_TASK(R.string.act_exec_task, null, null),
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

    LocusActionType(int label, Callable<TaskerAction> handler, Callable<DialogFragment> editFragment) {

        this.labelStringId = label;
        this.handler = handler;
        this.editFragment = editFragment;
    }

    private final int labelStringId;
    private final Callable<TaskerAction> handler;
    private final Callable<DialogFragment> editFragment;

    public int getLabelStringId(){
        return labelStringId;
    }

    public TaskerAction createHandler() {
        try {
            return handler.call();
        } catch (Exception e) {
            //TODO
            e.printStackTrace();
            return null;
        }
    }

    public DialogFragment createFragment() {
        try {
            if (editFragment == null) {
                return NotImplementedDialog.newInstance(labelStringId);
            }

            return editFragment.call();
        } catch (Exception e) {
            //TODO
            e.printStackTrace();
            return null;
        }
    }
}
