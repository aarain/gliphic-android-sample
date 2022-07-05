/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.interfaces;

/**
 * An interface allowing a boolean return value from a method which contains another interface.
 *
 * One example would be returning an object from a StringRequest or JSONObject request's overridden onResponse method
 * when using Volley to send a HTTP request.
 */

public interface BooleanCallback {
    void onReturn(boolean isSuccessful);
}
