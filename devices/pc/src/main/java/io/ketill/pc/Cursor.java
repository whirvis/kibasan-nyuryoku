package io.ketill.pc;

import io.ketill.IoDeviceObserver;
import io.ketill.IoFeature;
import org.jetbrains.annotations.NotNull;

/**
 * An I/O feature representing the cursor of a {@link Mouse}.
 */
public class Cursor extends IoFeature<CursorStateZ, CursorState> {

    /**
     * @param id the mouse cursor ID.
     * @throws NullPointerException     if {@code id} is {@code null}.
     * @throws IllegalArgumentException if {@code id} is empty or contains
     *                                  whitespace.
     */
    public Cursor(@NotNull String id) {
        super(id);
    }

    @Override
    protected @NotNull CursorStateZ getInternalState(@NotNull IoDeviceObserver observer) {
        return new CursorStateZ();
    }

    @Override
    protected @NotNull CursorState getContainerState(@NotNull CursorStateZ internalState) {
        return new CursorState(internalState);
    }

}
