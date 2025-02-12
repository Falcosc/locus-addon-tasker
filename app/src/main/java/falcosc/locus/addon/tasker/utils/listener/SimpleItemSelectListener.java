package falcosc.locus.addon.tasker.utils.listener;

import android.view.View;
import android.widget.AdapterView;

public class SimpleItemSelectListener extends ItemSelectListener {

    private final ValueSelectHandler mHandler;

    public interface ValueSelectHandler {
        void handleChange(Object selectedValue);
    }

    public SimpleItemSelectListener(CharSequence name, ValueSelectHandler handler) {
        super(name);
        mHandler = handler;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mHandler.handleChange(parent.getSelectedItem());
        super.onItemSelected(parent, view, position, id);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        mHandler.handleChange(null);
        super.onNothingSelected(parent);
    }
}
