package com.whirvis.kibasan.feature;

import org.joml.Vector4f;

public class Lightbar extends DeviceAnalog<Vector4f> {

	/**
	 * @param id
	 *            the lightbar ID.
	 * @throws NullPointerException
	 *             if {@code id} is {@code null}.
	 */
	public Lightbar(String id) {
		super(id);
	}

	@Override
	public Vector4f initial() {
		return new Vector4f();
	}

}
