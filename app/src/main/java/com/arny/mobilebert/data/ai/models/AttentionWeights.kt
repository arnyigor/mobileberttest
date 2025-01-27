package com.arny.mobilebert.data.ai.models

data class AttentionWeights(
    val layerWeights: List<LayerAttention>,
    val averageAttention: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttentionWeights

        if (layerWeights != other.layerWeights) return false
        if (!averageAttention.contentEquals(other.averageAttention)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = layerWeights.hashCode()
        result = 31 * result + averageAttention.contentHashCode()
        return result
    }
}