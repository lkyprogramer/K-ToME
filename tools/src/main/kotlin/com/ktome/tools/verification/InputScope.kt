package com.ktome.tools.verification

data class InputScope(
    val scopeId: String,
    val pathPrefixes: List<String> = emptyList(),
    val contractIds: List<String> = emptyList(),
    val tagIds: List<String> = emptyList(),
    val ownerRequired: Boolean = false,
    val requestedTaskPaths: List<String> = emptyList(),
) {
    init {
        require(scopeId.isNotBlank()) { "InputScope.scopeId must not be blank." }
        require(pathPrefixes.all(String::isNotBlank)) { "InputScope($scopeId).pathPrefixes must not contain blanks." }
        require(contractIds.all(String::isNotBlank)) { "InputScope($scopeId).contractIds must not contain blanks." }
        require(tagIds.all(String::isNotBlank)) { "InputScope($scopeId).tagIds must not contain blanks." }
        require(requestedTaskPaths.all(String::isNotBlank)) { "InputScope($scopeId).requestedTaskPaths must not contain blanks." }
    }

    fun matches(path: String): Boolean {
        val normalizedPath = normalizePath(path)
        return pathPrefixes.any { prefix -> normalizedPath.startsWith(normalizePath(prefix)) }
    }

    companion object {
        internal fun normalizePath(path: String): String =
            path.replace('\\', '/').removePrefix("./")
    }
}
