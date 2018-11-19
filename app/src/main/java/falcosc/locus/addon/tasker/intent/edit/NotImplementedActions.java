package falcosc.locus.addon.tasker.intent.edit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.View;
import android.widget.Button;

import falcosc.locus.addon.tasker.ProjectActivity;
import falcosc.locus.addon.tasker.R;

public class NotImplementedActions extends ProjectActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.not_implemented_actions);

        Button positiveBtn = findViewById(android.R.id.button1);
        positiveBtn.setText(R.string.share);
        positiveBtn.setOnClickListener((v) -> openWebPage(getString(R.string.issues_url)));

        Button negativeBtn = findViewById(android.R.id.button2);
        negativeBtn.setText(R.string.back);
        negativeBtn.setOnClickListener((v) -> finish());

        findViewById(android.R.id.button3).setVisibility(View.GONE);
    }

    private void openWebPage(@NonNull String url) {
        Uri webPage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webPage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}
