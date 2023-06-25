package io.ketill;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@IoApi.Friends(IoDevice.class)
final class IoDeviceFeatures implements Iterable<IoState<?>> {

    private final @NotNull ReadWriteLock featuresLock;
    private final @NotNull Map<String, IoFeature.Cache> features;
    private final @NotNull List<IoFeature<?>> featureList;
    private final @NotNull Map<IoFeature<?>, IoState<?>> statesMap;

    IoDeviceFeatures() {
        this.featuresLock = new ReentrantReadWriteLock();
        this.features = new HashMap<>();
        this.featureList = new ArrayList<>();
        this.statesMap = new HashMap<>();
    }

    @Nullable IoFeature.Cache getCache(@Nullable IoFeature<?> feature) {
        if (feature == null) {
            return null;
        }
        featuresLock.readLock().lock();
        try {
            IoFeature.Cache cache = features.get(feature.getId());
            if (cache == null || cache.feature != feature) {
                return null; /* same ID but different feature */
            }
            return cache;
        } finally {
            featuresLock.readLock().unlock();
        }
    }

    @Nullable IoFeature.Cache getCache(@Nullable String id) {
        if (id == null) {
            return null;
        }
        featuresLock.readLock().lock();
        try {
            return features.get(id);
        } finally {
            featuresLock.readLock().unlock();
        }
    }

    int size() {
        featuresLock.readLock().lock();
        try {
            return features.size();
        } finally {
            featuresLock.readLock().unlock();
        }
    }

    @NotNull List<@NotNull IoFeature<?>> getFeatures() {
        return Collections.unmodifiableList(featureList);
    }

    @SuppressWarnings("unchecked")
    <I, S extends IoState<I>> @NotNull S
    addFeature(@NotNull IoDevice device, @NotNull IoFeature<S> feature) {
        Objects.requireNonNull(feature, "feature cannot be null");

        featuresLock.writeLock().lock();
        try {
            String id = feature.getId();

            /*
             * As explained by the docs, we must not create a new instance
             * of the state. By keeping the original object intact, previous
             * references will remain valid. Furthermore, there can be no
             * two features with the same ID present at one time.
             */
            IoFeature.Cache current = features.get(id);
            if (current != null && current.feature == feature) {
                return (S) current.state;
            } else if (current != null) {
                String msg = "feature with ID \"" + id + "\" already present";
                throw new IoDeviceException(device, msg);
            }

            S state = feature.createVerifiedState();
            IoLogic<?> logic = feature.createVerifiedLogic(device, state);
            if (logic != null) {
                logic.startup();
            }

            features.put(id, new IoFeature.Cache(feature, state, logic));
            featureList.add(feature);
            statesMap.put(feature, state);

            return state;
        } finally {
            featuresLock.writeLock().unlock();
        }
    }

    @NotNull Map<@NotNull IoFeature<?>, @NotNull IoState<?>> getStates() {
        return Collections.unmodifiableMap(statesMap);
    }

    @Nullable IoFeature.Cache
    getCache(@Nullable String id, @NotNull Class<? extends IoFeature<?>> type) {
        Objects.requireNonNull(type, "type cannot be null");
        IoFeature.Cache cache = this.getCache(id);
        if (cache == null) {
            return null;
        }
        IoFeature<?> feature = cache.feature;
        if (!type.isAssignableFrom(feature.getClass())) {
            return null; /* unexpected type */
        }
        return cache;
    }

    @Override
    public @NotNull Iterator<@NotNull IoState<?>> iterator() {
        return statesMap.values().iterator();
    }

}