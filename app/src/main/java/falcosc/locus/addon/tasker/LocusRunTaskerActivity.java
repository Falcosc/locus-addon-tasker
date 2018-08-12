package falcosc.locus.addon.tasker;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class LocusRunTaskerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO handle intent and set %data
        //TODO set task buttons by getting task list: https://tasker.joaoapps.com/contentprovider.html

        setContentView(R.layout.run_task);
        Button closeButton = findViewById(R.id.btnClose); 
        closeButton.setOnClickListener(v -> finish());
    }

//TODO call task: 	if ( TaskerIntent.testStatus( this ).equals( TaskerIntent.Status.OK ) ) {
//TaskerIntent i = new TaskerIntent( "MY_USER_TASK_NAME" );
//i.addVariable( "%data", "value" );
//sendBroadcast( i );
//}

}
