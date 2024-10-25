package net.lctafrica.membership.api.util

import org.springframework.data.domain.Page

data class Result<T>(
    val success: Boolean,
    val msg: String? = null,
    val data: T? = null,
    var results: Int? = null
){
    init {
        if (data is Page<*>) results = data.totalPages
    }
}
