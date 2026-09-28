package com.blackboxbench.reproduction

/** The currently selected drawer destination. */
sealed class Nav {
    object MyTasks : Nav()
    object Today : Nav()
    object Recent : Nav()
    data class List(val id: String) : Nav()
    data class Tag(val name: String) : Nav()

    /** Preset filter rows that always exist under 过滤器. */
    val title: String
        get() = when (this) {
            is MyTasks -> "我的任务"
            is Today -> "今天"
            is Recent -> "最近修改过的"
            is List -> Repo.listName(id)
            is Tag -> name
        }

    fun sameAs(other: Nav): Boolean = when {
        this is MyTasks && other is MyTasks -> true
        this is Today && other is Today -> true
        this is Recent && other is Recent -> true
        this is List && other is List -> id == other.id
        this is Tag && other is Tag -> name == other.name
        else -> false
    }
}
