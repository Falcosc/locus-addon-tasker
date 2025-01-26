package falcosc.locus.addon.tasker.utils;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import com.asamm.logger.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import falcosc.locus.addon.tasker.BuildConfig;
import falcosc.locus.addon.tasker.RequiredDataMissingException;
import falcosc.locus.addon.tasker.reminder.VersionSelectReminder;
import falcosc.locus.addon.tasker.uc.ExtUpdateContainer;
import falcosc.locus.addon.tasker.uc.ExtUpdateContainerGetter;
import falcosc.locus.addon.tasker.uc.NavigationProgress;
import falcosc.locus.addon.tasker.uc.UpdateContainerFieldFactory;
import locus.api.android.ActionBasics;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.objects.LocusVersion;
import locus.api.android.objects.VersionCode;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;

public final class LocusCache {
    private static final String TAG = "LocusCache"; //NON-NLS
    private static final Object mSyncObj = new Object();
    private static final long UPDATE_CONTAINER_EXPIRATION = 950L;

    private static volatile LocusCache mInstance;

    private final Application mApplicationContext;

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Application getApplicationContext() {
        return mApplicationContext;
    }

    public final Set<String> mTrackRecordingKeys;
    public final Set<String> mTrackGuideKeys;
    public final Set<String> mLocationProgressKeys;
    public final Map<String, ExtUpdateContainerGetter> mExtUpdateContainerFieldMap;
    public final ArrayList<TaskerField> mUpdateContainerFields;
    public LocusVersion mLocusVersion;

    //selected track fields
    @Nullable
    public NavigationProgress.TrackData mLastSelectedTrack;

    //empty update container to avoid null checks because result is normally never null
    @SuppressWarnings("InstanceVariableOfConcreteClass")
    private ExtUpdateContainer mExtUpdateContainer = new ExtUpdateContainer(new UpdateContainer());
    private long mUpdateContainerExpiration;

    //reminders
    public final VersionSelectReminder versionSelectReminder;

    @SuppressWarnings("HardCodedStringLiteral")
    private LocusCache(Application context) {
        Logger.INSTANCE.registerLogger(new CacheFileLogger(context));
        Logger.d(TAG, "init Locus cache");
        Logger.d(TAG, BuildConfig.VERSION_NAME);

        mApplicationContext = context;

        mLocusVersion = LocusUtils.INSTANCE.getActiveVersion(context, VersionCode.UPDATE_01);
        Logger.d(TAG, "Locus version: " + mLocusVersion);

        Resources locusRes = null;
        try {
            locusRes = context.getPackageManager().getResourcesForApplication(mLocusVersion.getPackageName());
            Logger.d(TAG, "Found Locus resources");
        } catch (Exception e) {
            Logger.d(e, TAG, "Missing Locus resources");
        }
        Resources locusResources = locusRes;

        UpdateContainerFieldFactory factory = new UpdateContainerFieldFactory(locusResources, mLocusVersion);

        mUpdateContainerFields = factory.createUpdateContainerFields();
        mUpdateContainerFields.addAll(factory.createMapFields());

        ArrayList<TaskerField> trackRecStatsFields = factory.createTrackRecStatsFields();
        mTrackRecordingKeys = getLocusFieldKeys(trackRecStatsFields);
        mUpdateContainerFields.addAll(trackRecStatsFields);

        ArrayList<TaskerField> guideFields = factory.createGuideFields();
        mTrackGuideKeys = getLocusFieldKeys(guideFields);
        mUpdateContainerFields.addAll(guideFields);

        ArrayList<TaskerField> navigationProgressFields = UpdateContainerFieldFactory.createNavigationProgressFields();
        mLocationProgressKeys = getLocusFieldKeys(navigationProgressFields);
        mUpdateContainerFields.addAll(navigationProgressFields);

        mExtUpdateContainerFieldMap = createExtUpdateContainerFieldMap();

        Logger.d(TAG, "Locus fields created: " + mUpdateContainerFields.size());
        Logger.d(TAG, "Locus Field keys mapped - recording keys: " + mTrackRecordingKeys.size());
        Logger.d(TAG, "Locus Field keys mapped - guiding keys: " + mTrackGuideKeys.size());

        versionSelectReminder = new VersionSelectReminder(mApplicationContext);
    }

    @NonNull
    public LocusVersion requireLocusVersion() throws RequiredDataMissingException {
        if (mLocusVersion == null) {
            throw new RequiredDataMissingException("Locus Maps need to be installed");
        }
        return mLocusVersion;
    }

    @NonNull
    public static LocusCache getInstance(@NonNull Application context) {

        if (mInstance == null) {
            synchronized (mSyncObj) {
                if (mInstance == null) {
                    mInstance = new LocusCache(context);
                }
            }
        }
        return mInstance;
    }

    @NonNull
    public static LocusCache getInstanceUnsafe(@NonNull Context context) throws MissingAppContextException {
        if (mInstance == null) {
            Context appContext = context.getApplicationContext();
            if (appContext instanceof Application) {
                return getInstance((Application) appContext);
            } else {
                throw new MissingAppContextException();
            }
        }
        return mInstance;
    }

    @Nullable
    public static LocusCache getInstanceNullable() {
        return mInstance;
    }

    public static void initAsync(@NonNull Application context) {
        if (mInstance == null) {
            Thread thread = new Thread(() -> getInstance(context));
            thread.start();
        }
    }

    /**
     * used for debugging without process termination
     */
    @SuppressWarnings("unused")
    public static void reset() {
        mInstance = null;
    }

    @NonNull
    private Map<String, ExtUpdateContainerGetter> createExtUpdateContainerFieldMap() {
        Map<String, ExtUpdateContainerGetter> updateContainerFieldMap = new HashMap<>();
        for (TaskerField field : mUpdateContainerFields) {
            //do cast only once
            updateContainerFieldMap.put(field.mTaskerName, (ExtUpdateContainerGetter) field);
        }

        return updateContainerFieldMap;
    }

    @NonNull
    private static Set<String> getLocusFieldKeys(List<TaskerField> fields) {
        Set<String> keys = new HashSet<>();
        for (TaskerField field : fields) {
            keys.add(field.mTaskerName);
        }
        return keys;
    }

    @NonNull
    public ExtUpdateContainer getUpdateContainer() throws RequiredVersionMissingException, RequiredDataMissingException {
        long requestTime = System.currentTimeMillis();
        if (requestTime > mUpdateContainerExpiration) {
            UpdateContainer container = ActionBasics.INSTANCE.getUpdateContainer(mApplicationContext, requireLocusVersion());
            if (container != null) {
                mExtUpdateContainer = new ExtUpdateContainer(container);
                mUpdateContainerExpiration = requestTime + UPDATE_CONTAINER_EXPIRATION; //don't care about 1 second offset for manual update requests
            }
        } else {
            Logger.d(TAG, "getUpdateContainer cache hit, time to expiration: " //NON-NLS
                    + (mUpdateContainerExpiration - requestTime));
        }

        return mExtUpdateContainer;
    }

    public static class MissingAppContextException extends Exception {

        private static final long serialVersionUID = -1817542570335055712L;

        MissingAppContextException() {
            super("Please start the Locus Tasker Plugin before you use it.");
        }
    }
}
