import sbt.*

object AppDependencies {

  val bootstrapVersion = "10.3.0"
  val playSuffix       = "-play-30"

  val compile = Seq(
    "uk.gov.hmrc"                  %% s"bootstrap-frontend$playSuffix"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"            %% s"hmrc-mongo$playSuffix"          % "2.10.0",
    "uk.gov.hmrc"                  %% s"play-frontend-hmrc$playSuffix"  % "12.19.0",
    "com.sun.mail"                  % "javax.mail"                      % "1.6.2",
    "org.jsoup"                     % "jsoup"                           % "1.21.2",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"            % "2.17.1",
    "uk.gov.hmrc.objectstore"      %% s"object-store-client$playSuffix" % "2.5.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test$playSuffix"    % bootstrapVersion % "test",
    "org.scalamock"          %% "scalamock"          % "6.2.0"    % "test",
    "org.scalatest"          %% "scalatest"          % "3.2.19"   % "test",
    "com.vladsch.flexmark"    % "flexmark-all"       % "0.64.8"   % "test",
    "org.scalameta"          %% "munit-diff"         % "1.0.4"    % "test",
    "org.scalacheck"         %% "scalacheck"         % "1.19.0"   % "test",
    "org.scalatestplus"      %% "scalacheck-1-18"    % "3.2.19.0" % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1"    % "test",
    "com.github.tomakehurst"  % "wiremock-jre8"      % "3.0.1"    % "test"
  )
}
