package falcosc.locus.addon.tasker;

public class RequiredDataMissingException extends Exception {

    private static final long serialVersionUID = 8678744723903719896L;

    public RequiredDataMissingException(String message) {
        super(message);
    }
}
