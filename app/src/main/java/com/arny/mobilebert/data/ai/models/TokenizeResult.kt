package com.arny.mobilebert.data.ai.models

data class TokenizeResult(
    val inputIds: LongArray,
    val maskIds: LongArray,
    val typeIds: LongArray,
    val tokens: List<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TokenizeResult

        if (!inputIds.contentEquals(other.inputIds)) return false
        if (!maskIds.contentEquals(other.maskIds)) return false
        if (!typeIds.contentEquals(other.typeIds)) return false
        if (tokens != other.tokens) return false

        return true
    }

    override fun hashCode(): Int {
        var result = inputIds.contentHashCode()
        result = 31 * result + maskIds.contentHashCode()
        result = 31 * result + typeIds.contentHashCode()
        result = 31 * result + tokens.hashCode()
        return result
    }
}
