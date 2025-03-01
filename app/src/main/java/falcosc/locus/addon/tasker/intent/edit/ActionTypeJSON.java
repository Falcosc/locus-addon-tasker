package falcosc.locus.addon.tasker.intent.edit;

import android.view.View;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;

import com.asamm.logger.Logger;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import androidx.annotation.NonNull;
import falcosc.locus.addon.tasker.utils.ReportingHelper;
import falcosc.locus.addon.tasker.utils.listener.ItemSelectListener;

class ActionTypeJSON {
    private static final String TAG = "ActionTypeJSON"; //NON-NLS

    interface Setter<T> {
        void set(T value);
    }

    interface Getter<T> {
        T get();
    }

    final View mContent;
    final CheckBox mCheckbox;
    final OnItemSelectedListener onItemSelected;

    static class Bind {
        final Setter<Object> mSetter;
        final Getter<Object> mGetter;

        Bind(Setter<Object> setter, Getter<Object> getter) {
            mSetter = setter;
            mGetter = getter;
        }
    }

    private final Map<String, Bind> mKeyBindMap;

    ActionTypeJSON(@NonNull CheckBox checkbox, View content) {
        mContent = content;
        mCheckbox = checkbox;
        mKeyBindMap = new HashMap<>();
        onItemSelected = new ItemSelectListener(mCheckbox.getText());
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> setContentVisibility(isChecked));
    }

    private void setContentVisibility(boolean visible) {
        if (visible) {
            mContent.setVisibility(View.VISIBLE);
            mContent.getParent().requestChildFocus(mContent, mContent);
        } else {
            mContent.setVisibility(View.GONE);
        }
    }

    void setVisibility(List<View> views, int visibility) {
        if (views != null) {
            View view = null;
            for (int i = 0; i < views.size(); i++) {
                view = views.get(i);
                view.setVisibility(visibility);
            }
            if ((view != null) && (visibility == View.VISIBLE)) {
                mContent.getParent().requestChildFocus(view, view);
            }
        }
    }

    void bindKey(String key, Setter<Object> setter, Getter<Object> getter) {
        mKeyBindMap.put(key, new Bind(setter, getter));
    }

    void setJSON(JSONObject json) {
        if (json != null) {
            mContent.setVisibility(View.VISIBLE);
            mCheckbox.setChecked(true);
            for (Entry<String, Bind> entry : mKeyBindMap.entrySet()) {
                try {
                    Object value = json.opt(entry.getKey());
                    Bind bind = entry.getValue();
                    if (bind.mSetter != null) {
                        bind.mSetter.set(value);
                    }
                } catch (Exception e) {
                    Logger.w(e, TAG, ReportingHelper.getUserFriendlyName(e));
                }
            }
        }
    }

    boolean isChecked() {
        return mCheckbox.isChecked();
    }

    JSONObject getJSON() {
        JSONObject json = new JSONObject();
        for (Entry<String, Bind> entry : mKeyBindMap.entrySet()) {
            try {
                Object value = entry.getValue().mGetter.get();
                if ((value instanceof CharSequence) && StringUtils.isEmpty((CharSequence) value)) {
                    value = null;
                }
                //null get removed by put
                json.put(entry.getKey(), value);
            } catch (Exception e) {
                Logger.w(e, TAG, ReportingHelper.getUserFriendlyName(e));
            }

        }
        return json;
    }
}
