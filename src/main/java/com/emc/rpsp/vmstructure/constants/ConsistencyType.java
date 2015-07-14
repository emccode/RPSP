package com.emc.rpsp.vmstructure.constants;

public enum ConsistencyType {

    APPLICATION_CONSISTENT("Application-Consistent"),
    CRASH_CONSISTENT("Crash-Consistent"),
    UNKNOWN("Unknown-Consistency");

    private final String stateVal;

    private ConsistencyType(final String state) {
        stateVal = state;
    }

    public String value() {
        return stateVal;
    }

    public static ConsistencyType fromValue(String v) {
        return valueOf(v);
    }

}

