package falcosc.locus.addon.tasker.utils.listener;

import android.view.View;
import android.widget.AdapterView;

public class ItemSelectListener implements AdapterView.OnItemSelectedListener {

    private final CharSequence mName;

    public ItemSelectListener(CharSequence name){
        mName = name;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //selection can't be null;
        String selection = parent.getSelectedItem().toString();
        //set spinner description
        parent.setContentDescription(mName + ": " + selection);
        //view could be null during view recreation
        if (view != null) {
            //the inside text is not important anymore because of duplicate information
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            //view.setContentDescription(mCheckbox.getText() + ": " + selection);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        parent.setContentDescription(mName + ": - ");
    }
}
