import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-28" % "5.20.0",
    "uk.gov.hmrc"                  %% "auth-client"                % "5.8.0-play-28",
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-28"         % "0.59.0",
    "uk.gov.hmrc"                  %% "crypto"                     % "6.1.0",
    "uk.gov.hmrc"                  %% "play-frontend-hmrc"         % "2.0.0-play-28",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.12.5",
    "com.sun.mail"                  % "javax.mail"                 % "1.6.2",
    "org.jsoup"                     % "jsoup"                      % "1.14.3"
  )

  val test = Seq(
    "org.scalatest"          %% "scalatest"          % "3.2.8"   % "test, it",
    "com.vladsch.flexmark"    % "flexmark-all"       % "0.36.8"  % "test, it",
    "org.scalameta"          %% "munit"              % "0.7.29"  % "test, it",
    "org.scalacheck"         %% "scalacheck"         % "1.15.4"  % "test, it",
    "org.scalatestplus"      %% "scalacheck-1-15"    % "3.2.8.0" % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0"   % "it",
    "com.github.tomakehurst"  % "wiremock-jre8"      % "2.27.2"  % "it"
  )
}