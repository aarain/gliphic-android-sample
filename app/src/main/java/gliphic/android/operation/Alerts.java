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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import libraries.Vars;
import pojo.account.GroupShare;
import pojo.misc.ContactAndGroupNumberPair;

/**
 * Holds static list(s), whose items are displayed as alerts, as well as related static methods.
 */
public class Alerts {

    /**
     * The list of information related to sent and received shared groups.
     */
    private static List<GroupShare> groupShares = null;

    /**
     * This Comparator is supplied to the Collections.sort() method to sort a given list of GroupShare objects based on
     * each share-time, from smallest to largest.
     */
    private static Comparator<GroupShare> SORT_BY_SHARE_TIME =
            (gs1, gs2) -> Long.compare(gs1.getShareTime(), gs2.getShareTime());

    /**
     * Sort a list of GroupShare objects based on each share-time, from largest to smallest.
     *
     * @param groupShareList    The list of GroupShare objects to sort.
     */
    public static void reverseSortByShareTime(@NonNull List<GroupShare> groupShareList) {
        Collections.sort(groupShareList, Collections.reverseOrder(SORT_BY_SHARE_TIME));
    }

    /**
     * Get the list of all currently loaded GroupShare objects.
     *
     * @return                              The list of currently loaded GroupShare objects.
     * @throws NullStaticVariableException  Thrown when the GroupShare list is null.
     */
    public static List<GroupShare> getGroupShares() throws NullStaticVariableException {
        if (groupShares == null) {
            throw new NullStaticVariableException("Group shares list is null.");
        }

        return groupShares;
    }

    /**
     * Remove all loaded GroupShare objects.
     */
    public static void setNullGroupShares() {
        groupShares = null;
    }

    /**
     * Return true if and only if the given status represents an actionable group-share.
     *
     * An alert is defined to be actionable for the current contact if they are able directly modify its status/type
     * e.g. a received group-share which the current contact can choose to accept or decline.
     *
     * @param groupShareStatus  A single status representing any (positive) number of group-shares.
     * @return                  True iff the given status represents an actionable group-share.
     */
    public static boolean isActionableGroupShare(@NonNull Vars.GroupShareStatus groupShareStatus) {
        return groupShareStatus.equals(Vars.GroupShareStatus.PENDING_RECEIVED);
    }

    /**
     * Return the number of actionable alerts.
     *
     * @return  An integer greater than or equal to 0 representing the number of actionable alerts.
     */
    public static int getActionableAlertsCount() {
        int counter = 0;

        if (groupShares != null) {
            for (GroupShare groupShare : groupShares) {
                if (isActionableGroupShare(groupShare.getShareStatus())) {
                    counter++;
                }
            }
        }

        return counter;
    }

    /**
     * Get a static-stored GroupShare object for a given (unique) set of parameters.
     *
     * @param contactNumber     The contact number of the object to return.
     * @param groupNumber       The group number of the object to return.
     * @return                  The matching GroupShare or null if no such object exists.
     */
    @Nullable
    public static GroupShare getGroupShare(long contactNumber, long groupNumber) {
        if (groupShares == null) {
            return null;
        }

        for (GroupShare groupShare : groupShares) {
            if ( groupShare.getContact().getNumber() == contactNumber &&
                 groupShare.getGroup().getNumber()   == groupNumber ) {

                return groupShare;  // Assume that there is only one GroupShare matching the given parameters.
            }
        }

        return null;
    }

    /**
     * Remove the given group share object from the static list.
     *
     * @param groupShare    The GroupShare to remove. If this is null then do nothing.
     * @return              Return true if the given GroupShare was removed and false otherwise.
     */
    public static boolean safeRemoveGroupShare(@Nullable GroupShare groupShare) {
        return groupShares != null && groupShares.remove(groupShare);
    }

    /**
     * Return a list of removed group share objects for a given list contact and group number pairs.
     *
     * For each contact and group number pair, a corresponding group share object is returned iff it was found in the
     * stored (static) list and it was successfully removed from the list. Therefore the return list cannot be larger
     * than the list supplied to this method.
     *
     * @param contactAndGroupNumberPairs    The list of contact and group number pairs to find/remove group shares for.
     * @return                              The list if group share objects removed from the stored (static) list.
     */
    public static List<GroupShare> safeRemoveGroupShares(
            @Nullable List<ContactAndGroupNumberPair> contactAndGroupNumberPairs) {

        List<GroupShare> returnList = new ArrayList<>();

        if (contactAndGroupNumberPairs == null) {
            return returnList;
        }

        for (ContactAndGroupNumberPair cagnp : contactAndGroupNumberPairs) {
            if (cagnp == null) {
                continue;
            }

            final GroupShare groupShare = getGroupShare(cagnp.getContactNumber(), cagnp.getGroupNumber());
            if (safeRemoveGroupShare(groupShare)) {
                returnList.add(groupShare);
            }
        }

        return returnList;
    }

    /**
     * Compare two lists of GroupShare objects and append any objects in the new list which do not appear in the base
     * list, in descending share-time order.
     *
     * This method's implementation could be optimised further for the more general case where there could be
     * duplicates within each list as well as between lists, but the usage of this method is intended for the specific
     * case where the new list is an updated version of the base list.
     *
     * @param baseList  The list of GroupShare objects with which to compare the new list with.
     * @param newList   The list of GroupShare objects such that each object is defined to be duplicate or unique.
     */
    public static void appendNewGroupShares(@NonNull List<GroupShare> baseList, @NonNull List<GroupShare> newList) {
        List<GroupShare> uniqueGroupShares = new ArrayList<>();

        for (GroupShare newGroupShare : newList) {
            if (newGroupShare.getDuplicateGroupShare(baseList) == null) {
                uniqueGroupShares.add(newGroupShare);
            }
        }

        reverseSortByShareTime(uniqueGroupShares);

        baseList.addAll(uniqueGroupShares);
    }

    /**
     * @see #storeStatically(List)
     */
    public static GroupShare storeStatically(@NonNull GroupShare groupShare) {
        List<GroupShare> mutableGroupShareList = new ArrayList<>();
        mutableGroupShareList.add(groupShare);

        // Note that the Collections.singletonList() method could also be used as an argument instead of the mutable
        // list since even though the Collections.singletonList() method creates an immutable list, an
        // UnsupportedOperationException exception is only thrown when sorting the list.
        return storeStatically(mutableGroupShareList).get(0);
    }

    /**
     * Store a given list of GroupShare objects in the static list.
     * If the static list of GroupShare objects is null then create an empty list before adding to is.
     *
     * This method also sets the encrypted group key (string) for each GroupShare object to null.
     *
     * @param groupShareList    The list of GroupShare objects to store.
     * @return                  The list of GroupShare objects added/updated to/in the static list of all GroupShares.
     *                          Note that this list must be the same size as the input list, and cannot be larger than
     *                          the list of all GroupShares (at the point the GroupShares are returned).
     */
    public static List<GroupShare> storeStatically(@NonNull List<GroupShare> groupShareList) {
        List<GroupShare> returnList = new ArrayList<>();

        if (groupShares == null) {
            groupShares = new ArrayList<>();
        }

        for (GroupShare shareToStore : groupShareList) {
            final GroupShare duplicateGroupShare = shareToStore.getDuplicateGroupShare(groupShares);

            if (duplicateGroupShare == null) {
                groupShares.add(shareToStore);
                returnList.add(shareToStore);
            }
            else {
                duplicateGroupShare.updateGroupShareObject(shareToStore);
                returnList.add(duplicateGroupShare);
            }
        }

        for (GroupShare gs : groupShares) {
            gs.setEncryptedGroupKeyString(null);
        }

        return returnList;
    }
}
