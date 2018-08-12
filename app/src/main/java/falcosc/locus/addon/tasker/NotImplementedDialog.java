package falcosc.locus.addon.tasker;

public class MainActivity extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //TODO set action type label
        builder.setTitle ("TODO ACTION TYPE LABEL")
        builder.setMessage("This action is currently not implemented because nobody did request this. Please share your Tasker project idea at Github to tell me how I should implement this API to be usefull at Tasker.");
               .setNegativeButton("Back", null)
               .setPositiveButton("Share", (dialog, id) -> openWebPage("https://github.com/Falcosc/locus-addon-tasker/issues"));
        // Create the AlertDialog object and return it
        return builder.create();
    }

    public void openWebPage(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}