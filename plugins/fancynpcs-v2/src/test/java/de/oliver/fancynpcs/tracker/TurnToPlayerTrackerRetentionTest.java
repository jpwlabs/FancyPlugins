package de.oliver.fancynpcs.tracker;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TurnToPlayerTrackerRetentionTest {

    private static final UUID TEST_PLAYER_ID = UUID.fromString("81d5c89d-a225-4bbf-bd87-018ded56cbda");

    @Test
    void deferredStartEventCapturesUuidButNotPlayer() throws IllegalAccessException {
        assertSafeCapture(TurnToPlayerTracker.createStartLookingEventTask(null, TEST_PLAYER_ID));
    }

    @Test
    void deferredStopEventCapturesUuidButNotPlayer() throws IllegalAccessException {
        assertSafeCapture(TurnToPlayerTracker.createStopLookingEventTask(null, TEST_PLAYER_ID));
    }

    private static void assertSafeCapture(Runnable task) throws IllegalAccessException {
        Field[] captures = task.getClass().getDeclaredFields();

        assertFalse(
                Arrays.stream(captures).anyMatch(field -> Player.class.isAssignableFrom(field.getType())),
                "a deferred event task must never retain a Bukkit Player"
        );

        Field uuidCapture = Arrays.stream(captures)
                .filter(field -> field.getType() == UUID.class)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the task must capture the stable player UUID"));
        uuidCapture.setAccessible(true);
        assertEquals(TEST_PLAYER_ID, uuidCapture.get(task));
    }
}
