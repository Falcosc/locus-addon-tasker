package falcosc.locus.addon.tasker.intent.edit;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog.Builder;

import org.jetbrains.annotations.NotNull;

import falcosc.locus.addon.tasker.R;

public class NotImplementedDialog extends DialogFragment {

    private static final String TITLE_ID = "titleId"; //NON-NLS

    public static NotImplementedDialog newInstance(int titleId) {

        Bundle args = new Bundle();
        args.putInt(TITLE_ID, titleId);

        NotImplementedDialog fragment = new NotImplementedDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressWarnings("unused")
    @NotNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Builder builder = new Builder(requireContext());
        Bundle args = getArguments();
        if (args != null) {
            builder.setTitle(args.getInt(TITLE_ID));
        }
        builder.setMessage(R.string.action_not_implemented_desc);
        builder.setNegativeButton(R.string.back, null);
        builder.setPositiveButton(R.string.share, (dialog, id) -> openWebPage(getString(R.string.issues_url)));
        // Create the AlertDialog object and return it
        return builder.create();
    }

    private void openWebPage(String url) {
        Uri webPage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webPage);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}
