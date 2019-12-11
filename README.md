# M1CS Example

This project provides an example Assembly (control-assembly) and HCD (segment-hcd) for use by the 
M1CS team.  It contains Java examples of:
* Akka actor: standard code patterns, message handling, CSW integration
* CSW components and services usage: command sending and command handling/validation, event subscription, 
component CurrentState publishing.
* Component testing using JUnit and CSW testkit
* Akka actor testing using JUnit

This project implements an HCD (Hardware Control Daemon) and an Assembly using 
TMT Common Software ([CSW](https://github.com/tmtsoftware/csw)) APIs. 

## Subprojects

* control-assembly - an assembly that talks to the segment HCD
* segment-hcd - an HCD that talks to the segment hardware
* segment-deploy - for starting/deploying HCDs and assemblies

## CI Build and Test 

A Jenkins project is included that builds from this Github project and runs the test suites. The Jenkins environment for M1CS team members is located at https://52.36.63.204:8080

## Building and deploying manually

## Prerequisites for running Components

The CSW services need to be running before starting the components. 
This is done by starting the `csw-services.sh` script, which is installed as part of the csw build.
If you are not building csw from the sources, you can get the script as follows:

 - Download csw-apps zip from https://github.com/tmtsoftware/csw/releases.
 - Unzip the downloaded zip.
 - Go to the bin directory where you will find `csw-services.sh` script.
 - Run `./csw_services.sh --help` to get more information.
 - Run `./csw_services.sh start` to start the location service and config server.

## Building the HCD and Assembly Applications

 - Run `sbt segment-deploy/universal:packageBin`, this will create self contained zip in `segment-deploy/target/universal` directory
 - Unzip the generated zip and cd into the bin directory

Note: An alternative method is to run `sbt stage`, which installs the applications locally in `segment-deploy/target/universal/stage/bin`.

## Running the HCD and Assembly

Run the container cmd script with arguments. For example:

* Run the HCD in standalone mode with a local config file (The standalone config format is differennt than the container format):

```
./target/universal/stage/bin/segment-container-cmd-app --standalone --local ./src/main/resources/SampleHcdStandalone.conf
```

* Start the HCD and assembly in a container using the Java implementations:

```
./target/universal/stage/bin/segment-container-cmd-app --local ./src/main/resources/JSampleContainer.conf
```

* Or the Scala versions:

```
./target/universal/stage/bin/segment-container-cmd-app --local ./src/main/resources/SampleContainer.conf
```

