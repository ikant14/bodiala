package com.miirphys.bodiala.provider.rezlive.client.dto.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * The in-body {@code <Authentication>} block carried by every RezLive XML request.
 * Contains exactly {@code AgentCode} and {@code UserName}; the API key is sent as the
 * {@code x-api-key} HTTP header, never in the body.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Authentication {

    @XmlElement(name = "AgentCode")
    private String agentCode;

    @XmlElement(name = "UserName")
    private String userName;

    public Authentication() {
    }

    public Authentication(String agentCode, String userName) {
        this.agentCode = agentCode;
        this.userName = userName;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
