package falcosc.locus.addon.tasker.intent.edit;

import android.util.Log;
import android.view.View;
import android.widget.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

abstract class AbstractActionTypeJSON {
    interface Setter<T> {
        void set(T value);
    }

    interface Getter<T> {
        T get();
    }

    final View content;
    private final CheckBox checkbox;

    static class Bind {
        final Setter<Object> setter;
        final Getter<Object> getter;

        Bind(Setter<Object> setter, Getter<Object> getter) {
            this.setter = setter;
            this.getter = getter;
        }
    }

    private final Map<String, Bind> keyBindMap;

    AbstractActionTypeJSON(final CheckBox checkbox, final View content) {
        this.content = content;
        this.checkbox = checkbox;
        keyBindMap = new HashMap<>();
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> setContentVisibility(isChecked));
    }

    private void setContentVisibility(boolean visible) {
        if (visible) {
            content.setVisibility(View.VISIBLE);
            content.getParent().requestChildFocus(content, content);
        } else {
            content.setVisibility(View.GONE);
        }
    }

    final AdapterView.OnItemSelectedListener onItemSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String selection = parent.getSelectedItem().toString();
            //set spinner description
            parent.setContentDescription(checkbox.getText() + ": " + selection);
            //the inside text is not important anymore because of duplicate information
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            //view.setContentDescription(checkbox.getText() + ": " + selection);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            parent.setContentDescription(checkbox.getText() + ": - ");
        }
    };


    void bindKey(String key, Setter<Object> setter, Getter<Object> getter) {
        keyBindMap.put(key, new Bind(setter, getter));
    }

    void setJSON(JSONObject json) {
        if (json != null) {
            content.setVisibility(View.VISIBLE);
            checkbox.setChecked(true);
            for (Map.Entry<String, Bind> entry : keyBindMap.entrySet()) {
                try {
                    Object value = json.opt(entry.getKey());
                    Bind bind = entry.getValue();
                    if (bind.setter != null) {
                        bind.setter.set(value);
                    }
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage(), e);
                }
            }
        }
    }

    boolean isChecked() {
        return checkbox.isChecked();
    }

    JSONObject getJSON() {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, Bind> entry : keyBindMap.entrySet()) {
            try {
                Object value = entry.getValue().getter.get();
                if (value instanceof CharSequence && StringUtils.isEmpty((CharSequence) value)) {
                    value = null;
                }
                //null get removed by put
                json.put(entry.getKey(), value);
            } catch (Exception e) {
                Log.w(TAG, e.getMessage(), e);
            }

        }
        return json;
    }

    @SuppressWarnings("unchecked")
    static void setSpinnerValue(Spinner spinner, Object value) {
        if (value != null) {
            spinner.setSelection(((ArrayAdapter<Object>) spinner.getAdapter()).getPosition(value));
        }
    }
}
