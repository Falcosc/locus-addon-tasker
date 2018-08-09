package falcosc.locus.addon.tasker.utils;

import android.arch.core.util.Function;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.widget.Toast;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import java.util.*;

public class LocusCache {
    private static LocusCache instance;

    public final HashSet<String> trackRecordingKeys;
    public final Map<String, LocusField> updateContainerFieldMap;
    public final ArrayList<LocusField> updateContainerFields;
    public final LocusUtils.LocusVersion locusVersion;
    private final Resources locusResources;

    private LocusCache(Context context) {

        Toast.makeText(context, "Locus Addon Tasker Plugin Init", Toast.LENGTH_LONG).show();
        locusVersion = LocusUtils.getActiveVersion(context);

        Resources locusRes = null;
        try {
            locusRes = context.getPackageManager().getResourcesForApplication(locusVersion.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            //TODO
            e.printStackTrace();
        }
        locusResources = locusRes;
        updateContainerFields = createUpdateContainerFields();
        updateContainerFieldMap = createUpdateConfainerFieldMap();
        trackRecordingKeys = createUpdateContainerTrackRecKeys();
    }

    public static LocusCache getInstance(Context context) {
        if (instance == null) {
            instance = new LocusCache(context);
        }
        return instance;
    }

    public static void reset(){
        instance = null;
    }

    private LocusField cLocusField(String taskerVar, String locusResName, Function<UpdateContainer, String> updateContainerGetter) throws IllegalArgumentException{
        String label = null;
        if(locusResources != null){
            int id = locusResources.getIdentifier(locusResName, "string", locusVersion.getPackageName());
            if(id != 0) {
                label = locusResources.getString(id);
            }
        }

        if(StringUtils.isBlank(label)){
            label = taskerVar.replace('_', ' ');
            label = WordUtils.capitalize(label);
        }
        return new LocusField(taskerVar, label, updateContainerGetter);

    }

    private ArrayList<LocusField>  createUpdateContainerFields() throws IllegalArgumentException {
        ArrayList<LocusField> f = new ArrayList<>();
        //this is a custom order
        f.add(cLocusField("my_speed", "speed", u -> String.valueOf(u.getLocMyLocation().getSpeedOptimal())));
        f.add(cLocusField("my_altitude", "altitude", u -> String.valueOf(u.getLocMyLocation().getAltitude())));
        f.add(cLocusField("rec_start_time", "", u -> String.valueOf(u.getTrackRecStats().getStartTime())));

        return f;
    }

    private Map<String, LocusField> createUpdateConfainerFieldMap(){
        Map<String, LocusField> updateContainerFieldMap= new HashMap<>();
        for (LocusField field: updateContainerFields) {
            updateContainerFieldMap.put(field.taskerName, field);
        }

        return updateContainerFieldMap;
    }

    private HashSet<String> createUpdateContainerTrackRecKeys() {
        HashSet<String> trackRecordingKeys = new HashSet<>();
        for (String key : updateContainerFieldMap.keySet()) {
            if (key.startsWith("rec")) trackRecordingKeys.add(key);
        }
        return trackRecordingKeys;
    }
}
