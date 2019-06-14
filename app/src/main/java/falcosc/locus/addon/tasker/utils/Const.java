package falcosc.locus.addon.tasker.utils;

import java.text.SimpleDateFormat;
import java.util.Locale;

@SuppressWarnings("HardCodedStringLiteral")
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

    public static final long ONE_HOUR = 3600000L;

    public static final int NOTIFICATION_ID_GEOTAG = 1;
    public static final int NOTIFICATION_ID_COMMON_ERROR = 2;


    public static final SimpleDateFormat EXIF_DATE_FORMAT = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);

    private Const() {
    }
}
