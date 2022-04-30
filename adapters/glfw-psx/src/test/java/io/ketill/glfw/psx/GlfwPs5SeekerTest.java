package io.ketill.glfw.psx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.lwjgl.glfw.GLFW.*;

class GlfwPs5SeekerTest {

    @Test
    void __init__() {
        assertThrows(NullPointerException.class,
                () -> new GlfwPs5Seeker(0x00));
        assertThrows(IllegalArgumentException.class,
                () -> new GlfwPs5Seeker(0x01));

        /*
         * For the next tests to successfully execute, GLFW must successfully
         * initialize. If it fails to do so, that is fine. It just means the
         * current machine does not have access to GLFW.
         */
        assumeTrue(glfwInit());

        /*
         * Any operating system running this test should pass, assuming a
         * valid GLFW window pointer is passed. If an error occurs here,
         * something was not configured correctly. Likely, device GUIDs
         * are missing for the current operating system.
         */
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        long ptr_glfwWindow = glfwCreateWindow(1024, 768, "", 0L, 0L);
        assertDoesNotThrow(() -> new GlfwPs5Seeker(ptr_glfwWindow));

        /* make sure to shut down GLFW */
        glfwDestroyWindow(ptr_glfwWindow);
        glfwTerminate();
    }

}