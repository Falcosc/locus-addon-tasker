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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import falcosc.locus.addon.tasker.intent.edit.ActionTaskEdit;
import falcosc.locus.addon.tasker.intent.edit.LocusInfoEdit;
import falcosc.locus.addon.tasker.intent.edit.NotImplementedActions;
import falcosc.locus.addon.tasker.intent.edit.UpdateContainerEdit;
import falcosc.locus.addon.tasker.settings.SettingsActivity;
import falcosc.locus.addon.tasker.uc.ExtUpdateContainer;
import falcosc.locus.addon.tasker.uc.ExtUpdateContainerGetter;
import falcosc.locus.addon.tasker.utils.Const;
import falcosc.locus.addon.tasker.utils.LocusCache;
import falcosc.locus.addon.tasker.utils.ReportingHelper;
import locus.api.android.ActionBasics;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.objects.VersionCode;
import locus.api.android.utils.LocusUtils;
import locus.api.objects.extra.Location;

@SuppressWarnings("ClassWithTooManyTransitiveDependencies") //because of mock tasker start
public class MainActivity extends ProjectActivity {

    private static final String TAG = "MainActivity"; //NON-NLS
    private static final String IMPORTED_EXAMPLE_PROJECT_VER = "ImportedExampleProjectVer"; //NON-NLS
    private int i;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.imageView).setOnLongClickListener((v) -> exampleRequest());
        findViewById(R.id.imageView).setOnClickListener((v) -> exampleRequest());
        findViewById(R.id.import_example).setOnClickListener((v) -> importExample());

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (examplesAreMissing()) {
            importExample();
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
            Log.e(TAG, ReportingHelper.getUserFriendlyName(e), e);
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
            Log.e(TAG, ReportingHelper.getUserFriendlyName(e), e);
        }

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int importedVersion = sharedPref.getInt(IMPORTED_EXAMPLE_PROJECT_VER, 0);
        Log.i(TAG, IMPORTED_EXAMPLE_PROJECT_VER + ": " + importedVersion);
        return importedVersion < 1;
    }

    private boolean exampleRequest() {
        try {
            Context appContext = getApplicationContext();
            LocusCache locusCache = LocusCache.getInstanceUnsafe(appContext);
            UpdateContainer container = ActionBasics.INSTANCE.getUpdateContainer(appContext,
                    Objects.requireNonNull(LocusUtils.INSTANCE.getActiveVersion(appContext, VersionCode.UPDATE_01)));
            if (container == null) {
                Toast.makeText(this, "API can not get update container", Toast.LENGTH_LONG).show();
            } else {
                Location loc = container.getLocMyLocation();
                Toast.makeText(this, loc.getTime() + " API Location: " + loc.getLatitude() + ", " + loc.getLongitude() + ' ' +
                        loc.getLatitudeOriginal() + ", " + loc.getLongitudeOriginal(), Toast.LENGTH_LONG).show();
                ExtUpdateContainer extUpdate = locusCache.getUpdateContainer();
                ExtUpdateContainerGetter lf = locusCache.mExtUpdateContainerFieldMap.get("my_longitude");
                if (lf != null) {
                    String taskerLon = String.valueOf(lf.apply(extUpdate));
                    Toast.makeText(this, "Tasker Location: " + taskerLon, Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = e.getMessage();
            if(e.getCause() != null) {
                exceptionAsString += " " +e.getCause().getMessage();
            }
            exceptionAsString += "\n" + sw;
            Log.e(TAG, "Error: " + exceptionAsString, e);
            Toast.makeText(this, "Error: " + exceptionAsString, Toast.LENGTH_LONG).show();
            ((TextView) findViewById(R.id.middletxt)).setText("Error: " + exceptionAsString);
        }

        return true;
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
