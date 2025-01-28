package com.arny.mobilebert.data.ai.tokenizers

import com.arny.mobilebert.data.ai.tokenizers.CharChecker.isControl
import com.arny.mobilebert.data.ai.tokenizers.CharChecker.isInvalid
import com.arny.mobilebert.data.ai.tokenizers.CharChecker.isPunctuation
import com.arny.mobilebert.data.ai.tokenizers.CharChecker.isWhitespace
import java.util.Arrays
import java.util.Locale

/** Basic tokenization (punctuation splitting, lower casing, etc.)  */
class BasicTokenizer(private val doLowerCase: Boolean) {
    fun tokenize(text: String): MutableList<String> {
        val cleanedText: String = cleanText(text)

        val origTokens: MutableList<String> = whitespaceTokenize(cleanedText)

        val stringBuilder = StringBuilder()
        for (token in origTokens) {
            var token = token
            if (doLowerCase) {
                token = token.lowercase(Locale.getDefault())
            }
            val list: MutableList<String?> = runSplitOnPunc(token)
            for (subToken in list) {
                stringBuilder.append(subToken).append(" ")
            }
        }
        return whitespaceTokenize(stringBuilder.toString())
    }

    companion object {
        /* Performs invalid character removal and whitespace cleanup on text. */
        fun cleanText(text: String): String {
            val stringBuilder = StringBuilder("")
            for (index in 0 until text.length) {
                val ch = text[index]

                // Skip the characters that cannot be used.
                if (isInvalid(ch) || isControl(ch)) {
                    continue
                }
                if (isWhitespace(ch)) {
                    stringBuilder.append(" ")
                } else {
                    stringBuilder.append(ch)
                }
            }
            return stringBuilder.toString()
        }

        /* Runs basic whitespace cleaning and splitting on a piece of text. */
        fun whitespaceTokenize(text: String): MutableList<String> {
            return Arrays.asList<String?>(*text.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
        }

        /* Splits punctuation on a piece of text. */
        fun runSplitOnPunc(text: String): MutableList<String?> {
            val tokens: MutableList<String?> = ArrayList<String?>()
            var startNewWord = true
            for (i in 0 until text.length) {
                val ch = text[i]
                if (isPunctuation(ch)) {
                    tokens.add(ch.toString())
                    startNewWord = true
                } else {
                    if (startNewWord) {
                        tokens.add("")
                        startNewWord = false
                    }
                    tokens[tokens.size - 1] = tokens[tokens.size - 1] + ch
                }
            }

            return tokens
        }
    }
}