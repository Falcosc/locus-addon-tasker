package falcosc.locus.addon.tasker.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;
import falcosc.locus.addon.tasker.R;
import falcosc.locus.addon.tasker.utils.ReportingHelper;

public class SettingsFragment extends PreferenceFragmentCompat {

    final Map<String, ComponentName> mComponentsByKey = new HashMap<>();
    final Set<String> mManifestEnabledComponentNames = new HashSet<>();
    PackageManager mPackageManager;
    Context mContext;
    String mPackageName;


    private final Preference.OnPreferenceChangeListener mOnComponentEnableChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ComponentName componentName = mComponentsByKey.get(preference.getKey());
            if (componentName != null) {
                mPackageManager.setComponentEnabledSetting(componentName,
                        Boolean.TRUE.equals(newValue) ?
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                return true;
            }
            return false;
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        mContext = requireContext();
        mPackageManager = mContext.getPackageManager();
        mPackageName = mContext.getPackageName();

        try {
            PackageInfo packageInfo = mPackageManager.getPackageInfo(mPackageName, PackageManager.GET_ACTIVITIES);
            for (ComponentInfo componentInfo : packageInfo.activities) {
                if (componentInfo.enabled) {
                    mManifestEnabledComponentNames.add(componentInfo.name);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(mContext, ReportingHelper.getUserFriendlyName(e), Toast.LENGTH_LONG).show();
        }


        addComponentEnableChangeListener(getPreferenceScreen());
    }

    private void addComponentEnableChangeListener(PreferenceGroup preferenceGroup) {
        int preferenceCount = preferenceGroup.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = preferenceGroup.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                addComponentEnableChangeListener((PreferenceGroup) preference);
            }
            String key = preference.getKey();
            if ((key != null) && key.startsWith(mPackageName) && (preference instanceof TwoStatePreference)) {
                ComponentName componentName = new ComponentName(mContext, key);
                mComponentsByKey.put(key, componentName);
                ((TwoStatePreference) preference).setChecked(isComponentEnabled(componentName));
                preference.setOnPreferenceChangeListener(mOnComponentEnableChangeListener);
            }
        }
    }

    private boolean isComponentEnabled(ComponentName componentName) {
        int enableState = mPackageManager.getComponentEnabledSetting(componentName);
        if (enableState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            return mManifestEnabledComponentNames.contains(componentName.getClassName());
        }
        return enableState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }
}

