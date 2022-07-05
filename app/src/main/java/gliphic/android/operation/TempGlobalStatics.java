/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.operation;

import gliphic.android.exceptions.NullStaticVariableException;

import androidx.annotation.NonNull;

/**
 * This class provides methods to temporarily (i.e. fewer than 10 seconds) store static variables which can be accessed
 * globally.
 *
 * Example use cases include:
 * - An alternative to passing intents to activities, e.g. when starting a new activity from an existing activity,
 *   which also allows any arbitrary non-primitive object to be received. This can be used to avoid serialisation of
 *   Contact and Group objects which have circular references (cause by their common groups and known/extended contacts
 *   member variables).
 *
 * Note that this approach should not be taken lightly as these statics may be reset at any time should the application
 * decide to free up memory.
 */
public class TempGlobalStatics {

    /**
     * Used to get a contact from a ContactsAdapter in a recycler view and deliver it to a ContactDetailsActivity
     * instance.
     */
    private static Contact contactClicked = null;

    public static void setContactClicked(Contact contact) {
        contactClicked = contact;
    }

    @NonNull
    public static Contact getContactClicked() throws NullStaticVariableException {
        Contact contactClickedToReturn = contactClicked;

        if (contactClickedToReturn == null) {
            throw new NullStaticVariableException("Global static 'contactClicked' is null.");
        }

        contactClicked = null;
        return contactClickedToReturn;
    }

    /**
     * Used to get a group from a GroupsAdapter in a recycler view and deliver it to a GroupDetailsActivity instance.
     */
    private static Group groupClicked = null;

    public static void setGroupClicked(Group group) {
        groupClicked = group;
    }

    @NonNull
    public static Group getGroupClicked() throws NullStaticVariableException {
        Group groupClickedToReturn = groupClicked;

        if (groupClickedToReturn == null) {
            throw new NullStaticVariableException("Global static 'groupClicked' is null.");
        }

        groupClicked = null;
        return groupClickedToReturn;
    }

    /**
     * Erase all traces of the current contact from internal memory.
     */
    public static void setNullTempGlobalStatics() {
        contactClicked = null;
        groupClicked = null;
    }
}
