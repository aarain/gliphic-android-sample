/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.operation;

import gliphic.android.exceptions.ContactException;
import gliphic.android.exceptions.ContactUniquenessException;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.operation.misc.LoadContactObjectAndContact;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import libraries.GeneralUtils;
import libraries.Vars;
import libraries.Vars.ContactType;
import pojo.load.LoadContactObject;

/**
 * Defines a 'Contact' object and contains related static methods.
 *
 * A contact is another user. When a user adds a contact, both users become contacts of each other.
 * Contacts are necessary to be able to share groups.
 */
public class Contact {
    /* Strings to display to the user in an AlertDialog. */

    public static final String NAME_EMPTY_MSG = "You must choose a contact name.";

    public static final String NAME_NOT_PRINTABLE = "Your chosen contact name cannot contain any illegal " +
            "characters. Legal characters are uppercase and lowercase English letters, the digits 0 to 9, the space " +
            "character and the following characters:\n" + GeneralUtils.VALID_PUNCTUATION_AND_SYMBOLS;

    public static final String NAME_CONTAINS_END_SPACE = "Your chosen contact name cannot start or end with a space.";

    public static final String NAME_CONTAINS_CONSECUTIVE_CHARS = "Your chosen contact name cannot contain " +
            "consecutive punctuation or symbol characters. These characters are the space character and the " +
            "following characters:\n" + GeneralUtils.VALID_PUNCTUATION_AND_SYMBOLS;

    public static final String NAME_CONTAINS_ADJACENT_MARKS = "Your chosen contact name cannot contain adjacent " +
            "quotation marks and apostrophes. More explicitly, the two following combinations of characters are " +
            "illegal:\n Quote and apostrophe: \"',\n Apostrophe and quote: '\".";

    public static final String NAME_LENGTH_INVALID = String.format(
            "Your chosen contact name must be between %d and %d characters (inclusive).",
            Vars.CONTACT_NAME_MIN_LEN,
            Vars.CONTACT_NAME_MAX_LEN
    );

    /**
     * The contact who is currently signed in.
     */
    private static Contact currentContact = null;

    /**
     * The list of all contacts known to the user.
     * Known contacts might not have any groups in common with the user.
     */
    private static List<Contact> knownContacts = null;


    /**
     * The list of all contacts who are members of at least one group known to the user,
     * but are not added as known contacts.
     */
    private static List<Contact> extendedContacts = null;

    /**
     * This Comparator is supplied to the Collections.sort() method to sort a given contacts list based on each
     * contact's number, from smallest to largest.
     */
    static Comparator<Contact> SORT_BY_NUMBER = (c1, c2) -> Long.compare(c1.number, c2.number);

    /**
     * Get the Contact object associated with the currently signed-in contact.
     *
     * If the current contact is null, either because it has not been set yet or the application has reset it to
     * null, an exception is thrown.
     *
     * @return                                  The Contact object representing the currently signed-in contact.
     * @throws NullStaticVariableException      Thrown when the current contact is null.
     */
    public static Contact getCurrentContact() throws NullStaticVariableException {
        if (currentContact == null) {
            throw new NullStaticVariableException("Current contact is null.");
        }

        return currentContact;
    }

    /**
     * Get the list of all target contacts (known and extended contacts) from the internal cache.
     *
     * If both known and extended contacts lists are null, either because they have not been set yet or the application
     * has reset them to null, an exception is thrown.
     *
     * @return                              The list of target contacts associated with this contact.
     * @throws NullStaticVariableException  Thrown when the known and extended contacts lists are null.
     */
    public static List<Contact> getTargetContacts() throws NullStaticVariableException {
        if (knownContacts == null && extendedContacts == null) {
            throw new NullStaticVariableException("Known and extended contacts lists are null.");
        }
        else if (extendedContacts == null) {
            return knownContacts;
        }
        else if (knownContacts == null) {
            return extendedContacts;
        }
        else {
            Set<Contact> tempSet = new LinkedHashSet<>(knownContacts);
            tempSet.addAll(extendedContacts);
            return new ArrayList<>(tempSet);
        }
    }

    /**
     * Return the number of target contacts (known and extended contacts) stored in the internal cache.
     *
     * @return  The number of known and extended contacts cached by the application.
     */
    public static int getTargetContactsSize() {
        try {
            return getTargetContacts().size();
        }
        catch (NullStaticVariableException e) {
            return 0;
        }
    }

    /**
     * Get the list of known contacts from the internal cache.
     *
     * If the known contacts list is null, either because it has not been set yet or the application has reset it to
     * null, an exception is thrown.
     *
     * @return                              The list of known contacts associated with this contact.
     * @throws NullStaticVariableException  Thrown when the known contacts list is null.
     */
    public static List<Contact> getKnownContacts() throws NullStaticVariableException {
        if (knownContacts == null) {
            throw new NullStaticVariableException("Known contacts list is null.");
        }

        return knownContacts;
    }

    /**
     * Get the list of extended contacts from the internal cache.
     *
     * If the extended contacts list is null, either because it has not been set yet or the application has reset it to
     * null, an exception is thrown.
     *
     * @return                              The list of extended contacts associated with this contact.
     * @throws NullStaticVariableException  Thrown when the extended contacts list is null.
     */
    public static List<Contact> getExtendedContacts() throws NullStaticVariableException {
        if (extendedContacts == null) {
            throw new NullStaticVariableException("Extended contacts list is null.");
        }

        return extendedContacts;
    }

    /**
     * Set the current contact to null.
     *
     * This is useful when removing specific contact data from the application.
     */
    public static void setNullCurrentContact() {
        currentContact = null;
    }

    /**
     * Set the known contacts list to null.
     *
     * This is useful when removing specific contact data from the application.
     */
    public static void setNullKnownContacts() {
        knownContacts = null;
    }

    /**
     * Set the extended contacts list to null.
     *
     * This is useful when removing specific contact data from the application.
     */
    public static void setNullExtendedContacts() {
        extendedContacts = null;
    }

    /**
     * Check if a given string matches the conditions imposed on all contact names.
     *
     * @param name                  The string to check if it is a valid contact name.
     * @throws ContactException     Thrown when the given string is not a valid contact name.
     */
    public static void checkValidContactName(@NonNull String name) throws ContactException {
        if (name.isEmpty()) {
            throw new ContactException(NAME_EMPTY_MSG);
        }
        else if (!GeneralUtils.isPrintableString(name)) {
            throw new ContactException(NAME_NOT_PRINTABLE);
        }
        else if (GeneralUtils.containsALeadingOrTrailingSpace(name)) {
            throw new ContactException(NAME_CONTAINS_END_SPACE);
        }
        else if (GeneralUtils.containsConsecutiveLegalCharacters(name)) {
            throw new ContactException(NAME_CONTAINS_CONSECUTIVE_CHARS);
        }
        else if (GeneralUtils.containsAnAdjacentQuotationMarkAndApostrophe(name)) {
            throw new ContactException(NAME_CONTAINS_ADJACENT_MARKS);
        }
        else if (name.length() < Vars.CONTACT_NAME_MIN_LEN || name.length() > Vars.CONTACT_NAME_MAX_LEN) {
            throw new ContactException(NAME_LENGTH_INVALID);
        }
    }

    /**
     * Checks if the given contact number is in the given contact list and returns the contact.
     *
     * @param contactNumber             The number of the contact to search for.
     * @param contactList               The list of contacts to check for the given contact number.
     * @return                          The contact associated with the given number.
     * @throws ContactException         Thrown when no contact in the given list has the given number.
     * @throws NullPointerException     Thrown when attempting to get the number of a null Contact.
     */
    public static Contact getContactFromNumber(long contactNumber, @NonNull List<Contact> contactList)
            throws ContactException, NullPointerException {

        for (Contact c : contactList) {
            if (contactNumber == c.getNumber()) {
                return c;
            }
        }

        String s = "The contact number %d is not in the given list of contacts.";
        throw new ContactException(String.format(s, contactNumber));
    }

    /**
     * Checks if a given contact number is stored statically and returns the contact.
     *
     * Note that this method does not interact with the server so the list of contacts to check against may not be
     * complete.
     *
     * @param contactNumber         The number of the contact to search for.
     * @return                      The Contact object associated with the given number.
     * @throws ContactException     Thrown when the desired contact is not unavailable.
     */
    public static Contact getContactFromNumber(long contactNumber) throws ContactException {
        if (currentContact != null && contactNumber == currentContact.getNumber()) {
            return currentContact;
        }

        if (knownContacts != null) {
            for (Contact c: knownContacts) {
                if (contactNumber == c.getNumber()) {
                    return c;
                }
            }
        }

        if (extendedContacts != null) {
            for (Contact c: extendedContacts) {
                if (contactNumber == c.getNumber()) {
                    return c;
                }
            }
        }

        String s = "No contact object with the number %d is available from storage.";
        throw new ContactException(String.format(s, contactNumber));
    }

    /**
     * Checks if a given contact ID is stored statically and returns the contact.
     *
     * Note that this method does not interact with the server so the list of contacts to check against may not be
     * complete.
     *
     * @param contactId             The ID of the contact to search for.
     * @return                      The Contact object associated with the given ID.
     * @throws ContactException     Thrown when the desired contact is not unavailable.
     */
    private static Contact getContactFromId(@NonNull String contactId) throws ContactException {
        if (currentContact != null && contactId.equals(currentContact.getId())) {
            return currentContact;
        }

        if (knownContacts != null) {
            for (Contact c: knownContacts) {
                if (contactId.equals(c.getId())) {
                    return c;
                }
            }
        }

        if (extendedContacts != null) {
            for (Contact c: extendedContacts) {
                if (contactId.equals(c.getId())) {
                    return c;
                }
            }
        }

        String s = "No contact object with the ID %s is available from storage.";
        throw new ContactException(String.format(s, contactId));
    }

    /**
     * Compare two lists of contacts and append any contacts in the new list which do not appear in the base list,
     * in ascending group number order.
     *
     * Note that contact equality is gauged only by comparing contact numbers.
     *
     * This method's implementation could be optimised further for the more general case where there could be
     * duplicates within each list as well as between lists, but the usage of this method is intended for the specific
     * case where the new list is an updated version of the base list.
     *
     * @param baseList  The list of contacts with which to compare the new list with.
     * @param newList   The list of contacts such that each contact is defined to be a duplicate or unique contact.
     */
    public static void appendNewContacts(@NonNull List<Contact> baseList, @NonNull List<Contact> newList) {
        List<Contact> uniqueContacts = new ArrayList<>();

        for (Contact newContact : newList) {
            boolean isUniqueContact = true;

            for (Contact baseContact : baseList) {
                if (baseContact.getNumber() == newContact.getNumber()) {
                    isUniqueContact = false;
                    break;
                }
            }

            if (isUniqueContact) {
                uniqueContacts.add(newContact);
            }
        }

        Collections.sort(uniqueContacts, SORT_BY_NUMBER);

        baseList.addAll(uniqueContacts);
    }

    /**
     * Call the {@link #storeStatically()} method on a list of Contact objects.
     *
     * If caught, a ContactException is thrown after all contacts have had {@link #storeStatically()} called on them.
     *
     * Note that the contacts in the returned list are in the same order that they were processed, therefore contacts
     * which are stored as new objects will have the same index in both input and output lists and contacts which are
     * overwritten will have the same index in the input list as the contact which overwrote it in the output list.
     *
     * @param contactList           The list of contacts to call {@link #storeStatically()} on.
     * @return                      The list of all contact(s) returned from calling {@link #storeStatically()}.
     * @throws ContactException     Thrown by {@link #storeStatically()} when failing to store at least one contact.
     */
    public static List<Contact> storeStatically(@NonNull List<Contact> contactList) throws ContactException {
        List<Contact> returnedContacts  = new ArrayList<>();
        List<String>  exceptionMessages = new ArrayList<>();

        for (Contact contact : contactList) {
            try {
                returnedContacts.add(contact.storeStatically());
            }
            catch (ContactException e) {
                exceptionMessages.add(e.getMessage());
            }
        }

        if (!exceptionMessages.isEmpty()) {
            String s = "The following exceptions prevented %d out of %d contact(s) from being stored statically: %s";
            String suffix = StringUtils.join(exceptionMessages, "; ");
            throw new ContactException(String.format(s, exceptionMessages.size(), contactList.size(), suffix));
        }

        return returnedContacts;
    }

    /**
     * Create and return a Contact object from a given LoadContactObject object.
     *
     * @param loadContactObject         The LoadContactObject object to create a Contact object from.
     * @return                          The Contact object.
     * @throws ContactException         Thrown when the Contact object cannot be created.
     * @throws NullPointerException     Thrown when the given LoadContactObject is null.
     */
    public static Contact createContact(@NonNull LoadContactObject loadContactObject)
            throws ContactException, NullPointerException {

        return new Contact(
                loadContactObject.getNumber(),
                loadContactObject.getId(),
                loadContactObject.getName(),
                Base64.decode(loadContactObject.getImageString()),
                loadContactObject.getType(),
                false
        );
    }

    /**
     * Create and return a list of Contact objects from a given list of LoadContactObject objects.
     *
     * Note that the returned list is not sorted by contact number.
     *
     * @param loadContactObjectList     The list of LoadContactObject objects to create Contact objects from.
     * @return                          A list of Contact objects.
     * @throws ContactException         Thrown when a Contact object cannot be created.
     * @throws NullPointerException     Thrown when either the given list is null or any object in the list is null.
     */
    public static List<Contact> createContacts(@NonNull List<LoadContactObject> loadContactObjectList)
            throws ContactException, NullPointerException {

        List<Contact> contacts = new ArrayList<>();

        for (LoadContactObject lco : loadContactObjectList) {
            contacts.add(createContact(lco));
        }

        return contacts;
    }

    /**
     * Create and return a list of LoadContactObjectAndContact objects from a given list of LoadContactObject objects.
     *
     * The returned contacts will also (attempt to be) stored statically, and for each contact will either be an
     * updated version of an existing contact (if a contact with the same number is already stored) or a new contact
     * object.
     *
     * If an exception is caught a ContactException is thrown after all items in the input list have been looped over.
     *
     * Note that the returned list is not sorted by contact number.
     *
     * @param loadContactObjectList     The list of LoadContactObject objects to create Contact objects from.
     * @return                          A list of LoadContactObjectAndContact objects.
     * @throws ContactException         Thrown when a Contact object cannot be created.
     */
    public static List<LoadContactObjectAndContact> createAndStoreContactsWithLoadContactObjectList(
            @NonNull List<LoadContactObject> loadContactObjectList) throws ContactException {

        List<LoadContactObjectAndContact> loadContactObjectAndContacts = new ArrayList<>();
        List<String> exceptionMessages = new ArrayList<>();

        for (LoadContactObject lco : loadContactObjectList) {
            Contact contact;
            try {
                contact = createContact(lco).storeStatically();
            }
            catch (ContactException | NullPointerException e) {
                exceptionMessages.add(e.getMessage());
                continue;
            }

            loadContactObjectAndContacts.add(new LoadContactObjectAndContact(lco, contact));
        }

        if (!exceptionMessages.isEmpty()) {
            String s = "The following exceptions prevented %d out of %d contact(s) from being created or stored: %s";
            String suffix = StringUtils.join(exceptionMessages, "; ");
            throw new ContactException(
                    String.format(s, exceptionMessages.size(), loadContactObjectList.size(), suffix)
            );
        }

        return loadContactObjectAndContacts;
    }

    private long number = -1;
    private String id = null;
    private String name = null;
    private ObjectImage image = null;
    private ContactType contactType;
    /*
     * common groups - A list of Group objects which indicates which groups this contact has in common with the user.
     */
    private List<Group> commonGroups = new ArrayList<>();

    /**
     * Checks input variables for validity and throws an exception if they are invalid.
     * Then the contact variables are set.
     * Then these set variables are checked against all known and extended contacts for duplicate values;
     * throws an exception if duplicates are found and exits successfully otherwise.
     *
     * @param number            Uniquely (globally) defines the the Contact.
     * @param id                The fixed-length identifier used internally to differentiate contacts.
     *                          Every contact in existence has a unique ID, which is guaranteed by the fact that every
     *                          contact is created by the server.
     * @param name              The user name of the contact, defined by and visible to the user. The only limitation
     *                          on group names upon group creation is that they use "printable" characters.
     * @param image             The displayed image associated with the contact, defined by and visible to the user.
     *                          This integer references an image resource.
     * @param contactType       The classification for this contact as defined by the Type enum.
     * @param requireUniqueness When set to true this constructor will throw an exception if the given contact number/
     *                          ID is the same as a contact number/ID for an existing contact.
     * @throws ContactException Thrown when input is either invalid for any contact or is not unique between all
     *                          known/extended contacts when uniqueness is required.
     */
    public Contact(long number,
                   @NonNull String id,
                   @NonNull String name,
                   @NonNull byte[] image,
                   @NonNull ContactType contactType,
                   boolean requireUniqueness) throws ContactException {

        setNumber(number);
        setId(id);
        setName(name);
        setImage(image);
        this.contactType = contactType;

        if (requireUniqueness) {
            verifyContactUniqueness(number, id);
        }
    }

    private void resetContact(@NonNull String id,
                              @NonNull String name,
                              @NonNull ObjectImage objectImage,
                              @NonNull ContactType contactType) throws ContactException {

        this.setId(id);
        this.setName(name);
        this.setImage(objectImage);
        this.changeType(contactType);
    }

    /**
     * Verify that a new contact to initialize does not contain a duplicate number or ID, and throw an exception if it
     * does.
     *
     * Note that this only checks the local caches for currently loaded contacts and not the server.
     *
     * Since all contact information should have initially been obtained from the server, if the first uniqueness check
     * passes (name/ID) then the subsequent uniqueness check should also pass (ID/name); if this does not happen then
     * the server is somehow supplying invalid contact name/ID data, which is assumed not to occur, hence both
     * uniqueness checks throw an instance of the same exception class.
     *
     * @param number                        The contact number to test uniqueness for.
     * @param id                            The contact ID to test uniqueness for.
     * @throws ContactUniquenessException   Thrown when the given contact number or ID is already present in an
     *                                      existing Contact object instance.
     */
    private void verifyContactUniqueness(long number, @NonNull String id) throws ContactUniquenessException {
        try {
            getContactFromNumber(number);
        }
        catch (ContactException e1) {
            try {
                getContactFromId(id);
            }
            catch (ContactException e2) {
                // Not finding the contact from the number and ID implies that the contact is unique.
                return;
            }

            throw new ContactUniquenessException(String.format("Duplicate contact ID detected: %s", id));
        }

        throw new ContactUniquenessException(String.format("Duplicate contact number detected: %d", number));
    }

    /**
     * Add this contact instance to the list of known or extended contacts, or as the current contact.
     *
     * If the contactType is CURRENT:
     * - Change the variable assigned to currentContact from null to this object.
     *
     * If the contactType is KNOWN/EXTENDED:
     * - If the static list of known/extended contacts is null then create an empty list before adding this contact
     *   instance to it.
     * - If a contact with the same number already exists in the known or extended contacts list it is replaced with
     *   this contact (in the same position in the list), otherwise the contact is appended to the appropriate list.
     *
     * If the contactType is UNKNOWN:
     * - Do nothing since unknown contacts are not stored statically for multiple activities.
     *
     * @return                      Either this Contact instance or the already existing instance with the same contact
     *                              number (which should be reset with the details of this contact).
     * @throws ContactException     Thrown when this contact is stored statically elsewhere (e.g. in the known/extended
     *                              contact lists) or when there is already an existing currentContact.
     */
    public Contact storeStatically() throws ContactException {
        if (contactType == ContactType.CURRENT) {
            if (currentContact == null) {
                // Ensure that the current contact is removed from both lists before evaluating the if-conditions.
                final boolean inKnownContacts    = safeRemoveKnownContact();
                final boolean inExtendedContacts = safeRemoveExtendedContact();

                if (inKnownContacts || inExtendedContacts) {
                    String s = "This contact should not be in either known or extended contact lists. " +
                            "This contact has now been removed from both lists.";
                    throw new ContactException(s);
                }

                currentContact = this;

            }
            else {
                if (currentContact.number != this.number) {
                    String s = "Cannot overwrite the existing current contact with number %d. " +
                            "Set the existing current contact to null before assigning a new current contact.";
                    throw new ContactException(String.format(s, currentContact.getNumber()));
                }

                // Assume that this object is an up-to-date version of the existing currentContact.
                currentContact.resetContact(id, name, image, contactType);
            }

            return currentContact;
        }
        else if (contactType == ContactType.KNOWN || contactType == ContactType.EXTENDED) {
            if (knownContacts == null) {
                knownContacts = new ArrayList<>();
            }

            if (extendedContacts == null) {
                extendedContacts = new ArrayList<>();
            }

            final Contact duplicateContact;
            try {
                duplicateContact = getContactFromNumber(number);
            }
            catch (ContactException e) {
                // There is no existing known or extended contact with the same number as this so add this new contact.
                if (contactType == ContactType.KNOWN) {
                    knownContacts.add(this);
                    Collections.sort(knownContacts, SORT_BY_NUMBER);
                }
                else {
                    extendedContacts.add(this);
                    Collections.sort(extendedContacts, SORT_BY_NUMBER);
                }

                return this;
            }

            // Reset the members for the duplicate contact instead of storing this contact object.
            duplicateContact.resetContact(id, name, image, contactType);

            return duplicateContact;
        }
        else {
            // Currently, unknown contacts are not stored statically.
            return this;
        }
    }

    /**
     * Change the contact's contactType member variable; a CURRENT contact's type cannot be changed.
     *
     * If the input type is the same as the existing type then this method returns immediately.
     *
     * If the contact is already stored statically (locally) it is moved to the relevant static variable, otherwise
     * only its type is changed.
     *
     * @param contactType           The (different) contact type to change the member variable to.
     * @throws ContactException     Thrown when changing a contact to or from a current contact.
     */
    public void changeType(ContactType contactType) throws ContactException {
        if (this.contactType == contactType) {
            return;
        }

        if (this.contactType == ContactType.CURRENT || contactType == ContactType.CURRENT) {
            throw new ContactException("Cannot change a contact's type to or from CURRENT.");
        }

        if (this.contactType == ContactType.KNOWN) {
            this.contactType = contactType;     // No exception should be thrown from this point.

            if (safeRemoveKnownContact() && contactType == ContactType.EXTENDED) {
                // Add this contact to the extendedContacts list.
                this.storeStatically();
            }

            changeTypeForGroupContacts(ContactType.EXTENDED);
        }
        else if (this.contactType == ContactType.EXTENDED) {
            this.contactType = contactType;     // No exception should be thrown from this point.

            if (safeRemoveExtendedContact() && contactType == ContactType.KNOWN) {
                // Add this contact to the knownContacts list.
                this.storeStatically();
            }

            changeTypeForGroupContacts(ContactType.KNOWN);
        }
        else if (this.contactType == ContactType.UNKNOWN) {
            this.contactType = contactType;
        }
    }

    private void changeTypeForGroupContacts(ContactType groupContactsAddListType) {
        if (groupContactsAddListType != ContactType.KNOWN && groupContactsAddListType != ContactType.EXTENDED) {
            throw new Error(new IOException("Contact type must be KNOWN or EXTENDED."));
        }

        // Always attempt to remove this contact as a group known/extended contact
        // (if groupContactsAddListType is EXTENDED/KNOWN).
        try {
            for (Group g : Group.getKnownGroups()) {
                if (g.removeGroupContact(this) && this.contactType == groupContactsAddListType) {
                    // Add this contact as a group extended contact.
                    g.addGroupContact(this);
                }
            }
        }
        catch (NullStaticVariableException e) {
            // Do nothing; if the list of known/extended groups is null this should be dealt with elsewhere, and if
            // both known and extended group contact lists are null then this contact is not contained within them.
        }
    }

    private void setNumber(long number) throws ContactException {
        checkValidContactNumber(number);
        this.number = number;
    }

    private void setId(String id) throws ContactException {
        checkValidContactId(id);
        this.id = id;
    }

    public void setName(String name) throws ContactException {
        checkValidContactName(name);
        this.name = name;
    }

    private void setImage(byte[] image) {
        this.image = new ObjectImage(image);
    }

    private void setImage(ObjectImage objectImage) {
        this.image = objectImage;
    }

    public void addCommonGroup(Group group) {
        if (!commonGroups.contains(group)) {
            commonGroups.add(group);

            Collections.sort(commonGroups, Group.SORT_BY_NUMBER);
        }
    }

    public long getNumber() {
        return number;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ObjectImage getImage() {
        return image;
    }

    public ContactType getType() {
        return contactType;
    }

    public boolean isKnownContact() {
        return contactType == ContactType.KNOWN;
    }

    public boolean isExtendedContact() {
        return contactType == ContactType.EXTENDED;
    }

    public List<Group> getCommonGroups() {
        return commonGroups;
    }

    private void checkValidContactNumber(long number) throws ContactException {
        if (number < 0) {
            String s = "Contact number '%d' is negative.";
            throw new ContactException(String.format(s, number));
        }
    }

    public static void checkValidContactId(@NonNull String id) throws ContactException {
        if (id.length() != Vars.CONTACT_ID_LEN) {
            String s = "The contact ID must have %d characters.";
            throw new ContactException(String.format(s, Vars.CONTACT_ID_LEN));
        }
        else if (!GeneralUtils.isStringBase64(id)) {
            throw new ContactException("The contact ID contains invalid character(s).");
        }
    }

    private boolean safeRemoveKnownContact() {
        return knownContacts != null && knownContacts.remove(this);
    }

    private boolean safeRemoveExtendedContact() {
        return extendedContacts != null && extendedContacts.remove(this);
    }
}
