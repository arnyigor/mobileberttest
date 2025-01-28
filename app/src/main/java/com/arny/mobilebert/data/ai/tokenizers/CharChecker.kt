package com.arny.mobilebert.data.ai.tokenizers

internal object CharChecker {
    /** To judge whether it's an empty or unknown character.  */
    @JvmStatic
    fun isInvalid(ch: Char): Boolean {
        return (ch.code == 0 || ch.code == 0xfffd)
    }

    /** To judge whether it's a control character(exclude whitespace).  */
    @JvmStatic
    fun isControl(ch: Char): Boolean {
        if (Character.isWhitespace(ch)) {
            return false
        }
        val type = Character.getType(ch)
        return (type == Character.CONTROL.toInt() || type == Character.FORMAT.toInt())
    }

    /** To judge whether it can be regarded as a whitespace.  */
    @JvmStatic
    fun isWhitespace(ch: Char): Boolean {
        if (Character.isWhitespace(ch)) {
            return true
        }
        val type = Character.getType(ch)
        return (type == Character.SPACE_SEPARATOR.toInt() || type == Character.LINE_SEPARATOR.toInt() || type == Character.PARAGRAPH_SEPARATOR.toInt())
    }

    /** To judge whether it's a punctuation.  */
    @JvmStatic
    fun isPunctuation(ch: Char): Boolean {
        val type = Character.getType(ch)
        return (type == Character.CONNECTOR_PUNCTUATION.toInt() || type == Character.DASH_PUNCTUATION.toInt() || type == Character.START_PUNCTUATION.toInt() || type == Character.END_PUNCTUATION.toInt() || type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() || type == Character.FINAL_QUOTE_PUNCTUATION.toInt() || type == Character.OTHER_PUNCTUATION.toInt())
    }
}