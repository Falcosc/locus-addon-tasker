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
    private final LastDurations[] mLastBenchmark;
    private static long nanoSec = System.nanoTime();

    public enum Type {
        HANDLE_MESSAGE_TO_RECEIVE, FIND_PROCESSOR, PARSE_VARS, PROCESS, RETURN_VARS, FINISH_TO_NEXT_MESSAGE
    }

    /* Benchmark results:
IntentService

CREATE_TO_HANDLER: 51.4ms, 47.6ms, 28.4ms, 33.5ms, 50.2ms, 34.3ms, 43.8ms, 49.0ms, 74.5ms, 51.0ms
FIND_PROCESSOR: 1.8ms, 0.1ms, 0.1ms, 0.1ms, 0.8ms, 0.1ms, 0.6ms, 0.2ms, 0.2ms, 0.1ms
PARSE_VARS: 0.7ms, 0.3ms, 0.5ms, 0.4ms, 2.0ms, 0.8ms, 1.1ms, 0.9ms, 12.4ms, 0.4ms
PROCESS: 0.0ms, 0.0ms, 0.0ms, 0.0ms, 0.0ms, 0.0ms, 0.0ms, 0.0ms, 0.0ms, 0.0ms
RETURN_VARS: 3.3ms, 5.9ms, 8.0ms, 9.4ms, 5.4ms, 5.4ms, 4.9ms, 13.8ms, 22.4ms, 8.1ms
HANDLER_TO_UNBIND: 8.4ms, 8.9ms, 16.4ms, 18.5ms, 22.8ms, 18.6ms, 21.1ms, 8.3ms, 23.6ms, 9.5ms
UNBIND_TO_NEXT_CREATE: 27.1ms, 19.2ms, 27.2ms, 20.7ms, 17.1ms, 29.5ms, 17.9ms, 25.4ms, 19.7ms, 28.1ms

CREATE_TO_HANDLER: 45.5ms, 25.4ms, 60.7ms, 45.4ms, 67.8ms, 50.2ms, 29.9ms, 20.9ms, 41.8ms, 30.8ms
FIND_PROCESSOR: 0.2ms, 0.8ms, 0.1ms, 0.1ms, 0.3ms, 0.3ms, 0.1ms, 4.9ms, 1.0ms, 0.3ms
PARSE_VARS: 1.3ms, 1.3ms, 0.4ms, 0.4ms, 0.4ms, 0.4ms, 0.6ms, 0.8ms, 0.8ms, 3.2ms
PROCESS: 0.0ms, 0.0ms, 0.0ms, 0.0ms, 0.0ms, 0.0ms, 0.0ms, 0.0ms, 0.0ms, 0.0ms
RETURN_VARS: 3.9ms, 2.7ms, 2.9ms, 4.1ms, 14.8ms, 5.1ms, 5.5ms, 4.2ms, 7.8ms, 2.6ms
HANDLER_TO_UNBIND: 30.5ms, 36.8ms, 11.8ms, 14.1ms, 7.1ms, 18.8ms, 35.1ms, 8.5ms, 12.5ms, 9.8ms
UNBIND_TO_NEXT_CREATE: 17.5ms, 20.8ms, 20.0ms, 15.4ms, 38.9ms, 19.8ms, 23.5ms, 20.8ms, 14.9ms, 17.5ms

900-1000ms for 12 tasks with 4 variables and 0ms processing time

Broadcast Receiver

HANDLE_MESSAGE_TO_RECEIVE: 0.4ms, 0.1ms, 0.2ms, 0.2ms, 0.2ms, 0.7ms, 0.7ms, 0.2ms, 0.2ms, 1.2ms
FIND_PROCESSOR: 0.4ms, 0.1ms, 0.1ms, 0.1ms, 0.1ms, 0.8ms, 0.1ms, 0.6ms, 0.6ms, 0.2ms
PARSE_VARS: 0.0ms, 0.0ms, 0.0ms, 0.1ms, 0.0ms, 0.0ms, 0.1ms, 0.0ms, 0.0ms, 0.0ms
PROCESS: 8.9ms, 7.3ms, 7.4ms, 4.2ms, 5.6ms, 4.1ms, 6.8ms, 8.9ms, 5.2ms, 9.3ms
RETURN_VARS: 0.6ms, 0.1ms, 0.1ms, 0.3ms, 0.3ms, 0.1ms, 0.0ms, 0.1ms, 0.1ms, 0.3ms
FINISH_TO_NEXT_MESSAGE: 35.8ms, 35.3ms, 35.3ms, 34.4ms, 46.4ms, 33.8ms, 30.7ms, 23.8ms, 35.3ms, 52.8ms

500-550ml for 12 tasks with 4 variables and 70ms processing time
     */

    public ExecutionTimes(int historyLength) {
        mLastDurations = new LastDurations[LocusActionType.values().length];
        for (int i = 0; i < mLastDurations.length; i++) {
            mLastDurations[i] = new LastDurations(historyLength);
        }
        mLastBenchmark = new LastDurations[Type.values().length];
        for (int i = 0; i < mLastBenchmark.length; i++) {
            mLastBenchmark[i] = new LastDurations(historyLength);
        }
    }
    public void addDurationSinceLastAdd(Type action) {
        mLastBenchmark[action.ordinal()].add(System.nanoTime() - nanoSec);
        nanoSec = System.nanoTime();
    }

    public String extractDurations(Type action) {
        LastDurations lastDurations = mLastBenchmark[action.ordinal()];
        String lastDuratonsString = LongStream.of(lastDurations.getLastValues())
                .mapToObj(ExecutionTimes::formatNanoToMilli) //NON-NLS
                .collect(Collectors.joining(", "));
        lastDurations.reset();
        return lastDuratonsString;
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

