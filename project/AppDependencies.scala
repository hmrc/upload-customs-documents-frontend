import sbt.*

object AppDependencies {

  val bootstrapVersion     = "10.7.0"
  val playSuffix           = "-play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% s"bootstrap-frontend$playSuffix"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"              % "2.12.0",
    "uk.gov.hmrc"                  %% "play-frontend-hmrc-play-30"      % "13.9.0",
    "com.sun.mail"                  % "jakarta.mail"                    % "2.0.2",
    "org.jsoup"                     % "jsoup"                           % "1.22.2",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"            % "2.22.1",
    "uk.gov.hmrc.objectstore"      %% "object-store-client-play-30"     % "2.6.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalamock"          %% "scalamock"                   % "7.5.5"           % Test,
    "org.scalatest"          %% "scalatest"                   % "3.2.20"          % Test,
    "com.vladsch.flexmark"    % "flexmark-all"                % "0.64.8"          % Test,
    "org.scalameta"          %% "munit-diff"                  % "1.3.4"           % Test,
    "org.scalacheck"         %% "scalacheck"                  % "1.19.0"          % Test,
    "org.scalatestplus"      %% "scalacheck-1-18"             % "3.2.19.0"        % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"          % "7.0.2"           % Test,
    "com.github.tomakehurst"  % "wiremock-jre8"               % "3.0.1"           % Test,
    "uk.gov.hmrc"            %% s"bootstrap-test$playSuffix"  % bootstrapVersion  % Test
  )
}
