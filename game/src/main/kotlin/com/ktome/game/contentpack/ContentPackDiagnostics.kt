package com.ktome.game.contentpack

import com.ktome.core.phase.PackId
import com.ktome.core.world.solvability.ContentRef
import java.nio.file.Path

enum class ContentPackDiagnosticSeverity {
    ERROR,
    WARNING,
}

data class ContentPackDiagnostic(
    val code: String,
    val message: String,
    val severity: ContentPackDiagnosticSeverity = ContentPackDiagnosticSeverity.ERROR,
    val packId: PackId? = null,
    val targetRef: ContentRef? = null,
    val sourcePath: String? = null,
    val details: Map<String, String> = emptyMap(),
) {
    init {
        require(code.isNotBlank()) { "ContentPackDiagnostic.code must not be blank." }
        require(message.isNotBlank()) { "ContentPackDiagnostic.message must not be blank." }
        require(sourcePath == null || sourcePath.isNotBlank()) {
            "ContentPackDiagnostic.sourcePath must not be blank when present."
        }
        require(details.keys.all(String::isNotBlank)) { "ContentPackDiagnostic.details must not contain blank keys." }
        require(details.values.all(String::isNotBlank)) { "ContentPackDiagnostic.details must not contain blank values." }
    }
}

class ContentPackLoadException(
    val diagnostics: List<ContentPackDiagnostic>,
) : IllegalStateException(renderDiagnostics(diagnostics)) {
    init {
        require(diagnostics.isNotEmpty()) { "ContentPackLoadException requires at least one diagnostic." }
    }

    companion object {
        private fun renderDiagnostics(diagnostics: List<ContentPackDiagnostic>): String =
            diagnostics.joinToString(separator = "\n") { diagnostic ->
                buildString {
                    append(diagnostic.severity.name)
                    append(" [")
                    append(diagnostic.code)
                    append("] ")
                    append(diagnostic.message)
                    diagnostic.packId?.let { packId ->
                        append(" (pack=")
                        append(packId.value)
                        append(')')
                    }
                    diagnostic.targetRef?.let { targetRef ->
                        append(" (target=")
                        append(targetRef.registry.value)
                        append(':')
                        append(targetRef.id)
                        append(')')
                    }
                    diagnostic.sourcePath?.let { sourcePath ->
                        append(" (source=")
                        append(sourcePath)
                        append(')')
                    }
                }
            }
    }
}

internal fun MutableList<ContentPackDiagnostic>.addDiagnostic(
    code: String,
    message: String,
    severity: ContentPackDiagnosticSeverity = ContentPackDiagnosticSeverity.ERROR,
    packId: PackId? = null,
    targetRef: ContentRef? = null,
    sourcePath: Path? = null,
    details: Map<String, String> = emptyMap(),
) {
    add(
        ContentPackDiagnostic(
            code = code,
            message = message,
            severity = severity,
            packId = packId,
            targetRef = targetRef,
            sourcePath = sourcePath?.toString(),
            details = details,
        ),
    )
}
