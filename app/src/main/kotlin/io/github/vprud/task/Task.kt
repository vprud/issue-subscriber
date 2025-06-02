package io.github.vprud.task

import io.github.vprud.domain.Issue

typealias Notify = (Long, Issue) -> Unit

interface Task {
    fun run(notify: Notify)
}
