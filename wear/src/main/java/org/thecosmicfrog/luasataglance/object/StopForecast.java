package org.thecosmicfrog.luasataglance.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StopForecast implements Serializable {

    private static final long serialVersionUID = 0L;

    private String message;
    private List<Tram> inboundTrams;
    private List<Tram> outboundTrams;

    public StopForecast() {
        inboundTrams = new ArrayList<Tram>();
        outboundTrams = new ArrayList<Tram>();
    }

    public void addInboundTram(Tram t) {
        // Check there are actually inbound trams running.
        if (t != null)
            inboundTrams.add(t);
    }

    public void addOutboundTram(Tram t) {
        // Check there are actually outbound trams running.
        if (t != null)
            outboundTrams.add(t);
    }

    public void setMessage(String m) {
        message = m;
    }

    public void setInboundTrams(List<Tram> i) {
        inboundTrams = i;
    }

    public void setOutboundTrams(List<Tram> o) {
        outboundTrams = o;
    }

    public String getMessage() {
        return message;
    }

    public List<Tram> getInboundTrams() {
        return inboundTrams;
    }

    public List<Tram> getOutboundTrams() {
        return outboundTrams;
    }
}
