package falcosc.locus.addon.tasker;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class LocusRunTaskerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.run_task);
        
        addTaskButtons(findViewById(R.id.linearContent));
        
        Button closeButton = findViewById(R.id.btnClose); 
        closeButton.setOnClickListener(v -> finish());
    }
    
    private void addTaskButtons(View view){
        Cursor c = getContentResolver().query( Uri.parse( "content://net.dinglisch.android.tasker/tasks" ), null, null, null, null );
        if ( c != null ) {
            int nameCol = c.getColumnIndex( "name" );
            int projNameCol = c.getColumnIndex( "project_name" );

            while ( c.moveToNext() ) {
                String project = c.getString( projNameCol )
                String task = c.getString( nameCol );
                
                Button taskBtn = new Button(this);
                taskBtn.setText(task);
                taskBtn.setOnClickListener(v -> startTask(task));
                layout.addView(taskBtn);
            }

            c.close();
        }
    }
    
    private void startTask(String taskname){
        String taskerStatus = TaskerIntent.testStatus( this );
        if (taskerStatus.equals( TaskerIntent.Status.OK ) ) {
            TaskerIntent i = new TaskerIntent(taskname);
            //TODO translate intend to tasker variables
            i.addVariable( "%data", getAllIntentFieldsAsJSON(getIntent()));
            sendBroadcast( i );
        } else {
            //TODO error handling 
            //taskerStatus
        }
    }
    
    private String getAllIntentFieldsAsJSON(Intent intent){
        JsonObjectBuilder builder = Json.createObjectBuilder();

        Bundle bundle = data.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                builder.add(key + "(" + value.getClass().getName()+")", value.toString());
            }
        }
        return builder.toString();
    }

}
