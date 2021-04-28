package falcosc.locus.addon.tasker.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import falcosc.locus.addon.tasker.BuildConfig;
import falcosc.locus.addon.tasker.R;

public class ReportingHelper {

    private static final Pattern EXCEPTION = Pattern.compile("Exception", Pattern.LITERAL); //NON-NLS
    private final Context mContext;

    public ReportingHelper(Context context) {
        mContext = context;
    }

    public void sendErrorNotification(@NonNull String tag, @NonNull String message, @NonNull Throwable throwable) {
        Log.e(tag, message, throwable);

        try {
            NotificationCompat.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new NotificationCompat.Builder(mContext, createDefaultNotificationChannel());
            } else {
                //noinspection deprecation
                builder = new NotificationCompat.Builder(mContext);
            }

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO)
                    .setData(Uri.fromParts(Const.SCHEMA_MAIL, BuildConfig.CONTACT_EMAIL, null))
                    .putExtra(Intent.EXTRA_SUBJECT, mContext.getText(R.string.app_name) + " " + throwable.getClass().getSimpleName())
                    .putExtra(Intent.EXTRA_TEXT, getUserFriendlyName(throwable)
                            + "\n" + Log.getStackTraceString(throwable));

            PendingIntent pendingGetText = PendingIntent.getActivity(mContext, 0,
                    Intent.createChooser(emailIntent, mContext.getText(R.string.send_to_developer)), PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(android.R.drawable.ic_dialog_email, mContext.getText(R.string.send_to_developer), pendingGetText);

            builder.setSmallIcon(R.drawable.ic_warning)
                    .setContentTitle(message)
                    .setContentText(getUserFriendlyName(throwable))
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(Log.getStackTraceString(throwable))
                            .setSummaryText(mContext.getString(R.string.err_unexpected_problem))
                    );

            NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(Const.NOTIFICATION_ID_COMMON_ERROR, builder.build());
        } catch (Exception e) {
            Log.e(tag, "Can't create error notification", e); //NON-NLS
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String createDefaultNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(Const.NOTIFICATION_CHANNEL_ID,
                mContext.getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(mContext.getString(R.string.notification_channel_desc));
        NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
        return Const.NOTIFICATION_CHANNEL_ID;
    }

    public static String getUserFriendlyName(@NonNull Throwable throwable) {
        String exceptionName = throwable.getClass().getSimpleName();
        exceptionName = EXCEPTION.matcher(exceptionName).replaceAll(Matcher.quoteReplacement(""));
        return exceptionName + ": " + throwable.getLocalizedMessage();
    }

}
