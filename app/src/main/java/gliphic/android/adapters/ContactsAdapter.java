/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import gliphic.android.R;
import gliphic.android.operation.Contact;
import gliphic.android.operation.Group;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * The adapter for a list of Contacts.
 *
 * Facilitates creating a RecyclerView to list a user's Contacts.
 * The user's known contacts are displayed before the extended contacts in a single RecyclerView.
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactItemViewHolder> {
    public static final String GENERIC_LOAD_CONTACTS_FAILED_MSG = "Unable to load more contacts at this time.";

    public static final int MAX_NUM_OF_ITEMS_APPENDED = 50;

    private int maxNumOfItemsDisplayed = MAX_NUM_OF_ITEMS_APPENDED;

    // Display contacts for only this group, or set to null to avoid any restriction.
    private Group specifiedGroup;

    // All contacts available to this adapter before any filters are applied.
    private List<Contact> completeContactList  = new ArrayList<>();
    // The list of contacts available after all filters are applied.
    private List<Contact> filteredContactList  = new ArrayList<>();
    // The list of contacts to display after all filters are applied.
    // Note that this list should not be directly modified; use the setDisplayedContactList method to modify it.
    private List<Contact> displayedContactList = new ArrayList<>();

    // Filter(s) for this adapter.
    private String searchString = "";
    private boolean knownContactsIsChecked    = true;
    private boolean extendedContactsIsChecked = false;

    private boolean isNullOrContainsNulls(List<Contact> contactList) {
        if (contactList == null) {
            return true;
        }

        for (Contact contact : contactList) {
            if (contact == null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Constructor for initialising the adapter with contacts associated with any group.
     *
     * @param contactList                   The complete list of contacts for this contact. If this is null it is
     *                                      assumed that the app could not load any contacts from either the local
     *                                      cache or from the server.
     * @param applyTargetContactCheckBoxes  True iff the view containing this adapter instance makes use of
     *                                      known/extended contact CheckBoxes to filter the displayed contacts.
     */
    public ContactsAdapter(@Nullable List<Contact> contactList, boolean applyTargetContactCheckBoxes) {
        handleContactsAdapterConstructor(null, contactList, applyTargetContactCheckBoxes);
    }

    /**
     * Constructor for initialising the adapter with contacts associated with a specific group.
     *
     * @param group                         The non-null group to display contacts for.
     * @param contactList                   The list of contacts associated with the given group. If this is null it is
     *                                      assumed that the app could not load any contacts from either the local
     *                                      cache or from the server.
     * @param applyTargetContactCheckBoxes  True iff the view containing this adapter instance makes use of
     *                                      known/extended contact CheckBoxes to filter the displayed contacts.
     */
    public ContactsAdapter(@NonNull Group group,
                           @Nullable List<Contact> contactList,
                           boolean applyTargetContactCheckBoxes) {

        handleContactsAdapterConstructor(group, contactList, applyTargetContactCheckBoxes);
    }

    private void handleContactsAdapterConstructor(@Nullable Group group,
                                                  @Nullable List<Contact> contactList,
                                                  boolean applyTargetContactCheckBoxes) {

        // Assume that the calling method handles the else-case.
        if (!isNullOrContainsNulls(contactList)) {
            specifiedGroup = group;
            completeContactList.addAll(contactList);
            filteredContactList.addAll(filterDefault(contactList, applyTargetContactCheckBoxes));
            setDisplayedContactList(filteredContactList.subList(
                    0,
                    Math.min(filteredContactList.size(), maxNumOfItemsDisplayed)
            ));
        }
    }

    private void setDisplayedContactList(@NonNull List<Contact> newDisplayedContactList) {
        displayedContactList.clear();
        displayedContactList.addAll(newDisplayedContactList);
    }

    /**
     * Return the search string associated with the given adapter.
     *
     * @param contactsAdapter   An instance of this class (or null).
     * @return                  Null if the given adapter is null and a non-null string otherwise.
     */
    public static String getSearchString(@Nullable ContactsAdapter contactsAdapter) {
        if (contactsAdapter == null) {
            return null;
        }
        else {
            return contactsAdapter.searchString;
        }
    }

    static class ContactItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView contactImage;
        private TextView  contactName;
        private TextView  contactType;

        private ContactItemViewHolder(View view) {
            super(view);

            this.contactImage = view.findViewById(R.id.contact_item_img);
            this.contactName  = view.findViewById(R.id.contact_item_name);
            this.contactType  = view.findViewById(R.id.contact_item_type);
        }
    }

    @Override
    @NonNull
    public ContactItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactItemViewHolder contactItemViewHolder, int position) {
        Contact contactItem = getItemContact(position);

        contactItem.getImage().setImageView(contactItemViewHolder.contactImage);

        contactItemViewHolder.contactName.setText(contactItem.getName());

        final String displayedContactType;
        switch (contactItem.getType()) {
            case CURRENT:
                displayedContactType = "You";
                break;
            case KNOWN:
                displayedContactType = "Known contact";
                break;
            case EXTENDED:
                displayedContactType = "Extended contact";
                break;
            case UNKNOWN:
            default:
                displayedContactType = "Unknown contact";
                break;
        }

        contactItemViewHolder.contactType.setText(String.format("%s - %s", displayedContactType, contactItem.getId()));
    }

    @Override
    public int getItemCount() {
        return (displayedContactList != null ? displayedContactList.size() : 0);
    }

    /**
     * Get the total filtered number of contacts stored in this adapter.
     *
     * @return  Return the size of the filtered contacts list, if the list is null then return 0.
     */
    public int getFilteredItemCount() {
        return (filteredContactList != null ? filteredContactList.size() : 0);
    }

    /**
     * Get the total unfiltered number of contacts stored in this adapter.
     *
     * @return  Return the size of the complete contacts list, if the list is null then return 0.
     */
    public int getTotalItemCount() {
        return (completeContactList != null ? completeContactList.size() : 0);
    }

    /**
     * Return the number of filtered items associated with the given adapter.
     *
     * @param contactsAdapter   An instance of this class (or null).
     * @return                  Zero if the given adapter is null and the return value of
     *                          {@link #getFilteredItemCount()} otherwise.
     */
    public static int getFilteredItemCount(@Nullable ContactsAdapter contactsAdapter) {
        if (contactsAdapter == null) {
            return 0;
        }
        else {
            return contactsAdapter.getFilteredItemCount();
        }
    }

    /**
     * Return the contact at the given position in the displayed list of contacts.
     *
     * @param position  The zero-indexed position of the contact to return.
     * @return          The contact stored at the given position in the displayed contacts list.
     */
    public Contact getItemContact(int position) {
        return displayedContactList.get(position);
    }

    /**
     * Filter the list of contacts shown using the default values of the filters when an instance of this class is
     * created.
     *
     * @param contactListToFilter           The list of contacts to filter.
     * @param applyTargetContactCheckBoxes  True iff the view containing this adapter instance makes use of
     *                                      known/extended contact CheckBoxes to filter the displayed contacts.
     * @return                              A new list of filtered contacts.
     */
    private List<Contact> filterDefault(@NonNull List<Contact> contactListToFilter,
                                        boolean applyTargetContactCheckBoxes) {

        final List<Contact> partiallyFilteredContactList;

        if (applyTargetContactCheckBoxes) {
            partiallyFilteredContactList = filterCheckboxes(
                    contactListToFilter,
                    knownContactsIsChecked,
                    extendedContactsIsChecked
            );
        }
        else {
            partiallyFilteredContactList = new ArrayList<>(contactListToFilter);
        }

        return filterSearch(partiallyFilteredContactList, searchString);
    }

    /**
     * Filter the list of contacts shown based on the status of the CheckBoxes selected.
     *
     * The complete contact list is not updated in this method (see the update() method).
     *
     * @param contactListToFilter   The list of contacts to filter.
     * @param knownChecked          Set to true iff the known contacts CheckBox is selected.
     * @param extendedChecked       Set to true iff the extended contacts CheckBox is selected.
     * @return                      A new list of filtered contacts.
     */
    private List<Contact> filterCheckboxes(@NonNull List<Contact> contactListToFilter,
                                           boolean knownChecked,
                                           boolean extendedChecked) {

        knownContactsIsChecked    = knownChecked;
        extendedContactsIsChecked = extendedChecked;

        List<Contact> filteredContactList = new ArrayList<>();

        if (!knownChecked && !extendedChecked) {
            // No CheckBox filters are selected so do nothing.
        }
        else if (knownChecked && !extendedChecked) {
            for (Contact contact : contactListToFilter) {
                if (specifiedGroup == null) {
                    if (contact.isKnownContact()) {
                        filteredContactList.add(contact);
                    }
                }
                else {
                    if (specifiedGroup.containsGroupKnownContact(contact)) {
                        filteredContactList.add(contact);
                    }
                }
            }
        }
        else if (!knownChecked && extendedChecked) {
            for (Contact contact : contactListToFilter) {
                if (specifiedGroup == null) {
                    if (contact.isExtendedContact()) {
                        filteredContactList.add(contact);
                    }
                }
                else {
                    if (specifiedGroup.containsGroupExtendedContact(contact)) {
                        filteredContactList.add(contact);
                    }
                }
            }
        }
        else if (knownChecked && extendedChecked) {
            // Assume that all known contacts come before all extended contacts in this.completeContactList,
            // hence the list of all extended contacts can only contain a contact in this.completeContactList if there
            // are no more known contacts to add.
            for (Contact contact : contactListToFilter) {
                if (specifiedGroup == null) {
                    if (contact.isKnownContact()) {
                        filteredContactList.add(contact);
                    }
                    else if (contact.isExtendedContact()) {
                        filteredContactList.add(contact);
                    }
                }
                else {
                    if (specifiedGroup.containsGroupKnownContact(contact)) {
                        filteredContactList.add(contact);
                    }
                    else if (specifiedGroup.containsGroupExtendedContact(contact)) {
                        filteredContactList.add(contact);
                    }
                }
            }
        }

        return filteredContactList;
    }

    /**
     * Filter the list of contacts shown based on a given CharSequence provided by the user.
     *
     * It is assumed that the user input refers to the contact name which they are searching for.
     *
     * @param contactListToFilter   The list of contacts to filter.
     * @param charSequence          The user input to the EditText.
     * @return                      A new list of filtered contacts.
     */
    private List<Contact> filterSearch(@NonNull List<Contact> contactListToFilter, CharSequence charSequence) {
        searchString = charSequence.toString();
        List<Contact> filteredContactList = new ArrayList<>();

        // Set anchor points at the beginning of each space-separated string in the name, and ignore case.
        Pattern searchFilter = Pattern.compile("\\b" + Pattern.quote(searchString), Pattern.CASE_INSENSITIVE);

        for (Contact contact : contactListToFilter) {
            if (searchFilter.matcher(contact.getName()).find()) {
                filteredContactList.add(contact);
            }
        }

        return filteredContactList;
    }

    /**
     * Reset the maximum number of items on display.
     */
    private void resetMaxDisplaySize() {
        maxNumOfItemsDisplayed = MAX_NUM_OF_ITEMS_APPENDED;
    }

    /**
     * Clear the adapter by resetting the maximum number of items to display (the next time this adapter is used to
     * display items), resetting the adapter error state, resetting filters, clearing all lists and removing all items
     * bound to the view holder.
     *
     * Note that this is preferable to setting the adapter to null (in the calling method) since this adapter will
     * still maintain a view of which group to use (if any) to display contacts, allowing subsequent adapter-update
     * method calls; see the calling fragment's onResume and onStop method calls.
     */
    public void clear() {
        resetMaxDisplaySize();

        searchString = "";
        knownContactsIsChecked    = true;
        extendedContactsIsChecked = false;

        completeContactList.clear();
        filteredContactList.clear();
        displayedContactList.clear();

        notifyDataSetChanged();
    }

    /**
     * Reset the adapter to the same state as it was in when it was created (without creating a new instance of this
     * class) and update all lists.
     *
     * This reset method ignores the target contact CheckBox filters which may be stored in this adapter.
     *
     * @param completeContactList       The list of contacts to set as the complete list of contacts for this adapter.
     * @param resetAdapterLists         True if the complete list of contacts and the displayed list of contacts should
     *                                  be reset, false to reset neither of them.
     * @param resetDisplayedItemCount   True if the number of items displayed after the reset should be set to the
     *                                  default value, false if the display is to be appended to.
     */
    public void reset(@NonNull List<Contact> completeContactList,
                      boolean resetAdapterLists,
                      boolean resetDisplayedItemCount) {

        reset(
                completeContactList,
                resetAdapterLists,
                resetDisplayedItemCount,
                null,
                null
        );
    }

    /**
     * Reset the adapter to the same state as it was in when it was created (without creating a new instance of this
     * class) and update all lists.
     *
     * The checkbox values are required because not specifying these means that the application boolean values will be
     * ignored and the adapter defaults will be used (the same is not true for the search string, which correctly uses
     * the stored string value).
     *
     * @param completeContactList           The list of contacts to set as the complete list of contacts for this
     *                                      adapter. If this list cannot be obtained either from the local cache or
     *                                      from the server this list is expected to be null.
     * @param resetAdapterLists             True if the complete list of contacts and the displayed list of contacts
     *                                      should be reset, false to reset neither of them.
     * @param resetDisplayedItemCount       True if the number of items displayed after the reset should be set to the
     *                                      default value, false if the display is to be appended to.
     * @param knownContactsIsChecked        True if the known contacts CheckBox is selected, false otherwise.
     * @param extendedContactsIsChecked     True if the extended contacts CheckBox is selected, false otherwise.
     */
    public void reset(@Nullable List<Contact> completeContactList,
                      boolean resetAdapterLists,
                      boolean resetDisplayedItemCount,
                      boolean knownContactsIsChecked,
                      boolean extendedContactsIsChecked) {

        reset(
                completeContactList,
                resetAdapterLists,
                resetDisplayedItemCount,
                Boolean.valueOf(knownContactsIsChecked),
                Boolean.valueOf(extendedContactsIsChecked)
        );
    }

    private void reset(List<Contact> newCompleteContactList,
                       boolean resetAdapterLists,
                       boolean resetDisplayedItemCount,
                       Boolean newKnownContactsIsChecked,
                       Boolean newExtendedContactsIsChecked) {

        // Update this adapter's list of all contacts.

        if (isNullOrContainsNulls(newCompleteContactList)) {
            // Assume that the calling method handles this case.
            return;
        }

        // Either reset the entire data set or append to the existing display.
        if (resetAdapterLists) {
            completeContactList.clear();
            completeContactList.addAll(newCompleteContactList);
        }
        else {
            // Ensure that contacts are sorted with the already displayed contacts at the beginning of the list and the
            // new contacts to display at the end of the list. This is required for the notifyItemRangeInserted method
            // called in called in the append method.
            Contact.appendNewContacts(completeContactList, newCompleteContactList);
        }

        // Only apply filters to the complete list of contacts if they are all not null.
        if (newKnownContactsIsChecked != null && newExtendedContactsIsChecked != null) {
            knownContactsIsChecked    = newKnownContactsIsChecked;
            extendedContactsIsChecked = newExtendedContactsIsChecked;

            List<Contact> partiallyFilteredContactList = filterCheckboxes(
                    completeContactList,
                    newKnownContactsIsChecked,
                    newExtendedContactsIsChecked
            );
            filteredContactList = filterSearch(partiallyFilteredContactList, searchString);
        }
        else {
            filteredContactList.clear();
            filteredContactList.addAll(filterDefault(completeContactList, false));
        }

        // Either reset the displayed contacts or append to the existing display.
        if (resetDisplayedItemCount) {
            resetMaxDisplaySize();

            setDisplayedContactList(filteredContactList.subList(
                    0,
                    Math.min(filteredContactList.size(), maxNumOfItemsDisplayed)
            ));

            notifyDataSetChanged();
        }
        else {
            append(0);
        }
    }

    /**
     * Update a RecyclerView to show an up-to-date list of contacts, discarding the old list.
     *
     * The stored filterCheckboxes will also be applied to the list, and a maximum of maxNumOfItemsDisplayed will be
     * displayed.
     *
     * @param charSequence  The (new) CharSequence to filter contacts with.
     * @return              Return true if a server request is necessary after this method terminates, false otherwise.
     */
    public boolean update(CharSequence charSequence) {
        List<Contact> partiallyFilteredContactList = filterCheckboxes(
                completeContactList,
                knownContactsIsChecked,
                extendedContactsIsChecked
        );

        filteredContactList = filterSearch(partiallyFilteredContactList, charSequence);

        return updateDisplay();
    }

    /**
     * Update a RecyclerView to show an up-to-date list of contacts, discarding the old list.
     *
     * The stored filterSearch will also be applied to the list, and a maximum of maxNumOfItemsDisplayed will be
     * displayed.
     *
     * @param knownChecked      The (new) boolean status of the known contacts CheckBox to filter contacts with.
     * @param extendedChecked   The (new) boolean status of the extended contacts CheckBox to filter contacts with.
     * @return                  Return true if a server request is necessary after this method terminates,
     *                          false otherwise.
     */
    public boolean update(boolean knownChecked, boolean extendedChecked) {
        List<Contact> partiallyFilteredContactList = filterCheckboxes(
                completeContactList,
                knownChecked,
                extendedChecked
        );
        filteredContactList = filterSearch(partiallyFilteredContactList, searchString);

        return updateDisplay();
    }

    private boolean updateDisplay() {
        // Assume that no device shows at least MAX_NUM_OF_ITEMS_APPENDED items on one screen.
        // Displaying fewer than this number implies that more contacts should be requested from the server.

        boolean serverRequestRequired = false;

        final int numOfDisplayableContacts = filteredContactList
                .subList(0, Math.min(filteredContactList.size(), maxNumOfItemsDisplayed))
                .size();

        if (numOfDisplayableContacts < MAX_NUM_OF_ITEMS_APPENDED) {
            serverRequestRequired = true;
        }

        // Always reset the maximum number of items which are initially displayed to allow the view to be smoothly
        // redrawn, regardless of if a server request is required after this method returns.

        resetMaxDisplaySize();

        setDisplayedContactList(filteredContactList.subList(
                0,
                Math.min(filteredContactList.size(), MAX_NUM_OF_ITEMS_APPENDED)
        ));

        notifyDataSetChanged();

        return serverRequestRequired;
    }

    /**
     * Append the maximum number of items possible to the RecyclerView display. If fewer than this maximum are
     * available to append then no items are appended and a server request should be made to acquire more.
     *
     * This method is typically called within a call to a RecyclerView's addOnScrollListener() method when a user has
     * scrolled near/to the bottom of the RecyclerView.
     *
     * @return  Returns true if a new server request should be made to get more contacts and false otherwise.
     */
    public boolean append() {
        // If MAX_NUM_OF_ITEMS_APPENDED (or more) items are available to append then they should be appended without
        // requiring a server request.
        return append(MAX_NUM_OF_ITEMS_APPENDED - 1);
    }

    /**
     * Append items to the RecyclerView display based on the given threshold.
     *
     * @param appendedItemCountThreshold    The number of items available to append must be larger than this number.
     *                                      If this is not positive, items will always be appended if they are loaded.
     * @return                              Returns true if a new server request should be made to get more alerts and
     *                                      false otherwise.
     *                                      If a server request has already been made, this return value can be safely
     *                                      ignored.
     */
    private boolean append(int appendedItemCountThreshold) {
        final int oldListSize = getItemCount();
        final int newListSize = Math.min(filteredContactList.size(), oldListSize + MAX_NUM_OF_ITEMS_APPENDED);
        final int appendedItemCount = newListSize - oldListSize;

        if (appendedItemCount > Math.max(0, appendedItemCountThreshold)) {
            maxNumOfItemsDisplayed += MAX_NUM_OF_ITEMS_APPENDED;

            setDisplayedContactList(filteredContactList.subList(0, newListSize));

            // This method is more efficient than notifyDataSetChanged() so use it when possible.
            notifyItemRangeInserted(oldListSize, appendedItemCount);

            return false;
        }
        else {
            // If the number of items available to append is not larger than the threshold, request more from the
            // server before updating the displayed list.
            return true;
        }
    }
}
