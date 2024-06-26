resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns
)

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"     % "3.21.0")
addSbtPlugin("org.playframework" % "sbt-plugin"         % "3.0.2")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables" % "2.5.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"       % "2.5.2")
addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix"       % "0.12.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"      % "2.0.11")
