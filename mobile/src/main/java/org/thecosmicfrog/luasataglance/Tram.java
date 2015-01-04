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
        if (Integer.parseInt(dueMinutes) > 1)
            return String.format("%s\t\t\t\t\t\t\t\t%s", destination, dueMinutes + " mins");

        return String.format("%s\t\t\t\t\t\t\t\t%s", destination, dueMinutes + " min");
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
