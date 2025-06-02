package io.github.vprud.service

import io.github.vprud.domain.Issue

interface IssueCheckService {
    fun checkForUpdates(): Map<Long, List<Issue>>
}
