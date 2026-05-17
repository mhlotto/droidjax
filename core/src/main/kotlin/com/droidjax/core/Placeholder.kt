package com.droidjax.core

data class Placeholder(
    val index: Int,
    val start: Int,
    val end: Int,
    val label: String? = null,
    val defaultText: String = "",
) {
    init {
        require(index >= 0) { "Placeholder index must be non-negative." }
        require(start >= 0) { "Placeholder start must be non-negative." }
        require(end >= start) { "Placeholder end must be at or after start." }
    }

    val isEmpty: Boolean
        get() = start == end

    val range: IntRange
        get() = if (isEmpty) {
            start..start
        } else {
            start until end
        }
}
