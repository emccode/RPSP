package com.emc.rpsp.fal.commons;

import lombok.AllArgsConstructor;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@AllArgsConstructor
public enum SystemReportTransferMethod {
    ESRS("ESRS"),
    SMTP("SMTP"),
    FTPS("FTPS"),
    UNKNOWN("Unknown");

    private String name;

    public String toString() {
        return name;
    }

}
