package com.emc.rpsp.fal.commons;

import javax.xml.bind.annotation.*;

//import com.emc.fapi.utils.cache.ObjectsGenerator;

@SuppressWarnings("serial")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EsxUID")
public class EsxUID implements Validateable {

    //This member handles all EsxUID objects retrieval\creation
//	private static ObjectsGenerator<EsxUID> esxUIDgenerator = new ObjectsGenerator<EsxUID>();

    public static EsxUID generateEsxUID(String uuid) {
        return (new EsxUID(uuid));
    }

    @XmlElement(required = true)
    private String uuid;

    public EsxUID() {
    }

    public EsxUID(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EsxUID other = (EsxUID) obj;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EsxUID [uuid=").append(uuid).append("]");
        return builder.toString();
    }
}
