import sbt.*

object AppDependencies {

  val bootstrapVersion     = "10.7.0"
  val playSuffix           = "-play-30"

  val compile = Seq(
    "uk.gov.hmrc"                  %% s"bootstrap-frontend$playSuffix"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"              % "2.12.0",
    "uk.gov.hmrc"                  %% "play-frontend-hmrc-play-30"      % "12.32.0",
    "com.sun.mail"                  % "javax.mail"                      % "1.6.2",
    "org.jsoup"                     % "jsoup"                           % "1.21.2",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"            % "2.17.1",
    "uk.gov.hmrc.objectstore"      %% "object-store-client-play-30"     % "2.5.0"
  )

  val test = Seq(
    "org.scalamock"          %% "scalamock"                   % "6.2.0"           % "test",
    "org.scalatest"          %% "scalatest"                   % "3.2.19"          % "test, it",
    "com.vladsch.flexmark"    % "flexmark-all"                % "0.64.8"          % "test, it",
    "org.scalameta"          %% "munit-diff"                  % "1.0.4"           % "test, it",
    "org.scalacheck"         %% "scalacheck"                  % "1.19.0"          % "test, it",
    "org.scalatestplus"      %% "scalacheck-1-18"             % "3.2.19.0"        % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"          % "7.0.1"           % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8"               % "3.0.1"           % "it",
    "uk.gov.hmrc"            %% s"bootstrap-test$playSuffix"  % bootstrapVersion  % "test"
  )
}
