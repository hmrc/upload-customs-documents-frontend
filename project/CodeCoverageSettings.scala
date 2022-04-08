import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    ".*.Reverse[^.]*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    ".*Filters?",
    ".*Routes.*",
    ".*RoutesPrefix.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    "MicroserviceAuditConnector",
    "Module",
    "GraphiteStartUp",
    "uk.gov.hmrc.uploaddocuments.views.html.components.*"
  )

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 80.00,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}
