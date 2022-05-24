package io.ketill;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An interface for updating the state of a {@link RegisteredIoFeature}.
 *
 * @param <Z> the internal state type.
 * @param <P> the mapping parameters type.
 * @see MappedFeatureRegistry#mapFeature(IoFeature, Object, StateUpdater)
 * @see FeatureAdapter
 */
@FunctionalInterface
public interface StateUpdater<Z, P> {

    /**
     * A {@link StateUpdater} which takes in no parameters.
     *
     * @param <Z> the internal state type.
     * @see MappedFeatureRegistry#mapFeature(IoFeature, NoParams)
     */
    @FunctionalInterface
    interface NoParams<Z> {

        /**
         * Called by the input device when updating {@code feature}.
         *
         * @param state the state to update.
         */
        void update(@NotNull Z state);

    }

    /**
     * Called by the input device when updating {@code feature}.
     *
     * @param state the state to update.
     * @param params the mapping parameters.
     */
    void update(@NotNull Z state, @Nullable P params);

}
