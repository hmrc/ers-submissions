resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy",url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

addSbtPlugin("uk.gov.hmrc"          % "sbt-auto-build"          % "3.9.0")
addSbtPlugin("uk.gov.hmrc"          % "sbt-distributables"      % "2.2.0")
addSbtPlugin("org.scoverage"        % "sbt-scoverage"           % "2.0.8")
addSbtPlugin("com.beautiful-scala"  % "sbt-scalastyle"          % "1.5.1")
addSbtPlugin("net.virtual-void"     % "sbt-dependency-graph"    % "0.9.2")
addSbtPlugin("com.timushev.sbt"     % "sbt-updates"             % "0.6.4")
addSbtPlugin("com.typesafe.play"    % "sbt-plugin"              % "2.8.20")
