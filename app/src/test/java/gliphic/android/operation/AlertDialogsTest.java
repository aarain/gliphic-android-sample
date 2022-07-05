package gliphic.android.operation;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import gliphic.android.display.libraries.AlertDialogs;
import gliphic.android.exceptions.InvalidUserInputException;

public class AlertDialogsTest {
    private static final String VALID_EMAIL    = "ashleyarain@hotmail.com";
    private static final String VALID_PASSWORD = "P455w0rd";
    private static final String VALID_MOBILE   = "0123456789";

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    /* Check valid email address */

    @Ignore("This test fails with the message: " +
            "java.lang.NullPointerException" +
            "This tested method should also be covered in the Espresso tests during registration and sign-in.")
    @Test
    public void validCheckValidEmailAddress() throws InvalidUserInputException {
        AlertDialogs.checkValidEmailAddress(VALID_EMAIL);
    }

    @Test
    public void invalidCheckValidEmailAddressNull() throws InvalidUserInputException {
        expectedEx.expect(NullPointerException.class);

        AlertDialogs.checkValidEmailAddress(null);
    }

    @Test
    public void emptyEmailAddress() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(AlertDialogs.EMAIL_EMPTY_MSG);

        AlertDialogs.checkValidEmailAddress("");
    }

    @Test
    public void invalidLengthEmailAddress() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(AlertDialogs.EMAIL_LENGTH_INVALID);

        AlertDialogs.checkValidEmailAddress("lu");
    }

    @Ignore("This test fails with the message: " +
            "java.lang.NullPointerException" +
            "This tested method should also be covered in the Espresso tests during registration and sign-in.")
    @Test
    public void invalidSyntaxEmailAddress() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(AlertDialogs.EMAIL_INVALID_MSG);

        AlertDialogs.checkValidEmailAddress("lul lul lul lul");
    }

    /* Check (one) valid contact password */

    @Test
    public void validCheckValidContactPassword() throws InvalidUserInputException {
        AlertDialogs.checkValidContactPassword(VALID_PASSWORD);
    }

    @Test
    public void invalidCheckValidContactPasswordNull() throws InvalidUserInputException {
        expectedEx.expect(NullPointerException.class);

        AlertDialogs.checkValidContactPassword(null);
    }

    @Test
    public void emptyPassword() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(AlertDialogs.ENTERED_PASSWORD_EMPTY_MSG);

        AlertDialogs.checkValidContactPassword("");
    }

    @Test
    public void isNotPrintableStringPassword() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(AlertDialogs.ENTERED_PASSWORD_NOT_PRINTABLE);

        AlertDialogs.checkValidContactPassword(VALID_PASSWORD + "£");
    }

    @Test
    public void invalidLengthPassword() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(AlertDialogs.ENTERED_PASSWORD_LENGTH_INVALID);

        AlertDialogs.checkValidContactPassword("lul");
    }

    /* Check (two) valid contact passwords */

    @Test
    public void validCheckValidContactPasswords() throws InvalidUserInputException {
        AlertDialogs.checkValidContactPasswords(VALID_PASSWORD, VALID_PASSWORD, true);
        AlertDialogs.checkValidContactPasswords(VALID_PASSWORD, VALID_PASSWORD, false);
    }

    @Test
    public void invalidCheckValidContactPasswordsNull1() throws InvalidUserInputException {
        expectedEx.expect(NullPointerException.class);

        AlertDialogs.checkValidContactPasswords(null, VALID_PASSWORD, true);
    }

    @Test
    public void invalidCheckValidContactPasswordsNull2() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(AlertDialogs.CHOSEN_PASSWORDS_UNEQUAL_MSG);

        AlertDialogs.checkValidContactPasswords(VALID_PASSWORD, null, true);
    }

    @Test
    public void emptyFirstPassword() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(String.format(AlertDialogs.CHOSEN_PASSWORD_EMPTY_MSG, " "));

        AlertDialogs.checkValidContactPasswords("", VALID_PASSWORD, false);
    }

    @Test
    public void emptyChangedPassword() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(String.format(AlertDialogs.CHOSEN_PASSWORD_EMPTY_MSG, " new "));

        AlertDialogs.checkValidContactPasswords("", VALID_PASSWORD, true);
    }

    @Test
    public void isNotPrintableStringPasswords() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(AlertDialogs.CHOSEN_PASSWORD_NOT_PRINTABLE);

        AlertDialogs.checkValidContactPasswords(VALID_PASSWORD + "£", VALID_PASSWORD, true);
    }

    @Test
    public void invalidLengthPasswords() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(AlertDialogs.CHOSEN_PASSWORD_LENGTH_INVALID);

        AlertDialogs.checkValidContactPasswords("lul", VALID_PASSWORD, true);
    }

    @Test
    public void invalidRepeatedPasswords() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(AlertDialogs.CHOSEN_PASSWORDS_UNEQUAL_MSG);

        AlertDialogs.checkValidContactPasswords(VALID_PASSWORD, VALID_PASSWORD + "l", true);
    }

    /* Check valid mobile telephone number */

    @Ignore("This test fails with the message: " +
            "java.lang.RuntimeException: Method isGlobalPhoneNumber in android.telephony.PhoneNumberUtils not mocked." +
            "This tested method should also be covered in the Espresso tests during registration.")
    @Test
    public void validCheckValidMobileNumber() throws InvalidUserInputException {
        AlertDialogs.checkValidMobileNumber(VALID_MOBILE);
    }

    @Test
    public void invalidCheckValidMobileNumberNull() throws InvalidUserInputException {
        expectedEx.expect(NullPointerException.class);

        AlertDialogs.checkValidMobileNumber(null);
    }

    @Test
    public void emptyMobileNumber() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(AlertDialogs.MOBILE_EMPTY_MSG);

        AlertDialogs.checkValidMobileNumber("");
    }

    @Test
    public void invalidLengthMobileNumber() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(AlertDialogs.MOBILE_LENGTH_INVALID);

        AlertDialogs.checkValidMobileNumber("123");
    }

    @Ignore("This test fails with the message: " +
            "java.lang.RuntimeException: Method isGlobalPhoneNumber in android.telephony.PhoneNumberUtils not mocked." +
            "This tested method should also be covered in the Espresso tests during registration.")
    @Test
    public void invalidSyntaxMobileNumber() throws InvalidUserInputException {
        expectedEx.expect(InvalidUserInputException.class);
        expectedEx.expectMessage(AlertDialogs.MOBILE_INVALID_MSG);

        AlertDialogs.checkValidMobileNumber("lul lul lul lul");
    }
}
