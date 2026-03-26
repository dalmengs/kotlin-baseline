package com.dalmeng.template.other

import org.springframework.data.domain.Page

data class BasePagingResponse<R : Any>(
    val items: List<R>,
    val isFirstPage: Boolean,
    val isLastPage: Boolean,
    val pageSize: Int,
    val pageNumber: Int,
    val totalPages: Int,
    val totalItems: Long
) {
    companion object {
        fun <T : Any, R : Any> of(
            page: Page<T>,
            mapper: ((T) -> R)? = null
        ): BasePagingResponse<R> {

            @Suppress("UNCHECKED_CAST")
            val items = mapper?.let { page.content.map(it) }
                ?: (page.content as List<R>)

            return BasePagingResponse(
                items = items,
                isFirstPage = page.isFirst,
                isLastPage = page.isLast,
                pageSize = page.size,
                pageNumber = page.number + 1,
                totalPages = page.totalPages,
                totalItems = page.totalElements
            )
        }
    }
}