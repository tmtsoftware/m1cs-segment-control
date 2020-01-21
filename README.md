# M1CS Example

This project provides an example Assembly (control-assembly) and HCD (segment-hcd) for use by the 
M1CS team.  It contains Java examples of:
* an HCD (Hardware Control Daemon) and an Assembly using 
  TMT Common Software ([CSW](https://github.com/tmtsoftware/csw)) APIs
* creating Akka actors: standard code patterns, message handling, CSW integration
* CSW components and services usage: command sending and command handling/validation, event subscription, 
component CurrentState publishing.
* Component testing using JUnit and CSW testkit
* Akka actor testing using JUnit
* a Client project to execute commands and send events to components when deploying to a runtime environment.

This project also is used for the M1CS team to gain experience with the STIL Continuous Integration (CI) and BTEs.
An AWS BTE that includes a Jenkins build and test project is provided.  

This project implements . 

## M1CS Example Design

![Test Image 4](https://github.com/tmtsoftware/m1cs-segment-control/blob/master/M1CS%20example.jpg)

The Assembly and HCD example design is fairly typical of designs for assemblies and HCDs so far being explored by the controls group.

### Control Assembly example design
The JControlAssemblyHandler appears in grey color in the diagram as it is a CSW template class with hooks for lifecycle events and command handling events.  

During initialization, the Handler creates the JCommandHandlerActor, JEventHandlerActor, JEventPublisherActor and the JMonitorActor.  References to these actors are maintained by the Handler so that commands and events, etc can passed to them for processing.

During initialization, configuration for the assembly is obtained using the CSW Configuration Service.  An example of reading specific values from the service are included in the example code.

### Command Handling
Incoming commands to the assembly are first validated and then handled by the onSubmit() method that the programmer overrides.  In our case the command is passed to the JCommandHandlerActor for processing.  The example code shows how worker actors are used to handle commands asynchronously by creating a worker actor to handle each command.  The example contains one worker actor for the ‘SetConfigurationParameters’ command.

### HCD Command Handling
The Segment HCD also contains a JSegmentHcdHandlers class provided by the CSW framework that operates in an identical fashion as the one in the assembly.  During initialization the JSegmentHcdHander creates the JSegCommandHandlerActor, creates and starts the JStatePublisherActor and creates 492 JSegmentActor instances, one for each segment.

When the ‘SetConfigurationParameters’ command reaches the HCD, it is handled by the JSegmentHcdHandlers onSubmit() method that the programmer overrides, which passes it to the JSegCommandHandlerActor  that determines which JSegmentActor the command is meant for, and sends the appropriate message to that actor.

### Event Subscription and Handling
The JEventHandlerActor implements an example of event subscription and handling.  Any actor or actors can subscribe to events 
depending on the design of the assembly or HCD.


### State and Error Reporting
The JStatePublisherActor publishes a CurrentState message to the assembly.  The JStatePublisher uses TimerScheduler to send self generated messages at regular intervals. This is used to transmit state telemetry from the HCD(s)
to the assembly.  In the example, the assembly subscription callback delegates handling of the message to the JMonitorActor, that is used for state management of the assembly.  The MonitorActor also accepts manual state change messages from 
other actor components to change its state.  

The logging output associated with this example code can pollute other tests, so the 'Start' message sent from the JSegmentHcdHandlers.java class is commented out (the JMonitorActor supports start and stop messages that start and stop the timer):
```
        // uncomment to start the CurrentState publishing
        // statePublisherActor.tell(new JStatePublisherActor.StartMessage());
```
Events derived from monitor state and the HCD are published to outside the assembly using the JEventPublisherActor. 

## Testing
### JUnit Test Suites
Test suites covering individual components and combinations of components are included in the example:

**control-assembly/src/test/JSegmentAssemblyTest.java** - JUnit test suite for control-assembly component only

* testAssemblyShouldBeLocatableUsingLocationService - simple example test that tests that the assembly can be located by the location service.
	

**segment-hcd/src/test/JSegmentHcdTest.java** - JUnit test suite for segment HCD component only
* testHcdShouldBeLocatableUsingLocationService

**segment-deploy/src/test/JSegmentDeployTest** - JUnit test suite for assembly and HCD end to end tests
	
* testAssemblyHandlesCommand: traverses the entire command path of the example from the Assembly to the HCD to the designated segment actor for disposition.  This design differs from 492 HCDs and is proposed here merely as a design to compare and contrast strengths and weaknesses. It also serves as a proof of concept for one design approach that avoids using 492 HCDs.
* testPublishEvents - publishes events using the event service.  This example can be used to simulate events coming
from other subsystems. The JEventHandlerActor sets up a subscription and callback code handling the events from this test. 


### CI Build and Test 

A Jenkins project is included that builds from this Github project and runs the test suites. The Jenkins environment for M1CS team members is located at http://52.36.63.204:8080
The Jenkins test project that builds and runs the JUnit tests is named *M1CS_Segment_Control_Test*.

## Building and deploying manually
### Subprojects

* control-assembly - an assembly that talks to the segment HCD
* segment-hcd - an HCD that talks to the segment hardware
* segment-deploy - for starting/deploying HCDs and assemblies, plus client app code

### Prerequisites for running Components

#### CSW services
The CSW services need to be running before starting the components. 
This is done by starting the `csw-services.sh` script, which is installed as part of the csw build.
If you are not building csw from the sources, you can get the script as follows:

 - Download csw-apps zip from https://github.com/tmtsoftware/csw/releases.  This project is compatable with CSW version 1.0.0.
 - Unzip the downloaded zip.
 - Go to the bin directory where you will find `csw-services.sh` script.
 - Run `./csw_services.sh --help` to get more information.
 - Run `./csw_services.sh start` to start the location service and config server.

#### Setting up configuration files
The example uses a configuration file in the configuration service as an example of how to read a configuration from that service.
This file needs to be initialized in the configuration repository.

```
cd <project home>/segment-deploy/src/main/resources
./initialize-config.sh

<follow the login procedure for AAS authentication, config-admin/config-admin>
```

### Building the HCD and Assembly Applications

 - Run `sbt segment-deploy/universal:packageBin`, this will create self contained zip in `segment-deploy/target/universal` directory
 - Unzip the generated zip and cd into the bin directory

Note: An alternative method is to run `sbt stage`, which installs the applications locally in `segment-deploy/target/universal/stage/bin`.

### Running the HCD and Assembly

Run the container cmd script with arguments. For example:


* Start the HCD and assembly in a container using the Java implementations:

```
cd <project home directory>/segment-deploy
./target/universal/stage/bin/segment-container-cmd-app --local ./src/main/resources/JSampleContainer.conf
```

### Running the example Client
```
TBD
```


