package com.arny.mobilebert.data.search

data class SearchResult(
    val block: TextBlock,
    val score: Float,
    val relevance: String // Процент релевантности для отображения
)