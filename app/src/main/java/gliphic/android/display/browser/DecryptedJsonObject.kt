/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.display.browser

import org.json.JSONArray
import org.json.JSONObject

/**
 * Class used to read/construct any [JSONObject] received/sent from/to the web extension.
 */
class DecryptedJsonObject(private val message: JSONObject) {
    companion object {
        // Expected JSON object names.
        const val ALL_MESSAGES_NAME = "allMsgPairs"
        const val CIPHER_TEXT_NAME  = "ct"
        const val PLAIN_TEXT_NAME   = "pt"
    }

    data class MessagePair(val cipherText: String, val plainText: String)

    private val pairList = mutableListOf<MessagePair>()

    /**
     * Return true iff [pairList] is not empty.
     */
    fun hasPlainText(): Boolean {
        return pairList.isNotEmpty()
    }

    /**
     * Return the [JSONArray] from the [JSONObject] received from the web extension.
     */
    fun getJsonArray(): JSONArray {
        return message.get(CIPHER_TEXT_NAME) as JSONArray
    }

    /**
     * Add a known cipher text and plain text pair to this object.
     */
    fun add(cipherText: String, plainText: String) {
        pairList.add(MessagePair(cipherText, plainText))
    }

    /**
     * Construct and return a [JSONObject] using all added cipher text and plain text pairs.
     */
    fun toJsonObject(): JSONObject {
        val allMessages = JSONArray()

        for (pair in pairList) {
            val messagePair = JSONObject()
            messagePair.put(CIPHER_TEXT_NAME, pair.cipherText)
            messagePair.put(PLAIN_TEXT_NAME, pair.plainText)

            allMessages.put(messagePair)
        }

        val jsonObject = JSONObject()
        jsonObject.put(ALL_MESSAGES_NAME, allMessages)

        return jsonObject
    }
}