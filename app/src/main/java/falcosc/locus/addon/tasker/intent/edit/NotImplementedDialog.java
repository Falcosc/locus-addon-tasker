package falcosc.locus.addon.tasker.intent.edit;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

public class NotImplementedDialog extends DialogFragment {

    public static NotImplementedDialog newInstance(int titleId) {

        Bundle args = new Bundle();
        args.putInt("titleId", titleId);

        NotImplementedDialog fragment = new NotImplementedDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getArguments().getInt("titleId"));
        builder.setMessage("This action is currently not implemented because nobody did request this. Please share your Tasker project idea at Github to tell me how I should implement this API to be usefull at Tasker.");
        builder.setNegativeButton("Back", null);
        builder.setPositiveButton("Share", (dialog, id) -> openWebPage("https://github.com/Falcosc/locus-addon-tasker/issues"));
        // Create the AlertDialog object and return it
        return builder.create();
    }

    public void openWebPage(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}
