package falcosc.locus.addon.tasker;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import falcosc.locus.addon.tasker.intent.handler.NearestPointRequest;
import falcosc.locus.addon.tasker.utils.Const;

@SuppressWarnings("ClassWithTooManyTransitiveDependencies") //because of mock tasker start
public class MainActivity extends ProjectActivity {

    private int i;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.imageView).setOnLongClickListener((v) -> mockTaskerEditStart());
    }

    @SuppressWarnings({"HardCodedStringLiteral", "MagicNumber"}) //because it is just a mock
    private boolean mockTaskerEditStart() {
        Class<?>[] editClasses = {
                NearestPointRequest.class,
//                UpdateContainerEdit.class,
//                LocusInfoEdit.class,
//                ActionTaskEdit.class,
//                LocusGeoTagActivity.class,
//                GeotagPhotosService.class,
//                LocusRunTaskerActivity.class,
//                NotImplementedActions.class
        };


        try {
            new NearestPointRequest().getPoints();


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
