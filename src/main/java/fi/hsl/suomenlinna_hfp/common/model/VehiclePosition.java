package fi.hsl.suomenlinna_hfp.common.model;

public interface VehiclePosition {
    /**
     * Get the ID of the vehicle
     * @return ID of the vehicle
     */
    String getId();
    /**
     * Get the coordinates of the vehicle
     * @return Coordinates of the vehicle
     */
    LatLng getCoordinates();
    /**
     * Get the speed of the vehicle in metres per seconds
     * @return Speed in metres per second
     */
    double getSpeed();

    /**
     * Get the heading of the vehicle in degrees from north
     * @return Degrees from north
     */
    double getHeading();
    /**
     * Get the timestamp of the vehicle
     * @return Timestamp in milliseconds
     */
    long getTimestamp();
}
