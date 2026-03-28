package org.damon233.performtrackermod.data;

public enum DeviceType {
    PC("PC"),
    MAC("MAC"),
    EMB("EMB"),
    PHONE("PHONE"),
    HWM("HWM");

    private final String code;

    DeviceType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
