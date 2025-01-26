package falcosc.locus.addon.tasker.utils;

import android.content.Context;
import android.util.Log;

import com.asamm.logger.Logger;

import org.jetbrains.annotations.NonNls;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@NonNls
public class CacheFileLogger implements Logger.ILogger {
    private static final String TAG = "CacheFileLogger";
    private final SimpleDateFormat logTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS ", Locale.ENGLISH);
    private final File outputFile;

    public CacheFileLogger(Context context){
        File cacheDir = context.getCacheDir();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.ENGLISH);
        outputFile = new File(cacheDir, dateFormat.format(Calendar.getInstance().getTime()) + ".log");
    }

    private void writeToFile(@Nullable Throwable throwable, @NonNull String msg, @NonNull Object... args) {
        //noinspection ImplicitDefaultCharsetUsage before API 33 constructor does not support it and it's ok to have broken logs
        try(FileWriter fw = new FileWriter(outputFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter printWriter = new PrintWriter(bw)) {

            printWriter.print(logTime.format(Calendar.getInstance().getTime()));
            printWriter.printf(msg, args);
            printWriter.println();
            if(throwable != null) {
                printWriter.println(throwable.getMessage());
                if (throwable.getCause() != null) {
                    printWriter.println(throwable.getCause().getMessage());
                }
                throwable.printStackTrace(printWriter);
            }
        } catch (IOException e) {
            Log.wtf(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void logD(@Nullable Throwable throwable, @NonNull String tag, @NonNull String msg, @NonNull Object... args) {
        Log.d(tag, String.format(msg, args), throwable);
    }

    @Override
    public void logV(@NonNull String tag, @NonNull String msg, @NonNull Object... args) {
        Log.v(tag, String.format(msg, args));
    }

    @Override
    public void logI(@NonNull String tag, @NonNull String msg, @NonNull Object... args) {
        Log.i(tag, String.format(msg, args));
    }

    @Override
    public void logW(@Nullable Throwable throwable, @NonNull String tag, @NonNull String msg, @NonNull Object... args) {
        Log.w(tag, String.format(msg, args), throwable);
        writeToFile(throwable, "WARN " + tag + ": " + msg, args);
    }

    @Override
    public void logE(@Nullable Throwable throwable, @NonNull String tag, @NonNull String msg, @NonNull Object... args) {
        Log.e(tag, String.format(msg, args), throwable);
        writeToFile(throwable, "ERROR " + tag + ": " + msg, args);
    }
}
