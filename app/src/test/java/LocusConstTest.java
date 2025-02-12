import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import locus.api.android.utils.LocusConst;
import locus.api.android.utils.LocusUtils;

/** @noinspection HardCodedStringLiteral*/
public class LocusConstTest {

    Map<String, String> generalImplDetails = new HashMap<>();
    Map<String, String> outOfScope = new HashMap<>();
    Map<String, String> actionTasks = new HashMap<>();
    Map<String, String> notImplementedDoc = new HashMap<>();

    public LocusConstTest() {
        generalImplDetails.put("INTENT_ITEM_TRACK_TOOLS", "Run Tasks via Regex");
        generalImplDetails.put("INTENT_ITEM_SEARCH_LIST", "Run Tasks via Regex");
        generalImplDetails.put("INTENT_EXTRA_NAME", "Run Tasks via Regex");
        generalImplDetails.put("INTENT_EXTRA_ITEM_ID", "Run Tasks via Regex");
        generalImplDetails.put("INTENT_ITEM_POINT_TOOLS", "Run Tasks via Regex");
        generalImplDetails.put("INTENT_EXTRA_ITEMS_ID", "Run Tasks via Regex");
        generalImplDetails.put("INTENT_ITEM_POINTS_SCREEN_TOOLS", "Run Tasks via Regex");
        generalImplDetails.put("INTENT_ITEM_MAIN_FUNCTION_GC", "Run Tasks via Regex");
        generalImplDetails.put("INTENT_ITEM_MAIN_FUNCTION", "Run Tasks via Regex");
        generalImplDetails.put("INTENT_ITEM_GET_LOCATION", "Run Tasks via Regex");
        generalImplDetails.put("INTENT_EXTRA_PACKAGE_NAME", "General");
        generalImplDetails.put("INSTANCE", "General");
        generalImplDetails.put("ACTION_REFRESH_PERIODIC_UPDATE_LISTENERS", "Get Sensor and Stats");
        generalImplDetails.put("VALUE_UPDATE_CONTAINER", "Get Sensor and Stats");
        generalImplDetails.put("ACTION_PERIODIC_UPDATE", "Get Sensor and Stats");
        generalImplDetails.put("INTENT_EXTRA_START_NAVIGATION", "Display Extra");
        generalImplDetails.put("INTENT_EXTRA_CENTER_ON_DATA", "Display Extra");
        generalImplDetails.put("ACTION_REMOVE_DATA_SILENTLY", "Display Extra");
        generalImplDetails.put("ACTION_DISPLAY_DATA_SILENTLY", "Display Extra");
        generalImplDetails.put("ACTION_DISPLAY_DATA", "Display Extra");
        generalImplDetails.put("CONTENT_PROVIDER_PATH_DATA", "Locus Info");
        generalImplDetails.put("CONTENT_PROVIDER_PATH_INFO", "Locus Info");
        generalImplDetails.put("VALUE_LOCUS_INFO", "Locus Info");
        outOfScope.put("INTENT_EXTRA_CONFIRMATION", "Not used");
        outOfScope.put("ACTION_SERVICE_MAP_PROVIDER", "Tasker can't deliver Tiles fast enoght");
        outOfScope.put("ACTION_SERVICE_COMPUTE_TRACK_PROVIDER", "We don't implement track Calculation in Tasker");
        actionTasks.put("INTENT_EXTRA_LONGITUDE", "navigate_to");
        actionTasks.put("INTENT_EXTRA_LATITUDE", "navigate_to");
        actionTasks.put("INTENT_EXTRA_TRACK_REC_PROFILE", "track_record");
        actionTasks.put("INTENT_EXTRA_TRACK_REC_AUTO_SAVE", "track_record");
        actionTasks.put("INTENT_EXTRA_TRACK_REC_ACTION_AFTER", "track_record");
        actionTasks.put("ACTION_TRACK_RECORD_STOP", "track_record");
        actionTasks.put("ACTION_TRACK_RECORD_START", "track_record");
        actionTasks.put("ACTION_TRACK_RECORD_PAUSE", "track_record");
        actionTasks.put("ACTION_TRACK_RECORD_ADD_WPT", "track_record");
        actionTasks.put("VALUE_TRK_REC_ADD_WAYPOINT_BASIC", "track_record");
        actionTasks.put("VALUE_TRK_REC_ADD_WAYPOINT_AUDIO", "track_record");
        actionTasks.put("VALUE_TRK_REC_ADD_WAYPOINT_PHOTO", " track_record");
        actionTasks.put("VALUE_TRK_REC_ADD_WAYPOINT_VIDEO", " track_record");
        actionTasks.put("ACTION_NAVIGATION_START", "navigate_to");
        actionTasks.put("ACTION_LIVE_TRACKING_STOP", "live_tracking_asamm");
        actionTasks.put("ACTION_GUIDING_START", "guide_to");
        notImplementedDoc.put("INTENT_EXTRA_TRACKS_SINGLE", "Import GPX with dialog");
        notImplementedDoc.put("INTENT_EXTRA_TRACKS_MULTI", "Import GPX with dialog");
        notImplementedDoc.put("INTENT_EXTRA_TRACKS_FILE_URI", "Import GPX with dialog");
        notImplementedDoc.put("INTENT_EXTRA_POINT_OVERWRITE", "Import points with dialog");
        notImplementedDoc.put("INTENT_EXTRA_POINTS_FILE_URI", "Import points with dialog");
        notImplementedDoc.put("INTENT_EXTRA_POINTS_FILE_PATH", "Import points with dialog");
        notImplementedDoc.put("INTENT_EXTRA_POINTS_DATA_ARRAY", "Import points with dialog");
        notImplementedDoc.put("INTENT_EXTRA_POINTS_DATA", "Import points with dialog");
        notImplementedDoc.put("INTENT_EXTRA_POINT", "Display new points");
        notImplementedDoc.put("INTENT_EXTRA_LOCATION", "Pick location from Tasker");
        notImplementedDoc.put("INTENT_EXTRA_LOCATION_MAP_CENTER", "run_task_execute"); //TODO variable would be usefully
        notImplementedDoc.put("INTENT_EXTRA_LOCATION_GPS", "run_task_execute"); //TODO variable would be usefully
        notImplementedDoc.put("INTENT_EXTRA_CIRCLES_MULTI", "Display circles");
        notImplementedDoc.put("INTENT_EXTRA_CALL_IMPORT", "Import GPX with dialog");
        notImplementedDoc.put("INTENT_EXTRA_ADDRESS_TEXT", "Navigate to Address String");
        notImplementedDoc.put("ACTION_RECEIVE_LOCATION", "Pick location with Locus UI");
        notImplementedDoc.put("INTENT_EXTRA_GEOCACHE_CODE", "Point Changed Event");
        notImplementedDoc.put("ACTION_POINT_CHANGED", "Point Changed Event");
        notImplementedDoc.put("ACTION_PICK_LOCATION", "Pick location with Locus UI");
        notImplementedDoc.put("INTENT_EXTRA_FIELD_NOTES_IDS", "Get geocaching field notes");
        notImplementedDoc.put("INTENT_EXTRA_FIELD_NOTES_CREATE_LOG", "Log field notes online");
        notImplementedDoc.put("ACTION_LOG_FIELD_NOTES", "Log field notes online");
        notImplementedDoc.put("INTENT_EXTRA_ERROR", "Export Track as FIT/GPX/KML/TCX File");
        notImplementedDoc.put("ACTION_GET_TRACK_AS_FILE_BR", "Export Track as FIT/GPX/KML/TCX File");
        notImplementedDoc.put("ACTION_GET_TRACK_AS_FILE", "Export Track as FIT/GPX/KML/TCX File");
        notImplementedDoc.put("ACTION_DISPLAY_POINT_SCREEN", "Display point detail screen");
        notImplementedDoc.put("INTENT_EXTRA_ADD_NEW_WMS_MAP_URL", "Add WMS map");
        notImplementedDoc.put("ACTION_ADD_NEW_WMS_MAP", "Add WMS map");
        notImplementedDoc.put("ACTION_DISPLAY_STORE_ITEM", "Display point detail screen");
        notImplementedDoc.put("CONTENT_PROVIDER_PATH_ITEM_PURCHASE_STATE", "Check Locus Store purchase state");
        notImplementedDoc.put("PURCHASE_STATE_PURCHASED", "Check Locus Store purchase state");
        notImplementedDoc.put("PURCHASE_STATE_NOT_PURCHASED", "Check Locus Store purchase state");
        notImplementedDoc.put("PURCHASE_STATE_UNKNOWN", "Check Locus Store purchase state");
        notImplementedDoc.put("CONTENT_PROVIDER_AUTHORITY_DATA", "Search point by name");
        notImplementedDoc.put("CONTENT_PROVIDER_AUTHORITY_GEOCACHING", "Search point by name");
        notImplementedDoc.put("CONTENT_PROVIDER_PATH_WAYPOINT", "Search point by name");
        notImplementedDoc.put("CONTENT_PROVIDER_AUTHORITY_MAP_TOOLS", "Export rendered Map screenshot");
        notImplementedDoc.put("CONTENT_PROVIDER_PATH_MAP_PREVIEW", "Export rendered Map screenshot");
        notImplementedDoc.put("VALUE_MAP_PREVIEW", "Export rendered Map screenshot");
        notImplementedDoc.put("VALUE_MAP_PREVIEW_MISSING_TILES", "Export rendered Map screenshot");
        notImplementedDoc.put("CONTENT_PROVIDER_PATH_TRACK_RECORD_PROFILE_NAMES", "Get Track Recording Profiles");
        notImplementedDoc.put("CONTENT_PROVIDER_PATH_TRACK", "Display new tracks");

    }

    @Test
    public void testAllIntentsAreImplemented() {

        List<String> featureStrings = Arrays.stream(LocusConst.class.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .map(Field::getName)
                .filter(field -> !generalImplDetails.containsKey(field))
                .filter(field -> !outOfScope.containsKey(field))
                .filter(field -> !actionTasks.containsKey(field))
                .filter(field -> !notImplementedDoc.containsKey(field))
                .toList();

        Assert.assertTrue(featureStrings.isEmpty(), "Following Features are not implemented: " + StringUtils.join(featureStrings, '\n'));
    }

    @Test
    public void testQueryPackages() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Method packageNamesGetter = LocusUtils.class.getDeclaredMethod("getPackageNames");
        packageNamesGetter.setAccessible(true);

        String[] packageNames = (String[]) packageNamesGetter.invoke(LocusUtils.INSTANCE);
        String[] configuredPackages = {
            "menion.android.locus",
            "menion.android.locus.free.amazon",
            "menion.android.locus.free.samsung",
            "menion.android.locus.pro",
            "menion.android.locus.pro.amazon",
            "menion.android.locus.pro.asamm",
            "menion.android.locus.pro.computerBild"
        };
        Assert.assertNotNull(packageNames);
        Assert.assertEquals(packageNames, configuredPackages,
                "Make sure following Packages are added in Manifest.xml as Query.Package: \n" +
                Arrays.stream(packageNames).map(name -> "<package android:name=\"" + name + "\" />")
                        .collect(Collectors.joining("\n")));
    }
}
