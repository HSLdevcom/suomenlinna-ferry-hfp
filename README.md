# suomenlinna-ferry-hfp [![Test and create Docker image](https://github.com/HSLdevcom/suomenlinna-ferry-hfp/actions/workflows/test-and-build.yml/badge.svg)](https://github.com/HSLdevcom/suomenlinna-ferry-hfp/actions/workflows/test-and-build.yml)

Application for producing [HFP](https://digitransit.fi/en/developers/apis/4-realtime-api/vehicle-positions/) data from vehicles that have only location data available. The vehicle position will be matched to a scheduled trip from GTFS data and published over MQTT for customers.

## Development

Gradle build system is used.

1. Run tests with `./gradlew test`
2. Run application with `./gradlew run`
3. Build runnable JAR with `./gradlew shadowJar`

## Architecture

![Architecture diagram](docs/suomenlinna-ferry-hfp.png)

The application works by matching vehicle positions to scheduled trips by checking whether the vehicle is near the first stop of the trip when the trip should begin. Vehicle positions can be listened over MQTT or polled using HTTP. Schedules for the trips are found from a GTFS package that will be downloaded regularly.

Currently the application is optimized for matching Suomenlinna ferry positions available from [meri.digitraffic.fi](https://www.digitraffic.fi/meriliikenne/) to their scheduled trips, but other vehicles are also supported (e.g. robot buses). See `PollingVehicleStateProvider` and `MqttVesselLocationProvider` for examples.
