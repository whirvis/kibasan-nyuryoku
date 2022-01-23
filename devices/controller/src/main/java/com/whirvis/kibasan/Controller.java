package com.whirvis.kibasan;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.Map;

/**
 * A controller which and can send receive input data. Examples of controllers
 * include, but are not limited to: XBOX controllers, PlayStation controllers,
 * Nintendo Switch controllers, etc.
 * <p/>
 * <b>Note:</b> For input data to stay up-to-date, the controller must be
 * polled periodically via the {@link #poll()} method. It is recommended
 * to poll the controller once every application update.
 */
public abstract class Controller extends InputDevice {

    /**
     * The left and right analog sticks of the controller.<br>
     * These may not be present, and as such may be {@code null}.
     */
    public final @Nullable Vector3fc ls, rs;

    /**
     * The left and right analog triggers of the controller.<br>
     * These may not be present, and as such may be {@code null}.
     */
    public final @Nullable Trigger1fc lt, rt;

    /*
     * Due to some specificities with how InputDevice is constructed, this
     * field is initialized only when first required. If it were initialized
     * in this class's constructor, it would not be initialized in time for
     * device features registered via the @FeaturePresent annotation.
     */
    private @Nullable Map<RumbleMotor, Vibration1f> rumbleMotors;

    /**
     * Constructs a new {@code Controller}. If {@code ls}, {@code rs},
     * {@code lt}, or {@code rt} are not {@code null}, they will be
     * registered automatically during construction (assuming they are not
     * already registered via the {@link FeaturePresent} annotation.)
     *
     * @param id      the controller ID.
     * @param adapter the controller adapter.
     * @param ls      the left analog stick, may be {@code null}.
     * @param rs      the right analog stick, may be {@code null}.
     * @param lt      the left analog trigger, may be {@code null}.
     * @param rt      the right analog trigger, may be {@code null}.
     * @throws NullPointerException if {@code id} or {@code adapter} are
     *                              {@code null}.
     */
    public Controller(@NotNull String id, @NotNull DeviceAdapter<?> adapter,
                      @Nullable AnalogStick ls, @Nullable AnalogStick rs,
                      @Nullable AnalogTrigger lt, @Nullable AnalogTrigger rt) {
        super(id, adapter);

        this.ls = this.registerAndGetState(ls);
        this.rs = this.registerAndGetState(rs);
        this.lt = this.registerAndGetState(lt);
        this.rt = this.registerAndGetState(rt);
    }

    /**
     * A method for a special edge case; that being if a feature is not
     * already registered to the controller when specified at construction.
     * This method exists solely to prevent users from needing to use the
     * {@link FeaturePresent} annotation when extending {@code Controller}.
     *
     * @param feature the feature whose state to fetch, and to register if
     *                not already registered to this controller.
     * @param <S>     the state container type.
     * @return the state as returned by {@link #getState(DeviceFeature)},
     * {@code null} if {@code feature} is {@code null}.
     */
    /* @formatter:off */
    private <S> @Nullable S
            registerAndGetState(@Nullable DeviceFeature<S> feature) {
        if (feature == null) {
            return null;
        }
        if (!this.isRegistered(feature)) {
            this.registerFeature(feature);
        }
        return this.getState(feature);
    }
    /* @formatter:on */

    /* @formatter:off */
    @Override
    public <F extends DeviceFeature<S>, S> @NotNull RegisteredFeature<F, S>
            registerFeature(@NotNull F feature) {
        RegisteredFeature<F, S> registered = super.registerFeature(feature);
        if (feature instanceof RumbleMotor) {
            if (rumbleMotors == null) {
                this.rumbleMotors = new HashMap<>();
            }
            rumbleMotors.put((RumbleMotor) feature,
                        (Vibration1f) this.getState(feature));
        }
        return registered;
    }
    /* @formatter:on */

    @Override
    public void unregisterFeature(@NotNull DeviceFeature<?> feature) {
        super.unregisterFeature(feature);
        if (feature instanceof RumbleMotor && rumbleMotors != null) {
            rumbleMotors.remove(feature);
        }
    }

    /**
     * Sets the vibration force of each rumble motor. To prevent unexpected
     * behavior, the force is capped between a value of {@code 0.0F} and
     * {@code 1.0F}.
     *
     * @param force the vibration force to set each motor to.
     */
    public void rumble(float force) {
        if (rumbleMotors == null) {
            return;
        }
        float capped = Math.min(Math.max(force, 0.0F), 1.0F);
        for (Vibration1f vibration : rumbleMotors.values()) {
            vibration.force = capped;
        }
    }

}
