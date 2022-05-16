package io.ketill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("ConstantConditions")
class MappedFeatureRegistryTest {

    private MappedFeatureRegistry registry;

    @BeforeEach
    void createRegistry() {
        IoDeviceObserver events = mock(IoDeviceObserver.class);
        this.registry = new MappedFeatureRegistry(events);
    }

    @Test
    void testHasMapping() {
        /*
         * It makes no sense for a null feature to be mapped. As such, assume
         * this was a mistake by the user and throw an exception.
         */
        assertThrows(NullPointerException.class,
                () -> registry.hasMapping(null));
    }

    @Test
    void testMapFeature() {
        MockIoFeature feature = new MockIoFeature();
        AtomicBoolean updatedAdapter = new AtomicBoolean();

        /* feature must be registered for updates */
        registry.registerFeature(feature);

        /*
         * Ensure that the base mapFeature() method passes the correct
         * parameters when updating the feature. This is verified via
         * a randomly generated value on each test.
         */
        updatedAdapter.set(false);
        int paramsValue = new Random().nextInt();
        registry.mapFeature(feature, paramsValue,
                (s, p) -> updatedAdapter.set(p == paramsValue));
        assertTrue(registry.hasMapping(feature));
        registry.updateFeatures();
        assertTrue(updatedAdapter.get());

        /*
         * When mapping a feature without any specified parameters, but
         * still providing an updater which takes in a parameter, the
         * received parameter must be the feature itself.
         */
        updatedAdapter.set(false);
        registry.mapFeature(feature, (s, p) -> updatedAdapter.set(p == feature));
        registry.updateFeatures();
        assertTrue(updatedAdapter.get());

        /*
         * Mapping a feature with an updater that takes in no parameters
         * is allowed via a shorthand. Ensure that this shorthand works
         * by verifying that the feature was updated.
         */
        updatedAdapter.set(false);
        registry.mapFeature(feature, (s) -> updatedAdapter.set(true));
        registry.updateFeatures();
        assertTrue(updatedAdapter.get());

        /*
         * A null value is allowed for the parameters when mapping a feature.
         * While not recommended (StateUpdater.NoParams should be used in
         * this scenario), it is still not illegal. As such, no exception
         * should be thrown here.
         */
        /* @formatter:off */
        assertDoesNotThrow(() -> registry.mapFeature(feature,
                null, (f, s) -> {}));
        /* @formatter:on */

        /*
         * It makes no sense to map a null feature or map a feature to a
         * null updater. As such, assume these were mistakes by the user
         * and throw an exception.
         */
        assertThrows(NullPointerException.class,
                () -> registry.mapFeature(null, null, (f, s) -> {
        }));
        assertThrows(NullPointerException.class,
                () -> registry.mapFeature(feature, feature, null));
    }

    @Test
    void testUnmapFeature() {
        AtomicBoolean updatedAdapter = new AtomicBoolean();
        MockIoFeature feature = new MockIoFeature();

        /* feature must be registered for updates */
        RegisteredFeature<?, ?, ?> registeredFeature =
                registry.registerFeature(feature);

        /*
         * The unmapFeature() method should return false unless it has
         * actually unmapped a feature from an updater.
         */
        assertFalse(registry.unmapFeature(feature));
        registry.mapFeature(feature, (s) -> updatedAdapter.set(true));
        assertTrue(registry.unmapFeature(feature));
        assertFalse(registry.hasMapping(feature));

        /*
         * After a feature has been unmapped, its updater should be reassigned
         * to a no-op. For this test, that would mean the value of updated is
         * expected to remain false. If it is set to true, then unmapFeature()
         * has not properly unmapped the feature.
         */
        assertSame(registeredFeature.adapterUpdater,
                RegisteredFeature.NO_UPDATER);
        registry.updateFeatures();
        assertFalse(updatedAdapter.get());

        /*
         * It makes no sense to unmap a null feature. As such, assume
         * this was a mistake by the user and throw an exception.
         */
        assertThrows(NullPointerException.class,
                () -> registry.unmapFeature(null));
    }

    @Test
    void testGetFeatureCount() {
        /*
         * Since no features have been registered yet, this method should
         * return a value of zero.
         */
        assertEquals(0, registry.getFeatureCount());
    }

    @Test
    void testGetFeatures() {
        /*
         * The getFeatures() method provides a read-only view of all registered
         * features in a feature registry. Ensure that it never returns null
         * (even when it is empty) and that is unmodifiable from the outside.
         */
        Collection<RegisteredFeature<?, ?, ?>> features =
                registry.getFeatures();
        assertNotNull(features); /* this should never be null, only empty */
        assertThrows(UnsupportedOperationException.class, features::clear);
    }

    @Test
    void testGetFeatureRegistration() {
        MockIoFeature feature = new MockIoFeature();
        assertNull(registry.getFeatureRegistration(feature));

        /*
         * The getRegistered() method returns the instance of an earlier
         * registered feature. As such, it should return the same value
         * as the registerFeature() method.
         */
        RegisteredFeature<?, ?, ?> registeredFeature =
                registry.registerFeature(feature);
        assertSame(registeredFeature, registry.getFeatureRegistration(feature));

        /*
         * It makes no sense to get the registration of a null feature.
         * Assume this was a mistake by the user and throw an exception.
         */
        assertThrows(NullPointerException.class,
                () -> registry.getFeatureRegistration(null));
    }

    @Test
    void testGetState() {
        MockIoFeature feature = new MockIoFeature();

        /*
         * It makes no sense to retrieve the state of a null feature or the
         * state of a feature that is not yet registered. As such, assume
         * these were mistakes by the user and throw an exception.
         */
        assertThrows(NullPointerException.class, () -> registry.getState(null));
        assertThrows(IllegalStateException.class,
                () -> registry.getState(feature));

        /*
         * The value of state inside registeredField must match the value
         * returned by getState(), as it is a shorthand.
         */
        RegisteredFeature<?, ?, ?> registeredFeature =
                registry.registerFeature(feature);
        assertSame(registeredFeature.containerState,
                registry.getState(feature));
    }

    @Test
    void testGetInternalState() {
        MockIoFeature feature = new MockIoFeature();

        /*
         * It would not make sense to get the internal state of a null
         * feature or a feature which has not yet been registered. As
         * such, assume this was a user mistake and throw an exception.
         */
        assertThrows(NullPointerException.class,
                () -> registry.getInternalState(null));
        assertThrows(IllegalStateException.class,
                () -> registry.getInternalState(feature));

        /*
         * When the internal state of a feature is fetched, it should be
         * the same internal state contained in the feature registration.
         */
        RegisteredFeature<?, ?, ?> registered =
                registry.registerFeature(feature);
        Object internalState = registry.getInternalState(feature);
        assertSame(internalState, registered.internalState);
    }

    @Test
    void testRequestState() {
        MockIoFeature feature = new MockIoFeature();

        /*
         * It makes no sense to retrieve the state of a null feature.
         * Assume this was a user mistake and throw an exception.
         */
        assertThrows(NullPointerException.class, () -> registry.getState(null));
        assertThrows(IllegalStateException.class,
                () -> registry.getState(feature));

        /*
         * Unlike getState(), requestState() simply returns null if the
         * feature is not currently registered.
         */
        assertNull(registry.requestState(feature));
    }

    @Test
    void testRegisterFeature() {
        /* it is convient to test isRegistered() here */
        MockIoFeature feature = new MockIoFeature();
        assertFalse(registry.isFeatureRegistered(feature));
        RegisteredFeature<?, ?, ?> registeredFeature =
                registry.registerFeature(feature);
        assertTrue(registry.isFeatureRegistered(feature));

        /*
         * When a feature is registered, it should be updated even if it has
         * no mapping associated with it. Furthermore, the feature's update
         * should be called after the adapter's updater is called.
         */
        registry.updateFeatures();
        assertTrue(feature.updatedFeature);

        /*
         * Ensure that the fields within registeredFeature correlate to their
         * expected values. That being the contained feature, the state, as
         * well as the updater. If these are not their expected values, then
         * registerFeature() did not instantiate registeredFeature correctly.
         */
        assertSame(registeredFeature.feature, feature);
        assertSame(registeredFeature.adapterUpdater,
                RegisteredFeature.NO_UPDATER);

        /*
         * It makes no sense to register a null feature or a feature which
         * has already been registered. As such, assume these were mistakes
         * by the user and throw an exception.
         */
        assertThrows(NullPointerException.class,
                () -> registry.registerFeature(null));
        assertThrows(IllegalStateException.class,
                () -> registry.registerFeature(feature));

        /*
         * These states will be used to simulate the situation in which a
         * feature being registered shares an internal state and container
         * state with a previously registered feature.
         */
        MockIoFeature crewmate = new MockIoFeature();
        MockIoFeature imposter = new MockIoFeature();
        registry.registerFeature(crewmate);

        /*
         * The internal state of a feature cannot be the internal state or
         * container state of a previously registered feature.
         */
        imposter.internalState = crewmate.internalState;
        assertThrows(IllegalStateException.class,
                () -> registry.registerFeature(imposter));
        imposter.internalState = crewmate.containerState;
        assertThrows(IllegalStateException.class,
                () -> registry.registerFeature(imposter));

        /* use fresh internal state for next test */
        imposter.internalState = new Object();

        /*
         * The container state of a feature cannot be the internal state or
         * container state of a previously registered feature.
         */
        imposter.containerState = crewmate.internalState;
        assertThrows(IllegalStateException.class,
                () -> registry.registerFeature(imposter));
        imposter.containerState = crewmate.containerState;
        assertThrows(IllegalStateException.class,
                () -> registry.registerFeature(imposter));
    }

    @Test
    void testUnregisterFeature() {
        MockIoFeature feature = new MockIoFeature();
        registry.registerFeature(feature); /* required to unregister */

        registry.unregisterFeature(feature);
        assertFalse(registry.isFeatureRegistered(feature));

        /*
         * It makes no sense to unregister a null feature or a feature which
         * has already been unregistered. As such, assume these were mistakes
         * by the user and throw an exception.
         */
        assertThrows(NullPointerException.class,
                () -> registry.unregisterFeature(null));
        assertThrows(IllegalStateException.class,
                () -> registry.unregisterFeature(feature));
    }

}