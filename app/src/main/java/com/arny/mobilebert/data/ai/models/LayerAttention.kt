package com.arny.mobilebert.data.ai.models

data class LayerAttention(
    val weights: FloatArray,
    val headAttention: List<FloatArray>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LayerAttention

        if (!weights.contentEquals(other.weights)) return false
        if (headAttention != other.headAttention) return false

        return true
    }

    override fun hashCode(): Int {
        var result = weights.contentHashCode()
        result = 31 * result + headAttention.hashCode()
        return result
    }
}