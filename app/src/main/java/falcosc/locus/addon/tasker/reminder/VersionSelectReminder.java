package falcosc.locus.addon.tasker.reminder;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.widget.Toast;

import androidx.preference.PreferenceManager;
import falcosc.locus.addon.tasker.R;
import locus.api.android.ActionBasics;
import locus.api.android.objects.LocusInfo;
import locus.api.android.objects.LocusVersion;
import locus.api.android.utils.LocusUtils;

public class VersionSelectReminder {
    private Boolean mCheckVersionSelectRequired = false;
    private final SharedPreferences sharedPref;
    private static final String VERSION_SELECT_LAST_USAGE = "VersionSelectLastUsage"; //NON-NLS
    private final Context mContext;
    private long mReminderVisibleEndTime;

    public VersionSelectReminder(Context context) {
        mContext = context;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPref.getLong(VERSION_SELECT_LAST_USAGE, 0) == 0) {
            if (LocusUtils.INSTANCE.getAvailableVersions(context).size() > 1) {
                mCheckVersionSelectRequired = true;
            }
        } else {
            mCheckVersionSelectRequired = false;
        }
    }

    public void setVersionSelectLastUsage() {
        mCheckVersionSelectRequired = false;
        sharedPref.edit().putLong(VERSION_SELECT_LAST_USAGE, System.currentTimeMillis()).apply();
    }

    public void remindIfWrongVersionSelected(LocusVersion lv) {
        if (!mCheckVersionSelectRequired) {
            //reminder not needed
            return;
        }

        LocusInfo activeLocusInfo = new LocusInfo();
        int runningVersionCount = 0;
        activeLocusInfo.setPackageName(lv.getPackageName()); //set package name in case there is no running version
        for (LocusVersion availableVersion : LocusUtils.INSTANCE.getAvailableVersions(mContext)) {
            LocusInfo info = ActionBasics.INSTANCE.getLocusInfo(mContext, availableVersion);
            if ((info != null) && info.isRunning()) {
                runningVersionCount++;
                if (info.getLastActive() > activeLocusInfo.getLastActive()) {
                    activeLocusInfo = info;
                }
            }
        }

        if (lv.getPackageName().equals(activeLocusInfo.getPackageName())) {
            if (runningVersionCount <= 1) {
                //stop detection for this session if only one version is running and it is the correct version
                mCheckVersionSelectRequired = false;
            }
        } else {
            showReminder();
        }
    }

    private void showReminder() {
        long requestTime = System.currentTimeMillis();
        if (requestTime > mReminderVisibleEndTime) {
            mReminderVisibleEndTime = requestTime + (DateUtils.SECOND_IN_MILLIS * 7);
            Toast.makeText(mContext, R.string.version_select_reminder, Toast.LENGTH_LONG).show();
            Toast.makeText(mContext, R.string.version_select_reminder, Toast.LENGTH_LONG).show();
        }
    }
}
