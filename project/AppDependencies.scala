import sbt.*

object AppDependencies {

  val bootstrapVersion = "10.4.0"
  val playSuffix       = "-play-30"

  val compile = Seq(
    "uk.gov.hmrc"                  %% s"bootstrap-frontend$playSuffix"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"            %% s"hmrc-mongo$playSuffix"          % "2.10.0",
    "uk.gov.hmrc"                  %% s"play-frontend-hmrc$playSuffix"  % "12.20.0",
    "com.sun.mail"                  % "javax.mail"                      % "1.6.2",
    "org.jsoup"                     % "jsoup"                           % "1.21.2",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"            % "2.17.1",
    "uk.gov.hmrc.objectstore"      %% s"object-store-client$playSuffix" % "2.5.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test$playSuffix"    % bootstrapVersion,
    "org.scalamock"          %% "scalamock"                     % "6.2.0",
    "org.scalatest"          %% "scalatest"                     % "3.2.19",
    "com.vladsch.flexmark"    % "flexmark-all"                  % "0.64.8",
    "org.scalameta"          %% "munit-diff"                    % "1.0.4",
    "org.scalacheck"         %% "scalacheck"                    % "1.19.0",
    "org.scalatestplus"      %% "scalacheck-1-18"               % "3.2.19.0",
    "org.scalatestplus.play" %% "scalatestplus-play"            % "7.0.1",
    "com.github.tomakehurst"  % "wiremock-jre8"                 % "3.0.1"
  ).map(_ % Test)
}
