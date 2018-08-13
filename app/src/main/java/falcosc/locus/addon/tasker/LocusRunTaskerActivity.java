package falcosc.locus.addon.tasker;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import falcosc.locus.addon.tasker.utils.TaskerIntent;
import org.json.JSONException;
import org.json.JSONObject;

public class LocusRunTaskerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.run_task);

        addTaskButtons(findViewById(R.id.linearContent));

        Button closeButton = findViewById(R.id.btnClose);
        closeButton.setOnClickListener(v -> finish());

        Button shareButton = findViewById(R.id.btnShare);
        shareButton.setOnClickListener(v -> openWebPage("https://github.com/Falcosc/locus-addon-tasker/issues"));

    }

    private void addTaskButtons(ViewGroup viewGroup) {

        LayoutInflater inflater = LayoutInflater.from(this);

        Cursor c = getContentResolver().query(Uri.parse("content://net.dinglisch.android.tasker/tasks"), null, null, null, null);
        if (c != null) {
            int nameCol = c.getColumnIndex("name");

            while (c.moveToNext()) {
                String task = c.getString(nameCol);

                View view = inflater.inflate(R.layout.list_btn, viewGroup);
                Button taskBtn = view.findViewById(R.id.listBtn);
                taskBtn.setText(task);
                taskBtn.setOnClickListener(v -> startTask(task));
                viewGroup.addView(view);
            }

            c.close();
        }
    }

    public void openWebPage(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void startTask(String taskname) {
        TaskerIntent.Status taskerStatus = TaskerIntent.testStatus(this);
        if (taskerStatus.equals(TaskerIntent.Status.OK)) {
            TaskerIntent i = new TaskerIntent(taskname);
            //TODO translate intend to tasker variables
            i.addLocalVariable("%data", getAllIntentFieldsAsJSON(getIntent()));
            sendBroadcast(i);
        } else {
            //TODO error handling 
            //taskerStatus
        }
        finish();
    }

    private String getAllIntentFieldsAsJSON(Intent intent) {
        JSONObject json = new JSONObject();

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                try {
                    json.put(key + "(" + value.getClass().getName() + ")", value.toString());
                } catch (JSONException e) {
                    //TODO
                    e.printStackTrace();
                }
            }
        }
        return json.toString();
    }

}
