package com.arny.mobilebert.data.ai.tokenizers

/** Word piece tokenization to split a piece of text into its word pieces.  */
class WordpieceTokenizer(private val dic: Map<String, Int>) {
    /**
     * Tokenizes a piece of text into its word pieces. This uses a greedy longest-match-first
     * algorithm to perform tokenization using the given vocabulary. For example: input = "unaffable",
     * output = ["un", "##aff", "##able"].
     *
     * @param text: A single token or whitespace separated tokens. This should have already been
     * passed through `BasicTokenizer.
     * @return A list of wordpiece tokens.
     */
    fun tokenize(text: String): List<String> {
        val outputTokens = mutableListOf<String>()
        for (token in BasicTokenizer.whitespaceTokenize(text)) {
            if (token.length > MAX_INPUTCHARS_PER_WORD) {
                outputTokens.add(UNKNOWN_TOKEN)
                continue
            }

            var isBad = false // Mark if a word cannot be tokenized into known subwords.
            var start = 0
            val subTokens = mutableListOf<String>()

            while (start < token.length) {
                var curSubStr = ""

                var end = token.length // Longer substring matches first.
                while (start < end) {
                    val subStr =
                        if (start == 0) token.substring(start, end) else "##" + token.substring(
                            start,
                            end
                        )
                    if (dic.containsKey(subStr)) {
                        curSubStr = subStr
                        break
                    }
                    end--
                }

                // The word doesn't contain any known subwords.
                if ("" == curSubStr) {
                    isBad = true
                    break
                }

                // curSubStr is the longeset subword that can be found.
                subTokens.add(curSubStr)

                // Proceed to tokenize the resident string.
                start = end
            }

            if (isBad) {
                outputTokens.add(UNKNOWN_TOKEN)
            } else {
                outputTokens.addAll(subTokens)
            }
        }

        return outputTokens
    }

    companion object {
        private const val UNKNOWN_TOKEN = "[UNK]" // For unknown words.
        private const val MAX_INPUTCHARS_PER_WORD = 200
    }
}