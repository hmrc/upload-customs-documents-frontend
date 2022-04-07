package uk.gov.hmrc.uploaddocuments.support

import org.scalatest.matchers.Matcher
import org.scalatest.matchers.MatchResult
import scala.io.AnsiColor

trait StateMatchers {

  final def beState(expected: AnyRef): Matcher[AnyRef] =
    new Matcher[AnyRef] {
      override def apply(obtained: AnyRef): MatchResult =
        if (obtained == expected)
          MatchResult(true, "", s"")
        else if (IdentityUtils.identityOf(obtained) != IdentityUtils.identityOf(expected))
          MatchResult(
            false,
            s"State ${AnsiColor.CYAN}${IdentityUtils.identityOf(
              expected
            )}${AnsiColor.RESET} has been expected but got state ${AnsiColor.CYAN}${IdentityUtils
              .identityOf(obtained)}${AnsiColor.RESET}",
            s""
          )
        else {
          val diff = Diff(obtained, expected)
          MatchResult(
            false,
            s"Obtained state ${AnsiColor.CYAN}${IdentityUtils.identityOf(obtained)}${AnsiColor.RESET} content differs from the expected:\n$diff}",
            s""
          )
        }

    }

}
