package org.thecosmicfrog.luasataglance;

public class Tram {
    private String destination;
    private String direction;
    private String dueMinutes;

    public Tram(String de, String di, String du) {
        destination = de;
        direction = di;
        dueMinutes = du;
    }

    @Override
    public String toString() {
        return String.format("%s - %s - %s", destination, direction, dueMinutes);
    }

    public String getDestination() {
        return destination;
    }

    public String getDirection() {
        return direction;
    }

    public String getDueMinutes() {
        return dueMinutes;
    }
}
