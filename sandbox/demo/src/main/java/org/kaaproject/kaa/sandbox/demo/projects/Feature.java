//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.03.24 at 08:04:04 PM EET 
//


package org.kaaproject.kaa.sandbox.demo.projects;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for feature.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="feature">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="CONFIGURATION"/>
 *     &lt;enumeration value="PROFILING"/>
 *     &lt;enumeration value="NOTIFICATION"/>
 *     &lt;enumeration value="EVENT"/>
 *     &lt;enumeration value="USER_VERIFIER"/>
 *     &lt;enumeration value="DATA_COLLECTION"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "feature")
@XmlEnum
public enum Feature {

    CONFIGURATION,
    PROFILING,
    NOTIFICATION,
    EVENT,
    USER_VERIFIER,
    DATA_COLLECTION;

    public String value() {
        return name();
    }

    public static Feature fromValue(String v) {
        return valueOf(v);
    }

}
