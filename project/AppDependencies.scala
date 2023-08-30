import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-28" % "7.21.0",
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-28"         % "1.3.0",
    "uk.gov.hmrc"                  %% "play-frontend-hmrc"         % "7.19.0-play-28",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.14.1",
    "com.sun.mail"                  % "javax.mail"                 % "1.6.2",
    "org.jsoup"                     % "jsoup"                      % "1.16.1"
  )

  val test = Seq(
    "org.scalamock"          %% "scalamock"          % "5.2.0"    % "test",
    "org.scalatest"          %% "scalatest"          % "3.2.16"   % "test, it",
    "com.vladsch.flexmark"    % "flexmark-all"       % "0.64.8"   % "test, it",
    "org.scalameta"          %% "munit"              % "0.7.29"   % "test, it",
    "org.scalacheck"         %% "scalacheck"         % "1.17.0"   % "test, it",
    "org.scalatestplus"      %% "scalacheck-1-15"    % "3.2.11.0" % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0"    % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8"      % "2.35.0"   % "it"
  )
}
