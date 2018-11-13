package falcosc.locus.addon.tasker;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import falcosc.locus.addon.tasker.utils.LocusCache;

public abstract class ProjectActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //start reading locus resources files in background to speed up UI and to guaranty that we have an application context
        LocusCache.initAsync(getApplication());
    }
}
