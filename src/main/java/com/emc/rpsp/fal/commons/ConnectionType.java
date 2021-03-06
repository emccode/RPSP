package com.emc.rpsp.fal.commons;

import lombok.AllArgsConstructor;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@AllArgsConstructor
public enum ConnectionType {
    FIBER_CHANNEL("FiberChannel"),
    IP("IP"),
    NO_CONNECTION("NoConnection"),
    UNKNOWN("Unknown");

    private String name;

    public String toString() {
        return name;
    }

}
