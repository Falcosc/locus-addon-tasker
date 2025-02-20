package falcosc.locus.addon.tasker.utils;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import androidx.annotation.NonNull;
import falcosc.locus.addon.tasker.intent.LocusActionType;

public class ExecutionTimes {

    public static final ExecutionTimes INSTANCE = new ExecutionTimes(10);
    private static final long NS_IN_A_MS = 1_000_000;
    private static final long NS_IN_A_MS_TENS = NS_IN_A_MS / 10;
    private final LastDurations[] mLastDurations;

    public ExecutionTimes(int historyLength) {
        mLastDurations = new LastDurations[LocusActionType.values().length];
        for (int i = 0; i < mLastDurations.length; i++) {
            mLastDurations[i] = new LastDurations(historyLength);
        }
    }

    public void addDuration(@NonNull LocusActionType action, long duration) {
        mLastDurations[action.ordinal()].add(duration);
    }

    public String extractDurations(LocusActionType action) {
        LastDurations lastDurations = mLastDurations[action.ordinal()];
        String lastDuratonsString = LongStream.of(lastDurations.getLastValues())
                .mapToObj(ExecutionTimes::formatNanoToMilli) //NON-NLS
                .collect(Collectors.joining(", "));
        lastDurations.reset();
        return lastDuratonsString;
    }

    @NonNull
    public static String formatNanoToMilli(long nanoSeconds) {
        long milliseconds = nanoSeconds / NS_IN_A_MS;
        long decimal = (nanoSeconds % NS_IN_A_MS) / NS_IN_A_MS_TENS;
        //noinspection StringConcatenationMissingWhitespace
        return Long.toString(milliseconds) + '.' + decimal + "ms"; //NON-NLS
    }

    private static final class LastDurations {
        private final long[] buffer;
        private int writeIndex;
        private int populated;

        LastDurations(int size) {
            buffer = new long[size];
        }

        public void add(long duration) {
            buffer[writeIndex] = duration;
            writeIndex = (writeIndex + 1) % buffer.length;
            if (populated < buffer.length) {
                populated++;
            }
        }

        public long[] getLastValues() {
            long[] result = new long[Math.min(populated, buffer.length)];
            for (int i = 0; i < result.length; i++) {
                result[i] = buffer[((writeIndex + (buffer.length - populated)) + i) % buffer.length];
            }
            return result;
        }

        public void reset() {
            populated = 0;
        }
    }
}

