package io.github.vprud.domain

data class Issue(
    val number: Int,
    val repositoryUrl: String,
    val title: String,
    val body: String,
    val labels: List<String>,
    val htmlUrl: String,
    val state: String,
    val createdAt: java.time.Instant,
    val updatedAt: java.time.Instant,
)
