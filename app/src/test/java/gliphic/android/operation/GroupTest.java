package gliphic.android.operation;

import gliphic.android.TestUtils;
import gliphic.android.exceptions.ContactException;
import gliphic.android.exceptions.GroupException;
import gliphic.android.exceptions.GroupUniquenessException;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.operation.misc.LoadGroupObjectAndGroup;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.DecoderException;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsNot;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import libraries.Base256;
import libraries.GeneralUtils;
import libraries.GroupPermissions;
import libraries.Vars;
import pojo.load.LoadGroupObject;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class GroupTest {
    private static final String defaultGroupId = Vars.DEFAULT_GROUP_ID;
    private final char invalidCharacter = ((char) 13);

    // Valid parameters.
    public static final long validNum = 0;
    public static final long validNum2 = 1;
    public static final String validName = "Group name 1";
    public static final String validDescription = "Valid test group.";
    public static final String validDescription2 = "Another valid test group.";
    public static final String validId = defaultGroupId;
    public static final String validId2 = defaultGroupId.substring(1) + "A";
    public static final GroupPermissions validPermissions = PublishedTextTest.defaultGroupPerms;
    public static final byte[] validImage  = Vars.DisplayPicture.ANIMAL_DOVE.get();
    public static final byte[] validImage2 = Vars.DisplayPicture.ANIMAL_HORSE.get();
    public static final String validImageString = Base64.toBase64String(validImage);

    // All invalid parameters to test.
    private final long invalidNum = -1;
    private final String invalidNameChar = validName.substring(0, validName.length() - 1) + invalidCharacter;
    private final String invalidDescriptionLength =
            new String(new char[Vars.GROUP_DESCRIPTION_MAX_LEN + 1]).replace("\0", "a");
    private final String invalidDescriptionChar = invalidNameChar;
    private final String invalidIdLength = new String(new char[2]).replace("\0", defaultGroupId);
    private final String invalidIdChar = validId.substring(0, validId.length() - 1) + invalidCharacter;

    public static String generateGroupId(long groupNumber) {
        return String.format("%dABCDEFGHIJKLMNOPQRSTUVWXYZ", groupNumber).substring(0, Vars.GROUP_ID_LEN);
    }

    public static Group createValidGroup() throws GroupException {
        Group group = new Group(
                validNum,
                validImage,
                validName,
                validDescription,
                validId,
                validPermissions,
                false,
                true,
                true
        );
        group.setKey(PublishedTextTest.defaultGroupKey);

        return group;
    }

    public static Group createValidGroup2() throws GroupException {
        Group group = new Group(
                validNum2,
                validImage2,
                validName,
                validDescription2,
                validId2,
                validPermissions,
                false,
                false,
                true
        );
        group.setKey(PublishedTextTest.defaultGroupKey);

        return group;
    }

    public static Group createValidGroup3() throws GroupException {
        Group group = new Group(
                31564542,
                Vars.DisplayPicture.OTHER_NATIONAL_PARK.get(),
                validName,
                "jdsafkljdlska jfkldsaj klfjsl",
                generateGroupId(31564542),
                validPermissions,
                false,
                false,
                true
        );
        group.setKey(PublishedTextTest.defaultGroupKey);

        return group;
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @AfterClass
    public static void clearStaticListsAfterClass() {
        TestUtils.clearStaticLists();
    }

    @Before
    public void clearStaticListsBefore() {
        TestUtils.clearStaticLists();
    }

    /* Group initialisation - setting group variables */

    @Test
    public void validGroupInitialisation() throws GroupException {
        createValidGroup();
    }

    @Ignore("This test fails with the message: " +
            "java.lang.RuntimeException: Method decodeByteArray in android.graphics.BitmapFactory not mocked.")
    @Test
    public void validGroupInitialisationWithBitmapImage() throws GroupException {
        new Group(
                validNum,
                new byte[5],
                validName,
                validDescription,
                validId,
                validPermissions,
                false,
                true,
                true
        );
    }

    @Test
    public void invalidNumberGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage("Group number is negative:");
        new Group(
                invalidNum,
                validImage,
                validName,
                validDescription,
                invalidIdChar,
                validPermissions,
                false,
                false,
                true
        );
    }

    @Test
    public void invalidNameEmptyGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage(Group.NAME_EMPTY_MSG);
        new Group(
                validNum,
                validImage,
                "",
                validDescription,
                validId,
                validPermissions,
                false,
                false,
                true
        );
    }

    @Test
    public void invalidNameNotPrintableGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage(Group.NAME_NOT_PRINTABLE);
        new Group(
                validNum,
                validImage,
                "not £ printable",
                validDescription,
                validId,
                validPermissions,
                false,
                false,
                true
        );
    }

    @Test
    public void invalidNameEndSpaceGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage(Group.NAME_CONTAINS_END_SPACE);
        new Group(
                validNum,
                validImage,
                " end spaces ",
                validDescription,
                validId,
                validPermissions,
                false,
                false,
                true
        );
    }

    @Test
    public void invalidNameConsecutiveCharsGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage(Group.NAME_CONTAINS_CONSECUTIVE_CHARS);
        new Group(
                validNum,
                validImage,
                "consecutive--chars",
                validDescription,
                validId,
                validPermissions,
                false,
                false,
                true
        );
    }

    @Test
    public void invalidNameAdjacentMarksGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage(Group.NAME_CONTAINS_ADJACENT_MARKS);
        new Group(
                validNum,
                validImage,
                "adjacent\"'chars",
                validDescription,
                validId,
                validPermissions,
                false,
                false,
                true
        );
    }

    @Test
    public void invalidNameInvalidLengthGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage(Group.NAME_LENGTH_INVALID);
        new Group(
                validNum,
                validImage,
                "a",
                validDescription,
                validId,
                validPermissions,
                false,
                false,
                true
        );
    }

    @Test
    public void invalidDescriptionEmptyGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage(Group.DESCRIPTION_EMPTY_MSG);
        new Group(
                validNum,
                validImage,
                validName,
                "",
                validId,
                validPermissions,
                false,
                false,
                true
        );
    }

    @Test
    public void invalidDescriptionCharGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage(Group.DESCRIPTION_NOT_PRINTABLE);
        new Group(
                validNum,
                validImage,
                validName,
                invalidDescriptionChar,
                validId,
                validPermissions,
                false,
                false,
                true
        );
    }

    @Test
    public void invalidDescriptionLengthGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage(Group.DESCRIPTION_LENGTH_INVALID);
        new Group(
                validNum,
                validImage,
                validName,
                invalidDescriptionLength,
                validId,
                validPermissions,
                false,
                false,
                true
        );
    }

    @Test
    public void invalidDescriptionDuplicateGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupUniquenessException.class);
        expectedEx.expectMessage(Group.DESCRIPTION_NOT_UNIQUE);
        createValidGroup().storeStatically();
        createValidGroup();
    }

    @Test
    public void invalidIdLengthGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage(String.format("The group ID must have %d characters.", Vars.GROUP_ID_LEN));
        new Group(
                validNum,
                validImage,
                validName,
                validDescription,
                invalidIdLength,
                validPermissions,
                false,
                false,
                true
        );
    }

    @Test
    public void invalidIdCharGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage("The group ID contains invalid character(s).");
        new Group(
                validNum,
                validImage,
                validName,
                validDescription,
                invalidIdChar,
                validPermissions,
                false,
                false,
                true
        );
    }

    @Test
    public void invalidPermissionsNullGroupInitialisation() throws GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage("Group permissions object cannot be null.");
        new Group(
                validNum,
                validImage,
                validName,
                validDescription,
                validId,
                null,
                false,
                false,
                true
        );
    }

    /* Get group contacts */

    @Test
    public void getGroupTargetContactsExtendedContactsListIsEmpty() throws ContactException, GroupException {

        Group group = createValidGroup();

        Contact contact = ContactTest.createValidKnownContact();
        group.addGroupContact(contact);

        assertThat(group.getGroupTargetContacts(), is(Collections.singletonList(contact)));
    }

    @Test
    public void getGroupTargetContactsKnownContactsListIsEmpty() throws ContactException, GroupException {

        Group group = createValidGroup();

        Contact contact = ContactTest.createValidExtendedContact();
        group.addGroupContact(contact);

        assertThat(group.getGroupTargetContacts(), is(Collections.singletonList(contact)));
    }

    @Test
    public void validGetGroupTargetContacts() throws ContactException, GroupException {
        Group group = createValidGroup();

        Contact contact1 = ContactTest.createValidKnownContact();
        group.addGroupContact(contact1);
        assertThat(group.getGroupTargetContacts(), is(Collections.singletonList(contact1)));

        Contact contact2 = ContactTest.createValidKnownContact2();
        group.addGroupContact(contact2);
        assertThat(group.getGroupTargetContacts(), is(Arrays.asList(contact1, contact2)));
    }

    @Test
    public void getGroupTargetContactsSizeEmptyList() throws GroupException {
        assertThat(createValidGroup().getGroupTargetContacts().size(), is(0));
    }

    @Test
    public void getGroupTargetContactsSizeNonEmptyList() throws ContactException, GroupException {
        Group group = createValidGroup();
        Contact contact = ContactTest.createValidKnownContact();

        group.addGroupContact(contact);
        assertThat(group.getGroupTargetContacts().size(), is(1));
    }

    /* Contains group contacts */

    @Test
    public void trueContainsGroupKnownContact() throws ContactException, GroupException {
        final Group group = createValidGroup();
        final Contact knownContact = ContactTest.createValidKnownContact();

        group.addGroupContact(knownContact);
        assertThat(group.containsGroupKnownContact(knownContact), is(true));
    }

    @Test
    public void trueContainsGroupExtendedContact() throws ContactException, GroupException {
        final Group group = createValidGroup();
        final Contact extendedContact = ContactTest.createValidExtendedContact();

        group.addGroupContact(extendedContact);
        assertThat(group.containsGroupExtendedContact(extendedContact), is(true));
    }

    @Test
    public void falseContainsGroupKnownContactNullList() throws ContactException, GroupException {
        final Group group = createValidGroup();
        final Contact knownContact = ContactTest.createValidKnownContact();

        assertThat(group.containsGroupKnownContact(knownContact), is(false));
    }

    @Test
    public void falseContainsGroupExtendedContactNullList() throws ContactException, GroupException {
        final Group group = createValidGroup();
        final Contact extendedContact = ContactTest.createValidExtendedContact();

        assertThat(group.containsGroupExtendedContact(extendedContact), is(false));
    }

    @Test
    public void falseContainsGroupKnownContactContainsAnotherContact() throws ContactException, GroupException {
        final Group group = createValidGroup();
        final Contact knownContact1 = ContactTest.createValidKnownContact();
        final Contact knownContact2 = ContactTest.createValidKnownContact2();

        group.addGroupContact(knownContact1);
        assertThat(group.containsGroupKnownContact(knownContact2), is(false));
    }

    @Test
    public void falseContainsGroupExtendedContactContainsAnotherContact() throws ContactException, GroupException {
        final Group group = createValidGroup();
        final Contact extendedContact1 = ContactTest.createValidExtendedContact();
        final Contact extendedContact2 = ContactTest.createValidExtendedContact2();

        group.addGroupContact(extendedContact1);
        assertThat(group.containsGroupExtendedContact(extendedContact2), is(false));
    }

    /* Locate a contact */

    @Test
    public void validGetGroupContactFromNumber() throws ContactException, GroupException {
        final Group group = createValidGroup();
        final Contact knownContact1 = ContactTest.createValidKnownContact();
        final Contact extendedContact2 = ContactTest.createValidExtendedContact2();

        // Assert that a known contact's name is found.
        group.addGroupContact(knownContact1);

        Contact existingContact = group.getGroupContactFromNumber(ContactTest.validNum1);
        assertThat(group.getGroupTargetContacts(), CoreMatchers.hasItem(existingContact));

        // Assert that an extended contact's name is found.
        group.addGroupContact(extendedContact2);

        existingContact = group.getGroupContactFromNumber(ContactTest.validNum4);
        assertThat(group.getGroupTargetContacts(), CoreMatchers.hasItem(existingContact));
    }

    @Test
    public void invalidGetGroupContactFromNumberUnknownContact() throws ContactException, GroupException {
        expectedEx.expect(ContactException.class);
        expectedEx.expectMessage("Given group contact number is unknown to this contact: ");

        final Group group = createValidGroup();
        final Contact knownContact = ContactTest.createValidKnownContact();
        group.addGroupContact(knownContact);

        group.getGroupContactFromNumber(ContactTest.unknownNum);
    }

    /* Remove a group */

    @Test
    public void removeGroupAllLists() throws Exception {
        Contact knownContact   = ContactTest.createValidKnownContact();
        Contact currentContact = ContactTest.createValidCurrentContact();
        Group group = createValidGroup();

        group.storeStatically();
        knownContact.storeStatically();
        currentContact.storeStatically();
        knownContact.addCommonGroup(group);
        currentContact.addCommonGroup(group);

        assertThat(Group.getKnownGroups(),           is(CoreMatchers.hasItem(group)));
        assertThat(knownContact.getCommonGroups(),   is(CoreMatchers.hasItem(group)));
        assertThat(currentContact.getCommonGroups(), is(CoreMatchers.hasItem(group)));

        group.removeGroup();

        assertThat(Group.getKnownGroups(),           is(not(CoreMatchers.hasItem(group))));
        assertThat(knownContact.getCommonGroups(),   is(not(CoreMatchers.hasItem(group))));
        assertThat(currentContact.getCommonGroups(), is(not(CoreMatchers.hasItem(group))));
    }

    @Test
    public void removeGroupNullCommonGroups() throws Exception {
        Contact knownContact   = ContactTest.createValidKnownContact();
        Contact currentContact = ContactTest.createValidCurrentContact();
        Group group = createValidGroup();

        group.storeStatically();
        knownContact.storeStatically();
        currentContact.storeStatically();

        assertThat(Group.getKnownGroups(),           is(CoreMatchers.hasItem(group)));
        assertThat(knownContact.getCommonGroups(),   is(not(CoreMatchers.hasItem(group))));
        assertThat(currentContact.getCommonGroups(), is(not(CoreMatchers.hasItem(group))));

        group.removeGroup();

        assertThat(Group.getKnownGroups(),           is(not(CoreMatchers.hasItem(group))));
        assertThat(knownContact.getCommonGroups(),   is(not(CoreMatchers.hasItem(group))));
        assertThat(currentContact.getCommonGroups(), is(not(CoreMatchers.hasItem(group))));
    }

    @Test
    public void removeGroupNullKnownGroups() throws Exception {
        Contact knownContact   = ContactTest.createValidKnownContact();
        Contact currentContact = ContactTest.createValidCurrentContact();
        Group group = createValidGroup();

        knownContact.storeStatically();
        currentContact.storeStatically();
        knownContact.addCommonGroup(group);
        currentContact.addCommonGroup(group);

        try {
            Group.getKnownGroups();
            fail();
        } catch (NullStaticVariableException e) { /* Exception expected.*/ }

        assertThat(knownContact.getCommonGroups(),   is(CoreMatchers.hasItem(group)));
        assertThat(currentContact.getCommonGroups(), is(CoreMatchers.hasItem(group)));

        group.removeGroup();

        try {
            Group.getKnownGroups();
            fail();
        } catch (NullStaticVariableException e) { /* Exception expected.*/ }

        assertThat(knownContact.getCommonGroups(),   is(not(CoreMatchers.hasItem(group))));
        assertThat(currentContact.getCommonGroups(), is(not(CoreMatchers.hasItem(group))));
    }

    /* Add a contact to a group */

    @Test
    public void validAddGroupKnownContact() throws GroupException, ContactException {
        Group group = createValidGroup();
        Contact knownContact1 = ContactTest.createValidKnownContact();
        Contact knownContact2 = ContactTest.createValidKnownContact2();

        knownContact1.storeStatically();

        assertThat(group.getGroupTargetContacts().size(), is(0));

        // Add contact to null list.
        group.addGroupContact(knownContact1);
        assertThat(group.containsGroupKnownContact(knownContact1), is(true));
        assertThat(group.getGroupTargetContacts().size(), is(1));

        // Add contact to non-null list.
        group.addGroupContact(knownContact2);
        assertThat(group.containsGroupKnownContact(knownContact2), is(true));
        assertThat(group.getGroupTargetContacts().size(), is(2));

        // Adding the same contact to the group does not modify the list of contacts.
        group.addGroupContact(knownContact2);
        assertThat(group.containsGroupKnownContact(knownContact2), is(true));
        assertThat(group.getGroupTargetContacts().size(), is(2));
    }

    @Test
    public void validAddGroupExtendedContact() throws GroupException, ContactException {
        Group group = createValidGroup();
        Contact extendedContact1 = ContactTest.createValidExtendedContact();
        Contact extendedContact2 = ContactTest.createValidExtendedContact2();

        extendedContact1.storeStatically();

        assertThat(group.getGroupTargetContacts().size(), is(0));

        // Add contact to null list.
        group.addGroupContact(extendedContact1);
        assertThat(group.containsGroupExtendedContact(extendedContact1), is(true));
        assertThat(group.getGroupTargetContacts().size(), is(1));

        // Add contact to non-null list.
        group.addGroupContact(extendedContact2);
        assertThat(group.containsGroupExtendedContact(extendedContact2), is(true));
        assertThat(group.getGroupTargetContacts().size(), is(2));

        // Adding the same contact to the group does not modify the list of contacts.
        group.addGroupContact(extendedContact2);
        assertThat(group.containsGroupExtendedContact(extendedContact2), is(true));
        assertThat(group.getGroupTargetContacts().size(), is(2));
    }

    /* Remove a contact from a group */

    @Test
    public void removeGroupKnownContact() throws GroupException, ContactException {
        // First add the contact.
        Group baseGroup = createValidGroup();
        Contact baseContact = ContactTest.createValidKnownContact();

        baseContact.storeStatically();
        baseGroup.addGroupContact(baseContact);

        // Now removeFromContactsLists the contact.
        assertThat(baseGroup.removeGroupContact(baseContact), is(true));

        assertThat(baseGroup.containsGroupKnownContact(baseContact), is(false));
    }

    @Test
    public void removeGroupExtendedContact() throws GroupException, ContactException {
        // First add the contact.
        Group baseGroup = createValidGroup();
        Contact baseContact = ContactTest.createValidExtendedContact();

        baseContact.storeStatically();
        baseGroup.addGroupContact(baseContact);

        // Now removeFromContactsLists the contact.
        assertThat(baseGroup.removeGroupContact(baseContact), is(true));

        assertThat(baseGroup.containsGroupExtendedContact(baseContact), is(false));
    }

    @Test
    public void removeGroupContactNullLists() throws GroupException, ContactException {
        // Do not add the contact anywhere.
        Group baseGroup = createValidGroup();
        Contact baseContact = ContactTest.createValidKnownContact();

        // Now removeFromContactsLists the contact and check that no error occurs.
        assertThat(baseGroup.removeGroupContact(baseContact), is(false));
    }

    /* Static add to known groups */

    @Test
    public void validStoreStaticallyList() throws Exception {
        final long    groupNumber1   = 5387;
        final byte[]  groupImage1    = Vars.DisplayPicture.ANIMAL_CAT.get();
        final String  groupName1     = "Group 5387";
        final String  groupDesc1     = "Group description for group 5387.";
        final String  groupId1       = GeneralUtils.generateString(Vars.GROUP_ID_LEN);
        final String  groupEncKey1   = Base64.toBase64String(PublishedTextTest.contact0group0EncryptedKey);
        final String  groupKeyIv1    = Base64.toBase64String(PublishedTextTest.groupKeyIv);
        final int     groupPerms1    = 0;
        final boolean groupOpen1     = false;
        final boolean groupSelected1 = false;

        final long    groupNumber2   = 657;
        final byte[]  groupImage2    = Vars.DisplayPicture.ANIMAL_DOG.get();
        final String  groupName2     = "Group 657";
        final String  groupDesc2     = "Group description for group 657.";
        final String  groupId2       = GeneralUtils.generateString(Vars.GROUP_ID_LEN);
        final String  groupEncKey2   = Base64.toBase64String(PublishedTextTest.contact0group0EncryptedKey);
        final String  groupKeyIv2    = Base64.toBase64String(PublishedTextTest.groupKeyIv);
        final int     groupPerms2    = 1;
        final boolean groupOpen2     = false;
        final boolean groupSelected2 = false;

        // Add groups to the known groups list, one of which has the same group number but different other values.
        Group originalGroup = new Group(
                groupNumber1,
                validImage,
                validName,
                validDescription2,
                validId2,
                validPermissions,
                false,
                false,
                true
        ).storeStatically();

        createValidGroup().storeStatically();

        // Replace one of the original groups and insert a new one.
        final LoadGroupObject loadGroupObject1 = new LoadGroupObject(
                groupNumber1,
                Base256.toBase64(groupId1),
                groupName1,
                Base64.toBase64String(groupImage1),
                groupDesc1,
                groupEncKey1,
                groupKeyIv1,
                groupPerms1,
                groupOpen1,
                groupSelected1
        );
        final LoadGroupObject loadGroupObject2 = new LoadGroupObject(
                groupNumber2,
                Base256.toBase64(groupId2),
                groupName2,
                Base64.toBase64String(groupImage2),
                groupDesc2,
                groupEncKey2,
                groupKeyIv2,
                groupPerms2,
                groupOpen2,
                groupSelected2
        );

        final List<Group> allKnownGroups = Group.createGroups(
                Arrays.asList(loadGroupObject1, loadGroupObject2)
        );
        List<Group> returnedGroups = Group.storeStatically(allKnownGroups);

        // Check that the group created from loadGroupObject1 does not override the object reference of originalGroup.
        assertThat(returnedGroups, CoreMatchers.hasItem(originalGroup));

        assertThat(Group.getKnownGroups().size(),   is(3));
        assertThat(allKnownGroups.size(),           is(2));
        assertThat(returnedGroups.size(),           is(2));

        // Assert that the returned list of groups have the correct parameters.
        final Group lgdoGroup1 = allKnownGroups.get(0);
        final Group lgdoGroup2 = allKnownGroups.get(1);
        assertThat(lgdoGroup1.getNumber(),                  is(groupNumber1));
        assertThat(lgdoGroup1.getId(),                      is(groupId1));
        assertThat(lgdoGroup1.getName(),                    is(groupName1));
        assertThat(lgdoGroup1.getImage().getResourceInt(),  is(new ObjectImage(groupImage1).getResourceInt()));
        assertThat(lgdoGroup1.getDescription(),             is(groupDesc1));
        assertThat(lgdoGroup1.getKey(),                     is(nullValue()));
        assertThat(lgdoGroup1.getPermissions(),             is(GroupPermissions.valueOf(groupPerms1)));
        assertThat(lgdoGroup1.isOpen(),                     is(groupOpen1));
        assertThat(lgdoGroup1.isSelected(),                 is(groupSelected1));
        assertThat(lgdoGroup2.getNumber(),                  is(groupNumber2));
        assertThat(lgdoGroup2.getId(),                      is(groupId2));
        assertThat(lgdoGroup2.getName(),                    is(groupName2));
        assertThat(lgdoGroup2.getImage().getResourceInt(),  is(new ObjectImage(groupImage2).getResourceInt()));
        assertThat(lgdoGroup2.getDescription(),             is(groupDesc2));
        assertThat(lgdoGroup2.getKey(),                     is(nullValue()));
        assertThat(lgdoGroup2.getPermissions(),             is(GroupPermissions.valueOf(groupPerms2)));
        assertThat(lgdoGroup2.isOpen(),                     is(groupOpen2));
        assertThat(lgdoGroup2.isSelected(),                 is(groupSelected2));
    }

    @Test
    public void validAddToKnownGroupsEncryptedKeyAndIvStringsAreIgnored() throws Exception {
        final LoadGroupObject loadGroupObject = new LoadGroupObject(
                0,
                Base256.toBase64(validId),
                validName,
                validImageString,
                validDescription,
                "blblbllblblbllblblblblblblbl",
                "SoBayed",
                0,
                false,
                false
        );

        Group.createGroup(loadGroupObject).storeStatically();
        assertThat(Group.getKnownGroups().size(), is(1));
    }

    // There is no need to test both Group.storeStatically methods for invalid cases since they both call the same
    // handleStoreStatically method.

    @Test
    public void invalidAddToKnownGroupsBadNumber() throws Exception {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage("Group number is negative:");

        final LoadGroupObject loadGroupObject = new LoadGroupObject(
                -1,
                Base256.toBase64(validId),
                "",
                validImageString,
                "some description",
                null,
                null,
                0,
                false,
                true
        );

        Group.createGroup(loadGroupObject).storeStatically();
    }

    @Test
    public void invalidAddToKnownGroupsBadId() throws Exception {
        expectedEx.expect(GroupException.class);
        String s = "The group ID must have %d characters.";
        expectedEx.expectMessage(String.format(s, Vars.GROUP_ID_LEN));

        final LoadGroupObject loadGroupObject = new LoadGroupObject(
                0,
                Base256.toBase64(invalidIdLength),
                validName,
                validImageString,
                validDescription,
                null,
                null,
                0,
                false,
                true
        );

        Group.createGroup(loadGroupObject).storeStatically();
    }

    @Test
    public void invalidAddToKnownGroupsBadImageString() throws Exception {
        expectedEx.expect(DecoderException.class);

        final LoadGroupObject loadGroupObject = new LoadGroupObject(
                0,
                Base256.toBase64(validId),
                validName,
                "saf*(^",
                validDescription,
                null,
                null,
                0,
                false,
                true
        );

        Group.createGroup(loadGroupObject).storeStatically();
    }

    @Test
    public void invalidAddToKnownGroupsBadName() throws Exception {
        // Only one type of invalid name is tested here, the rest are tested elsewhere.
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage(Group.NAME_EMPTY_MSG);

        final LoadGroupObject loadGroupObject = new LoadGroupObject(
                0,
                Base256.toBase64(validId),
                "",
                validImageString,
                validDescription,
                null,
                null,
                0,
                false,
                true
        );

        Group.createGroup(loadGroupObject).storeStatically();
    }

    @Test
    public void invalidAddToKnownGroupsBadDescription() throws Exception {
        // Only one type of invalid description is tested here, the rest are tested elsewhere.
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage(Group.DESCRIPTION_EMPTY_MSG);

        final LoadGroupObject loadGroupObject = new LoadGroupObject(
                0,
                Base256.toBase64(validId),
                validName,
                validImageString,
                "",
                null,
                null,
                0,
                false,
                true
        );

        Group.createGroup(loadGroupObject).storeStatically();
    }

    @Test
    public void invalidAddToKnownGroupsBadPermissions() throws Exception {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage("Group permissions object cannot be null.");

        final LoadGroupObject loadGroupObject = new LoadGroupObject(
                0,
                Base256.toBase64(validId),
                validName,
                validImageString,
                validDescription,
                null,
                null,
                9999999,
                false,
                true
        );

        Group.createGroup(loadGroupObject).storeStatically();
    }

    /* Known groups */

    @Test
    public void validGetKnownGroups() throws GroupException, NullStaticVariableException {
        Group baseGroup = createValidGroup();

        baseGroup.storeStatically();

        assertThat(Group.getKnownGroups(),                      is(Collections.singletonList(baseGroup)));
        assertThat(Group.getKnownGroups(false), is(Collections.singletonList(baseGroup)));
        assertThat(Group.getKnownGroups(true),  is(Collections.emptyList()));
    }

    @Test
    public void nullGetKnownGroups() throws NullStaticVariableException {
        expectedEx.expect(NullStaticVariableException.class);
        expectedEx.expectMessage("Known groups list is null.");

        Group.getKnownGroups();
    }

    @Test
    public void nullGetKnownGroupsIgnoreDefaultGroup() throws NullStaticVariableException {
        expectedEx.expect(NullStaticVariableException.class);
        expectedEx.expectMessage("Known groups list is null.");

        Group.getKnownGroups(true);
    }

    @Test
    public void validStoreStaticallyOnMember() throws GroupException, NullStaticVariableException {
        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();

        assertThat(group1.storeStatically(), is(group1));   // Test adding a group to a null list.
        assertThat(group2.storeStatically(), is(group2));   // Test adding a group to an existing list.
        assertThat(Group.getKnownGroups(), is(Arrays.asList(group1, group2)));

        // Assert that no duplicates are added.
        group1.storeStatically();
        assertThat(Group.getKnownGroups().size(), is(2));

        // Assert that storing a new group with an existing group number returns the original group, and also
        // implicitly check that reassigning a description and ID for the same group number does not throw a
        // GroupUniquenessException.
        Group returnedGroup = new Group(
                group1.getNumber(),
                validImage,
                validName,
                group1.getDescription(),
                group1.getId(),
                validPermissions,
                false,
                false,
                false
        ).storeStatically();

        assertThat(Group.getKnownGroups().size(), is(2));
        assertThat(returnedGroup, is(group1));
    }

    @Test
    public void invalidStoreStaticallyOnMemberExistingNumberAndDuplicateDescription() throws Exception {
        expectedEx.expect(GroupUniquenessException.class);
        expectedEx.expectMessage("All of your group descriptions must be unique.");

        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();
        group1.storeStatically();
        group2.storeStatically();

        new Group(
                group1.getNumber(),
                validImage,
                validName,
                group2.getDescription(),
                validId,
                validPermissions,
                false,
                false,
                false
        ).storeStatically();
    }

    @Test
    public void invalidStoreStaticallyOnMemberExistingNumberAndDuplicateId() throws Exception {
        expectedEx.expect(GroupUniquenessException.class);
        expectedEx.expectMessage("All of your group IDs must be unique.");

        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();
        group1.storeStatically();
        group2.storeStatically();

        new Group(
                group1.getNumber(),
                validImage,
                validName,
                validDescription,
                group2.getId(),
                validPermissions,
                false,
                false,
                false
        ).storeStatically();
    }

    @Test
    public void validStoreStaticallyGroupList() throws GroupException, NullStaticVariableException {
        Group baseGroup = createValidGroup();
        Group baseGroup2 = createValidGroup2();

        List<Group> groupList = Arrays.asList(baseGroup, baseGroup2);
        Group.storeStatically(groupList);
        assertThat(Group.getKnownGroups(), is(groupList));

        // Assert that no duplicates are added.
        Group.storeStatically(groupList);
        assertThat(Group.getKnownGroups().size(), is(groupList.size()));
    }

    @Test
    public void invalidStoreStaticallyGroupList() throws GroupException {
        expectedEx.expect(GroupException.class);
        String s = "The following exceptions prevented %d out of %d group(s) from being stored statically: ";
        expectedEx.expectMessage(String.format(s, 2, 3));
        expectedEx.expectMessage(Group.DESCRIPTION_NOT_UNIQUE);
        expectedEx.expectMessage(Group.ID_NOT_UNIQUE);

        // Set up the scenario where two GroupExceptions will be thrown when calling storeStatically() on a group.

        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();
        group1.storeStatically();
        group2.storeStatically();

        Group invalidGroup1 = new Group(
                group1.getNumber(),
                validImage,
                group1.getName(),
                group2.getDescription(),
                group1.getId(),
                group1.getPermissions(),
                group1.isOpen(),
                group1.isSelected(),
                false
        );

        Group invalidGroup2 = new Group(
                group1.getNumber(),
                validImage,
                group1.getName(),
                group1.getDescription(),
                group2.getId(),
                group1.getPermissions(),
                group1.isOpen(),
                group1.isSelected(),
                false
        );

        // The thrown exception shows that the first two groups in the list throw an exception and the last does not.
        Group.storeStatically(Arrays.asList(invalidGroup1, invalidGroup2, createValidGroup3()));
    }

    /* Remove contact from static list(s) */

    @Test
    public void removeGroupFromKnownGroups() throws GroupException, NullStaticVariableException {
        Group group = createValidGroup();

        group.storeStatically();
        assertThat(Group.getKnownGroups().contains(group), is(true));

        // Test that the group is removed from the known groups list.
        group.removeFromStaticStore();
        assertThat(Group.getKnownGroups().contains(group), is(false));
        assertThat(Group.getKnownGroups().size(), is(0));

        // Test that no exception is thrown when removing a group from a list it is not in.
        group.removeFromStaticStore();
        assertThat(Group.getKnownGroups().contains(group), is(false));
        assertThat(Group.getKnownGroups().size(), is(0));
    }

    @Test
    public void removeFromNullKnownGroups() throws GroupException {
        Group group = createValidGroup();

        // Test that no exception is thrown when attempting to remove a group from a null known groups lists.
        group.removeFromStaticStore();
    }

    /* Group selection */

    @Test
    public void validGetSelectedGroup() throws GroupException, NullStaticVariableException {
        Group baseGroup = createValidGroup();
        // First set a non-null selected group.
        baseGroup.selectGroup();

        assertThat(Group.getSelectedGroup(), is(baseGroup));
    }

    @Test
    public void nullGetSelectedGroup() throws NullStaticVariableException {
        expectedEx.expect(NullStaticVariableException.class);
        expectedEx.expectMessage("Selected group is null.");

        Group.getSelectedGroup();
    }

    @Test
    public void validSetSelectedGroup() throws GroupException, NullStaticVariableException {
        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();

        // First set the selected group where the existing selected group is null.
        assertThat(group1.selectGroup(), is(group1));
        assertThat(group1.isSelected(), is(true));
        assertThat(Group.getKnownGroups(), CoreMatchers.hasItem(group1));

        // Now set the selected group where the existing selected group is non-null.
        assertThat(group2.selectGroup(), is(group2));
        assertThat(group1.isSelected(), is(false));
        assertThat(group2.isSelected(), is(true));
        assertThat(Group.getKnownGroups(), CoreMatchers.hasItem(group1));
        assertThat(Group.getKnownGroups(), CoreMatchers.hasItem(group2));

        // Check that re-selecting the same group changes nothing.
        assertThat(group2.selectGroup(), is(group2));
        assertThat(group1.isSelected(), is(false));
        assertThat(group2.isSelected(), is(true));
        assertThat(Group.getKnownGroups(), CoreMatchers.hasItem(group1));
        assertThat(Group.getKnownGroups(), CoreMatchers.hasItem(group2));

        // Check that a duplicate group (number) does not get selected, but instead selects the existing group.
        Group group2Duplicate = new Group(
                group2.getNumber(),
                group2.getImage().getImageBytes(),
                group2.getName(),
                group2.getDescription(),
                group2.getId(),
                group2.getPermissions(),
                false,
                false,
                false
        );

        assertThat(group2Duplicate.selectGroup(), is(group2));
        assertThat(group1.isSelected(), is(false));
        assertThat(group2.isSelected(), is(true));
        assertThat(group2Duplicate.isSelected(), is(false));
        assertThat(Group.getKnownGroups(), CoreMatchers.hasItem(group1));
        assertThat(Group.getKnownGroups(), CoreMatchers.hasItem(group2));
        assertThat(Group.getKnownGroups(), not(CoreMatchers.hasItem(group2Duplicate)));
    }

//    @Test
//    public void validSelectGroup() throws GroupException, NullStaticVariableException {
//        Group baseGroup = new Group(
//                validNum, validName, validDescription, validId, validKey, validPermissions,
//                false, true);
//        Group baseGroup2 = new Group(
//                validNum, validName, validDescription2, validId2, validKey, validPermissions,
//                false, false);
//
//        // Manually add groups and set the Selected_Group global.
//        Group.knownGroups.add(baseGroup);
//        Group.knownGroups.add(baseGroup2);
//        Group.setSelectedGroup(baseGroup);
//
//        baseGroup2.selectGroup();
//        assertThat(baseGroup.getSelected(), is(false));
//        assertThat(baseGroup2.getSelected(), is(true));
//        assertThat(Group.getSelectedGroup(), is(baseGroup2));
//    }
//
//    @Test
//    public void invalidSelectGroup() throws GroupException, NullStaticVariableException {
//        expectedEx.expect(GroupException.class);
//        expectedEx.expectMessage("Cannot select group which is unknown to the user; group ID:");
//
//        Group baseGroup = new Group(
//                validNum, validName, validDescription, validId, validKey, validPermissions,
//                false, true);
//        Group baseGroup2 = new Group(
//                validNum, validName, validDescription2, validId2, validKey, validPermissions,
//                false, false);
//
//        // Do not add the second group so that an exception is raised.
//        Group.knownGroups.add(baseGroup);
//        Group.setSelectedGroup(baseGroup);
//
//        baseGroup2.selectGroup();
//    }

    /* Get group from number */

    @Test
    public void checkValidCharsIsGroupAccessDenied() throws GroupException {
        assertThat(
                new Group(
                        validNum,
                        validImage,
                        validName,
                        validDescription,
                        validId,
                        GroupPermissions.INACTIVE_DENIED,
                        false,
                        false,
                        true
                ).getPermissions().isDenied(),
                is(true)
        );

        for (GroupPermissions permissions : Arrays.asList(
                GroupPermissions.INACTIVE_OWNER,
                GroupPermissions.INACTIVE_MEMBER,
                GroupPermissions.INACTIVE_DISTRIBUTOR,
                GroupPermissions.INACTIVE_REVOKER
            )) {

            assertThat(
                    new Group(
                            validNum,
                            validImage,
                            validName,
                            validDescription,
                            validId,
                            permissions,
                            false,
                            false,
                            true
                    ).getPermissions().isDenied(),
                    is(false)
            );
        }
    }

    @Test
    public void checkValidCharsIsGroupActive() throws GroupException {
        assertThat(
                new Group(
                        validNum,
                        validImage,
                        validName,
                        validDescription,
                        validId,
                        GroupPermissions.ACTIVE_DENIED,
                        false,
                        false,
                        true
                ).getPermissions().isActive(),
                is(true)
        );

        assertThat(
                new Group(
                        validNum,
                        validImage,
                        validName,
                        validDescription,
                        validId,
                        GroupPermissions.INACTIVE_DENIED,
                        false,
                        false,
                        true
                ).getPermissions().isActive(),
                is(false)
        );
    }

    /* Contact list uniqueness */

    @Test
    public void appendNewGroupsNewUniqueGroups() throws GroupException {
        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();
        Group group3 = createValidGroup3();

        List<Group> baseList = new ArrayList<>();
        List<Group> newList  = new ArrayList<>();

        baseList.add(group1);
        newList.add(group1);
        newList.add(group3);    // Test that this group is sorted at the end.
        newList.add(group2);

        Group.appendNewGroups(baseList, newList);

        assertThat(baseList, is(Arrays.asList(group1, group2, group3)));
    }

    @Test
    public void appendNewGroupsNoNewUniqueGroups() throws GroupException {
        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();

        List<Group> baseList = new ArrayList<>();
        List<Group> newList  = new ArrayList<>();

        baseList.add(group1);
        baseList.add(group2);
        newList.add(group1);
        newList.add(group2);

        Group.appendNewGroups(baseList, newList);

        assertThat(baseList, is(newList));
    }

    @Test
    public void appendNewGroupsBaseUniqueGroups() throws GroupException {
        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();
        Group group3 = createValidGroup3();

        List<Group> baseList  = new ArrayList<>();
        List<Group> newList = new ArrayList<>();

        baseList.add(group1);
        baseList.add(group3);   // The order of items in the base list is preserved.
        baseList.add(group2);
        newList.add(group1);
        newList.add(group2);

        Group.appendNewGroups(baseList, newList);

        assertThat(baseList, is(Arrays.asList(group1, group3, group2)));
    }

    @Test
    public void appendNewGroupsEmptyBaseGroupsList() throws GroupException {
        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();

        List<Group> baseList  = new ArrayList<>();
        List<Group> newList = new ArrayList<>();

        newList.add(group1);
        newList.add(group2);

        Group.appendNewGroups(baseList, newList);

        assertThat(baseList, is(newList));
    }

    @Test
    public void appendNewGroupsEmptyNewGroupsList() throws GroupException {
        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();

        List<Group> baseList  = new ArrayList<>();
        List<Group> newList = new ArrayList<>();

        baseList.add(group1);
        baseList.add(group2);

        Group.appendNewGroups(baseList, newList);

        assertThat(baseList, is(Arrays.asList(group1, group2)));
    }

    /* Get group from number */

    @Test
    public void groupNumberIsInList() throws GroupException {
        Group baseGroup = createValidGroup();
        List<Group> groupList = Collections.singletonList(baseGroup);

        Group existingGroup = Group.getGroupFromNumber(baseGroup.getNumber(), groupList);
        assertThat(groupList, CoreMatchers.hasItem(existingGroup));
    }

    @Test
    public void groupNumberIsNotInList() throws GroupException {
        long nonExistentGroupNumber = 1234567890987654321L;
        String s = "The group number %d is not in the given list of groups.";
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage(String.format(s, nonExistentGroupNumber));

        Group.getGroupFromNumber(nonExistentGroupNumber, Collections.emptyList());
    }

    @Test
    public void getGroupFromNumberNullInList() throws GroupException {
        expectedEx.expect(NullPointerException.class);

        Group.getGroupFromNumber(validNum, Arrays.asList(null, createValidGroup()));
    }

    @Test
    public void validGetKnownGroupFromNumber() throws Exception {
        Group baseGroup = createValidGroup();
        baseGroup.storeStatically();

        Group retrievedGroup = Group.getGroupFromNumber(baseGroup.getNumber());
        assertThat(Group.getKnownGroups(), CoreMatchers.hasItem(retrievedGroup));
    }

    @Test
    public void validGetSelectedGroupFromNumber() throws Exception {
        Group baseGroup = createValidGroup();
        baseGroup.selectGroup();

        Group retrievedGroup = Group.getGroupFromNumber(baseGroup.getNumber());
        assertThat(Group.getSelectedGroup(), is(retrievedGroup));
    }

    @Test
    public void invalidGetUnavailableGroupFromNumber() throws Exception {
        Group unavailableGroup = createValidGroup();

        expectedEx.expect(GroupException.class);
        String s = "No group object with the number %d is available from storage.";
        expectedEx.expectMessage(String.format(s, unavailableGroup.getNumber()));

        Group.getGroupFromNumber(unavailableGroup.getNumber());
    }

    /* Get group from ID */

    @Test
    public void groupIdIsKnown() throws NullStaticVariableException, GroupException {
        Group baseGroup = createValidGroup();
        baseGroup.storeStatically();

        Group existingGroup = Group.getGroupFromId(baseGroup.getId());
        assertThat(Group.getKnownGroups(), CoreMatchers.hasItem(existingGroup));
    }

    @Test
    public void groupIdIsNotKnown() throws NullStaticVariableException, GroupException {
        expectedEx.expect(GroupException.class);
        expectedEx.expectMessage("Given group ID is unknown to the user:");

        createValidGroup().storeStatically();

        Group.getGroupFromId("UnknownGroupId");
    }

    /* Set group description */

    @Test
    public void setGroupDescriptionSameAsExisting() throws GroupException {
        Group group1 = createValidGroup();
        group1.storeStatically();

        group1.setDescription(group1.getDescription(), true);
    }

    @Test
    public void setGroupDescriptionDuplicateInKnownGroups() throws GroupException {
        expectedEx.expect(GroupUniquenessException.class);
        expectedEx.expectMessage(Group.DESCRIPTION_NOT_UNIQUE);

        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();
        group1.storeStatically();
        group2.storeStatically();

        group2.setDescription(group1.getDescription(), true);
    }

    @Test
    public void setGroupDescriptionDuplicateIsSelectedGroup() throws GroupException {
        expectedEx.expect(GroupUniquenessException.class);
        expectedEx.expectMessage(Group.DESCRIPTION_NOT_UNIQUE);

        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();
        group1.selectGroup();

        group2.setDescription(group1.getDescription(), true);
    }

    /* Group uniqueness */

    @Test
    public void storedGroupsAreNotUnique() throws GroupException {
        expectedEx.expect(GroupUniquenessException.class);

        Group group = new Group(
                validNum,
                validImage,
                validName,
                validDescription,
                validId,
                validPermissions,
                false,
                false,
                false
        );

        group.storeStatically();
        group.storeStatically();
        group.verifyGroupUniqueness();
    }

    @Test
    public void uniqueGroups() throws GroupException {
        Group baseGroup = createValidGroup();
        Group baseGroup2 = createValidGroup2();

        baseGroup.storeStatically();
        baseGroup2.verifyGroupUniqueness();
    }

    @Test
    public void duplicateNumberInGroups() throws GroupException {
        expectedEx.expect(GroupUniquenessException.class);
        expectedEx.expectMessage("Duplicate number:");

        Group baseGroup = createValidGroup();
        Group baseGroup2 = new Group(
                validNum,
                validImage,
                validName,
                validDescription2,
                validId2,
                validPermissions,
                false,
                false,
                true
        );

        baseGroup.storeStatically();
        baseGroup2.verifyGroupUniqueness();
    }

    @Test
    public void duplicateDescriptionInGroups() throws GroupException {
        expectedEx.expect(GroupUniquenessException.class);
        expectedEx.expectMessage("Duplicate description:");

        Group baseGroup = createValidGroup();
        Group baseGroup2 = new Group(
                validNum2,
                validImage,
                validName,
                validDescription,
                validId2,
                validPermissions,
                false,
                false,
                true
        );

        baseGroup.storeStatically();
        baseGroup2.verifyGroupUniqueness();
    }

    @Test
    public void duplicateIdInGroups() throws GroupException {
        expectedEx.expect(GroupUniquenessException.class);
        expectedEx.expectMessage("Duplicate id:");

        Group baseGroup = createValidGroup();
        Group baseGroup2 = new Group(
                validNum2,
                validImage,
                validName,
                validDescription2,
                validId,
                validPermissions,
                false,
                false,
                true
        );

        baseGroup.storeStatically();
        baseGroup2.verifyGroupUniqueness();
    }

    @Test
    public void moreThanOneSelectedGroup() throws GroupException {
        expectedEx.expect(GroupUniquenessException.class);
        expectedEx.expectMessage("Duplicate selected:");

        Group baseGroup = new Group(
                validNum,
                validImage,
                validName,
                validDescription,
                validId,
                validPermissions,
                false,
                true,
                true
        );
        Group baseGroup2 = new Group(
                validNum2,
                validImage,
                validName,
                validDescription2,
                validId2,
                validPermissions,
                false,
                true,
                true
        );

        baseGroup.storeStatically();
        baseGroup2.verifyGroupUniqueness();
    }

    /* Adding contacts and group contacts sorts them automatically by number */

    @Test
    public void storeKnownGroupsSortsByNumber() throws GroupException, NullStaticVariableException {
        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();

        Group.storeStatically(Collections.singletonList(group2));
        Group.storeStatically(Collections.singletonList(group1));
        assertThat(Group.getKnownGroups().get(0), is(group1));
        assertThat(Group.getKnownGroups().get(1), is(group2));

        Group.setNullKnownGroups();

        Group.storeStatically(Arrays.asList(group2, group1));
        assertThat(Group.getKnownGroups().get(0), is(group1));
        assertThat(Group.getKnownGroups().get(1), is(group2));
    }

    @Test
    public void addToKnownGroupsOneGroupSortsByNumber() throws Exception {
        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();

        final String encryptedKey   = Base64.toBase64String(PublishedTextTest.contact0group0EncryptedKey);
        final String encryptedKeyIv = Base64.toBase64String(PublishedTextTest.groupKeyIv);
        final int    permissions    = 0;

        for (Group g : Arrays.asList(group2, group1)) {
            Group.createGroup(
                    new LoadGroupObject(
                            g.getNumber(),
                            Base256.toBase64(g.getId()),
                            g.getName(),
                            Base64.toBase64String(g.getImage().getImageBytes()),
                            g.getDescription(),
                            encryptedKey,
                            encryptedKeyIv,
                            permissions,
                            g.isOpen(),
                            g.isSelected()
                    )
            ).storeStatically();
        }

        // The Group objects themselves cannot be compared since the static Group.storeStatically() method accepts a
        // LoadGroupObject argument and not a Group argument.
        assertThat(Group.getKnownGroups().get(0).getNumber(), is(group1.getNumber()));
        assertThat(Group.getKnownGroups().get(1).getNumber(), is(group2.getNumber()));
    }

    @Test
    public void addToKnownGroupsMultipleGroupsSortsByNumber() throws Exception {
        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();

        final String encryptedKey   = Base64.toBase64String(PublishedTextTest.contact0group0EncryptedKey);
        final String encryptedKeyIv = Base64.toBase64String(PublishedTextTest.groupKeyIv);
        final int    permissions    = 0;

        final LoadGroupObject group1Object = new LoadGroupObject(
                group1.getNumber(),
                Base256.toBase64(group1.getId()),
                group1.getName(),
                Base64.toBase64String(group1.getImage().getImageBytes()),
                group1.getDescription(),
                encryptedKey,
                encryptedKeyIv,
                permissions,
                group1.isOpen(),
                group1.isSelected()
        );

        final LoadGroupObject group2Object = new LoadGroupObject(
                group2.getNumber(),
                Base256.toBase64(group2.getId()),
                group2.getName(),
                Base64.toBase64String(group2.getImage().getImageBytes()),
                group2.getDescription(),
                encryptedKey,
                encryptedKeyIv,
                permissions,
                group2.isOpen(),
                group2.isSelected()
        );

        List<Group> groups = Group.createGroups(Arrays.asList(group2Object, group1Object));
        Group.storeStatically(groups);
        assertThat(Group.getKnownGroups().get(0).getNumber(), is(group1.getNumber()));
        assertThat(Group.getKnownGroups().get(1).getNumber(), is(group2.getNumber()));
    }

    @Test
    public void addGroupKnownContactSortByNumber() throws ContactException, GroupException {
        Contact contact1 = ContactTest.createValidKnownContact();
        Contact contact2 = ContactTest.createValidKnownContact2();
        Group group = createValidGroup();

        group.addGroupContact(contact2);
        group.addGroupContact(contact1);

        assertThat(group.getGroupTargetContacts().get(0), is(contact1));
        assertThat(group.getGroupTargetContacts().get(1), is(contact2));
    }

    @Test
    public void addGroupExtendedContactSortByNumber() throws ContactException, GroupException {
        Contact contact1 = ContactTest.createValidExtendedContact();
        Contact contact2 = ContactTest.createValidExtendedContact2();
        Group group = createValidGroup();

        group.addGroupContact(contact2);
        group.addGroupContact(contact1);

        assertThat(group.getGroupTargetContacts().get(0), is(contact1));
        assertThat(group.getGroupTargetContacts().get(1), is(contact2));
    }

    @Test
    public void storeStaticallySortsByNumber() throws GroupException, NullStaticVariableException {
        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();

        group2.storeStatically();
        group1.storeStatically();

        assertThat(Group.getKnownGroups().get(0), is(group1));
        assertThat(Group.getKnownGroups().get(1), is(group2));
    }

    /* Create groups from LoadGroupObject  */

    @Test
    public void validCreateGroup() throws Exception {
        Group group1 = createValidGroup();

        final LoadGroupObject loadGroupObject1 = new LoadGroupObject(
                group1.getNumber(),
                Base256.toBase64(group1.getId()),
                group1.getName(),
                Base64.toBase64String(group1.getImage().getImageBytes()),
                group1.getDescription(),
                group1.getPermissions().get(),
                group1.isOpen(),
                group1.isSelected()
        );

        Group group = Group.createGroup(loadGroupObject1);

        assertThat(group.getNumber(),                   is(group1.getNumber()));
        assertThat(group.getName(),                     is(group1.getName()));
        assertThat(group.getId(),                       is(group1.getId()));
        assertThat(group.getImage().getResourceInt(),   is(group.getImage().getResourceInt()));
        assertThat(group.getDescription(),              is(group1.getDescription()));
        assertThat(group.getPermissions(),              is(group1.getPermissions()));
        assertThat(group.isOpen(),                      is(group1.isOpen()));
        assertThat(group.isSelected(),                  is(group1.isSelected()));
        assertThat(group.getKey(),                      is(nullValue()));
    }

    @Test
    public void validCreateGroups() throws Exception {
        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();

        final LoadGroupObject loadGroupObject1 = new LoadGroupObject(
                group1.getNumber(),
                Base256.toBase64(group1.getId()),
                group1.getName(),
                Base64.toBase64String(group1.getImage().getImageBytes()),
                group1.getDescription(),
                group1.getPermissions().get(),
                group1.isOpen(),
                group1.isSelected()
        );

        final LoadGroupObject loadGroupObject2 = new LoadGroupObject(
                group2.getNumber(),
                Base256.toBase64(group2.getId()),
                group2.getName(),
                Base64.toBase64String(group2.getImage().getImageBytes()),
                group2.getDescription(),
                group2.getPermissions().get(),
                group2.isOpen(),
                group2.isSelected()
        );

        List<Group> groups = Group.createGroups(
                Arrays.asList(loadGroupObject1, loadGroupObject2)
        );

        assertThat(groups.size(), is(2));
        assertThat(groups.get(0).getNumber(),                   is(group1.getNumber()));
        assertThat(groups.get(0).getName(),                     is(group1.getName()));
        assertThat(groups.get(0).getId(),                       is(group1.getId()));
        assertThat(groups.get(0).getImage().getResourceInt(),   is(group1.getImage().getResourceInt()));
        assertThat(groups.get(0).getDescription(),              is(group1.getDescription()));
        assertThat(groups.get(0).getPermissions(),              is(group1.getPermissions()));
        assertThat(groups.get(0).isOpen(),                      is(group1.isOpen()));
        assertThat(groups.get(0).isSelected(),                  is(group1.isSelected()));
        assertThat(groups.get(1).getNumber(),                   is(group2.getNumber()));
        assertThat(groups.get(1).getName(),                     is(group2.getName()));
        assertThat(groups.get(1).getId(),                       is(group2.getId()));
        assertThat(groups.get(1).getImage().getResourceInt(),   is(group2.getImage().getResourceInt()));
        assertThat(groups.get(1).getDescription(),              is(group2.getDescription()));
        assertThat(groups.get(1).getPermissions(),              is(group2.getPermissions()));
        assertThat(groups.get(1).isOpen(),                      is(group2.isOpen()));
        assertThat(groups.get(1).isSelected(),                  is(group2.isSelected()));
    }

    @Test
    public void validCreateGroupsWithLoadGroupObjectList() throws Exception {
        final String groupId1          = generateGroupId(349867);
        final String groupName1        = "dddddkdkdkdkaaa";
        final String groupDescription1 = "alf la la lalal alpladd";
        final byte[] groupImage1       = Vars.DisplayPicture.ANIMAL_CAT.get();
        final long   groupNumber2      = 61645454L;
        final String groupId2          = generateGroupId(groupNumber2);
        final String groupName2        = "namenanmddfdaa";
        final String groupDescription2 = "paapfjg kal l laa";
        final byte[] groupImage2       = Vars.DisplayPicture.ANIMAL_DOG.get();

        // Create and store group(s) to test that one of the returned groups is overwritten.
        Group group1 = createValidGroup();
        Group group2 = createValidGroup2();
        group1.storeStatically();

        final LoadGroupObject loadGroupObject1 = new LoadGroupObject(
                group1.getNumber(),
                Base256.toBase64(groupId1),
                groupName1,
                Base64.toBase64String(groupImage1),
                groupDescription1,
                group1.getPermissions().get(),
                group1.isOpen(),
                group1.isSelected()
        );

        final LoadGroupObject loadGroupObject2 = new LoadGroupObject(
                groupNumber2,
                Base256.toBase64(groupId2),
                groupName2,
                Base64.toBase64String(groupImage2),
                groupDescription2,
                group2.getPermissions().get(),
                group2.isOpen(),
                group2.isSelected()
        );

        List<LoadGroupObjectAndGroup> returnList = Group.createAndStoreGroupsWithLoadGroupObjectList(
                Arrays.asList(loadGroupObject1, loadGroupObject2)
        );

        assertThat(returnList.size(), is(2));
        assertThat(returnList.get(0).getLoadGroupObject(), is(loadGroupObject1));
        assertThat(returnList.get(1).getLoadGroupObject(), is(loadGroupObject2));

        Group returnedGroup1 = returnList.get(0).getGroup();
        Group returnedGroup2 = returnList.get(1).getGroup();

        assertThat(returnedGroup1, is(group1));
        assertThat(returnedGroup2, IsNot.not(group2));

        assertThat(returnedGroup1.getNumber(),                  is(group1.getNumber()));
        assertThat(returnedGroup1.getName(),                    is(groupName1));
        assertThat(returnedGroup1.getId(),                      is(groupId1));
        assertThat(returnedGroup1.getImage(),                   is(group1.getImage()));
        assertThat(returnedGroup1.getDescription(),             is(groupDescription1));
        assertThat(returnedGroup1.getPermissions(),             is(group1.getPermissions()));
        assertThat(returnedGroup1.isOpen(),                     is(group1.isOpen()));
        assertThat(returnedGroup1.isSelected(),                 is(group1.isSelected()));

        assertThat(returnedGroup2.getNumber(),                  is(groupNumber2));
        assertThat(returnedGroup2.getName(),                    is(groupName2));
        assertThat(returnedGroup2.getId(),                      is(groupId2));
        assertThat(returnedGroup2.getImage().getResourceInt(),  is(new ObjectImage(groupImage2).getResourceInt()));
        assertThat(returnedGroup2.getDescription(),             is(groupDescription2));
        assertThat(returnedGroup2.getPermissions(),             is(group2.getPermissions()));
        assertThat(returnedGroup2.isOpen(),                     is(group2.isOpen()));
        assertThat(returnedGroup2.isSelected(),                 is(group2.isSelected()));
    }

    @Test
    public void invalidCreateGroupInvalidParameter() throws Exception {
        expectedEx.expect(GroupException.class);

        // Only one invalid Group constructor parameter case is tested.
        Group.createGroup(new LoadGroupObject(
                invalidNum,
                Base256.toBase64(validId),
                validName,
                validImageString,
                validDescription,
                Base64.toBase64String(PublishedTextTest.contact0group0EncryptedKey),
                Base64.toBase64String(PublishedTextTest.groupKeyIv),
                0,
                false,
                false
        ));
    }

    @Test
    public void invalidCreateGroupsInvalidParameter() throws Exception {
        expectedEx.expect(GroupException.class);

        // Only one invalid Group constructor parameter case is tested.
        Group.createGroups(Collections.singletonList(new LoadGroupObject(
                invalidNum,
                Base256.toBase64(validId),
                validName,
                validImageString,
                validDescription,
                Base64.toBase64String(PublishedTextTest.contact0group0EncryptedKey),
                Base64.toBase64String(PublishedTextTest.groupKeyIv),
                0,
                false,
                false
        )));
    }

    @Test
    public void invalidCreateGroupsWithLoadGroupObjectListInvalidParameter() throws Exception {
        expectedEx.expect(GroupException.class);

        // Only one invalid Group constructor parameter case is tested.
        Group.createAndStoreGroupsWithLoadGroupObjectList(Collections.singletonList(new LoadGroupObject(
                invalidNum,
                Base256.toBase64(validId),
                validName,
                validImageString,
                validDescription,
                Base64.toBase64String(PublishedTextTest.contact0group0EncryptedKey),
                Base64.toBase64String(PublishedTextTest.groupKeyIv),
                0,
                false,
                false
        )));
    }

    @Test
    public void invalidCreateAndStoreGroupsWithLoadGroupObjectList() throws Exception {
        expectedEx.expect(GroupException.class);
        String s = "The following exceptions prevented %d out of %d group(s) from being created or stored: ";
        expectedEx.expectMessage(String.format(s, 2, 3));
        s = "Group number is negative: %d";
        expectedEx.expectMessage(String.format(s, invalidNum));
        expectedEx.expectMessage(Group.NAME_EMPTY_MSG);

        LoadGroupObject loadGroupObject1 = new LoadGroupObject(
                invalidNum,
                Base256.toBase64(validId),
                validName,
                validImageString,
                validDescription,
                Base64.toBase64String(PublishedTextTest.contact0group0EncryptedKey),
                Base64.toBase64String(PublishedTextTest.groupKeyIv),
                0,
                false,
                false
        );
        LoadGroupObject loadGroupObject2 = new LoadGroupObject(
                validNum,
                Base256.toBase64(validId),
                "",
                validImageString,
                validDescription,
                Base64.toBase64String(PublishedTextTest.contact0group0EncryptedKey),
                Base64.toBase64String(PublishedTextTest.groupKeyIv),
                0,
                false,
                false
        );
        LoadGroupObject loadGroupObject3 = new LoadGroupObject(
                validNum,
                Base256.toBase64(validId),
                validName,
                validImageString,
                validDescription,
                Base64.toBase64String(PublishedTextTest.contact0group0EncryptedKey),
                Base64.toBase64String(PublishedTextTest.groupKeyIv),
                0,
                false,
                false
        );

        Group.createAndStoreGroupsWithLoadGroupObjectList(
                Arrays.asList(loadGroupObject1, loadGroupObject2, loadGroupObject3)
        );
    }
}
