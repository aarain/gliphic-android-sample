/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.interfaces;

import gliphic.android.operation.Contact;

/**
 * An interface allowing a contact and an access token to be returned from a method which contains another interface.
 *
 * One example would be returning an object from a StringRequest or JSONObject request's overridden onResponse method
 * when using Volley to send a HTTP request.
 */

public interface ContactAndAccessTokenCallback extends BaseContactCallback {
    void onReturn(final Contact contact, final String accessToken);
}
