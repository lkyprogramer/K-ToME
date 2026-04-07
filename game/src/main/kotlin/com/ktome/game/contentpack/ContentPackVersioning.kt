package com.ktome.game.contentpack

internal data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemanticVersion> {
    init {
        require(major >= 0) { "SemanticVersion.major must not be negative." }
        require(minor >= 0) { "SemanticVersion.minor must not be negative." }
        require(patch >= 0) { "SemanticVersion.patch must not be negative." }
    }

    override fun compareTo(other: SemanticVersion): Int =
        compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"
}

internal enum class VersionOperator {
    LT,
    LTE,
    GT,
    GTE,
    EQ,
}

internal data class VersionConstraint(
    val operator: VersionOperator,
    val version: SemanticVersion,
) {
    fun matches(candidate: SemanticVersion): Boolean =
        when (operator) {
            VersionOperator.LT -> candidate < version
            VersionOperator.LTE -> candidate <= version
            VersionOperator.GT -> candidate > version
            VersionOperator.GTE -> candidate >= version
            VersionOperator.EQ -> candidate == version
        }
}

internal data class VersionRange(
    val raw: String,
    val constraints: List<VersionConstraint>,
) {
    init {
        require(raw.isNotBlank()) { "VersionRange.raw must not be blank." }
        require(constraints.isNotEmpty()) { "VersionRange.constraints must not be empty." }
    }

    fun matches(candidate: SemanticVersion): Boolean = constraints.all { constraint -> constraint.matches(candidate) }
}

internal object VersionRangeParser {
    private val tokenPattern = Regex("""^(<=|>=|<|>|=)?(\d+)\.(\d+)\.(\d+)$""")

    fun parse(raw: String): VersionRange {
        val tokens = raw.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        require(tokens.isNotEmpty()) { "Version range must not be blank." }
        return VersionRange(
            raw = raw,
            constraints = tokens.map(::parseConstraint),
        )
    }

    fun parseVersion(raw: String): SemanticVersion {
        val match = requireNotNull(tokenPattern.matchEntire(raw.trim())) {
            "Semantic version '$raw' must use MAJOR.MINOR.PATCH format."
        }
        return SemanticVersion(
            major = match.groupValues[2].toInt(),
            minor = match.groupValues[3].toInt(),
            patch = match.groupValues[4].toInt(),
        )
    }

    private fun parseConstraint(token: String): VersionConstraint {
        val match = requireNotNull(tokenPattern.matchEntire(token)) {
            "Unsupported version-range token '$token'."
        }
        val operator =
            when (match.groupValues[1]) {
                "<" -> VersionOperator.LT
                "<=" -> VersionOperator.LTE
                ">" -> VersionOperator.GT
                ">=" -> VersionOperator.GTE
                "", "=" -> VersionOperator.EQ
                else -> error("Unsupported version operator '${match.groupValues[1]}'.")
            }
        return VersionConstraint(
            operator = operator,
            version =
                SemanticVersion(
                    major = match.groupValues[2].toInt(),
                    minor = match.groupValues[3].toInt(),
                    patch = match.groupValues[4].toInt(),
                ),
        )
    }
}
