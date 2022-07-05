package gliphic.android.operation;

import gliphic.android.TestUtils;
import gliphic.android.exceptions.ContactException;
import gliphic.android.exceptions.ContactUniquenessException;
import gliphic.android.exceptions.GroupException;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.operation.misc.LoadContactObjectAndContact;

import org.bouncycastle.util.encoders.Base64;
import org.hamcrest.CoreMatchers;
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

import libraries.Vars;
import libraries.Vars.ContactType;
import pojo.load.LoadContactObject;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class ContactTest {
    // Valid parameters.
    public static final long validNum1  = 4;
    public static final long validNum2  = 17;
    public static final long validNum3  = 588;
    public static final long validNum4  = 99521;
    public static final long currentNum = 0;
    public static final long unknownNum = 1234567890;
    public static final String validId   = "abcdefghijkl";
    public static final String validId1  = generateIdFromNumber(validNum1);
    public static final String validId2  = generateIdFromNumber(validNum2);
    public static final String validId3  = generateIdFromNumber(validNum3);
    public static final String validId4  = generateIdFromNumber(validNum4);
    public static final String currentId = generateIdFromNumber(currentNum);
    public static final String unknownId = generateIdFromNumber(unknownNum);
    public static final String validName  = "Valid Name 1";
    public static final String validName2 = "Valid Name 2";
    public static final byte[] validImage  = Vars.DisplayPicture.LADY_FIREFIGHTER.get();
    public static final byte[] validImage1 = Vars.DisplayPicture.MAN_ASTRONAUT.get();
    public static final byte[] validImage2 = Vars.DisplayPicture.MAN_STUDENT.get();
    public static final byte[] validImage3 = Vars.DisplayPicture.MAN_SINGER.get();
    public static final byte[] validImage4 = Vars.DisplayPicture.MAN_TECHNOLOGIST.get();
    public static final String validImageString = Base64.toBase64String(validImage);

    private static String generateIdFromNumber(long contactNumber) {
        return String.format("%d%s", contactNumber, validId).substring(0, Vars.CONTACT_ID_LEN);
    }

    public static Contact createValidKnownContact() throws ContactException {
        return new Contact(
                validNum1,
                validId1,
                validName,
                validImage1,
                ContactType.KNOWN,
                true
        );
    }

    public static Contact createValidKnownContact2() throws ContactException {
        return new Contact(
                validNum2,
                validId2,
                validName,
                validImage2,
                ContactType.KNOWN,
                true
        );
    }

    public static Contact createValidExtendedContact() throws ContactException {
        return new Contact(
                validNum3,
                validId3,
                validName,
                validImage3,
                ContactType.EXTENDED,
                true
        );
    }

    public static Contact createValidExtendedContact2() throws ContactException {
        return new Contact(
                validNum4,
                validId4,
                validName,
                validImage4,
                ContactType.EXTENDED,
                true
        );
    }

    public static Contact createValidCurrentContact() throws ContactException {
        return new Contact(
                currentNum,
                currentId,
                validName,
                validImage,
                ContactType.CURRENT,
                true
        );
    }

    public static Contact createValidUnknownContact() throws ContactException {
        return new Contact(
                unknownNum,
                unknownId,
                validName,
                validImage,
                ContactType.UNKNOWN,
                true
        );
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

    /* Contact initialisation */

    @Test
    public void validContactInitialisation() throws ContactException {
        Contact baseContact = createValidKnownContact();
        assertThat(baseContact.getNumber(), is(validNum1));
        assertThat(baseContact.getName(), is(validName));
    }

    @Ignore("This test fails with the message: " +
            "java.lang.RuntimeException: Method decodeByteArray in android.graphics.BitmapFactory not mocked.")
    @Test
    public void validContactInitialisationWithBitmapImage() throws ContactException {
        new Contact(
                validNum1,
                validId1,
                validName,
                new byte[5],
                ContactType.KNOWN,
                true
        );
    }

    @Test
    public void invalidNumberInitialisation() throws ContactException {
        expectedEx.expect(ContactException.class);
        expectedEx.expectMessage("Contact number");
        expectedEx.expectMessage("is negative.");

        new Contact(-4, validId1, validName, validImage1, ContactType.KNOWN, true);
    }

    @Test
    public void invalidLengthId() throws ContactException {
        final String invalidId = "";
        String expMsg = "The contact ID must have %d characters.";

        expectedEx.expect(ContactException.class);
        expectedEx.expectMessage(String.format(expMsg, Vars.CONTACT_ID_LEN));

        new Contact(validNum1, invalidId, validName, validImage1, ContactType.KNOWN, true);
    }

    @Test
    public void invalidCharactersId() throws ContactException {
        final String invalidId = String.format(";%s", validId).substring(0, Vars.CONTACT_ID_LEN);

        expectedEx.expect(ContactException.class);
        expectedEx.expectMessage("The contact ID contains invalid character(s).");

        new Contact(validNum1, invalidId, validName, validImage1, ContactType.KNOWN, true);
    }

    @Test
    public void emptyName() throws ContactException {
        expectedEx.expect(ContactException.class);
        expectedEx.expectMessage(Contact.NAME_EMPTY_MSG);

        new Contact(validNum1, validId1, "", validImage1, ContactType.KNOWN, true);
    }

    @Test
    public void isNotPrintableStringName() throws ContactException {
        expectedEx.expect(ContactException.class);
        expectedEx.expectMessage(Contact.NAME_NOT_PRINTABLE);

        final String name = "Not £ printable string";
        new Contact(validNum1, validId1, name, validImage1, ContactType.KNOWN, true);
    }

    @Test
    public void containsALeadingOrTrailingSpaceName() throws ContactException {
        expectedEx.expect(ContactException.class);
        expectedEx.expectMessage(Contact.NAME_CONTAINS_END_SPACE);

        final String name = " leading space";
        new Contact(validNum1, validId1, name, validImage1, ContactType.KNOWN, true);
    }

    @Test
    public void containsConsecutiveLegalCharactersName() throws ContactException {
        expectedEx.expect(ContactException.class);
        expectedEx.expectMessage(Contact.NAME_CONTAINS_CONSECUTIVE_CHARS);

        final String name = "Consecutive -- dashes";
        new Contact(validNum1, validId1, name, validImage1, ContactType.KNOWN, true);
    }

    @Test
    public void containsAnAdjacentQuotationMarkAndApostropheName() throws ContactException {
        expectedEx.expect(ContactException.class);
        expectedEx.expectMessage(Contact.NAME_CONTAINS_ADJACENT_MARKS);

        final String name = "Adjacent \"' marks";
        new Contact(validNum1, validId1, name, validImage1, ContactType.KNOWN, true);
    }

    @Test
    public void invalidLengthName() throws ContactException {
        expectedEx.expect(ContactException.class);
        expectedEx.expectMessage(Contact.NAME_LENGTH_INVALID);

        final String name = "This contact name is far too long and should not be allowed to exist.";
        new Contact(validNum1, validId1, name, validImage1, ContactType.KNOWN, true);
    }

    @Test
    public void duplicateContactNumberInitialisation() throws ContactException {
        final long duplicateNumber = validNum1;

        expectedEx.expect(ContactUniquenessException.class);
        expectedEx.expectMessage(String.format("Duplicate contact number detected: %s", duplicateNumber));

        createValidKnownContact().storeStatically();
        new Contact(
                duplicateNumber,
                validId2,
                validName2,
                validImage2,
                ContactType.EXTENDED,
                true
        );
    }

    @Test
    public void duplicateContactIdInitialisation() throws ContactException {
        final String duplicateId = validId1;

        expectedEx.expect(ContactUniquenessException.class);
        expectedEx.expectMessage(String.format("Duplicate contact ID detected: %s", duplicateId));

        createValidKnownContact().storeStatically();
        new Contact(
                validNum2,
                duplicateId,
                validName2,
                validImage2,
                ContactType.EXTENDED,
                true
        );
    }

    @Test
    public void allowDuplicateContactNumberAndIdInitialisation() throws ContactException {
        createValidKnownContact().storeStatically();
        new Contact(validNum1, validId1, validName, validImage1, ContactType.KNOWN, false);
    }

    /* Current contact */

    @Test
    public void validGetCurrentContact() throws ContactException, NullStaticVariableException {
        Contact contact = createValidCurrentContact();

        contact.storeStatically();

        assertThat(Contact.getCurrentContact(), is(contact));
    }

    @Test
    public void nullGetCurrentContact() throws NullStaticVariableException {
        expectedEx.expect(NullStaticVariableException.class);
        expectedEx.expectMessage("Current contact is null.");

        Contact.getCurrentContact();
    }

    /* Known/Extended contacts */

    @Test
    public void getTargetContactsBothListsNull() throws NullStaticVariableException {
        expectedEx.expect(NullStaticVariableException.class);
        expectedEx.expectMessage("Known and extended contacts lists are null.");

        Contact.getTargetContacts();
    }

    @Test
    public void getTargetContactsExtendedContactsListIsNull() throws ContactException, NullStaticVariableException {
        Contact contact = createValidKnownContact();
        contact.storeStatically();

        assertThat(Contact.getTargetContacts(), is(Collections.singletonList(contact)));
    }

    @Test
    public void getTargetContactsKnownContactsListIsNull() throws ContactException, NullStaticVariableException {
        Contact contact = createValidExtendedContact();
        contact.storeStatically();

        assertThat(Contact.getTargetContacts(), is(Collections.singletonList(contact)));
    }

    @Test
    public void getTargetContactsBothListsNonNull() throws ContactException, NullStaticVariableException {
        Contact contact1 = createValidKnownContact();
        Contact contact2 = createValidExtendedContact();
        contact1.storeStatically();
        contact2.storeStatically();

        assertThat(Contact.getTargetContacts(), is(Arrays.asList(contact1, contact2)));
    }

    @Test
    public void positiveGetTargetContactsSize() throws ContactException {
        createValidKnownContact().storeStatically();

        assertThat(Contact.getTargetContactsSize(), is(1));
    }

    @Test
    public void zeroGetTargetContactsSize() {
        assertThat(Contact.getTargetContactsSize(), is(0));
    }

    @Test
    public void validGetKnownContacts() throws ContactException, NullStaticVariableException {
        Contact contact = createValidKnownContact();
        contact.storeStatically();
        assertThat(Contact.getKnownContacts(), is(Collections.singletonList(contact)));
    }

    @Test
    public void validGetExtendedContacts() throws ContactException, NullStaticVariableException {
        Contact contact = createValidExtendedContact();
        contact.storeStatically();
        assertThat(Contact.getExtendedContacts(), is(Collections.singletonList(contact)));
    }

    @Test
    public void nullGetKnownContacts() throws NullStaticVariableException {
        expectedEx.expect(NullStaticVariableException.class);
        expectedEx.expectMessage("Known contacts list is null.");

        Contact.getKnownContacts();
    }

    @Test
    public void nullGetExtendedContacts() throws NullStaticVariableException {
        expectedEx.expect(NullStaticVariableException.class);
        expectedEx.expectMessage("Extended contacts list is null.");

        Contact.getExtendedContacts();
    }

    /* Store a contact in/as a global static */

    @Test
    public void validStoreStaticallyKnownContact() throws ContactException, NullStaticVariableException {
        Contact contact1 = createValidKnownContact();
        Contact contact2 = createValidKnownContact2();

        // First add a contact where the existing list is null.
        contact1.storeStatically();
        assertThat(Contact.getKnownContacts().size(), is(1));

        // Now add a contact where the existing list is non-empty.
        contact2.storeStatically();
        assertThat(Contact.getKnownContacts().size(), is(2));

        // Add an existing contact and assert that replaces the existing contact.
        Contact returnedContact = new Contact(
                contact1.getNumber(),
                validId,
                "some name",
                validImage,
                ContactType.KNOWN,
                false
        ).storeStatically();
        assertThat(returnedContact, is(contact1));
        assertThat(Contact.getKnownContacts().size(), is(2));
    }

    @Test
    public void validStoreStaticallyExtendedContact() throws ContactException, NullStaticVariableException {
        Contact contact1 = createValidExtendedContact();
        Contact contact2 = createValidExtendedContact2();

        // First add a contact where the existing list is null.
        contact1.storeStatically();
        assertThat(Contact.getExtendedContacts().size(), is(1));

        // Now add a contact where the existing list is non-empty.
        contact2.storeStatically();
        assertThat(Contact.getExtendedContacts().size(), is(2));

        // Add an existing contact and assert that replaces the existing contact.
        Contact returnedContact = new Contact(
                contact1.getNumber(),
                validId,
                "some name",
                validImage,
                ContactType.EXTENDED,
                false
        ).storeStatically();
        contact1.storeStatically();
        assertThat(returnedContact, is(contact1));
        assertThat(Contact.getExtendedContacts().size(), is(2));
    }

    @Test
    public void validStoreStaticallyRemoveExistingKnownContact() throws ContactException, NullStaticVariableException {
        Contact contact1 = createValidKnownContact();
        Contact contact2 = createValidKnownContact2();

        contact1.storeStatically();
        contact2.storeStatically();

        // Check that a contact with the same number as an existing contact is inserted into the known contacts list.
        final long contactNumber  = contact1.getNumber();
        final String contactId    = validId;
        final String contactName  = "Some contact name.";
        final byte[] contactImage = validImage;

        Contact newKnownContact = new Contact(
                contactNumber,
                contactId,
                contactName,
                contactImage,
                ContactType.KNOWN,
                false
        );
        newKnownContact.storeStatically();

        assertThat(Contact.getKnownContacts().size(),       is(2));
        assertThat(Contact.getExtendedContacts().size(),    is(0));

        // The original extended contact is still the object reference but its members have changed.
        Contact updatedKnownContact = Contact.getKnownContacts().get(0);
        assertThat(updatedKnownContact,                     is(contact1));
        assertThat(updatedKnownContact.getNumber(),         is(newKnownContact.getNumber()));
        assertThat(updatedKnownContact.getId(),             is(newKnownContact.getId()));
        assertThat(updatedKnownContact.getName(),           is(newKnownContact.getName()));
        assertThat(updatedKnownContact.getImage(),          is(newKnownContact.getImage()));
        assertThat(updatedKnownContact.getType(),           is(newKnownContact.getType()));

        // Check that a new extended contact with the same number as an existing known contact removes the known
        // contact and adds the extended contact.
        contact1.storeStatically();    // Revert the previous change.
        Contact newExtendedContact = new Contact(
                contactNumber,
                contactId,
                contactName,
                contactImage,
                ContactType.EXTENDED,
                false
        );
        newExtendedContact.storeStatically();

        assertThat(Contact.getKnownContacts().size(),       is(1));
        assertThat(Contact.getExtendedContacts().size(),    is(1));

        // The original extended contact is still the object reference but its members have changed.
        Contact updatedExtendedContact = Contact.getExtendedContacts().get(0);
        assertThat(updatedExtendedContact,                  is(contact1));
        assertThat(updatedExtendedContact.getNumber(),      is(newExtendedContact.getNumber()));
        assertThat(updatedExtendedContact.getId(),          is(newExtendedContact.getId()));
        assertThat(updatedExtendedContact.getName(),        is(newExtendedContact.getName()));
        assertThat(updatedExtendedContact.getImage(),       is(newExtendedContact.getImage()));
        assertThat(updatedExtendedContact.getType(),        is(newExtendedContact.getType()));
    }

    @Test
    public void validStoreStaticallyRemoveExistingExtendedContact()
            throws ContactException, NullStaticVariableException {

        Contact contact1 = createValidExtendedContact();
        Contact contact2 = createValidExtendedContact2();

        contact1.storeStatically();
        contact2.storeStatically();

        // Check that a contact with the same number as an existing contact is inserted into the known contacts list.
        final long contactNumber  = contact1.getNumber();
        final String contactId    = validId;
        final String contactName  = "Some contact name.";
        final byte[] contactImage = validImage;

        Contact newExtendedContact = new Contact(
                contactNumber,
                contactId,
                contactName,
                contactImage,
                ContactType.EXTENDED,
                false
        );
        newExtendedContact.storeStatically();

        assertThat(Contact.getKnownContacts().size(),       is(0));
        assertThat(Contact.getExtendedContacts().size(),    is(2));

        // The original extended contact is still the object reference but its members have changed.
        Contact updatedExtendedContact = Contact.getExtendedContacts().get(0);
        assertThat(updatedExtendedContact,                  is(contact1));
        assertThat(updatedExtendedContact.getNumber(),      is(newExtendedContact.getNumber()));
        assertThat(updatedExtendedContact.getId(),          is(newExtendedContact.getId()));
        assertThat(updatedExtendedContact.getName(),        is(newExtendedContact.getName()));
        assertThat(updatedExtendedContact.getImage(),       is(newExtendedContact.getImage()));
        assertThat(updatedExtendedContact.getType(),        is(newExtendedContact.getType()));

        // Check that a new known contact with the same number as an existing extended contact removes the extended
        // contact and adds the known contact.
        contact1.storeStatically();    // Revert the previous change.
        Contact newKnownContact = new Contact(
                contactNumber,
                contactId,
                contactName,
                contactImage,
                ContactType.KNOWN,
                false
        );
        newKnownContact.storeStatically();

        assertThat(Contact.getKnownContacts().size(),       is(1));
        assertThat(Contact.getExtendedContacts().size(),    is(1));

        // The original extended contact is still the object reference but its members have changed.
        Contact updatedKnownContact = Contact.getKnownContacts().get(0);
        assertThat(updatedKnownContact,                     is(contact1));
        assertThat(updatedKnownContact.getNumber(),         is(newKnownContact.getNumber()));
        assertThat(updatedKnownContact.getId(),             is(newKnownContact.getId()));
        assertThat(updatedKnownContact.getName(),           is(newKnownContact.getName()));
        assertThat(updatedKnownContact.getImage(),          is(newKnownContact.getImage()));
        assertThat(updatedKnownContact.getType(),           is(newKnownContact.getType()));
    }

    @Test
    public void validStoreStaticallyUnknownContact() throws ContactException, NullStaticVariableException {
        expectedEx.expect(NullStaticVariableException.class);
        expectedEx.expectMessage("Current contact is null.");

        Contact contact = createValidUnknownContact();
        contact.storeStatically();  // This should do nothing.

        assertThat(Contact.getTargetContactsSize(), is(0));
        Contact.getCurrentContact();
    }

    @Test
    public void validStoreStaticallyCurrentContact() throws ContactException, NullStaticVariableException {
        Contact contact = createValidCurrentContact();
        contact.storeStatically();

        assertThat(Contact.getCurrentContact(), is(contact));
    }

    @Test
    public void validStoreStaticallyCurrentContactModifiesExistingContactWithIdenticalNumber()
            throws ContactException, NullStaticVariableException {

        Contact contact1 = createValidCurrentContact();

        Contact contact2 = new Contact(
                contact1.getNumber(),
                validId,
                validName,
                validImage,
                ContactType.CURRENT,
                false
        );

        assertThat(contact2.storeStatically(), is(contact2));
        assertThat(contact1.storeStatically(), is(contact2));

        // The original current contact is still the object reference but its members have changed.
        Contact updatedCurrentContact = Contact.getCurrentContact();
        assertThat(updatedCurrentContact,             is(contact2));
        assertThat(updatedCurrentContact.getNumber(), is(contact1.getNumber()));
        assertThat(updatedCurrentContact.getId(),     is(contact1.getId()));
        assertThat(updatedCurrentContact.getName(),   is(contact1.getName()));
        assertThat(updatedCurrentContact.getImage(),  is(contact1.getImage()));
        assertThat(updatedCurrentContact.getType(),   is(contact1.getType()));
    }

    @Test
    public void invalidStoreStaticallyCurrentContactOverwritesExistingDifferentContact() throws ContactException {
        expectedEx.expect(ContactException.class);
        String s = "Cannot overwrite the existing current contact with number %d. " +
                "Set the existing current contact to null before assigning a new current contact.";
        expectedEx.expectMessage(String.format(s, currentNum));

        createValidCurrentContact().storeStatically();

        Contact contact = new Contact(
                validNum1,
                validId,
                validName,
                validImage,
                ContactType.CURRENT,
                false
        );
        contact.storeStatically();
    }

    @Test
    public void invalidStoreStaticallyCurrentContactInTargetContactList() throws Exception {
        expectedEx.expect(ContactException.class);
        String s = "This contact should not be in either known or extended contact lists. " +
                "This contact has now been removed from both lists.";
        expectedEx.expectMessage(s);

        Contact contact = createValidCurrentContact();

        // Manually manipulate the known/extended contacts lists.
        // Ensure that the known contacts list is non-null before manipulating it.
        createValidKnownContact().storeStatically();
        Contact.getKnownContacts().add(contact);

        contact.storeStatically();
    }

    /* Change contact type */

    @Test
    public void changeTypeToKnownWhenAlreadyKnown() throws ContactException, NullStaticVariableException {
        expectedEx.expect(NullStaticVariableException.class);

        Contact contact = createValidKnownContact();
        contact.changeType(ContactType.KNOWN);

        assertThat(contact.getType(), is(ContactType.KNOWN));

        // This is the only method call which can throw a NullStaticVariableException in this test.
        Contact.getKnownContacts();
    }

    @Test
    public void changeTypeToExtendedWhenAlreadyExtended() throws ContactException, NullStaticVariableException {
        expectedEx.expect(NullStaticVariableException.class);

        Contact contact = createValidExtendedContact();
        contact.changeType(ContactType.EXTENDED);

        assertThat(contact.getType(), is(ContactType.EXTENDED));

        // This is the only method call which can throw a NullStaticVariableException in this test.
        Contact.getExtendedContacts();
    }

    @Test
    public void changeTypeToKnownWithNullStaticLists() throws ContactException, NullStaticVariableException {
        expectedEx.expect(NullStaticVariableException.class);

        Contact contact = createValidExtendedContact();
        contact.changeType(ContactType.KNOWN);

        assertThat(contact.getType(), is(ContactType.KNOWN));

        // This is the only method call which can throw a NullStaticVariableException in this test.
        Contact.getKnownContacts();
    }

    @Test
    public void changeTypeToExtendedWithNullStaticLists() throws ContactException, NullStaticVariableException {
        expectedEx.expect(NullStaticVariableException.class);

        Contact contact = createValidKnownContact();
        contact.changeType(ContactType.EXTENDED);

        assertThat(contact.getType(), is(ContactType.EXTENDED));

        // This is the only method call which can throw a NullStaticVariableException in this test.
        Contact.getKnownContacts();
    }

    @Test
    public void changeTypeToKnownMoveToKnownContactsList() throws ContactException, NullStaticVariableException {
        Contact contact = createValidExtendedContact();
        contact.storeStatically();
        contact.changeType(ContactType.KNOWN);

        assertThat(contact.getType(),             is(ContactType.KNOWN));
        assertThat(Contact.getKnownContacts(),    CoreMatchers.hasItem(contact));
        assertThat(Contact.getExtendedContacts(), not(CoreMatchers.hasItem(contact)));
    }

    @Test
    public void changeTypeToExtendedMoveToExtendedContactsList() throws ContactException, NullStaticVariableException {
        Contact contact = createValidKnownContact();
        contact.storeStatically();
        contact.changeType(ContactType.EXTENDED);

        assertThat(contact.getType(),             is(ContactType.EXTENDED));
        assertThat(Contact.getKnownContacts(),    not(CoreMatchers.hasItem(contact)));
        assertThat(Contact.getExtendedContacts(), CoreMatchers.hasItem(contact));
    }

    @Test
    public void changeTypeToUnknownWhenNotStoredStatically() throws ContactException {
        Contact contact1 = createValidKnownContact();
        Contact contact2 = createValidExtendedContact();

        contact1.changeType(ContactType.UNKNOWN);
        contact2.changeType(ContactType.UNKNOWN);

        assertThat(contact1.getType(), is(ContactType.UNKNOWN));
        assertThat(contact2.getType(), is(ContactType.UNKNOWN));
    }

    @Test
    public void changeTypeToUnknownWhenStoredStatically() throws ContactException, NullStaticVariableException {
        Contact contact1 = createValidKnownContact();
        Contact contact2 = createValidExtendedContact();
        contact1.storeStatically();
        contact2.storeStatically();
        assertThat(Contact.getKnownContacts(),    CoreMatchers.hasItem(contact1));
        assertThat(Contact.getExtendedContacts(), CoreMatchers.hasItem(contact2));

        contact1.changeType(ContactType.UNKNOWN);
        contact2.changeType(ContactType.UNKNOWN);

        assertThat(contact1.getType(), is(ContactType.UNKNOWN));
        assertThat(contact2.getType(), is(ContactType.UNKNOWN));
        assertThat(Contact.getKnownContacts(),    not(CoreMatchers.hasItem(contact1)));
        assertThat(Contact.getExtendedContacts(), not(CoreMatchers.hasItem(contact2)));
    }

    @Test
    public void changeTypeKnownToExtendedGroupContact()
            throws ContactException, GroupException, NullStaticVariableException {

        Group group = GroupTest.createValidGroup();
        Contact contact = createValidKnownContact();

        contact.storeStatically();
        group.storeStatically();
        group.addGroupContact(contact);

        changeTypeAndAssertOnGroupContacts(ContactType.KNOWN, contact, group);

        contact.changeType(ContactType.EXTENDED);

        changeTypeAndAssertOnGroupContacts(ContactType.EXTENDED, contact, group);
    }

    @Test
    public void changeTypeExtendedToKnownGroupContact()
            throws ContactException, GroupException, NullStaticVariableException {

        Group group = GroupTest.createValidGroup();
        Contact contact = createValidExtendedContact();

        contact.storeStatically();
        group.storeStatically();
        group.addGroupContact(contact);

        changeTypeAndAssertOnGroupContacts(ContactType.EXTENDED, contact, group);

        contact.changeType(ContactType.KNOWN);

        changeTypeAndAssertOnGroupContacts(ContactType.KNOWN, contact, group);
    }

    @Test
    public void changeTypeKnownToUnknownGroupContact()
            throws ContactException, GroupException, NullStaticVariableException {

        Group group = GroupTest.createValidGroup();
        Contact contact = createValidKnownContact();

        contact.storeStatically();
        group.storeStatically();
        group.addGroupContact(contact);

        changeTypeAndAssertOnGroupContacts(ContactType.KNOWN, contact, group);

        contact.changeType(ContactType.UNKNOWN);

        changeTypeAndAssertOnGroupContacts(ContactType.UNKNOWN, contact, group);
    }

    @Test
    public void changeTypeExtendedToUnknownGroupContact()
            throws ContactException, GroupException, NullStaticVariableException {

        Group group = GroupTest.createValidGroup();
        Contact contact = createValidExtendedContact();

        contact.storeStatically();
        group.storeStatically();
        group.addGroupContact(contact);

        changeTypeAndAssertOnGroupContacts(ContactType.EXTENDED, contact, group);

        contact.changeType(ContactType.UNKNOWN);

        changeTypeAndAssertOnGroupContacts(ContactType.UNKNOWN, contact, group);
    }

    private void changeTypeAndAssertOnGroupContacts(ContactType contactType, Contact contact, Group group)
            throws ContactException, NullStaticVariableException {

        if (contactType == ContactType.KNOWN) {
            assertThat(Contact.getKnownContacts(),                  CoreMatchers.hasItem(contact));
            assertThat(Contact.getExtendedContacts(),               not(CoreMatchers.hasItem(contact)));
            assertThat(group.containsGroupKnownContact(contact),    is(true));
            assertThat(group.containsGroupExtendedContact(contact), is(false));
            assertThat((group.getGroupContactFromNumber(contact.getNumber()).getType()), is(ContactType.KNOWN));
        }
        else if (contactType == ContactType.EXTENDED) {
            assertThat(Contact.getKnownContacts(),                  not(CoreMatchers.hasItem(contact)));
            assertThat(Contact.getExtendedContacts(),               CoreMatchers.hasItem(contact));
            assertThat(group.containsGroupKnownContact(contact),    is(false));
            assertThat(group.containsGroupExtendedContact(contact), is(true));
            assertThat((group.getGroupContactFromNumber(contact.getNumber()).getType()), is(ContactType.EXTENDED));
        }
        else if (contactType == ContactType.UNKNOWN) {
            assertThat(Contact.getTargetContacts(),                 not(CoreMatchers.hasItem(contact)));
            assertThat(group.getGroupTargetContacts(),              not(CoreMatchers.hasItem(contact)));
        }
    }

    @Test
    public void invalidChangeTypeToCurrentContact() throws ContactException {
        expectedEx.expect(ContactException.class);
        expectedEx.expectMessage("Cannot change a contact's type to or from CURRENT.");

        createValidKnownContact().changeType(ContactType.CURRENT);
    }

    @Test
    public void invalidChangeTypeFromCurrentContact() throws ContactException {
        expectedEx.expect(ContactException.class);
        expectedEx.expectMessage("Cannot change a contact's type to or from CURRENT.");

        createValidCurrentContact().changeType(ContactType.KNOWN);     // Any type could be specified here.
    }

    /* Get contact from number */

    @Test
    public void contactNumberIsInList() throws ContactException {
        Contact contact = createValidKnownContact();
        List<Contact> contactList = Collections.singletonList(contact);

        Contact existingContact = Contact.getContactFromNumber(contact.getNumber(), contactList);
        assertThat(contactList, CoreMatchers.hasItem(existingContact));
    }

    @Test
    public void contactNumberIsNotInList() throws ContactException {
        long nonExistentContactNumber = 1234567890987654321L;
        String s = "The contact number %d is not in the given list of contacts.";
        expectedEx.expect(ContactException.class);
        expectedEx.expectMessage(String.format(s, nonExistentContactNumber));

        Contact.getContactFromNumber(nonExistentContactNumber, Collections.emptyList());
    }

    @Test
    public void getContactFromNumberNullInList() throws ContactException {
        expectedEx.expect(NullPointerException.class);

        Contact.getContactFromNumber(validNum1, Arrays.asList(null, createValidKnownContact()));
    }

    @Test
    public void validGetCurrentContactFromNumber() throws NullStaticVariableException, ContactException {
        Contact currentContact = createValidCurrentContact();
        currentContact.storeStatically();

        Contact retrievedContact = Contact.getContactFromNumber(currentContact.getNumber());
        assertThat(Contact.getCurrentContact(), is(retrievedContact));
    }

    @Test
    public void validGetKnownContactFromNumber() throws NullStaticVariableException, ContactException {
        Contact knownContact = createValidKnownContact();
        knownContact.storeStatically();

        Contact retrievedContact = Contact.getContactFromNumber(knownContact.getNumber());
        assertThat(Contact.getKnownContacts(), CoreMatchers.hasItem(retrievedContact));
    }

    @Test
    public void validGetExtendedContactFromNumber() throws NullStaticVariableException, ContactException {
        Contact extendedContact = createValidExtendedContact2();
        extendedContact.storeStatically();

        Contact retrievedContact = Contact.getContactFromNumber(extendedContact.getNumber());
        assertThat(Contact.getExtendedContacts(), CoreMatchers.hasItem(retrievedContact));
    }

    @Test
    public void invalidGetUnknownContactFromNumber() throws ContactException {
        Contact unknownContact = createValidUnknownContact();
        unknownContact.storeStatically();

        expectedEx.expect(ContactException.class);
        String s = "No contact object with the number %d is available from storage.";
        expectedEx.expectMessage(String.format(s, unknownContact.getNumber()));

        Contact.getContactFromNumber(unknownContact.getNumber());
    }

    @Test
    public void invalidGetUnavailableContactFromNumber() throws ContactException {
        Contact unavailableContact = createValidKnownContact();

        expectedEx.expect(ContactException.class);
        String s = "No contact object with the number %d is available from storage.";
        expectedEx.expectMessage(String.format(s, unavailableContact.getNumber()));

        Contact.getContactFromNumber(unavailableContact.getNumber());
    }

    /* Contact list uniqueness */

    @Test
    public void appendNewContactsNewUniqueContacts() throws ContactException {
        Contact contact1 = createValidKnownContact();
        Contact contact2 = createValidKnownContact2();
        Contact contact3 = createValidExtendedContact();
        Contact contact4 = createValidExtendedContact2();

        List<Contact> baseList = new ArrayList<>();
        List<Contact> newList  = new ArrayList<>();

        baseList.add(contact1);
        baseList.add(contact2);
        newList.add(contact1);
        newList.add(contact4);  // Test that this contact is sorted at the end.
        newList.add(contact2);
        newList.add(contact3);

        Contact.appendNewContacts(baseList, newList);

        assertThat(baseList, is(Arrays.asList(contact1, contact2, contact3, contact4)));
    }

    @Test
    public void appendNewContactsNoNewUniqueContacts() throws ContactException {
        Contact contact1 = createValidKnownContact();
        Contact contact2 = createValidKnownContact2();

        List<Contact> baseList = new ArrayList<>();
        List<Contact> newList  = new ArrayList<>();

        baseList.add(contact1);
        baseList.add(contact2);
        newList.add(contact1);
        newList.add(contact2);

        Contact.appendNewContacts(baseList, newList);

        assertThat(baseList, is(newList));
    }

    @Test
    public void appendNewContactsBaseUniqueContacts() throws ContactException {
        Contact contact1 = createValidKnownContact();
        Contact contact2 = createValidKnownContact2();
        Contact contact3 = createValidExtendedContact();
        Contact contact4 = createValidExtendedContact2();

        List<Contact> baseList = new ArrayList<>();
        List<Contact> newList  = new ArrayList<>();

        baseList.add(contact1);
        baseList.add(contact4);     // Test that this contact is sorted at the end.
        baseList.add(contact3);
        baseList.add(contact2);
        newList.add(contact1);
        newList.add(contact2);

        Contact.appendNewContacts(baseList, newList);

        assertThat(baseList, is(Arrays.asList(contact1, contact4, contact3, contact2)));
    }

    @Test
    public void appendNewContactsEmptyBaseContactsList() throws ContactException {
        Contact contact1 = createValidKnownContact();
        Contact contact2 = createValidKnownContact2();

        List<Contact> baseList = new ArrayList<>();
        List<Contact> newList  = new ArrayList<>();

        newList.add(contact1);
        newList.add(contact2);

        Contact.appendNewContacts(baseList, newList);

        assertThat(baseList, is(newList));
    }

    @Test
    public void appendNewContactsEmptyNewContactsList() throws ContactException {
        Contact contact1 = createValidKnownContact();
        Contact contact2 = createValidKnownContact2();

        List<Contact> baseList = new ArrayList<>();
        List<Contact> newList  = new ArrayList<>();

        baseList.add(contact1);
        baseList.add(contact2);

        Contact.appendNewContacts(baseList, newList);

        assertThat(baseList, is(Arrays.asList(contact1, contact2)));
    }

    /* Static add to target contacts */

    @Test
    public void validStoreStaticallyList() throws ContactException {
        final long        contactNumber1 = 33554;
        final String      contactId1     = generateIdFromNumber(contactNumber1);
        final String      contactName1   = "some name 1";
        final ContactType contactType1   = ContactType.KNOWN;
        final byte[]      contactImage1  = Vars.DisplayPicture.OTHER_MUSIC.get();
        final long        contactNumber2 = 555;
        final String      contactId2     = generateIdFromNumber(contactNumber2);
        final String      contactName2   = "some name 2";
        final ContactType contactType2   = ContactType.EXTENDED;
        final byte[]      contactImage2  = Vars.DisplayPicture.OTHER_PAINTING.get();

        // Add contacts to the target contacts list, one of which has the same number but different other values.
        Contact originalContact = new Contact(
                contactNumber1,
                validId,
                validName,
                validImage,
                ContactType.KNOWN,
                true
        ).storeStatically();

        createValidKnownContact().storeStatically();

        // Replace one of the original contacts and insert a new one.
        final LoadContactObject loadContactObject1 = new LoadContactObject(
                contactNumber1,
                contactId1,
                contactName1,
                Base64.toBase64String(contactImage1),
                contactType1
        );
        final LoadContactObject loadContactObject2 = new LoadContactObject(
                contactNumber2,
                contactId2,
                contactName2,
                Base64.toBase64String(contactImage2),
                contactType2
        );

        final List<Contact> allTargetContacts = Contact.createContacts(
                Arrays.asList(loadContactObject1, loadContactObject2)
        );
        List<Contact> returnedContacts = Contact.storeStatically(allTargetContacts);

        // Check that the contact created from loadContactObject1 does not override the object reference of
        // originalContact.
        assertThat(returnedContacts, CoreMatchers.hasItem(originalContact));

        assertThat(allTargetContacts.size(),        is(2));
        assertThat(Contact.getTargetContactsSize(), is(3));
        assertThat(returnedContacts.size(),         is(2));

        // Assert that the returned list of contacts have the correct parameters.
        final Contact lcdoContact1 = allTargetContacts.get(0);
        final Contact lcdoContact2 = allTargetContacts.get(1);
        assertThat(lcdoContact1.getNumber(),                    is(contactNumber1));
        assertThat(lcdoContact1.getId(),                        is(contactId1));
        assertThat(lcdoContact1.getName(),                      is(contactName1));
        assertThat(lcdoContact1.getImage().getResourceInt(),    is(new ObjectImage(contactImage1).getResourceInt()));
        assertThat(lcdoContact1.getType(),                      is(contactType1));
        assertThat(lcdoContact2.getNumber(),                    is(contactNumber2));
        assertThat(lcdoContact2.getId(),                        is(contactId2));
        assertThat(lcdoContact2.getName(),                      is(contactName2));
        assertThat(lcdoContact2.getImage().getResourceInt(),    is(new ObjectImage(contactImage2).getResourceInt()));
        assertThat(lcdoContact2.getType(),                      is(contactType2));
    }

    @Test
    public void invalidStoreStaticallyList() throws ContactException, NullStaticVariableException {
        // Create this contact before a contact with the same number is stored statically to avoid a uniqueness
        // exception.
        final Contact newCurrentContact = createValidCurrentContact();

        expectedEx.expect(ContactException.class);
        String s = "The following exceptions prevented %d out of %d contact(s) from being stored statically: ";
        expectedEx.expectMessage(String.format(s, 2, 3));
        s = "Cannot overwrite the existing current contact with number %d.";
        expectedEx.expectMessage(String.format(s, newCurrentContact.getNumber()));
        s = "Set the existing current contact to null before assigning a new current contact.";
        expectedEx.expectMessage(s);

        // Set up the scenario where two ContactExceptions will be thrown when calling storeStatically() on a contact.

        createValidExtendedContact2().storeStatically();

        new Contact(
                newCurrentContact.getNumber(),
                newCurrentContact.getId(),
                newCurrentContact.getName(),
                newCurrentContact.getImage().getImageBytes(),
                ContactType.CURRENT,
                true
        ).storeStatically();

        Contact currentContact = new Contact(
                validNum2,
                validId2,
                validName,
                validImage2,
                ContactType.CURRENT,
                true
        );

        // Illegally add a contact (with a bad type) directly to the list of target contacts.
        Contact.getTargetContacts().add(currentContact);

        // The thrown exception shows that the first two contacts in the list throw an exception and the last does not.
        Contact.storeStatically(Arrays.asList(currentContact, currentContact, newCurrentContact));
    }

    /* Common groups */

    @Test
    public void validGetCommonGroups() throws ContactException, GroupException {
        Contact baseContact = createValidKnownContact();
        Group baseGroup = GroupTest.createValidGroup();

        baseContact.addCommonGroup(baseGroup);

        assertThat(baseContact.getCommonGroups(), is(Collections.singletonList(baseGroup)));
    }

    @Test
    public void validAddToCommonGroups() throws ContactException, GroupException {
        Contact baseContact = createValidKnownContact();
        Group baseGroup = GroupTest.createValidGroup();
        Group baseGroup2 = GroupTest.createValidGroup2();

        baseContact.addCommonGroup(baseGroup);   // Test adding a group to a null list.
        baseContact.addCommonGroup(baseGroup2);  // Test adding a group to an existing list.
        assertThat(baseContact.getCommonGroups(), is(Arrays.asList(baseGroup, baseGroup2)));

        // Adding the same group to the contact does not modify the list of groups.
        baseContact.addCommonGroup(baseGroup2);
        assertThat(baseContact.getCommonGroups(), is(Arrays.asList(baseGroup, baseGroup2)));
    }

    /* Adding contacts and group contacts sorts them automatically by number */

    @Test
    public void storeKnownContactsSortsByNumber() throws ContactException, NullStaticVariableException {
        Contact contact1 = createValidKnownContact();
        Contact contact2 = createValidKnownContact2();

        Contact.storeStatically(Collections.singletonList(contact2));
        Contact.storeStatically(Collections.singletonList(contact1));
        assertThat(Contact.getKnownContacts().get(0), is(contact1));
        assertThat(Contact.getKnownContacts().get(1), is(contact2));

        Contact.setNullKnownContacts();

        Contact.storeStatically(Arrays.asList(contact2, contact1));
        assertThat(Contact.getKnownContacts().get(0), is(contact1));
        assertThat(Contact.getKnownContacts().get(1), is(contact2));
    }

    @Test
    public void storeExtendedContactsSortsByNumber() throws ContactException, NullStaticVariableException {
        Contact contact1 = createValidExtendedContact();
        Contact contact2 = createValidExtendedContact2();

        Contact.storeStatically(Collections.singletonList(contact2));
        Contact.storeStatically(Collections.singletonList(contact1));
        assertThat(Contact.getExtendedContacts().get(0), is(contact1));
        assertThat(Contact.getExtendedContacts().get(1), is(contact2));

        Contact.setNullExtendedContacts();

        Contact.storeStatically(Arrays.asList(contact2, contact1));
        assertThat(Contact.getExtendedContacts().get(0), is(contact1));
        assertThat(Contact.getExtendedContacts().get(1), is(contact2));
    }

    @Test
    public void storeStaticallyNewKnownContact() throws ContactException, NullStaticVariableException {
        Contact contact1 = createValidKnownContact();
        Contact contact2 = createValidKnownContact2();

        contact2.storeStatically();
        contact1.storeStatically();

        assertThat(Contact.getKnownContacts().get(0), is(contact1));
        assertThat(Contact.getKnownContacts().get(1), is(contact2));
    }

    @Test
    public void storeStaticallyNewExtendedContact() throws ContactException, NullStaticVariableException {
        Contact contact1 = createValidExtendedContact();
        Contact contact2 = createValidExtendedContact2();

        contact2.storeStatically();
        contact1.storeStatically();

        assertThat(Contact.getExtendedContacts().get(0), is(contact1));
        assertThat(Contact.getExtendedContacts().get(1), is(contact2));
    }

    @Test
    public void storeStaticallyReplaceKnownContact() throws ContactException, NullStaticVariableException {
        Contact contact1 = createValidKnownContact();
        Contact contact2 = new Contact(
                contact1.getNumber(),
                contact1.getId(),
                contact1.getName(),
                contact1.getImage().getImageBytes(),
                ContactType.KNOWN,
                true
        );

        contact2.storeStatically();
        Contact.storeStatically(Collections.singletonList(contact1));
        contact1.storeStatically();

        assertThat(Contact.getKnownContacts().size(), is(1));

        // The original known contact is still the object reference but its members have changed.
        Contact updatedKnownContact = Contact.getKnownContacts().get(0);
        assertThat(updatedKnownContact,             is(contact2));
        assertThat(updatedKnownContact.getNumber(), is(contact1.getNumber()));
        assertThat(updatedKnownContact.getId(),     is(contact1.getId()));
        assertThat(updatedKnownContact.getName(),   is(contact1.getName()));
        assertThat(updatedKnownContact.getImage(),  is(contact1.getImage()));
        assertThat(updatedKnownContact.getType(),   is(contact1.getType()));
    }

    @Test
    public void storeStaticallyReplaceExtendedContact() throws ContactException, NullStaticVariableException {
        Contact contact1 = createValidExtendedContact();
        Contact contact2 = new Contact(
                contact1.getNumber(),
                contact1.getId(),
                contact1.getName(),
                contact1.getImage().getImageBytes(),
                ContactType.EXTENDED,
                true
        );

        contact2.storeStatically();
        Contact.storeStatically(Collections.singletonList(contact1));
        contact1.storeStatically();

        assertThat(Contact.getExtendedContacts().size(), is(1));

        // The original extended contact is still the object reference but its members have changed.
        Contact updatedExtendedContact = Contact.getExtendedContacts().get(0);
        assertThat(updatedExtendedContact,              is(contact2));
        assertThat(updatedExtendedContact.getNumber(),  is(contact1.getNumber()));
        assertThat(updatedExtendedContact.getId(),      is(contact1.getId()));
        assertThat(updatedExtendedContact.getName(),    is(contact1.getName()));
        assertThat(updatedExtendedContact.getImage(),   is(contact1.getImage()));
        assertThat(updatedExtendedContact.getType(),    is(contact1.getType()));
    }

    @Test
    public void addCommonGroupSortsByNumber() throws ContactException, GroupException {
        Contact contact = createValidKnownContact();   // This could be a contact of any type.
        Group group1 = GroupTest.createValidGroup();
        Group group2 = GroupTest.createValidGroup2();

        contact.addCommonGroup(group2);
        contact.addCommonGroup(group1);

        assertThat(contact.getCommonGroups().get(0), is(group1));
        assertThat(contact.getCommonGroups().get(1), is(group2));
    }

    /* Create contacts from LoadContactObject  */

    @Test
    public void validCreateContact() throws ContactException {
        final long        contactNumber1 = 33554;
        final String      contactId1     = generateIdFromNumber(contactNumber1);
        final String      contactName1   = "some name 1";
        final byte[]      contactImage1  = Vars.DisplayPicture.OTHER_MUSIC.get();
        final ContactType contactType1   = ContactType.KNOWN;

        final LoadContactObject loadContactObject1 = new LoadContactObject(
                contactNumber1,
                contactId1,
                contactName1,
                Base64.toBase64String(contactImage1),
                contactType1
        );

        Contact contact = Contact.createContact(loadContactObject1);

        assertThat(contact.getNumber(),                 is(contactNumber1));
        assertThat(contact.getId(),                     is(contactId1));
        assertThat(contact.getName(),                   is(contactName1));
        assertThat(contact.getImage().getResourceInt(), is(new ObjectImage(contactImage1).getResourceInt()));
        assertThat(contact.getType(),                   is(contactType1));
    }

    @Test
    public void validCreateContacts() throws ContactException {
        final long        contactNumber1 = 33554;
        final String      contactId1     = generateIdFromNumber(contactNumber1);
        final String      contactName1   = "some name 1";
        final byte[]      contactImage1  = Vars.DisplayPicture.OTHER_MUSIC.get();
        final ContactType contactType1   = ContactType.KNOWN;
        final long        contactNumber2 = 555;
        final String      contactId2     = generateIdFromNumber(contactNumber2);
        final String      contactName2   = "some name 2";
        final byte[]      contactImage2  = Vars.DisplayPicture.OTHER_PAINTING.get();
        final ContactType contactType2   = ContactType.EXTENDED;

        final LoadContactObject loadContactObject1 = new LoadContactObject(
                contactNumber1,
                contactId1,
                contactName1,
                Base64.toBase64String(contactImage1),
                contactType1
        );
        final LoadContactObject loadContactObject2 = new LoadContactObject(
                contactNumber2,
                contactId2,
                contactName2,
                Base64.toBase64String(contactImage2),
                contactType2
        );

        List<Contact> contacts = Contact.createContacts(
                Arrays.asList(loadContactObject1, loadContactObject2)
        );

        assertThat(contacts.size(), is(2));
        assertThat(contacts.get(0).getNumber(),                 is(contactNumber1));
        assertThat(contacts.get(0).getId(),                     is(contactId1));
        assertThat(contacts.get(0).getName(),                   is(contactName1));
        assertThat(contacts.get(0).getImage().getResourceInt(), is(new ObjectImage(contactImage1).getResourceInt()));
        assertThat(contacts.get(0).getType(),                   is(contactType1));
        assertThat(contacts.get(1).getNumber(),                 is(contactNumber2));
        assertThat(contacts.get(1).getId(),                     is(contactId2));
        assertThat(contacts.get(1).getName(),                   is(contactName2));
        assertThat(contacts.get(1).getImage().getResourceInt(), is(new ObjectImage(contactImage2).getResourceInt()));
        assertThat(contacts.get(1).getType(),                   is(contactType2));
    }

    @Test
    public void validCreateContactsWithLoadContactObjectList() throws ContactException {
        final String      contactId1     = validId;
        final String      contactName1   = "some name 1";
        final ContactType contactType1   = ContactType.KNOWN;
        final byte[]      contactImage1  = Vars.DisplayPicture.OTHER_MUSIC.get();
        final long        contactNumber2 = 555;
        final String      contactId2     = generateIdFromNumber(contactNumber2);
        final String      contactName2   = "some name 2";
        final byte[]      contactImage2  = Vars.DisplayPicture.OTHER_PAINTING.get();
        final ContactType contactType2   = ContactType.EXTENDED;

        // Create and store contact(s) to test that one of the returned contacts is overwritten.
        Contact contact1 = createValidKnownContact();
        Contact contact2 = createValidKnownContact2();
        contact1.storeStatically();

        final LoadContactObject loadContactObject1 = new LoadContactObject(
                contact1.getNumber(),
                contactId1,
                contactName1,
                Base64.toBase64String(contactImage1),
                contactType1
        );
        final LoadContactObject loadContactObject2 = new LoadContactObject(
                contactNumber2,
                contactId2,
                contactName2,
                Base64.toBase64String(contactImage2),
                contactType2
        );

        List<LoadContactObjectAndContact> returnList = Contact.createAndStoreContactsWithLoadContactObjectList(
                Arrays.asList(loadContactObject1, loadContactObject2)
        );

        assertThat(returnList.size(), is(2));
        assertThat(returnList.get(0).getLoadContactObject(), is(loadContactObject1));
        assertThat(returnList.get(1).getLoadContactObject(), is(loadContactObject2));

        Contact returnedContact1 = returnList.get(0).getContact();
        Contact returnedContact2 = returnList.get(1).getContact();

        assertThat(returnedContact1, is(contact1));
        assertThat(returnedContact2, CoreMatchers.not(contact2));

        assertThat(returnedContact1.getNumber(),                    is(contact1.getNumber()));
        assertThat(returnedContact1.getId(),                        is(contactId1));
        assertThat(returnedContact1.getName(),                      is(contactName1));
        assertThat(returnedContact1.getImage(),                     is(contact1.getImage()));
        assertThat(returnedContact1.getType(),                      is(contactType1));

        assertThat(returnedContact2.getNumber(),                    is(contactNumber2));
        assertThat(returnedContact2.getId(),                        is(contactId2));
        assertThat(returnedContact2.getName(),                      is(contactName2));
        assertThat(returnedContact2.getImage().getResourceInt(), is(new ObjectImage(contactImage2).getResourceInt()));
        assertThat(returnedContact2.getType(),                      is(contactType2));
    }

    @Test
    public void invalidCreateContactInvalidParameter() throws ContactException {
        expectedEx.expect(ContactException.class);

        // Only one invalid Contact constructor parameter case is tested.
        Contact.createContact(new LoadContactObject(
                -1,
                validId,
                validName,
                validImageString,
                ContactType.KNOWN
        ));
    }

    @Test
    public void invalidCreateContactsWithLoadContactObjectListInvalidParameter() throws ContactException {
        expectedEx.expect(ContactException.class);

        // Only one invalid Contact constructor parameter case is tested.
        Contact.createAndStoreContactsWithLoadContactObjectList(Collections.singletonList(new LoadContactObject(
                -1,
                validId,
                validName,
                validImageString,
                ContactType.KNOWN
        )));
    }

    @Test
    public void invalidCreateAndStoreContactsWithLoadContactObjectList() throws ContactException {
        final long invalidContactNum = -1;
        final String invalidContactName = "";

        expectedEx.expect(ContactException.class);
        String s = "The following exceptions prevented %d out of %d contact(s) from being created or stored: ";
        expectedEx.expectMessage(String.format(s, 2, 3));
        s = "Contact number '%d' is negative.";
        expectedEx.expectMessage(String.format(s, invalidContactNum));
        expectedEx.expectMessage(Contact.NAME_EMPTY_MSG);

        LoadContactObject loadContactObject1 = new LoadContactObject(
                invalidContactNum,
                validId,
                validName,
                validImageString,
                ContactType.KNOWN
        );
        LoadContactObject loadContactObject2 = new LoadContactObject(
                validNum1,
                validId1,
                invalidContactName,
                validImageString,
                ContactType.KNOWN
        );
        LoadContactObject loadContactObject3 = new LoadContactObject(
                validNum1,
                validId1,
                validName,
                validImageString,
                ContactType.KNOWN
        );

        Contact.createAndStoreContactsWithLoadContactObjectList(
                Arrays.asList(loadContactObject1, loadContactObject2, loadContactObject3)
        );
    }
}
