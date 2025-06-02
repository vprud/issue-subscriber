package io.github.vprud.domain

data class Subscription(
    val chatId: Long,
    val repository: String,
    val labels: Set<String> = emptySet(),
    var lastCheckedIssueId: Int? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Subscription

        if (chatId != other.chatId) return false
        if (repository != other.repository) return false

        return true
    }

    override fun hashCode(): Int {
        var result = chatId.hashCode()
        result = 31 * result + repository.hashCode()
        return result
    }
}
