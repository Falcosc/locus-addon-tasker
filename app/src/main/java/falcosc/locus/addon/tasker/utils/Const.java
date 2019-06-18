package falcosc.locus.addon.tasker.utils;

import org.apache.commons.lang3.time.DateParser;
import org.apache.commons.lang3.time.FastDateFormat;

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

    public static final DateParser EXIF_DATE_FORMAT = FastDateFormat.getInstance("yyyy:MM:dd HH:mm:ss",Locale.US);
    public static final String MIME_TYPE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_TEXT = "text/plain";
    public static final String SCHEMA_MAIL = "mailto";

    private Const() {
    }
}
