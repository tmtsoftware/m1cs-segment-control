package org.tmt.m1cs.segmentdeploy

import csw.framework.deploy.hostconfig.HostConfig

object SegmentHostConfigApp extends App {

  HostConfig.start("segment-host-config-app", args)

}
