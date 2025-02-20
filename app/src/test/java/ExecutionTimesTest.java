import org.testng.Assert;
import org.testng.annotations.Test;

import falcosc.locus.addon.tasker.intent.LocusActionType;
import falcosc.locus.addon.tasker.utils.ExecutionTimes;

/**
 * @noinspection HardCodedStringLiteral
 */
public class ExecutionTimesTest {

    @Test
    public void testNotFull() {
        ExecutionTimes times = new ExecutionTimes(10);
        populate(times, 5);
        Assert.assertEquals(times.extractDurations(LocusActionType.UPDATE_CONTAINER_REQUEST), "0.0ms, 1.0ms, 2.0ms, 3.0ms, 4.0ms");
        Assert.assertEquals(times.extractDurations(LocusActionType.UPDATE_CONTAINER_REQUEST), "");
    }

    @Test
    public void testOverflow() {
        ExecutionTimes times = new ExecutionTimes(10);
        populate(times, 15);
        Assert.assertEquals(times.extractDurations(LocusActionType.UPDATE_CONTAINER_REQUEST),
                "5.0ms, 6.0ms, 7.0ms, 8.0ms, 9.0ms, 10.0ms, 11.0ms, 12.0ms, 13.0ms, 14.0ms");
        Assert.assertEquals(times.extractDurations(LocusActionType.UPDATE_CONTAINER_REQUEST), "");
        populate(times, 3);
        Assert.assertEquals(times.extractDurations(LocusActionType.UPDATE_CONTAINER_REQUEST), "0.0ms, 1.0ms, 2.0ms");
    }

    public static void populate(ExecutionTimes times, int amount) {
        for (int i = 0; i < amount; i++) {
            times.addDuration(LocusActionType.UPDATE_CONTAINER_REQUEST, i * 1_000_000L);
        }
    }
}
