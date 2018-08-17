package falcosc.locus.addon.tasker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.imageView).setOnLongClickListener((v) -> mockTaskerEditStart());
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private boolean mockTaskerEditStart() {
        try {
            Intent intent = new Intent(this, TaskerEditActivity.class);
            intent.setPackage(this.getPackageName());
            intent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BREADCRUMB, "Tasker");
            intent.putExtra("net.dinglisch.android.tasker.extras.HOST_CAPABILITIES", 254);
            startActivity(intent);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }
}
