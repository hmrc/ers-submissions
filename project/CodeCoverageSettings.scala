import sbt.Setting
import scoverage.ScoverageKeys.{coverageMinimumStmtTotal, *}

object CodeCoverageSettings {

  private val settings: Seq[Setting[?]] = Seq(
    coverageExcludedPackages := "<empty>;Reverse.*;..*Routes.*;testOnlyDoNotUseInAppConf.*",
    coverageMinimumStmtTotal := 98,
    coverageFailOnMinimum := true,
    coverageHighlighting := true
  )

  def apply(): Seq[Setting[?]] = settings

}
