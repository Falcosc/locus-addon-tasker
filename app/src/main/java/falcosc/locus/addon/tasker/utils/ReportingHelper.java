package falcosc.locus.addon.tasker.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import falcosc.locus.addon.tasker.R;

public class ReportingHelper {

    private final Context mContext;

    public ReportingHelper(Context context) {
        mContext = context;
    }

    public void sendErrorNotification(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);

        try {
            NotificationCompat.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //TODO do we need advanced channel management?
                builder = new NotificationCompat.Builder(mContext, NotificationChannel.DEFAULT_CHANNEL_ID);
            } else {
                //noinspection deprecation
                builder = new NotificationCompat.Builder(mContext);
            }

            //TODO share with email

            builder.setSmallIcon(R.drawable.ic_warning)
                    .setContentTitle(message)
                    .setContentText(throwable.getClass().getSimpleName() + ": " + throwable.getLocalizedMessage())
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(Log.getStackTraceString(throwable))
                            .setSummaryText(mContext.getString(R.string.err_unexpected_problem))
                    );

            NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            //noinspection ConstantConditions
            mNotificationManager.notify(Const.NOTIFICATION_ID_COMMON_ERROR, builder.build());
        } catch (Exception e) {
            Log.e(tag, "Can't create error notification", e); //NON-NLS
        }

    }
}
