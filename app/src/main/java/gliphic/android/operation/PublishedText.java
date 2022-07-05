/*
(c) Ashley Arain 2017

The copyright in this software is the property of Ashley Arain. This
software may not be copied, disclosed, licensed, modified, reproduced,
sold, transferred or used in part or in whole or in any manner or form
other than in accordance with the licence agreement provided with this
software or otherwise without the prior written consent of Ashley Arain.
*/

package gliphic.android.operation;

import gliphic.android.exceptions.GroupException;
import gliphic.android.exceptions.NullStaticVariableException;
import gliphic.android.exceptions.PublishedTextException;
import gliphic.android.exceptions.UnknownGroupIdException;

import org.bouncycastle.crypto.InvalidCipherTextException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import libraries.Base256;
import libraries.Base256Exception;
import libraries.BouncyCastleInterpreter;
import libraries.GeneralUtils;
import libraries.Vars;

/**
 * Defines a 'PublishedText' object, which can be initialized with either:
 *     A plaintext (and related values necessary for message encryption),
 *     A publishedText.
 *
 * The publishedText string is the final string which is displayed to the public, and contains the start and end tags
 * used to identify a message, the cipher text, the initialisation vector used to create the cipher text, the group ID
 * and the message time-out.
 */
public class PublishedText {
    // The group associated with this PublishedText object.
    private Group group = null;

    // The plain text message to encrypt.
    private byte[] plainText = null;

    // The complete cipher text as displayed to the public.
    private String publishedText = null;

    // The initialisation vector used for encryption/decryption.
    private byte[] iv = null;

    // The raw cipher text.
    private byte[] rawCipherText = null;

    // The encrypted concatenation of the time out and the raw cipher text.
    private byte[] timeOutCipherText = null;

    // The time-out for the message, either NO_TIMEOUT for no time-out or the number of seconds since epoch.
    private Long timeOut = null;

    /*
     * Exception messages which the user should not see, but when detected should be replaced with
     * an appropriate AlertDialog message.
     */
    public static final String EMPTY_PUB_TEXT_EXP =
            "Cannot construct PublishedText object with an empty published text message.";
    public static final String BAD_START_TAG_EXP =
            "Cannot construct PublishedText object with incorrect start tag: ";
    public static final String BAD_END_TAG_EXP =
            "Cannot construct PublishedText object with incorrect end tag: ";
    public static final String INVALID_GROUP_ID =
            "Cannot construct PublishedText object with invalid group ID.";
    public static final String PUBLISHED_TEXT_TOO_SHORT_EXP =
            "Cannot construct PublishedText object with too-little published text length: ";
    public static final String MESSAGE_BYTES_TOO_SHORT_EXP =
            "Cannot construct PublishedText object with too few decoded cipher message bytes: ";
    public static final String UNKNOWN_GROUP_EXP =
            "Cannot construct PublishedText object with unknown group ID: ";
    public static final String GROUP_INACTIVE_EXP =
            "Cannot construct PublishedText object with inactive group; group ID: ";
    public static final String GROUP_ACCESS_DENIED_EXP =
            "Cannot construct PublishedText object with access denied group; group ID: ";
    public static final String GROUP_INACTIVE_AND_ACCESS_DENIED_EXP =
            "Cannot construct PublishedText object with inactive and access denied group; group ID: ";

    /**
     * Constructor for initialising PublishedText with a plaintext message, group and time-out.
     *
     * Requesting a published text from the server is not done automatically.
     *
     * @param plainText                     The string representing a human-readable plaintext message.
     * @param msgTimeOut                    The number of seconds since epoch which determines the date that the server
     *                                      will refuses to decrypt the rawCipherText.
     * @param group                         A Group object accessible by the contact. No assumption is made on this
     *                                      group being stored in any local cache e.g. the list of known groups.
     * @throws InvalidCipherTextException   Thrown when encrypting the plainText.
     * @throws PublishedTextException       Thrown when checking the method inputs.
     */
    public PublishedText(String plainText,
                         long msgTimeOut,
                         Group group) throws InvalidCipherTextException, PublishedTextException {

        if (plainText.isEmpty()) {
            String s = "Cannot construct PublishedText object with an empty plain text message.";
            throw new PublishedTextException(s);
        }

        // Prevent setting any time-out over 1000 years in the future.
        long currentTime = System.currentTimeMillis() / 1000;
        if ((msgTimeOut != 0 && msgTimeOut <= currentTime) || msgTimeOut > currentTime + 60*60*24*365.25*1000) {
            String s = "Cannot construct PublishedText object with invalid time-out: %d";
            throw new PublishedTextException(String.format(s, msgTimeOut));
        }

        this.group = group;
        this.plainText = plainText.getBytes(StandardCharsets.UTF_8);
        this.iv = GeneralUtils.generateIv();
        this.timeOut = msgTimeOut;

        this.rawCipherText = BouncyCastleInterpreter.aesOperatePadInput(
                true,
                this.plainText,
                this.iv,
                this.group.getKey()
        );
    }

    /**
     * Constructor for initialising PublishedText with a published text.
     *
     * If the group is not in the cached list of known groups then an exception is thrown, so it is recommended to
     * retrieve all known groups before calling this constructor.
     *
     * Requesting a plain text from the server is not done automatically.
     *
     * @param publishedText                 The string representing the complete cipher message, as published.
     * @throws Base256Exception             Thrown when decoding the main body of the publishedText.
     * @throws PublishedTextException       Thrown when checking the publishedText is valid.
     * @throws UnknownGroupIdException      Thrown when no group with the associated ID can be found.
     *                                      This thrown exception does not guarantee that the current contact does not
     *                                      have access to this group since it may not be loaded yet.
     */
    public PublishedText(String publishedText)
            throws Base256Exception, PublishedTextException, UnknownGroupIdException {

        // Preliminary checks on the published text length and start/end tags.

        if (publishedText.isEmpty()) {
            throw new PublishedTextException(EMPTY_PUB_TEXT_EXP);
        }
        else if (publishedText.length() <= Vars.START_TAG.length() + Vars.GROUP_ID_LEN + Vars.END_TAG.length()) {
            throw new PublishedTextException(PUBLISHED_TEXT_TOO_SHORT_EXP + publishedText.length());
        }

        String startTag = publishedText.substring(0, Vars.START_TAG.length());
        if (!startTag.equals(Vars.START_TAG)) {
            throw new PublishedTextException(BAD_START_TAG_EXP + startTag);
        }

        String endTag = publishedText.substring(publishedText.length() - Vars.END_TAG.length());
        if (!endTag.equals(Vars.END_TAG)) {
            throw new PublishedTextException(BAD_END_TAG_EXP + endTag);
        }

        // Discard the start/end tags and split the group ID from the cipher message.

        String publishedTextWithoutTags = publishedText.substring(
                Vars.START_TAG.length(),
                publishedText.length() - Vars.END_TAG.length()
        );

        String groupId   = publishedTextWithoutTags.substring(0, Vars.GROUP_ID_LEN);
        String cipherMsg = publishedTextWithoutTags.substring(Vars.GROUP_ID_LEN);

        try {
            Group.checkValidId(groupId);
        }
        catch (GroupException e) {
            throw new PublishedTextException(INVALID_GROUP_ID);
        }

        // Decode the cipher message; an exception is thrown if it is malformed or too short.

        byte[] cipherMsgBytes = Base256.toBytes(cipherMsg);

        if (cipherMsgBytes.length <= Vars.IV_LEN) {
            throw new PublishedTextException(MESSAGE_BYTES_TOO_SHORT_EXP + cipherMsgBytes.length);
        }

        // Get the group associated with the group ID; if no group is found throw an exception.

        try {
            this.group = Group.getGroupFromId(groupId);
        }
        catch (NullStaticVariableException | GroupException e) {
            throw new UnknownGroupIdException(UNKNOWN_GROUP_EXP + groupId + "\n" + e.getMessage(), groupId);
        }

        // Check that the group permissions allow the group to be used.

        boolean failCondition1 = !this.group.getPermissions().isActive();
        boolean failCondition2 = this.group.getPermissions().isDenied();
        if (failCondition1 && failCondition2) {
            throw new PublishedTextException(GROUP_INACTIVE_AND_ACCESS_DENIED_EXP + groupId);
        }
        else if (failCondition1) {
            throw new PublishedTextException(GROUP_INACTIVE_EXP + groupId);
        }
        else if (failCondition2) {
            throw new PublishedTextException(GROUP_ACCESS_DENIED_EXP + groupId);
        }

        // Set the remaining member variables.

        this.iv                = Arrays.copyOfRange(cipherMsgBytes, 0, Vars.IV_LEN);

        this.timeOutCipherText = Arrays.copyOfRange(cipherMsgBytes, Vars.IV_LEN, cipherMsgBytes.length);

        this.publishedText = publishedText;
    }

    public Group getGroup() {
        return group;
    }

    public byte[] getPlainText() {
        return plainText;
    }

    public String getPublishedText() {
        return publishedText;
    }

    public byte[] getIv() {
        return iv;
    }

    public byte[] getRawCipherText() {
        return rawCipherText;
    }

    public byte[] getTimeOutCipherText() {
        return timeOutCipherText;
    }

    public Long getTimeOut() {
        return timeOut;
    }

    /**
     * After requesting a plain text to be encrypted by the server, construct the published text.
     *
     * This method also initialises the raw cipher text and the published text member variables.
     *
     * @param timeOutCipherText     The encrypted text bytes received from the server.
     * @throws IOException          Thrown when writing to the ByteArrayOutputStream.
     */
    public void constructPublishedText(byte[] timeOutCipherText) throws IOException {
        this.timeOutCipherText = timeOutCipherText;

        ByteArrayOutputStream pubTextOutStream = new ByteArrayOutputStream();
        pubTextOutStream.write(this.iv);
        pubTextOutStream.write(this.timeOutCipherText);

        final String cipherMessage = Base256.fromBytes(pubTextOutStream.toByteArray());
        this.publishedText = Vars.START_TAG + this.group.getId() + cipherMessage + Vars.END_TAG;
    }

    /**
     * After requesting a published text to be decrypted by the server, decrypt the encrypted group key, and use this
     * key to decrypt raw cipher text and obtain the plain text.
     *
     * This method also initialises the given arguments, the message time-out and raw cipher text, as the associated
     * member variables.
     *
     * @param timeOut                       The time-out for the decrypted message.
     * @param rawCipherText                 The decrypted published text received from the server.
     * @param encryptedGroupKey             The encrypted key required to obtain the plain text.
     * @param groupKeyIv                    The initialisation vector required to obtain the group key.
     * @param dataEncryptionKey             The key used by the user to encrypt data saved on the server.
     * @throws InvalidCipherTextException   Thrown when decrypting using BouncyCastle.
     */
    public void decryptRawCipherText(long timeOut,
                                     byte[] rawCipherText,
                                     byte[] encryptedGroupKey,
                                     byte[] groupKeyIv,
                                     byte[] dataEncryptionKey) throws InvalidCipherTextException {

        // Do not set the group key since it is only used in this method during decryption.
        this.timeOut = timeOut;
        this.rawCipherText = rawCipherText;

        // Decrypt the encrypted group key.
        byte[] groupKey = BouncyCastleInterpreter.aesOperate(
                false,
                encryptedGroupKey,
                groupKeyIv,
                dataEncryptionKey
        );

        // Decrypt the message to obtain the plain text.
        this.plainText = BouncyCastleInterpreter.aesOperatePadInput(
                false,
                this.rawCipherText,
                this.iv,
                groupKey
        );
    }
}