package falcosc.locus.addon.tasker;

import android.Manifest;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.asamm.logger.Logger;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import falcosc.locus.addon.tasker.intent.edit.ActionTaskEdit;
import falcosc.locus.addon.tasker.intent.edit.LocusInfoEdit;
import falcosc.locus.addon.tasker.intent.edit.NotImplementedActions;
import falcosc.locus.addon.tasker.intent.edit.TrackPointsEdit;
import falcosc.locus.addon.tasker.intent.edit.UpdateContainerEdit;
import falcosc.locus.addon.tasker.settings.SettingsActivity;
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

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (examplesAreMissing()) {
            importExample();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.err_notification_permission), Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.import_example){
            importExample();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void importExample() {
        Intent importIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.TaskerExampleProjectImportLink)));
        try {
            startActivity(importIntent); //taskershare does always respond 0 with null data, does not make sense to check it
            getPreferences(Context.MODE_PRIVATE).edit().putInt(IMPORTED_EXAMPLE_PROJECT_VER, 1).apply();
        } catch (ActivityNotFoundException e) {
            Logger.e(e, TAG, ReportingHelper.getUserFriendlyName(e));
        }
    }

    private boolean examplesAreMissing() {
        try (Cursor cursor = getContentResolver().query(Uri.parse("content://net.dinglisch.android.tasker/tasks"), //NON-NLS
                null, null, null, null)) {
            if (cursor != null) {
                int projNameCol = cursor.getColumnIndex("project_name"); //NON-NLS
                while (cursor.moveToNext()) {
                    if ("LocusMap Examples".equalsIgnoreCase(cursor.getString(projNameCol))) { //NON-NLS
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(e, TAG, ReportingHelper.getUserFriendlyName(e));
        }

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int importedVersion = sharedPref.getInt(IMPORTED_EXAMPLE_PROJECT_VER, 0);
        Logger.i(TAG, IMPORTED_EXAMPLE_PROJECT_VER + ": " + importedVersion);
        return importedVersion < 1;
    }

    @SuppressWarnings({"HardCodedStringLiteral", "MagicNumber"}) //because it is just a mock
    private boolean mockTaskerEditStart() {
        Class<?>[] editClasses = {
                UpdateContainerEdit.class,
                LocusInfoEdit.class,
                TrackPointsEdit.class,
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
