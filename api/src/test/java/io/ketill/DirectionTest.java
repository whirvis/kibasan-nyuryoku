package io.ketill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DirectionTest {

    @Test
    void aliases() {
        assertSame(Direction.NORTH, Direction.UP);
        assertSame(Direction.SOUTH, Direction.DOWN);
        assertSame(Direction.WEST, Direction.LEFT);
        assertSame(Direction.EAST, Direction.RIGHT);
    }

    @Test
    void fromId() {
        assertThrows(IllegalArgumentException.class,
                () -> Direction.fromId(-1));
        for (Direction value : Direction.values()) {
            assertSame(value, Direction.fromId(value.id));
        }
    }

}