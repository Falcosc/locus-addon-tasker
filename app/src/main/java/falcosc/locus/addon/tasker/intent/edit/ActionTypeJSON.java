package falcosc.locus.addon.tasker.intent.edit;

import android.app.Dialog;
import android.util.Log;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

class ActionTypeJSON {
    private static final String TAG = "ActionTypeJSON"; //NON-NLS

    interface Setter<T> {
        void set(T value);
    }

    interface Getter<T> {
        T get();
    }

    final View mContent;
    @SuppressWarnings("WeakerAccess") //not private because of anonymous access
    final CheckBox mCheckbox;

    static class Bind {
        final Setter<Object> mSetter;
        final Getter<Object> mGetter;

        Bind(Setter<Object> setter, Getter<Object> getter) {
            mSetter = setter;
            mGetter = getter;
        }
    }

    private final Map<String, Bind> mKeyBindMap;

    ActionTypeJSON(CheckBox checkbox, View content) {
        mContent = content;
        mCheckbox = checkbox;
        mKeyBindMap = new HashMap<>();
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

    static void setVarSelectDialog(Dialog varSelectDialog, EditText text, View varSelectBtn) {
        if (varSelectDialog != null) {
            text.setOnFocusChangeListener((v, hasFocus) -> varSelectBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE));
            varSelectBtn.setOnClickListener(v -> varSelectDialog.show());
        }
    }

    final OnItemSelectedListener onItemSelected = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            //selection can't be null;
            String selection = parent.getSelectedItem().toString();
            //set spinner description
            parent.setContentDescription(mCheckbox.getText() + ": " + selection);
            //view could be null during view recreation
            if (view != null) {
                //the inside text is not important anymore because of duplicate information
                view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                //view.setContentDescription(mCheckbox.getText() + ": " + selection);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            parent.setContentDescription(mCheckbox.getText() + ": - ");
        }
    };


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
                    Log.w(TAG, e.getMessage(), e);
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
