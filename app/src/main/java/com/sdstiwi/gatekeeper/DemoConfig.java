package com.sdstiwi.gatekeeper;

/**
 * Portfolio/demo configuration.
 * The repository is intentionally runnable without Firebase or an ESP32.
 */
public final class DemoConfig {
    private DemoConfig() {}

    /** Set to false only when a real ESP32 access-control device is configured. */
    public static final boolean DEMO_MODE = true;

    /**
     * Bluetooth MAC address used only in hardware mode.
     * Leave empty in the public demo configuration and set it locally when needed.
     */
    public static final String ESP32_DEVICE_ADDRESS = "";

    public static final String TEACHER_EMAIL = "teacher1@example.com";
    public static final String TEACHER_PASSWORD = "password123";

    public static final String STUDENT_EMAIL = "student1@example.com";
    public static final String STUDENT_PASSWORD = "password123";
}
