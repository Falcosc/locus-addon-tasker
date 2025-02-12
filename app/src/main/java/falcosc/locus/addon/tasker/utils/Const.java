package falcosc.locus.addon.tasker.utils;

import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Locale;

@SuppressWarnings({"WeakerAccess"})
public final class Const {
    /**
     * field list key for get update container
     */
    public static final String INTENT_EXTRA_FIELD_LIST = "INTENT_EXTRA_FIELD_LIST";

    public static final String INTENT_EXTRA_FIELD_JSON = "INTENT_EXTRA_FIELD_JSON";

    public static final String INTEND_EXTRA_ADDON_ACTION_TYPE = "LOCUS_ADDON_ACTION_TYPE";

    public static final String INTENT_ACTION_TASK_EXTRA_KEY = "tasks";

    public static final String INTENT_EXTRA_GEOTAG_FILES = "files";

    public static final String INTENT_EXTRA_GEOTAG_OFFSET = "offset";

    public static final String INTENT_EXTRA_GEOTAG_REPORT_NON_MATCH = "REPORT_NON_MATCH";

    public static final String INTENT_EXTRA_TRK_POINTS_TYPE = "INTENT_EXTRA_TRK_POINTS_TYPE";
    public static final String INTENT_EXTRA_TRK_SOURCE = "INTENT_EXTRA_TRK_SOURCE";
    public static final String INTENT_EXTRA_LOCATION_FIELDS = "INTENT_EXTRA_LOCATION_FIELDS";
    public static final String INTENT_EXTRA_WAYPOINT_FIELDS = "INTENT_EXTRA_WAYPOINT_FIELDS";
    public static final String INTENT_EXTRA_OFFSET = "INTENT_EXTRA_OFFSET";
    public static final String INTENT_EXTRA_COUNT = "INTENT_EXTRA_COUNT";

    public static final int NOTIFICATION_ID_GEOTAG = 1;
    public static final int NOTIFICATION_ID_COMMON_ERROR = 2;

    public static final FastDateFormat EXIF_DATE_FORMAT = FastDateFormat.getInstance("yyyy:MM:dd HH:mm:ss", Locale.US);
    public static final String MIME_TYPE_IMAGES = "image/*";
    public static final String MIME_TYPE_WEBP = "image/webp";
    public static final String MIME_TYPE_PNG = "image/png";
    public static final String MIME_TYPE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_TEXT = "text/plain";
    public static final String SCHEMA_MAIL = "mailto";
    public static final String AUTHORITY_EXTERNAL_STORAGE = "com.android.externalstorage.documents";

    public static final String NOTIFICATION_CHANNEL_ID = "LOCUS_TASKER";
    public static final long NOTIFICATION_REPEAT_AFTER = 300L;
    public static final TaskerField ERROR_MSG_VAR = new TaskerField("errmsg", "Error Message"); // NON-NLS

    private Const() {
    }
}
