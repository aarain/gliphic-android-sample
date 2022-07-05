package gliphic.android.operation;

import gliphic.android.exceptions.PublishedTextException;
import gliphic.android.exceptions.UnknownGroupIdException;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.generators.SCrypt;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import libraries.Base256Exception;
import libraries.BouncyCastleInterpreter;
import libraries.GroupPermissions;
import libraries.Vars;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class PublishedTextTest {
    private static Group defaultGroup;
    private final String plainText = "test";
//    private final String publishedText = "|~123456ᶀȫᑨṽᙑℙᘠὂҤᓖሡȨፕቀ⑨④ጲᴍ⣎ᶋ∣⤤ȏՄᘥṚ↓ἲᒓ⟇ȍ⧕ᘆⓦ⫁⩥~|";
//    private final byte[] iv = {-107, -79, -85, 98, 26, 24, -128, -85, 68, 125, -102, -45};
//    private final byte[] rawCipherText = {
//            -75, 74, -41, 67, -85, 59, 100, -2, 64, -34, 43, 4, 63, 109, -119, 55, -115, -125, 71, -2
//    };
//    private final byte[] timeOutCipherText = {
//            50, -90, -113, 71, 81, -88, 85, -44, -108, -46, -3, 42, 83, -88, -24, -19,
//            89, 102, -60, -81, 43, 24, -13, -55, 125, -23, -11, -69, -86, -61, 100, -51,
//            -50, 24, -33, -36, 123, -3, -83, -56, 108
//    };
    private final String publishedText =
        "|~123456789ĽùÛġñÙìűîĮCyŨÞŷîĎĈñĮeČŊñŗlkŖīōÉĞı-YťlÒŲÉôËÃ1İÔÉčŪÏĶĴRŢæþbÕŐÔnņĩÔr~|";
    private final byte[] iv = {
            (byte)0xbd, (byte)0x79, (byte)0x5b, (byte)0xa1, (byte)0x71, (byte)0x59, (byte)0x6c, (byte)0xf1,
            (byte)0x6e, (byte)0xae, (byte)0x0c, (byte)0x3c
    };
    private final byte[] rawCipherText = {
            (byte)0x53, (byte)0x3f, (byte)0x68, (byte)0x15, (byte)0xac, (byte)0x00, (byte)0x9f, (byte)0x32,
            (byte)0xea, (byte)0x31, (byte)0xe8, (byte)0x30, (byte)0xbd, (byte)0xb9, (byte)0x3a, (byte)0x9f,
            (byte)0x3c, (byte)0xa9, (byte)0xa4, (byte)0x04, (byte)0x16, (byte)0xcb, (byte)0x64, (byte)0x97,
            (byte)0x4b, (byte)0xa6, (byte)0xfe, (byte)0xaf, (byte)0xe8, (byte)0x73, (byte)0xd4, (byte)0x3f
    };
    private final byte[] timeOutCipherText = {
            (byte)0xe8, (byte)0x5e, (byte)0xf7, (byte)0x6e, (byte)0x8e, (byte)0x88, (byte)0x71, (byte)0xae,
            (byte)0x28, (byte)0x8c, (byte)0xca, (byte)0x71, (byte)0xd7, (byte)0x2f, (byte)0x2e, (byte)0xd6,
            (byte)0xab, (byte)0xcd, (byte)0x49, (byte)0x9e, (byte)0xb1, (byte)0x3e, (byte)0x22, (byte)0xe5,
            (byte)0x2f, (byte)0x52, (byte)0xf2, (byte)0x49, (byte)0x74, (byte)0x4b, (byte)0x43, (byte)0x01,
            (byte)0xb0, (byte)0x54, (byte)0x49, (byte)0x8d, (byte)0xea, (byte)0x4f, (byte)0xb6, (byte)0xb4,
            (byte)0x1b, (byte)0xe2, (byte)0x66, (byte)0x7e, (byte)0x25, (byte)0x55, (byte)0xd0, (byte)0x54,
            (byte)0x31, (byte)0xc6, (byte)0xa9, (byte)0x54, (byte)0x35
    };
    public static byte[] defaultGroupKey = {
            (byte)0x20, (byte)0x34, (byte)0xf6, (byte)0x6d, (byte)0x01, (byte)0x70, (byte)0x92, (byte)0xb3,
            (byte)0xa8, (byte)0x02, (byte)0xb3, (byte)0xe5, (byte)0x86, (byte)0x3f, (byte)0x87, (byte)0x8c,
            (byte)0x20, (byte)0x34, (byte)0xf6, (byte)0x6d, (byte)0x01, (byte)0x70, (byte)0x92, (byte)0xb3,
            (byte)0xa8, (byte)0x02, (byte)0xb3, (byte)0xe5, (byte)0x86, (byte)0x3f, (byte)0x87, (byte)0x8c
    };
    public static GroupPermissions defaultGroupPerms = GroupPermissions.ACTIVE_MEMBER;

    private static byte[] dataEncryptionKey;

    // These byte arrays are copied directly from the server tests.
    private static byte[] encSalt = {
            (byte) 0x10, (byte) 0x46, (byte) 0xAB, (byte) 0xC9, (byte) 0xC3, (byte) 0xAB, (byte) 0xC9, (byte) 0xC3,
            (byte) 0xAB, (byte) 0xC9, (byte) 0xC3, (byte) 0x40, (byte) 0x45, (byte) 0xD6, (byte) 0x91, (byte) 0x40
    };
    public static byte[] contact0group0EncryptedKey = {
            (byte)0xef, (byte)0x2a, (byte)0x27, (byte)0xb9, (byte)0x37, (byte)0x43, (byte)0xc7, (byte)0x3e,
            (byte)0x96, (byte)0x57, (byte)0xe9, (byte)0xb9, (byte)0x5f, (byte)0xb3, (byte)0xf2, (byte)0x2c,
            (byte)0xf2, (byte)0xcf, (byte)0x37, (byte)0x24, (byte)0xb1, (byte)0xc3, (byte)0x1e, (byte)0xda,
            (byte)0x74, (byte)0x76, (byte)0x4a, (byte)0x36, (byte)0x9e, (byte)0xd4, (byte)0xaf, (byte)0x10,
            (byte)0x2c, (byte)0xa6, (byte)0xcf, (byte)0x19, (byte)0xa8, (byte)0x4b, (byte)0xb0, (byte)0xa5,
            (byte)0x9c, (byte)0x2d, (byte)0x3d, (byte)0x43, (byte)0xa8, (byte)0x15, (byte)0xdc, (byte)0xb9
    };
    public static byte[] groupKeyIv = {
            (byte) 0xF6, (byte) 0x6D, (byte) 0x01, (byte) 0x70, (byte) 0x92, (byte) 0xBD, (byte) 0x87, (byte) 0x8F,
            (byte) 0x78, (byte) 0x02, (byte) 0xB3, (byte) 0x34
    };

    private static byte[] getDataEncryptionKey() throws IllegalAccessException, NoSuchFieldException {
        // Use reflection to get the private static final SCrypt parameters.
        BouncyCastleInterpreter bouncyCastleInterpreter = new BouncyCastleInterpreter();
        Field field = BouncyCastleInterpreter.class.getDeclaredField("SCRYPT_N");
        field.setAccessible(true);
        final int SCRYPT_N = (int) field.get(bouncyCastleInterpreter);

        field = BouncyCastleInterpreter.class.getDeclaredField("SCRYPT_r");
        field.setAccessible(true);
        final int SCRYPT_r = (int) field.get(bouncyCastleInterpreter);

        field = BouncyCastleInterpreter.class.getDeclaredField("SCRYPT_p");
        field.setAccessible(true);
        final int SCRYPT_p = (int) field.get(bouncyCastleInterpreter);

        field = BouncyCastleInterpreter.class.getDeclaredField("SCRYPT_dkLen");
        field.setAccessible(true);
        final int SCRYPT_dkLen = (int) field.get(bouncyCastleInterpreter);

        return SCrypt.generate(
                "P455w0rd".getBytes(StandardCharsets.UTF_8),
                encSalt,
                SCRYPT_N,
                SCRYPT_r,
                SCRYPT_p,
                SCRYPT_dkLen
        );
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @BeforeClass
    public static void setCurrentContact() throws Exception {
        ContactTest.createValidCurrentContact().storeStatically();
    }

    @BeforeClass
    public static void initializeDataEncryptionKey() throws IllegalAccessException, NoSuchFieldException {
        dataEncryptionKey = getDataEncryptionKey();
    }

    @Before
    public void resetGroups() throws Exception {
        Group.setNullKnownGroups();
        defaultGroup = GroupTest.createValidGroup();
        defaultGroup.storeStatically();
    }

    // Initialising PublishedText with plain text and group.

    @Test
    public void initialisePublishedTextFromPlaintext() throws InvalidCipherTextException, PublishedTextException {
        PublishedText ct = new PublishedText(plainText, Vars.NO_TIME_OUT, defaultGroup);

        assertThat(ct.getGroup(),         is(defaultGroup));
        assertThat(ct.getPlainText(),     is(plainText.getBytes(StandardCharsets.UTF_8)));
        assertThat(ct.getIv(),            is(notNullValue()));
        assertThat(ct.getTimeOut(),       is(Vars.NO_TIME_OUT));
        assertThat(ct.getRawCipherText(), is(notNullValue()));

        assertThat(ct.getTimeOutCipherText(), is(nullValue()));
        assertThat(ct.getPublishedText(),     is(nullValue()));
    }

    @Test
    public void initialiseWithEmptyPlaintext() throws InvalidCipherTextException, PublishedTextException {
        String s = "Cannot construct PublishedText object with an empty plain text message.";
        expectedEx.expect(PublishedTextException.class);
        expectedEx.expectMessage(s);

        new PublishedText("", Vars.NO_TIME_OUT, defaultGroup);
    }

    @Test
    public void initialiseWithNegativeTimeOut() throws InvalidCipherTextException, PublishedTextException {
        long invalidTimeOut = -1;
        expectedEx.expect(PublishedTextException.class);
        expectedEx.expectMessage("Cannot construct PublishedText object with invalid time-out: ");
        expectedEx.expectMessage(Long.toString(invalidTimeOut));

        new PublishedText(plainText, invalidTimeOut, defaultGroup);
    }

    @Test
    public void initialiseWithExpiredTimeOut() throws InvalidCipherTextException, PublishedTextException {
        long invalidTimeOut = (System.currentTimeMillis() / 1000) - 1;
        expectedEx.expect(PublishedTextException.class);
        expectedEx.expectMessage("Cannot construct PublishedText object with invalid time-out: ");
        expectedEx.expectMessage(Long.toString(invalidTimeOut));

        new PublishedText(plainText, invalidTimeOut, defaultGroup);
    }

    @Test
    public void initialiseWithDistantFutureTimeOut() throws InvalidCipherTextException, PublishedTextException {
        long invalidTimeOut = (System.currentTimeMillis() / 1000) + 31557600000L + 1;
        expectedEx.expect(PublishedTextException.class);
        expectedEx.expectMessage("Cannot construct PublishedText object with invalid time-out: ");
        expectedEx.expectMessage(Long.toString(invalidTimeOut));

        new PublishedText(plainText, invalidTimeOut, defaultGroup);
    }

    // Initialising PublishedText with cipher text.

    @Test
    public void initialisePublishedTextFromCipherText() throws Exception {
        PublishedText ct = new PublishedText(publishedText);

        assertThat(ct.getGroup(), is(defaultGroup));
        assertThat(ct.getIv(), is(iv));
        assertThat(ct.getTimeOutCipherText(), is(timeOutCipherText));
        assertThat(ct.getPublishedText(), is(publishedText));
    }

    @Test
    public void initialiseWithEmptyPublishedText() throws Exception {
        expectedEx.expect(PublishedTextException.class);
        expectedEx.expectMessage(PublishedText.EMPTY_PUB_TEXT_EXP);

        new PublishedText("");
    }

    @Test
    public void initialiseWithInvalidShortTotalLength() throws Exception {
        expectedEx.expect(PublishedTextException.class);
        expectedEx.expectMessage(PublishedText.PUBLISHED_TEXT_TOO_SHORT_EXP);

        new PublishedText("~");
    }

    @Test
    public void initialiseWithInvalidStartTag() throws Exception {
        expectedEx.expect(PublishedTextException.class);
        expectedEx.expectMessage(PublishedText.BAD_START_TAG_EXP);

        String badStartTagPubText = "GG" + publishedText.substring(Vars.START_TAG.length());
        new PublishedText(badStartTagPubText);
    }

    @Test
    public void initialiseWithInvalidEndTag() throws Exception {
        expectedEx.expect(PublishedTextException.class);
        expectedEx.expectMessage(PublishedText.BAD_END_TAG_EXP);

        String badEndTagPubText = publishedText.substring(0, publishedText.length() - Vars.END_TAG.length()) + "GG";
        new PublishedText(badEndTagPubText);
    }

    @Test
    public void initialiseWithInvalidGroupId() throws Exception {
        expectedEx.expect(PublishedTextException.class);
        expectedEx.expectMessage(PublishedText.INVALID_GROUP_ID);

        String badEndTagPubText = publishedText.substring(0, Vars.START_TAG.length()) +
                (char) 60 +
                publishedText.substring(Vars.START_TAG.length() + 1);
        new PublishedText(badEndTagPubText);
    }

    @Test
    public void initialiseWithInvalidShortMsgLength() throws Exception {
        expectedEx.expect(PublishedTextException.class);
        expectedEx.expectMessage(PublishedText.MESSAGE_BYTES_TOO_SHORT_EXP);

        String s = Vars.START_TAG + Group.getGroupFromNumber(0).getId() + "ab" + Vars.END_TAG;
        new PublishedText(s);
    }

    @Test
    public void initialiseWithUnknownGroupId() throws Exception {
        expectedEx.expect(UnknownGroupIdException.class);
        expectedEx.expectMessage(PublishedText.UNKNOWN_GROUP_EXP);

        Group.setNullKnownGroups();
        new Group(
                0,
                Vars.DisplayPicture.ANIMAL_CAT.get(),
                "blah",
                "blah blah",
                GroupTest.generateGroupId(0),
                defaultGroupPerms,
                false,
                false,
                true
        ).storeStatically();

        new PublishedText(publishedText);
    }

    @Test
    public void initialiseWithInactiveAndAccessDeniedGroup() throws Exception {
        expectedEx.expect(PublishedTextException.class);
        expectedEx.expectMessage(PublishedText.GROUP_INACTIVE_AND_ACCESS_DENIED_EXP);
        expectedEx.expectMessage(defaultGroup.getId());

        GroupPermissions groupPerms = defaultGroup.getPermissions();
        try {
            defaultGroup.setPermissions(GroupPermissions.INACTIVE_DENIED);

            new PublishedText(publishedText);
        }
        finally {
            defaultGroup.setPermissions(groupPerms);
        }
    }

    @Test
    public void initialiseWithInactiveGroup() throws Exception {
        expectedEx.expect(PublishedTextException.class);
        expectedEx.expectMessage(PublishedText.GROUP_INACTIVE_EXP);
        expectedEx.expectMessage(defaultGroup.getId());

        GroupPermissions groupPerms = defaultGroup.getPermissions();
        try {
            defaultGroup.setPermissions(GroupPermissions.INACTIVE_OWNER);

            new PublishedText(publishedText);
        }
        finally {
            defaultGroup.setPermissions(groupPerms);
        }
    }

    @Test
    public void initialiseWithAccessDeniedGroup() throws Exception {
        expectedEx.expect(PublishedTextException.class);
        expectedEx.expectMessage(PublishedText.GROUP_ACCESS_DENIED_EXP);
        expectedEx.expectMessage(defaultGroup.getId());

        GroupPermissions groupPerms = defaultGroup.getPermissions();
        try {
            defaultGroup.setPermissions(GroupPermissions.ACTIVE_DENIED);

            new PublishedText(publishedText);
        }
        finally {
            defaultGroup.setPermissions(groupPerms);
        }
    }

    // Test constructPublishedText().

    @Test
    public void validConstructPublishedText()
            throws InvalidCipherTextException, PublishedTextException, IOException,
            IllegalAccessException, NoSuchFieldException {

        PublishedText pt = new PublishedText(plainText, Vars.NO_TIME_OUT, defaultGroup);

        // Use reflection to set the randomly generated initialisation vector to a known value.
        Field field = PublishedText.class.getDeclaredField("iv");
        field.setAccessible(true);
        field.set(pt, iv);

        pt.constructPublishedText(timeOutCipherText);

        assertThat(pt.getTimeOutCipherText(), is(timeOutCipherText));
        assertThat(pt.getPublishedText(),     is(publishedText));
    }

    // Test decryptRawCipherText().

    @Test
    public void validDecryptRawCipherText()
            throws InvalidCipherTextException, Base256Exception, PublishedTextException, UnknownGroupIdException {

        PublishedText pt = new PublishedText(publishedText);

        pt.decryptRawCipherText(
                Vars.NO_TIME_OUT,
                rawCipherText,
                contact0group0EncryptedKey,
                groupKeyIv,
                dataEncryptionKey
        );

        assertThat(pt.getTimeOut(),                                         is(Vars.NO_TIME_OUT));
        assertThat(pt.getRawCipherText(),                                   is(rawCipherText));
        assertThat(new String(pt.getPlainText(), StandardCharsets.UTF_8), is(plainText));
    }
}
