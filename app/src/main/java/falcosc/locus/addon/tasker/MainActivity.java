package falcosc.locus.addon.tasker;

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import falcosc.locus.addon.tasker.intent.edit.ActionTaskEdit;
import falcosc.locus.addon.tasker.intent.edit.LocusInfoEdit;
import falcosc.locus.addon.tasker.intent.edit.NotImplementedActions;
import falcosc.locus.addon.tasker.intent.edit.UpdateContainerEdit;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.ReportingHelper;

@SuppressWarnings("ClassWithTooManyTransitiveDependencies") //because of mock tasker start
public class MainActivity extends ProjectActivity {

    private static final String TAG = "MainActivity"; //NON-NLS
    private static final String IMPORTED_EXAMPLE_PROJECT_VER = "ImportedExampleProjectVer"; //NON-NLS
    private int i;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.imageView).setOnLongClickListener((v) -> mockTaskerEditStart());
        findViewById(R.id.import_example).setOnClickListener((v) -> importExample());

        if(examplesAreMissing()){
            importExample();
        }
    }

    private void importExample(){
        Intent importIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.TaskerExampleProjectImportLink)));
        try {
            startActivity(importIntent); //taskershare does allways respond 0 with null data, does not make sense to check it
            getPreferences(Context.MODE_PRIVATE).edit().putInt(IMPORTED_EXAMPLE_PROJECT_VER, 1).apply();
        } catch (ActivityNotFoundException e){
            Log.e(TAG, ReportingHelper.getUserFriendlyName(e), e);
        }
    }

    private boolean examplesAreMissing(){
        try (Cursor cursor = getContentResolver().query(Uri.parse("content://net.dinglisch.android.tasker/tasks"), //NON-NLS
                null, null, null, null)) {
            if(cursor != null) {
                int projNameCol = cursor.getColumnIndex( "project_name" ); //NON-NLS
                while (cursor.moveToNext()) {
                    if("LocusMap Examples".equalsIgnoreCase(cursor.getString(projNameCol))){ //NON-NLS
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, ReportingHelper.getUserFriendlyName(e), e);
        }

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int importedVersion = sharedPref.getInt(IMPORTED_EXAMPLE_PROJECT_VER, 0);
        Log.i(TAG, IMPORTED_EXAMPLE_PROJECT_VER + ": " + importedVersion);
        return importedVersion < 1;
    }

    @SuppressWarnings({"HardCodedStringLiteral", "MagicNumber"}) //because it is just a mock
    private boolean mockTaskerEditStart() {
        Class<?>[] editClasses = {
                UpdateContainerEdit.class,
                LocusInfoEdit.class,
                ActionTaskEdit.class,
                LocusGeoTagActivity.class,
                GeotagPhotosService.class,
                LocusRunTaskerActivity.class,
                NotImplementedActions.class
        };

        try {
            Class<?> testClass = editClasses[i++ % editClasses.length];
            Intent intent = new Intent(this, testClass);
            intent.setPackage(getPackageName());
            intent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BREADCRUMB, "Tasker");
            intent.putExtra("net.dinglisch.android.tasker.extras.HOST_CAPABILITIES", 254);
            intent.putExtra("net.dinglisch.android.tasker.RELEVANT_VARIABLES", new String[]{"%my_local_var", "%MY_GLOBAL_VAR"});
            ArrayList<Uri> files = new ArrayList<>();
            files.add(Uri.parse("empty:"));
            intent.putParcelableArrayListExtra(Const.INTENT_EXTRA_GEOTAG_FILES, files);

            if (Service.class.isAssignableFrom(testClass)) {
                ContextCompat.startForegroundService(this, intent);
            } else {
                startActivity(intent);
            }
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }


}
