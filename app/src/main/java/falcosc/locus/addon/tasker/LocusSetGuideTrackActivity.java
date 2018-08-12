package falcosc.locus.addon.tasker;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class LocusSetGuideTrackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO handle intent and set Track

        setContentView(R.layout.calc_remain_elevation);
        Button closeButton = findViewById(R.id.btnClose); 
        closeButton.setOnClickListener(v -> finish());
    }
}
