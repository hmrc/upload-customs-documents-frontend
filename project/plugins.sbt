resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns
)

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"     % "3.8.0")
addSbtPlugin("com.typesafe.play" % "sbt-plugin"         % "2.8.18")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables" % "2.1.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"      % "2.0.6")
addSbtPlugin("com.lucidchart"    % "sbt-scalafmt"       % "1.16")
addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix"       % "0.10.4")
