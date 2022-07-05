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
import gliphic.android.exceptions.GroupException;
import gliphic.android.exceptions.GroupKeyException;
import gliphic.android.exceptions.GroupUniquenessException;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.operation.misc.LoadGroupObjectAndGroup;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import libraries.Base256;
import libraries.GeneralUtils;
import libraries.GroupPermissions;
import libraries.Vars;
import pojo.load.LoadGroupObject;

/**
 * Defines a 'Group' object and contains related static methods.
 *
 * A Group is used to encrypt and decrypt messages using a symmetric key which only members of the
 * Group have access to, and can be shared with any number of people.
 */
public class Group {
    // Strings to display to the user in an AlertDialog.
    public static final String NAME_EMPTY_MSG = "You must choose a group name.";

    public static final String NAME_NOT_PRINTABLE = "A group name cannot contain any illegal " +
            "characters. Legal characters are uppercase and lowercase English letters, the digits 0 to 9, the space " +
            "character and the following characters:\n" + GeneralUtils.VALID_PUNCTUATION_AND_SYMBOLS;

    public static final String NAME_CONTAINS_END_SPACE = "A group name cannot start or end with a space.";

    public static final String NAME_CONTAINS_CONSECUTIVE_CHARS = "A group name cannot contain " +
            "consecutive punctuation or symbol characters. These characters are the space character and the " +
            "following characters:\n" + GeneralUtils.VALID_PUNCTUATION_AND_SYMBOLS;

    public static final String NAME_CONTAINS_ADJACENT_MARKS = "A group name cannot contain adjacent " +
            "quotation marks and apostrophes. More explicitly, the two following combinations of characters are " +
            "illegal:\n Quote and apostrophe: \"',\n Apostrophe and quote: '\".";

    public static final String NAME_LENGTH_INVALID = String.format(
            "A group name must be between %d and %d characters (inclusive).",
            Vars.GROUP_NAME_MIN_LEN,
            Vars.GROUP_NAME_MAX_LEN
    );

    public static final String DESCRIPTION_EMPTY_MSG = "You must choose a group description.";

    public static final String DESCRIPTION_NOT_PRINTABLE = "A group description cannot contain any illegal " +
            "characters. Legal characters are uppercase and lowercase English letters, the digits 0 to 9, the space " +
            "character and the following characters:\n" + GeneralUtils.VALID_PUNCTUATION_AND_SYMBOLS;

    public static final String DESCRIPTION_LENGTH_INVALID = String.format(
            "A group description must be between %d and %d characters (inclusive).",
            Vars.GROUP_DESCRIPTION_MIN_LEN,
            Vars.GROUP_DESCRIPTION_MAX_LEN
    );

    public static final String DESCRIPTION_NOT_UNIQUE = "All of your group descriptions must be unique.";
    public static final String          ID_NOT_UNIQUE = "All of your group IDs must be unique.";

    /**
     * The list of all groups known to the user.
     */
    private static List<Group> knownGroups = null;

    /**
     * The currently selected group, which is used to encrypt messages.
     */
    private static Group selectedGroup = null;

    /**
     * This Comparator is supplied to the Collections.sort() method to sort a given groups list based on each
     * group's number, from smallest to largest.
     */
    static Comparator<Group> SORT_BY_NUMBER = (g1, g2) -> Long.compare(g1.number, g2.number);

    /**
     * @see #getKnownGroups(boolean) with a 'false' boolean argument.
     */
    public static List<Group> getKnownGroups() throws NullStaticVariableException {
        return getKnownGroups(false);
    }

    /**
     * Get the list of groups stored by the application which are known to this contact.
     *
     * If the known groups list is null, either because it has not been set yet or the application has reset it to
     * null, an exception is thrown.
     *
     * @param ignoreDefaultGroup            True if the default group should not be returned, false if the group can be
     *                                      returned (there is no guarantee that the group will be returned).
     * @return                              The list of (all) groups available for this contact to use.
     * @throws NullStaticVariableException  Thrown when the known groups list is null.
     */
    public static List<Group> getKnownGroups(final boolean ignoreDefaultGroup) throws NullStaticVariableException {
        if (knownGroups == null) {
            throw new NullStaticVariableException("Known groups list is null.");
        }

        final List<Group> returnedGroups;
        if (ignoreDefaultGroup) {
            returnedGroups = new ArrayList<>();

            for (Group g : knownGroups) {
                if (g.getNumber() != Vars.DEFAULT_GROUP_NUMBER) {
                    returnedGroups.add(g);
                }
            }
        }
        else {
            returnedGroups = knownGroups;
        }

        return returnedGroups;
    }

    /**
     * Get the group stored by the application which is marked as selected.
     *
     * If the selected group is null, either because it has not been set yet or the application has reset it to null,
     * an exception is thrown.
     *
     * @return                              The (only) group instance which is selected.
     * @throws NullStaticVariableException  Thrown when the selected group is null.
     */
    public static Group getSelectedGroup() throws NullStaticVariableException {
        if (selectedGroup == null) {
            throw new NullStaticVariableException("Selected group is null.");
        }

        return selectedGroup;
    }

    /**
     * Set the known groups list to null.
     *
     * This is useful when removing specific contact data from the application.
     */
    public static void setNullKnownGroups() {
        knownGroups = null;
    }

    /**
     * Set the selected group to null.
     *
     * This is useful when removing specific contact data from the application.
     */
    public static void setNullSelectedGroup() {
        selectedGroup = null;
    }

    /**
     * Checks if the given group number is in the given group list and returns the group.
     *
     * @param groupNumber               The number of the group to search for.
     * @param groupList                 The list of groups to check for the given group number.
     * @return                          The group associated with the given number.
     * @throws GroupException           Thrown when no group in the given list has the given number.
     * @throws NullPointerException     Thrown when attempting to get the number of a null Group.
     */
    public static Group getGroupFromNumber(long groupNumber, @NonNull List<Group> groupList)
            throws GroupException, NullPointerException {

        for (Group g : groupList) {
            if (groupNumber == g.getNumber()) {
                return g;
            }
        }

        String s = "The group number %d is not in the given list of groups.";
        throw new GroupException(String.format(s, groupNumber));
    }

    /**
     * Checks if a given group number is stored statically and returns the group.
     *
     * Note that this method does not interact with the server so the list of groups to check against may not be
     * complete.
     *
     * @param groupNumber       The number of the group to search for.
     * @return                  The group object associated with the given number.
     * @throws GroupException   Thrown when the desired group is not unavailable.
     */
    public static Group getGroupFromNumber(long groupNumber) throws GroupException {
        if (knownGroups != null) {
            for (Group g : knownGroups) {
                if (groupNumber == g.getNumber()) {
                    return g;
                }
            }
        }

        String s = "No group object with the number %d is available from storage.";
        throw new GroupException(String.format(s, groupNumber));
    }

    /**
     * Checks if the given group ID is the known group ID for this user and returns the group.
     *
     * @param groupId   The string representing a potentially known group ID.
     * @return Group    The group associated with the given ID.
     */
    public static Group getGroupFromId(String groupId) throws NullStaticVariableException, GroupException {
        if (knownGroups == null) {
            throw new NullStaticVariableException("Known groups list is null.");
        }

        for (Group g : knownGroups) {
            if (groupId.equals(g.getId())) {
                return g;
            }
        }
        String s = "Given group ID is unknown to the user: %s";
        throw new GroupException(String.format(s, groupId));
    }

    /**
     * Check that the given name is a valid group name; if it is not an exception is thrown.
     *
     * @param name              The name to check.
     * @throws GroupException   Thrown when the given name is not a valid group name.
     */
    public static void checkValidName(String name) throws GroupException {
        if (name.isEmpty()) {
            throw new GroupException(NAME_EMPTY_MSG);
        }
        else if (!GeneralUtils.isPrintableString(name)) {
            throw new GroupException(NAME_NOT_PRINTABLE);
        }
        else if (GeneralUtils.containsALeadingOrTrailingSpace(name)) {
            throw new GroupException(NAME_CONTAINS_END_SPACE);
        }
        else if (GeneralUtils.containsConsecutiveLegalCharacters(name)) {
            throw new GroupException(NAME_CONTAINS_CONSECUTIVE_CHARS);
        }
        else if (GeneralUtils.containsAnAdjacentQuotationMarkAndApostrophe(name)) {
            throw new GroupException(NAME_CONTAINS_ADJACENT_MARKS);
        }
        else if (name.length() < Vars.GROUP_NAME_MIN_LEN || name.length() > Vars.GROUP_NAME_MAX_LEN) {
            throw new GroupException(NAME_LENGTH_INVALID);
        }
    }

    /**
     * Check if a given input is valid to be set as a group description.
     *
     * If the cached list of known groups is null then this method does not throw an exception, therefore this method
     * MUST be used in conjunction with a server request to check/set the group description (since there are also
     * server-side checks) and it is recommended to request retrieve all known groups before calling this method.
     * If this method is called within this class then calling this method in conjunction with a server request for
     * group information is also acceptable.
     *
     * @param description           The potential group description.
     * @param requireUniqueness     When set to true this method will throw an exception if the given description is
     *                              the same as in any existing group.
     * @throws GroupException       Thrown when the description fails any check.
     */
    public static void checkValidDescription(String description, boolean requireUniqueness) throws GroupException {
        checkValidDescription(description, requireUniqueness, null);
    }

    private static void checkValidDescription(String description,
                                             boolean requireUniqueness,
                                             Group groupToModify) throws GroupException {
        if (description.isEmpty()) {
            throw new GroupException(DESCRIPTION_EMPTY_MSG);
        }
        else if (!GeneralUtils.isPrintableString(description)) {
            throw new GroupException(DESCRIPTION_NOT_PRINTABLE);
        }
        else if ( description.length() < Vars.GROUP_DESCRIPTION_MIN_LEN ||
                  description.length() > Vars.GROUP_DESCRIPTION_MAX_LEN ) {
            throw new GroupException(DESCRIPTION_LENGTH_INVALID);
        }

        if (requireUniqueness) {
            List<Group> groupsToCheck = new ArrayList<>();
            // Note that the selected group should always be in the list of known groups.
            if (knownGroups != null) {
                groupsToCheck.addAll(knownGroups);  // It is assumed that all groups in knownGroups are non-null.
            }

            for (Group g : groupsToCheck) {
                // If groupToModify is non-null then ignore the current description for this group since it will have
                // its description modified.
                if (!g.equals(groupToModify) && g.getDescription().equals(description)) {
                    throw new GroupUniquenessException(DESCRIPTION_NOT_UNIQUE);
                }
            }
        }
    }

    private static void checkValidNumber(long number) throws GroupException {
        if (number < 0) {
            String s = "Group number is negative: %d";
            throw new GroupException(String.format(s, number));
        }
    }

    public static void checkValidId(String id) throws GroupException {
        checkValidId(id, false, null);
    }

    private static void checkValidId(String id, boolean requireUniqueness, Group groupToModify) throws GroupException {
        if (id.length() != Vars.GROUP_ID_LEN) {
            String s = "The group ID must have %d characters.";
            throw new GroupException(String.format(s, Vars.GROUP_ID_LEN));
        }
        if (!Base256.isValidString(id)) {
            String s = "The group ID contains invalid character(s).";
            throw new GroupException(String.format(s, id));
        }

        if (requireUniqueness) {
            List<Group> groupsToCheck = new ArrayList<>();
            // Note that the selected group should always be in the list of known groups.
            if (knownGroups != null) {
                groupsToCheck.addAll(knownGroups);  // It is assumed that all groups in knownGroups are non-null.
            }

            for (Group g : groupsToCheck) {
                // If groupToModify is non-null then ignore the current ID for this group since it will have
                // its ID modified.
                if (!g.equals(groupToModify) && g.getId().equals(id)) {
                    throw new GroupUniquenessException(ID_NOT_UNIQUE);
                }
            }
        }
    }

    private static void checkValidKey(byte[] key) throws GroupKeyException {
        if (key.length != Vars.AES_KEY_LEN) {
            String s = "Group key has invalid length: %d";
            throw new GroupKeyException(String.format(s, key.length));
        }
    }

    private static void checkValidPermissions(GroupPermissions groupPermissions) throws GroupException {
        if (groupPermissions == null) {
            throw new GroupException("Group permissions object cannot be null.");
        }
    }

    /**
     * Compare two lists of groups and append any groups in the new list which do not appear in the base list,
     * in ascending group number order.
     *
     * Note that group equality is gauged only by comparing group numbers.
     *
     * This method's implementation could be optimised further for the more general case where there could be
     * duplicates within each list as well as between lists, but the usage of this method is intended for the specific
     * case where the new list is an updated version of the base list.
     *
     * @param baseList  The list of groups with which to compare the new list with.
     * @param newList   The list of groups such that each group is defined to be a duplicate or unique group.
     */
    public static void appendNewGroups(@NonNull List<Group> baseList, @NonNull List<Group> newList) {
        List<Group> uniqueGroups = new ArrayList<>();

        for (Group newGroup : newList) {
            boolean isUniqueGroup = true;

            for (Group baseGroup : baseList) {
                if (baseGroup.getNumber() == newGroup.getNumber()) {
                    isUniqueGroup = false;
                    break;
                }
            }

            if (isUniqueGroup) {
                uniqueGroups.add(newGroup);
            }
        }

        Collections.sort(uniqueGroups, SORT_BY_NUMBER);

        baseList.addAll(uniqueGroups);
    }

    /**
     * Call the {@link #storeStatically()} method on a list of Group objects.
     *
     * If caught, a GroupException is thrown after all groups have had {@link #storeStatically()} called on them.
     *
     * Note that the groups in the returned list are in the same order that they were processed, therefore groups
     * which are stored as new objects will have the same index in both input and output lists and groups which are
     * overwritten will have the same index in the input list as the group which overwrote it in the output list.
     *
     * @param groupList         The list of contacts to call {@link #storeStatically()} on.
     * @return                  The list of all group(s) returned from calling {@link #storeStatically()}.
     * @throws GroupException   Thrown by {@link #storeStatically()} when failing to store at least one group.
     */
    public static List<Group> storeStatically(@NonNull List<Group> groupList) throws GroupException {
        List<Group>  returnedGroups    = new ArrayList<>();
        List<String> exceptionMessages = new ArrayList<>();

        for (Group group : groupList) {
            try {
                returnedGroups.add(group.storeStatically());
            }
            catch (GroupException e) {
                exceptionMessages.add(e.getMessage());
            }
        }

        if (!exceptionMessages.isEmpty()) {
            String s = "The following exceptions prevented %d out of %d group(s) from being stored statically: %s";
            String suffix = StringUtils.join(exceptionMessages, "; ");
            throw new GroupException(String.format(s, exceptionMessages.size(), groupList.size(), suffix));
        }

        return returnedGroups;
    }

    /**
     * Create and return a Group object from a given LoadGroupObject object.
     *
     * @param loadGroupObject           The LoadGroupObject object to create a Group object from.
     * @return                          The Group object.
     * @throws GroupException           Thrown when the Group object cannot be created.
     * @throws NullPointerException     Thrown when the given LoadGroupObject is null.
     */
    public static Group createGroup(@NonNull LoadGroupObject loadGroupObject)
            throws GroupException, NullPointerException {

        return new Group(
                loadGroupObject.getNumber(),
                Base64.decode(loadGroupObject.getImageString()),
                loadGroupObject.getName(),
                loadGroupObject.getDescription(),
                Base256.fromBase64(loadGroupObject.getIdBase64()),
                GroupPermissions.valueOf(loadGroupObject.getPermissions()),
                loadGroupObject.isOpen(),
                loadGroupObject.isSelected(),
                false
        );
    }

    /**
     * Create and return a list of Group objects from a given list of LoadGroupObject objects.
     *
     * Note that the returned list is not sorted by group number.
     *
     * @param loadGroupObjectList       The list of LoadGroupObject objects to create Group objects from.
     * @return                          A list of Group objects.
     * @throws GroupException           Thrown when a Group object cannot be created.
     * @throws NullPointerException     Thrown when either the given list is null or any object in the list is null.
     */
    public static List<Group> createGroups(@NonNull List<LoadGroupObject> loadGroupObjectList)
            throws GroupException, NullPointerException {

        List<Group> groups = new ArrayList<>();

        for (LoadGroupObject lgo : loadGroupObjectList) {
            groups.add(createGroup(lgo));
        }

        return groups;
    }

    /**
     * Create and return a list of LoadGroupObjectAndGroup objects from a given list of LoadGroupObject objects.
     *
     * The returned groups will also (attempt to be) stored statically, and for each group will either be an
     * updated version of an existing group (if a group with the same number is already stored) or a new group
     * object.
     *
     * If an exception is caught a GroupException is thrown after all items in the input list have been looped over.
     *
     * Note that the returned list is not sorted by group number.
     *
     * @param loadGroupObjectList       The list of LoadGroupObject objects to create Group objects from.
     * @return                          A list of LoadGroupObjectAndGroup objects.
     * @throws GroupException           Thrown when a Group object cannot be created.
     */
    public static List<LoadGroupObjectAndGroup> createAndStoreGroupsWithLoadGroupObjectList(
            @NonNull List<LoadGroupObject> loadGroupObjectList) throws GroupException {

        List<LoadGroupObjectAndGroup> loadGroupObjectAndGroups = new ArrayList<>();
        List<String> exceptionMessages = new ArrayList<>();

        for (LoadGroupObject lgo : loadGroupObjectList) {
            Group group;
            try {
                group = createGroup(lgo).storeStatically();
            }
            catch (GroupException | NullPointerException e) {
                exceptionMessages.add(e.getMessage());
                continue;
            }

            loadGroupObjectAndGroups.add(new LoadGroupObjectAndGroup(lgo, group));
        }

        if (!exceptionMessages.isEmpty()) {
            String s = "The following exceptions prevented %d out of %d group(s) from being created or stored: %s";
            String suffix = StringUtils.join(exceptionMessages, "; ");
            throw new GroupException(
                    String.format(s, exceptionMessages.size(), loadGroupObjectList.size(), suffix)
            );
        }

        return loadGroupObjectAndGroups;
    }

    private long number = -1;
    private ObjectImage image;
    private String name = null;
    private String description = null;
    private String id = null;
    private byte[] key = null;
    private GroupPermissions permissions = null;
    private boolean open = false;
    private boolean selected = false;
    /*
     * groupKnownContacts - A list of Contact objects indicating which contacts, which the user has added,
     * have access to use this group.
     *
     * groupExtendedContacts - A list of Contact objects indicating which contacts, which the user has not added,
     * have access to use this group.
     */
    private List<Contact> groupKnownContacts = new ArrayList<>();
    private List<Contact> groupExtendedContacts = new ArrayList<>();

    /**
     * Checks input variables for validity and throws an exception if they are invalid.
     * Then the group variables are set.
     * Then these set variables are checked against all known groups for duplicate values;
     * throws an exception if duplicates are found and exits successfully otherwise.
     *
     * Group variables not initialized in this constructor:
     *        key               The symmetric key bytes used to encrypt and decrypt plaintext messages.
     *                          Once a user has been given access to a group they gain access to use this key
     *                          regardless of their group permissions, and since this is the key plaintext it is
     *                          effectively available permanently (although, critically, it is never available to the
     *                          server).
     *
     * @param number            Uniquely defines the the group (globally).
     * @param image             The displayed image associated with the group, defined by and visible to the user.
     * @param name              The name of the group, defined by and visible to the user.
     *                          The only limitation on group names upon group creation is that they use "printable"
     *                          characters.
     * @param description       A verbose description of the group, defined by and visible to the user.
     *                          Descriptions must use printable characters and be unique between all groups a user has
     *                          access to.
     * @param id                The fixed-length identifier used internally to differentiate groups.
     *                          Every group in existence has a unique ID, which is guaranteed by the fact that every
     *                          group is created by the server.
     * @param permissions       A 2-character array informing the user whether the group is active or not and whether
     *                          they have the ability to decrypt messages encrypted under this group.
     * @param open              A boolean specifying whether or not this group is open to search and add.
     *                          For every user exactly one group must have this variable set to true.
     * @param selected          A boolean specifying whether or not a user has this group selected to perform
     *                          encryption/decryption operations. For every user exactly one group must have this
     *                          variable set to true.
     * @param requireUniqueness When set to true this constructor will throw an exception if the given group number,
     *                          description, ID, open or selected values are the same as in any existing group.
     * @throws GroupException   Thrown when input is either invalid for any group or is not unique between all known
     *                          groups when uniqueness is required.
     */
    public Group(long number,
                 @NonNull byte[] image,
                 @NonNull String name,
                 @NonNull String description,
                 @NonNull String id,
                 @NonNull GroupPermissions permissions,
                 boolean open,
                 boolean selected,
                 boolean requireUniqueness) throws GroupException {

        setNumber(number);
        setImage(image);
        setName(name);
        setDescription(description, requireUniqueness);
        setId(id, requireUniqueness);
        setPermissions(permissions);
        setOpen(open);
        setSelected(selected);

        if (requireUniqueness) {
            verifyGroupUniqueness();
        }
    }

    private void resetGroup(@NonNull ObjectImage objectImage,
                            @NonNull String name,
                            @NonNull String description,
                            @NonNull String id,
                            @NonNull GroupPermissions permissions,
                            boolean open,
                            boolean selected) throws GroupException {

        setImage(objectImage);
        setName(name);
        setDescription(description, true);
        setId(id, true);
        setPermissions(permissions);
        setOpen(open);
        setSelected(selected);
    }

    private void setNumber(long number) throws GroupException {
        checkValidNumber(number);
        this.number = number;
    }

    private void setImage(byte[] image) {
        this.image = new ObjectImage(image);
    }

    private void setImage(ObjectImage objectImage) {
        this.image = objectImage;
    }

    public void setName(String name) throws GroupException {
        checkValidName(name);
        this.name = name;
    }

    public void setDescription(String description, boolean requireUniqueness) throws GroupException {
        checkValidDescription(description, requireUniqueness, this);
        this.description = description;
    }

    private void setId(String id, boolean requireUniqueness) throws GroupException {
        checkValidId(id, requireUniqueness, this);
        this.id = id;
    }

    public void setNullKey() {
        this.key = null;
    }

    public void setKey(byte[] key) throws GroupKeyException {
        checkValidKey(key);
        this.key = key;
    }

    public void setPermissions(GroupPermissions groupPermissions) throws GroupException {
        checkValidPermissions(groupPermissions);
        this.permissions = groupPermissions;
    }

    private void setOpen(boolean open) {
        this.open = open;
    }

    private void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Remove a given group from all lists.
     *
     * This includes the list(s) of common groups for all available contacts and the list of all known groups.
     */
    public void removeGroup() {
        // If a NullStaticVariableException is caught then the related list cannot contain this group
        // (and thus does not need to be removed).

        try {
            for (Contact c : Contact.getTargetContacts()) {
                c.getCommonGroups().remove(this);
            }
        }
        catch (NullStaticVariableException e) { /* Do nothing.*/ }

        try {
            Contact.getCurrentContact().getCommonGroups().remove(this);
        }
        catch (NullStaticVariableException e) { /* Do nothing.*/ }

        try {
            getKnownGroups().remove(this);
        }
        catch (NullStaticVariableException e) { /* Do nothing.*/ }
    }

    public void addGroupContact(Contact contact) {
        if (contact.isKnownContact() && !groupKnownContacts.contains(contact)) {
            groupKnownContacts.add(contact);

            Collections.sort(groupKnownContacts, Contact.SORT_BY_NUMBER);
        }
        else if (contact.isExtendedContact() && !groupExtendedContacts.contains(contact)) {
            groupExtendedContacts.add(contact);

            Collections.sort(groupExtendedContacts, Contact.SORT_BY_NUMBER);
        }
        // Do nothing if the contact type is not known or extended.
    }

    public boolean removeGroupContact(Contact contact) {
        boolean groupContactRemoved = false;

        if (groupKnownContacts.remove(contact)) {
            groupContactRemoved = true;
        }

        if (groupExtendedContacts.remove(contact)) {
            groupContactRemoved = true;
        }

        return groupContactRemoved;
    }

    public Contact getGroupContactFromNumber(long contactNum) throws ContactException {
        for (Contact c: groupKnownContacts) {
            if (contactNum == c.getNumber()) {
                return c;
            }
        }

        for (Contact c: groupExtendedContacts) {
            if (contactNum == c.getNumber()) {
                return c;
            }
        }

        String s = "Given group contact number is unknown to this contact: %d";
        throw new ContactException(String.format(s, contactNum));
    }

    public long getNumber() {
        return number;
    }

    public ObjectImage getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    public byte[] getKey() {
        return key;
    }

    public GroupPermissions getPermissions() {
        return permissions;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isSelected() {
        return selected;
    }

    /**
     * Get the list of known and extended contacts associated with this group instance.
     *
     * @return  The list of known and extended contacts associated with this group instance.
     */
    public List<Contact> getGroupTargetContacts() {
        if (groupExtendedContacts.isEmpty()) {
            return groupKnownContacts;
        }
        else if (groupKnownContacts.isEmpty()) {
            return groupExtendedContacts;
        }
        else {
            Set<Contact> tempSet = new LinkedHashSet<>(groupKnownContacts);
            tempSet.addAll(groupExtendedContacts);
            return new ArrayList<>(tempSet);
        }
    }

    /**
     * Check if a given contact is contained within this group's known contacts list.
     *
     * @param contact   The contact to check.
     * @return          Return true if this group's known contacts list contains the given contact and false otherwise.
     */
    public boolean containsGroupKnownContact(Contact contact) {
        return groupKnownContacts.contains(contact);
    }

    /**
     * Check if a given contact is contained within this group's extended contacts list.
     *
     * @param contact   The contact to check.
     * @return          Return true if this group's extended contacts list contains the given contact and false
     *                  otherwise.
     */
    public boolean containsGroupExtendedContact(Contact contact) {
        return groupExtendedContacts.contains(contact);
    }

    /**
     * Add this group instance to the list of known groups.
     * If the static list of known groups is null then create an empty list before adding this group instance to it.
     *
     * @return                  Either this Group instance or the already existing instance with the same group number
     *                          (which should be reset with the details of this group).
     * @throws GroupException   Thrown when reassigning the members of a group with a duplicate number to this group.
     */
    public Group storeStatically() throws GroupException {
        if (knownGroups == null) {
            knownGroups = new ArrayList<>();
        }

        final Group duplicateGroup;
        try {
            duplicateGroup = getGroupFromNumber(number);
        }
        catch (GroupException e) {
            // There is no existing known group with the same number as this so add this new group.
            knownGroups.add(this);
            Collections.sort(knownGroups, SORT_BY_NUMBER);

            return this;
        }

        // Reset the members for the duplicate group instead of storing this group object.
        duplicateGroup.resetGroup(image, name, description, id, permissions, open, selected);

        return duplicateGroup;
    }

    /**
     * Remove this group instance from the list of known groups.
     */
    public void removeFromStaticStore() {
        if (knownGroups != null) {
            knownGroups.remove(this);
        }
    }

    /**
     * Mark this group instance as the currently selected group and deselect the previously selected group.
     * Also change the global static selectedGroup to reflect this, and ensure that this group is in the list of known
     * groups.
     *
     * @return                  The selected group, which may not be this group if a duplicate group already existed.
     * @throws GroupException   Thrown when ensuring that the selected group is stored statically.
     */
    public Group selectGroup() throws GroupException {
        try {
            getSelectedGroup().setSelected(false);
        }
        catch (NullStaticVariableException e) {
            // The existing selected group only needs to be accessed if it is not null, so do nothing here.
        }

        // Ensure that the selected group is always in the list of known groups.
        Group groupToSelect = this.storeStatically();

        groupToSelect.setSelected(true);
        selectedGroup = groupToSelect;
        return groupToSelect;
    }

    /**
     * Checks all known groups and this group for multiple groups with the same value for specific variables.
     * The only unique variables are the group description and ID, and only one open and selected group is allowed.
     *
     * Note: This method does NOT ensure that there is AT LEAST one open and selected group as this would break the
     * ability to add groups to an empty knownGroups list, i.e. when known groups are initialized during sign-in.
     *
     * If the cached list of known groups is null then this method does not throw an exception, therefore this method
     * MUST be used in conjunction with a server request for group information since there are also server-side checks.
     *
     * @throws GroupUniquenessException     Thrown when a duplicate attribute has been detected.
     */
    public void verifyGroupUniqueness() throws GroupUniquenessException {
        if (knownGroups == null) {
            return;
        }

        // A shallow copy of knownGroups is sufficient since no groups are modified in this method.
        List<Group> allGroups = new ArrayList<>(knownGroups);

        allGroups.add(this);

        // If this is true then calling this function is redundant.
        if (allGroups.size() < 2) { return; }

        for (int i = 1; i < allGroups.size(); i++) {
            for (int j = 0; j < i; j++) {
                if ( allGroups.get(i).getNumber() == allGroups.get(j).getNumber() ) {
                    String s = "Duplicate number: " + allGroups.get(i).getNumber();
                    throw new GroupUniquenessException(s);
                }
                if ( allGroups.get(i).getDescription().equals(allGroups.get(j).getDescription()) ) {
                    String s = "Duplicate description: " + allGroups.get(i).getDescription();
                    throw new GroupUniquenessException(s);
                }
                if ( allGroups.get(i).getId().equals(allGroups.get(j).getId()) ) {
                    String s = "Duplicate id: " + allGroups.get(i).getId();
                    throw new GroupUniquenessException(s);
                }
            }
        }

        boolean selectedGroupFound = false;
        for (int i = 0; i < allGroups.size(); i++) {
            if (allGroups.get(i).isSelected()) {
                if (selectedGroupFound) {
                    String s = "Duplicate selected: " + allGroups.get(i).isSelected();
                    throw new GroupUniquenessException(s);
                }
                else {
                    selectedGroupFound = true;
                }
            }
        }
    }
}
