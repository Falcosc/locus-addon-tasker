package falcosc.locus.addon.tasker;

import org.jetbrains.annotations.Nls;

import androidx.annotation.NonNull;

public class RequiredDataMissingException extends Exception {

    private static final long serialVersionUID = 8678744723903719896L;

    public RequiredDataMissingException(@NonNull @Nls String message) {
        super(message);
    }

    public RequiredDataMissingException(@NonNull String message, Throwable caused) {
        super(message, caused);
    }
}
