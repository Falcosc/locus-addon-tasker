package falcosc.locus.addon.tasker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import falcosc.locus.addon.tasker.thridparty.TaskerIntent;
import falcosc.locus.addon.tasker.utils.LocusCache;
import locus.api.android.utils.IntentHelper;
import locus.api.android.utils.LocusConst;
import locus.api.objects.GeoData;
import locus.api.objects.extra.GeoDataExtra;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Point;
import locus.api.objects.extra.Track;
import locus.api.objects.extra.TrackStats;

public class LocusGeoTagActivity extends ProjectActivity {

    private static final String TAG = "LocusGeoTagActivity"; //NON-NLS
    private static final Pattern NON_WORD_CHAR_PATTERN = Pattern.compile("[^\\w]");  //NON-NLS
    private static final int REQUEST_CODE_OPEN_DIRECTORY = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!IntentHelper.INSTANCE.isIntentTrackTools(getIntent())) {
            //TODO test this
            finish();
        }
        //loadTrack();

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {


        Log.i(TAG, "requestCode: " + requestCode);

        if(resultCode == RESULT_OK && requestCode == REQUEST_CODE_OPEN_DIRECTORY){
            if (Build.VERSION.SDK_INT < 24) {
                //TODO
                return;
            }
            Uri uriTree = resultData.getData();
            Log.i(TAG, "Uri: " + uriTree.toString());

            Intent serviceIntent = new Intent(this,
                    GeotagPhotosService.class);
            //set locus intent data
            serviceIntent.putExtras(getIntent());
            String[] fileUries = (String[]) Stream.of(DocumentFile.fromTreeUri(this, uriTree).listFiles())
                    .filter(file -> "image/jpeg".equals(file.getType()))
                    .map(file -> file.getUri().toString())
                    .toArray( String[]::new );
            serviceIntent.putExtra("files" , fileUries);
            ContextCompat.startForegroundService(this, serviceIntent);
        }
        setResult(RESULT_OK, getIntent());
        finish();
    }


}
