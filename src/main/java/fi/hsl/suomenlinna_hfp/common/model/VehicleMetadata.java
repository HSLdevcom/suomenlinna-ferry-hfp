package fi.hsl.suomenlinna_hfp.common.model;

public interface VehicleMetadata {
    /**
     * Get the ID of the vehicle
     * @return ID of the vehicle
     */
    String getId();
    /**
     * Get the label of the vehicle (e.g. vessel name) or null if unavailable
     * @return Label of the vehicle or null if unavailable
     */
    String getLabel();
}
