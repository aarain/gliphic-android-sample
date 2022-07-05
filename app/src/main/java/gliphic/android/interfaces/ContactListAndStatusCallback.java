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

import java.util.List;

/**
 * An interface allowing a list of contacts and a boolean request status for a single HTTPS request type to be returned
 * from a method which contains another interface.
 *
 * One example would be returning an object from a StringRequest or JSONObject request's overridden onResponse method
 * when using Volley to send a HTTP request, such that the calling method takes no action when a server request is
 * already in progress (assuming that no action is taken in the called method when a request is already in progress).
 */

public interface ContactListAndStatusCallback {
    void onReturn(final List<Contact> contactList, final boolean requestInProgress);
}
