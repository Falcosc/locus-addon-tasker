package falcosc.locus.addon.tasker;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

public class MainActivity extends ProjectActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.imageView).setOnLongClickListener((v) -> mockTaskerEditStart());
    }

    @SuppressWarnings({"HardCodedStringLiteral", "MagicNumber"}) //because it is just a mock
    private boolean mockTaskerEditStart() {
        try {
            Intent intent = new Intent(this, TaskerEditActivity.class);
            intent.setPackage(getPackageName());
            intent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BREADCRUMB, "Tasker");
            intent.putExtra("net.dinglisch.android.tasker.extras.HOST_CAPABILITIES", 254);
            intent.putExtra("net.dinglisch.android.tasker.RELEVANT_VARIABLES", new String[]{"%my_local_var", "%MY_GLOBAL_VAR"});
            startActivity(intent);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }
}
