package com.whirvis.kibasan.seeker;

import com.whirvis.kibasan.InputDevice;
import com.whirvis.kibasan.SwitchProController;
import com.whirvis.kibasan.adapter.glfw.GlfwSwitchProAdapter;

public class GlfwSwitchProSeeker extends GlfwJoystickSeeker {

	public GlfwSwitchProSeeker(long ptr_glfwWindow) {
		super(SwitchProController.class, ptr_glfwWindow);
	}

	@Override
	protected InputDevice createDevice(long ptr_glfwWindow, int glfwJoystick) {
		return new SwitchProController(
				new GlfwSwitchProAdapter(ptr_glfwWindow, glfwJoystick));
	}

}
