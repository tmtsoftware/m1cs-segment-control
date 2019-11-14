package org.tmt.m1cs.segmentdeploy

import csw.framework.deploy.containercmd.ContainerCmd

object SegmentContainerCmdApp extends App {

  ContainerCmd.start("segment-container-cmd-app", args)

}
