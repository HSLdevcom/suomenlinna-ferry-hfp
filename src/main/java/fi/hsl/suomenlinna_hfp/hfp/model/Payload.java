package fi.hsl.suomenlinna_hfp.hfp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Payload {
    private final String desi;
    private final String dir;
    private final Integer oper;
    private final Integer veh;
    private final String tst;
    private final Integer tsi;
    private final Double spd;
    private final Integer hdg;
    private final Double lat;
    @JsonProperty("long")
    private final Double lon;
    private final Double acc;
    private final Integer dl;
    private final Double odo;
    private final Integer drst;
    private final String oday;
    private final Integer jrn;
    private final Integer line;
    private final String start;
    private final String loc;
    private final String stop;
    private final String route;
    private final Integer occu;
    private final String label;

    public Payload(String desi, String dir, Integer oper, Integer veh, String tst, Integer tsi, Double spd, Integer hdg, Double lat, Double lon, Double acc, Integer dl, Double odo, Integer drst, String oday, Integer jrn, Integer line, String start, String loc, String stop, String route, Integer occu, String label) {
        this.desi = desi;
        this.dir = dir;
        this.oper = oper;
        this.veh = veh;
        this.tst = tst;
        this.tsi = tsi;
        this.spd = spd;
        this.hdg = hdg;
        this.lat = lat;
        this.lon = lon;
        this.acc = acc;
        this.dl = dl;
        this.odo = odo;
        this.drst = drst;
        this.oday = oday;
        this.jrn = jrn;
        this.line = line;
        this.start = start;
        this.loc = loc;
        this.stop = stop;
        this.route = route;
        this.occu = occu;
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payload payload = (Payload) o;
        return Objects.equals(desi, payload.desi) &&
                Objects.equals(dir, payload.dir) &&
                Objects.equals(oper, payload.oper) &&
                Objects.equals(veh, payload.veh) &&
                Objects.equals(tst, payload.tst) &&
                Objects.equals(tsi, payload.tsi) &&
                Objects.equals(spd, payload.spd) &&
                Objects.equals(hdg, payload.hdg) &&
                Objects.equals(lat, payload.lat) &&
                Objects.equals(lon, payload.lon) &&
                Objects.equals(acc, payload.acc) &&
                Objects.equals(dl, payload.dl) &&
                Objects.equals(odo, payload.odo) &&
                Objects.equals(drst, payload.drst) &&
                Objects.equals(oday, payload.oday) &&
                Objects.equals(jrn, payload.jrn) &&
                Objects.equals(line, payload.line) &&
                Objects.equals(start, payload.start) &&
                Objects.equals(loc, payload.loc) &&
                Objects.equals(stop, payload.stop) &&
                Objects.equals(route, payload.route) &&
                Objects.equals(occu, payload.occu) &&
                Objects.equals(label, payload.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(desi, dir, oper, veh, tst, tsi, spd, hdg, lat, lon, acc, dl, odo, drst, oday, jrn, line, start, loc, stop, route, occu, label);
    }

    @Override
    public String toString() {
        return "Payload{" +
                "desi='" + desi + '\'' +
                ", dir='" + dir + '\'' +
                ", oper=" + oper +
                ", veh=" + veh +
                ", tst='" + tst + '\'' +
                ", tsi=" + tsi +
                ", spd=" + spd +
                ", hdg=" + hdg +
                ", lat=" + lat +
                ", lon=" + lon +
                ", acc=" + acc +
                ", dl=" + dl +
                ", odo=" + odo +
                ", drst=" + drst +
                ", oday='" + oday + '\'' +
                ", jrn=" + jrn +
                ", line=" + line +
                ", start='" + start + '\'' +
                ", loc='" + loc + '\'' +
                ", stop='" + stop + '\'' +
                ", route='" + route + '\'' +
                ", occu=" + occu +
                ", label='" + label + '\'' +
                '}';
    }
}
