package io.gitlab.arturbosch.detekt.cli.out

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.cli.baseline.Baseline
import io.gitlab.arturbosch.detekt.cli.baseline.BaselineFormat
import io.gitlab.arturbosch.detekt.cli.baseline.Blacklist
import io.gitlab.arturbosch.detekt.cli.baseline.Whitelist
import io.gitlab.arturbosch.detekt.cli.baselineId
import io.gitlab.arturbosch.detekt.core.exists
import io.gitlab.arturbosch.detekt.core.isFile
import java.nio.file.Path
import java.time.Instant

/**
 * @author Artur Bosch
 */
class DetektBaselineFormat(reportsPath: Path) {

	companion object {
		val BASELINE_FILE = "baseline.xml"
	}

	private val baselinePath = reportsPath.resolve(BASELINE_FILE)

	private val listings: Pair<Whitelist, Blacklist>? =
			if (baselineExists()) {
				val format = BaselineFormat.read(baselinePath)
				format.whitelist to format.blacklist
			} else null

	fun filter(smells: List<Finding>) =
			if (listings != null) {
				val whiteFiltered = smells.filterNot { finding -> listings.first.ids.contains(finding.baselineId) }
				val blackFiltered = whiteFiltered.filterNot { finding -> listings.second.ids.contains(finding.baselineId) }
				blackFiltered
			} else smells

	fun create(smells: List<Finding>) {
		val timestamp = Instant.now().toEpochMilli().toString()
		val blacklist = if (baselineExists()) {
			BaselineFormat.read(baselinePath).blacklist
		} else {
			Blacklist(emptyList(), timestamp)
		}
		val ids = smells.map { it.baselineId }
		val smellBaseline = Baseline(blacklist, Whitelist(ids, timestamp))
		BaselineFormat.write(smellBaseline, baselinePath)
		println("Successfully wrote smell baseline to $baselinePath")
	}

	private fun baselineExists() = baselinePath.exists() && baselinePath.isFile()

}
