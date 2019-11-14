import com.typesafe.sbt.SbtNativePackager.Universal

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `control-assembly`,
  `segment-hcd`,
  `segment-deploy`
)

lazy val `segment` = project
  .in(file("."))
  .aggregate(aggregatedProjects: _*)

lazy val `control-assembly` = project
  .settings(
    libraryDependencies ++= Dependencies.ControlAssembly
  )

lazy val `segment-hcd` = project
  .settings(
    libraryDependencies ++= Dependencies.SegmentHcd
  )

lazy val `segment-deploy` = project
  .dependsOn(
    `control-assembly`,
    `segment-hcd`
  )
  .enablePlugins(JavaAppPackaging, CswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.SegmentDeploy,
    // This is the placeholder for setting JVM options via sbt native packager.
    // You can add more JVM options below.
//    javaOptions in Universal ++= Seq(
//      // -J params will be added as jvm parameters
//      "-J-Xmx8GB",
//      "J-XX:+UseG1GC", // G1GC is default in jdk9 and above
//      "J-XX:MaxGCPauseMillis=30" // Sets a target for the maximum GC pause time. This is a soft goal, and the JVM will make its best effort to achieve it
//    )
  )
