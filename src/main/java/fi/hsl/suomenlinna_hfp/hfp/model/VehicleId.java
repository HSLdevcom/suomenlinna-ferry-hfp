package fi.hsl.suomenlinna_hfp.hfp.model;

import java.util.Objects;

public class VehicleId {
    public final int operatorId;
    public final int vehicleId;

    public VehicleId(int operatorId, int vehicleId) {
        this.operatorId = operatorId;
        this.vehicleId = vehicleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleId vehicleId1 = (VehicleId) o;
        return operatorId == vehicleId1.operatorId &&
                vehicleId == vehicleId1.vehicleId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(operatorId, vehicleId);
    }

    @Override
    public String toString() {
        return operatorId+"/"+vehicleId;
    }
}
