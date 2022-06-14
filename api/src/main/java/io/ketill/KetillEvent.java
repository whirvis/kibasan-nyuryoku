package io.ketill;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The base for events emitted in Ketill I/O.
 * <p>
 * <b>Thread safety:</b> This class is <i>thread-safe.</i>
 * Extending classes must also be thread-safe.
 *
 * @param <T> the emitter type.
 */
public abstract class KetillEvent<T> {

    private final T emitter;

    /**
     * Constructs a new {@code KetillEvent}.
     *
     * @param emitter the object which emitted this event.
     */
    public KetillEvent(T emitter) {
        this.emitter = Objects.requireNonNull(emitter,
                "emitter cannot be null");
    }

    /**
     * Returns the object which emitted this event.
     *
     * @return the object which emitted this event.
     */
    public final @NotNull T getEmitter() {
        return this.emitter;
    }

}
