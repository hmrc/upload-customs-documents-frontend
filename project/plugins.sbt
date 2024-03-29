resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns
)

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"     % "3.18.0")
addSbtPlugin("org.playframework" % "sbt-plugin"         % "3.0.1")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables" % "2.4.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"      % "2.0.9")
addSbtPlugin("com.lucidchart"    % "sbt-scalafmt"       % "1.16")
addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix"       % "0.11.1")
