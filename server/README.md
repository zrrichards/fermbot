# Fermbot
## The Raspberry Pi -based fermentation controller

This project is still under active development and is not stable

Version: 0.1

This project is licenced under the GPL v3. See the license for details. This project may freely be used by any commercial
brewery. However, any modifications to the software must be licensed and released under the GPL v3

##Structure
The Fermbot is broken down into two modules:
1. The monitor collects fermentation statistics and uploads them to brewfather. It takes a snapshot of the fermentation
   every 15 minutes.
2. The controller is responsible for activating the heating and cooling relays to control fermentation

##Setup and Installation


##Hardware Requirements
Below is how to assemble the hardware. See the [BOM](BOM.md) document for the required hardware

by default 1-wire protocol is to be enabled on GPIO4

... in progress ...

## API
Below describes the API of the webserver portion of Fermbot. The front-end client of this API is in the `../webclient` directory.

Right now the Fermbot only supports a single batch at a time. That may be changed in the future.
###Configuration of the current Fermentation Profile
Here is an example of how to configure the following fermentation profile (here is a lager)

|Temperature |Duration        |Stage Description   |Include Ramp In Stage |
|------------|----------------|--------------------|----------------------|
|48F         |Until SG=1.023  |Primary Fermentation|N/A                   |
|62F         |For 2 days      |Diacetyl Rest       |false                 |
|34F         |For 14 days     |Cold Crash          |true                  |

*Note: Including the ramp for a specific-gravity based setpoint is nonsensical, there is no 'ramp' to a gravity. Thus, the "includeRamp" property is not applicable
setting the ramp to 'false' for the 62F stage means that the beer will be held for two days at 62F, regardless of the amount of time it takes for the beer to reach that temperature.
The setting of the ramp to 'true' for the 34F means that the 14day clock will start ticking as soon as this stage starts, so if it takes the beer 2 days to go from 62F to 34F, then
the beer will be held at 34F for 12 days (14 days in the setpoint minus the two days it takes to reach the setpoint)*

```json
[
 {
	"tempSetpoint": "48F",
	"untilSg": 1.023,
	"stageDescription": "Primary"
 },
 {
	"tempSetpoint": "62F",
	"duration": "P2D", 
	"stageDescription": "Diacetyl Rest",
	"includeRamp": false 
 },
 {
	"tempSetpoint": "34F",
	"duration": "P14D",
	"stageDescription": "Cold Crash",
	"includeRamp": true
 }
]
```
**Note** Durations are in [ISO-8601 format]("https://www.digi.com/resources/documentation/digidocs/90001437-13/reference/r_iso_8601_duration_format.htm")

The fermentation profile can be retrieved or changed via a GET or POST request to the "/profile" URL.

The JSON payload of the current stage can be retrieved by issuing a GET request to the "/profile/currentStage" URL
The current stage can be immediately set by sending a POST request to "/profile/currentStage" containing the 0-based index of the stage. For example, to immediately change the above profile to the 
"diacetyl rest stage", send a post request with the String "1" to the given URL

Each time the profile is updated, it is persisted to disk and will be retrieved upon next startup. The profile is serialized to JSON and written under 
`./.fermbot/current-profile.json` and the current stage (integer index of the stage within the profile) is written under
`./.fermbot/current-profile-stage`