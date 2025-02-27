package falcosc.locus.addon.tasker.utils;

import com.asamm.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import falcosc.locus.addon.tasker.intent.edit.TrackPointsEdit;
import falcosc.locus.addon.tasker.intent.handler.TrackPointsRequest;
import locus.api.objects.extra.GeoDataExtra;
import locus.api.objects.extra.Location;
import locus.api.objects.geoData.GeoDataHelperKt;
import locus.api.objects.geoData.Point;
import locus.api.objects.geoData.Track;

public class TrackPointCache {
    private static final String NAME = "Name"; //NON-NLS
    private static final String ID = "Id"; //NON-NLS
    private static final String WAYPOINT_INDEX = "WaypointIndex";

    public static final String DEFAULT_FIELDS = "Description, Comment, RteIndex";
    public static final String ROUTING_FIELDS = "Description, Comment, RteIndex, RteTimeI, RteDistanceF, RteSpeedF, RtePointAction, " +
            "RtePointPassPlaceNotify, RteStreet";

    /**
     * @noinspection HardCodedStringLiteral these are technical field names
     */
    private static final Map<String, Integer> fieldToWaypointExtra = Map.ofEntries(
            Map.entry("Description", GeoDataExtra.PAR_DESCRIPTION),
            Map.entry("Comment", GeoDataExtra.PAR_COMMENT),
            Map.entry("RelativeWorkingDir", GeoDataExtra.PAR_RELATIVE_WORKING_DIR),
            Map.entry("Type", GeoDataExtra.PAR_TYPE),
            Map.entry("GeocacheCode", GeoDataExtra.PAR_GEOCACHE_CODE),
            Map.entry("PoiAlertInclude", GeoDataExtra.PAR_POI_ALERT_INCLUDE),
            Map.entry("Language", GeoDataExtra.PAR_LANGUAGE),
            Map.entry("AddressStreet", GeoDataExtra.PAR_ADDRESS_STREET),
            Map.entry("AddressCity", GeoDataExtra.PAR_ADDRESS_CITY),
            Map.entry("AddressRegion", GeoDataExtra.PAR_ADDRESS_REGION),
            Map.entry("AddressPostCode", GeoDataExtra.PAR_ADDRESS_POST_CODE),
            Map.entry("AddressCountry", GeoDataExtra.PAR_ADDRESS_COUNTRY),
            Map.entry("RteIndex", GeoDataExtra.PAR_RTE_INDEX),
            Map.entry("RteDistanceF", GeoDataExtra.PAR_RTE_DISTANCE_F),
            Map.entry("RteTimeI", GeoDataExtra.PAR_RTE_TIME_I),
            Map.entry("RteSpeedF", GeoDataExtra.PAR_RTE_SPEED_F),
            Map.entry("RteTurnCost", GeoDataExtra.PAR_RTE_TURN_COST),
            Map.entry("RteStreet", GeoDataExtra.PAR_RTE_STREET),
            Map.entry("RtePointAction", GeoDataExtra.PAR_RTE_POINT_ACTION),
            Map.entry("RtePointPassPlaceNotify", GeoDataExtra.PAR_RTE_POINT_PASS_PLACE_NOTIFY),
            Map.entry("RteComputeType", GeoDataExtra.PAR_RTE_COMPUTE_TYPE),
            Map.entry("RteSimpleRoundabouts", GeoDataExtra.PAR_RTE_SIMPLE_ROUNDABOUTS),
            Map.entry("RtePlanDefinition", GeoDataExtra.PAR_RTE_PLAN_DEFINITION),
            Map.entry("RteMaxSpeeds", GeoDataExtra.PAR_RTE_MAX_SPEEDS),
            Map.entry("RteWayTypes", GeoDataExtra.PAR_RTE_WAY_TYPES),
            Map.entry("RteSurfaces", GeoDataExtra.PAR_RTE_SURFACES),
            Map.entry("RteWarnings", GeoDataExtra.PAR_RTE_WARNINGS),
            Map.entry("OsmNotesId", GeoDataExtra.PAR_OSM_NOTES_ID),
            Map.entry("OsmNotesClosed", GeoDataExtra.PAR_OSM_NOTES_CLOSED),
            Map.entry("LopointsId", GeoDataExtra.PAR_LOPOINTS_ID),
            Map.entry("LopointsLabels", GeoDataExtra.PAR_LOPOINTS_LABELS),
            Map.entry("LopointsOpeningHours", GeoDataExtra.PAR_LOPOINTS_OPENING_HOURS),
            Map.entry("LopointsTimezone", GeoDataExtra.PAR_LOPOINTS_TIMEZONE),
            Map.entry("LopointsGeometry", GeoDataExtra.PAR_LOPOINTS_GEOMETRY),
            Map.entry("Lomedia", GeoDataExtra.PAR_LOMEDIA),
            Map.entry("LopointReviews", GeoDataExtra.PAR_LOPOINT_REVIEWS)
    );

    /**
     * @noinspection HardCodedStringLiteral these are technical field names
     */
    private static final Map<String, Function<Location, Object>> fieldToLocGetter = Map.ofEntries(
            Map.entry(ID, Location::getId),
            Map.entry("Lat", Location::getLatitude),
            Map.entry("Lon", Location::getLongitude),
            Map.entry("Altitude", Location::getAltitude),
            Map.entry("Time", loc -> (loc.getTime() == 0L) ? null : loc.getTime()),
            Map.entry("Bearing", Location::getBearing),
            Map.entry("Speed", Location::getSpeed)
    );

    public final Track mTrack;
    public String[] mSelectLocFields;
    public String[] mSelectWaypointExtras;
    private int mSelectCount;
    private int mSelectOffset;

    private final JSONObject[] mWaypoints;
    private final JSONObject[] mPoints;
    private static final String TAG = "TrackPointCache";

    public TrackPointCache(@NonNull Track track) {
        mTrack = track;
        mWaypoints = serializeWayPoints(track.getWaypoints());
        mPoints = serializePoints(track.getPoints());
        try {
            for (int i = 0; i < track.getWaypoints().size(); i++) {
                int trackPointIndex = GeoDataHelperKt.getParameterRteIndex(track.getWaypoints().get(i));
                if ((trackPointIndex > -1) && (trackPointIndex < mPoints.length)) {
                    mPoints[trackPointIndex].put(WAYPOINT_INDEX, i);
                }
            }
        } catch (JSONException e) {
            Logger.e(e, TAG); //no message because index can't raise conversion issues
        }
    }

    @NonNull
    private static JSONObject[] serializeWayPoints(@NonNull List<Point> points) {
        JSONObject[] jsonObjects = new JSONObject[points.size()];
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            JSONObject jsonPoint = serializeTrackLocation(p.getLocation());
            GeoDataExtra extra = p.getExtraData();
            try {
                if (extra != null) {
                    for (Map.Entry<String, Integer> entry : fieldToWaypointExtra.entrySet()) {
                        Object value = extra.getParameter(entry.getValue());
                        if (value != null) {
                            jsonPoint.put(entry.getKey(), value);
                        }
                    }
                    if (jsonPoint.has("RtePointAction")) {
                        jsonPoint.put("RtePointAction", GeoDataHelperKt.getParameterRteAction(p));
                    }
                }
                jsonPoint.put(ID, p.getId()); //Overwrite location id with waypoint id
                jsonPoint.put(NAME, p.getName());

            } catch (JSONException e) {
                Logger.e(e, TAG, "Can't process Waypoint: " + jsonPoint); //NON-NLS
            }
            jsonObjects[i] = jsonPoint;
        }
        return jsonObjects;
    }

    @NonNull
    private static JSONObject serializeTrackLocation(@NonNull Location loc) {
        JSONObject jsonPoint = new JSONObject();
        for (Map.Entry<String, Function<Location, Object>> entry : fieldToLocGetter.entrySet()) {
            Object value = entry.getValue().apply(loc);
            if (value != null) {
                try {
                    jsonPoint.put(entry.getKey(), value);
                } catch (JSONException e) {
                    Logger.e(e, TAG, "Can't convert " + entry.getKey() + " " + value); //NON-NLS
                }
            }
        }
        return jsonPoint;
    }

    @NonNull
    private static JSONObject[] serializePoints(@NonNull List<Location> points) {
        JSONObject[] jsonObjects = new JSONObject[points.size()];
        for (int i = 0; i < points.size(); i++) {
            jsonObjects[i] = serializeTrackLocation(points.get(i));
        }
        return jsonObjects;
    }

    @NonNull
    private JSONArray getScope(@NonNull JSONObject[] sourceObjects, String[] selectFields) {
        int sourceCount = Math.max(0, sourceObjects.length - mSelectOffset);
        int count = Math.min(sourceCount, mSelectCount);
        JSONArray jsonArray = new JSONArray();
        try {
            for (int i = 0; i < count; i++) {
                jsonArray.put(new JSONObject(sourceObjects[mSelectOffset + i], selectFields));
            }
        } catch (JSONException e) {
            Logger.e(e, TAG); //impossible, we only copy already validated json objects
        }
        return jsonArray;
    }

    public void setSelectFields(@Nullable String locFields, @Nullable String waypointExtras) {
        mSelectLocFields = findMatchingFields(locFields, fieldToLocGetter.keySet());
        mSelectWaypointExtras = findMatchingFields(waypointExtras, fieldToWaypointExtra.keySet());
    }

    public void setSelectAmount(int count, int offset) {
        mSelectCount = count;
        mSelectOffset = Math.max(0, offset);
    }

    @NonNull
    public static List<String> getValidLocationFields() {
        List<String> fields = new ArrayList<>(fieldToLocGetter.keySet());
        fields.add(WAYPOINT_INDEX);
        fields.sort(String::compareTo);
        return fields;
    }

    @NonNull
    public static List<String> getValidWaypointExtras() {
        return fieldToWaypointExtra.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @NonNull
    public static String[] findMatchingFields(@Nullable String userFields, @NonNull Set<String> fields) {
        if (userFields == null) {
            return new String[0];
        }
        Map<String, String> lowerFields = fields.stream().collect(Collectors.toMap(
                field -> field.toLowerCase(Locale.ROOT),
                field -> field
        ));
        return getFieldsTrimmed(userFields).map(userField -> lowerFields.get(userField.toLowerCase(Locale.ROOT)))
                .filter(Objects::nonNull).toArray(String[]::new);
    }

    @NonNull
    public static Collection<String> findInvalidFields(@Nullable String userFields, @NonNull Collection<String> fields) {
        if (userFields == null) {
            return new ArrayList<>();
        }
        Map<String, String> lowerUserFields = getFieldsTrimmed(userFields)
                .filter(field -> !field.isEmpty())
                .filter(field -> (field.charAt(0) != '%'))
                .collect(Collectors.toMap(
                        field -> field.toLowerCase(Locale.ROOT),
                        field -> field
                ));

        fields.forEach(knownField -> lowerUserFields.remove(knownField.toLowerCase(Locale.ROOT)));
        return lowerUserFields.values();
    }

    @NonNull
    public static Stream<String> getFieldsTrimmed(@NonNull String userFields) {
        return Arrays.stream(userFields.split(","))
                .map(String::trim);
    }

    @NonNull
    public JSONObject getSelectedSegment(@NonNull TrackPointsRequest.Type pointType) {
        JSONObject jsonResponse = new JSONObject();
        try {
            jsonResponse.put(TrackPointsEdit.OFFSET_KEY, mSelectOffset);
            if (pointType == TrackPointsRequest.Type.WAYPOINTS) {
                jsonResponse.put(TrackPointsEdit.WAYPOINTS_KEY, getScope(mWaypoints, mSelectWaypointExtras));
            } else if (pointType == TrackPointsRequest.Type.POINTS) {
                jsonResponse.put(TrackPointsEdit.POINTS_KEY, getScope(mPoints, mSelectLocFields));
            } else {
                jsonResponse.put(TrackPointsEdit.WAYPOINTS_KEY, getScope(mWaypoints, mSelectWaypointExtras));
                jsonResponse.put(TrackPointsEdit.POINTS_KEY, getScope(mPoints, mSelectLocFields));
            }
        } catch (JSONException e) {
            Logger.e(e, TAG); //impossible, we only put JSONArrays into the result
        }
        return jsonResponse;
    }
}
