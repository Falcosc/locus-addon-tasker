package falcosc.locus.addon.tasker;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import falcosc.locus.addon.tasker.thridparty.TaskerIntent;

import org.json.JSONException;
import org.json.JSONObject;

public class LocusRunTaskerActivity extends ProjectActivity {

    private static final String TAG = "LocusRunTaskerActivity"; //NON-NLS

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.run_task);

        addTaskButtons(findViewById(R.id.linearContent));

        Button closeButton = findViewById(R.id.btnClose);
        closeButton.setOnClickListener(v -> finish());

        Button shareButton = findViewById(R.id.btnShare);
        shareButton.setOnClickListener(v -> openWebPage(getString(R.string.issues_link)));
    }

    private void addTaskButtons(@NonNull ViewGroup viewGroup) {

        LayoutInflater inflater = LayoutInflater.from(this);

        try (Cursor cursor = getContentResolver().query(Uri.parse("content://net.dinglisch.android.tasker/tasks"), //NON-NLS
                null, null, null, null)) {
            assert cursor != null;
            int nameCol = cursor.getColumnIndex("name"); //NON-NLS

            while (cursor.moveToNext()) {
                String task = cursor.getString(nameCol);

                View view = inflater.inflate(R.layout.list_btn, viewGroup, false);
                Button taskBtn = view.findViewById(R.id.listBtn);
                taskBtn.setText(task);
                taskBtn.setOnClickListener(v -> startTask(task));
                viewGroup.addView(view);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void openWebPage(@NonNull String url) {
        Uri webPage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webPage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void startTask(@NonNull String taskName) {
        TaskerIntent.Status taskerStatus = TaskerIntent.testStatus(this);
        if (taskerStatus == TaskerIntent.Status.OK) {
            TaskerIntent intent = new TaskerIntent(taskName);
            //TODO translate intend to tasker variables
            intent.addLocalVariable("%data", getAllIntentFieldsAsJSON(getIntent())); //NON-NLS
            sendBroadcast(intent);
        } else {
            Toast.makeText(this, getString(R.string.err_cant_start_task) + " " + taskerStatus.name(), Toast.LENGTH_LONG).show();
        }
        finish();
    }

    @NonNull
    private static String getAllIntentFieldsAsJSON(@NonNull Intent intent) {
        JSONObject json = new JSONObject();

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                try {
                    if (value != null) {
                        json.put(key + "(" + value.getClass().getName() + ")", value.toString());
                    }
                } catch (JSONException e) {
                    Log.i(TAG, "exception at key " + key, e); //NON-NLS
                }
            }
        }
        return json.toString();
    }

}
