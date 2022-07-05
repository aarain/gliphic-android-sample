package gliphic.android;

import gliphic.android.operation.Alerts;
import gliphic.android.operation.Contact;
import gliphic.android.operation.Group;
import gliphic.android.operation.PublishedText;
import gliphic.android.operation.TempGlobalStatics;

import java.io.IOException;

public class TestUtils {
    // Used to convert a single byte to and from its literal string representation.
    private static final String[] byteStrings = {
            "00", "01", "02", "03", "04", "05", "06", "07",
            "08", "09", "0A", "0B", "0C", "0D", "0E", "0F",
            "10", "11", "12", "13", "14", "15", "16", "17",
            "18", "19", "1A", "1B", "1C", "1D", "1E", "1F",
            "20", "21", "22", "23", "24", "25", "26", "27",
            "28", "29", "2A", "2B", "2C", "2D", "2E", "2F",
            "30", "31", "32", "33", "34", "35", "36", "37",
            "38", "39", "3A", "3B", "3C", "3D", "3E", "3F",
            "40", "41", "42", "43", "44", "45", "46", "47",
            "48", "49", "4A", "4B", "4C", "4D", "4E", "4F",
            "50", "51", "52", "53", "54", "55", "56", "57",
            "58", "59", "5A", "5B", "5C", "5D", "5E", "5F",
            "60", "61", "62", "63", "64", "65", "66", "67",
            "68", "69", "6A", "6B", "6C", "6D", "6E", "6F",
            "70", "71", "72", "73", "74", "75", "76", "77",
            "78", "79", "7A", "7B", "7C", "7D", "7E", "7F",
            "80", "81", "82", "83", "84", "85", "86", "87",
            "88", "89", "8A", "8B", "8C", "8D", "8E", "8F",
            "90", "91", "92", "93", "94", "95", "96", "97",
            "98", "99", "9A", "9B", "9C", "9D", "9E", "9F",
            "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7",
            "A8", "A9", "AA", "AB", "AC", "AD", "AE", "AF",
            "B0", "B1", "B2", "B3", "B4", "B5", "B6", "B7",
            "B8", "B9", "BA", "BB", "BC", "BD", "BE", "BF",
            "C0", "C1", "C2", "C3", "C4", "C5", "C6", "C7",
            "C8", "C9", "CA", "CB", "CC", "CD", "CE", "CF",
            "D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7",
            "D8", "D9", "DA", "DB", "DC", "DD", "DE", "DF",
            "E0", "E1", "E2", "E3", "E4", "E5", "E6", "E7",
            "E8", "E9", "EA", "EB", "EC", "ED", "EE", "EF",
            "F0", "F1", "F2", "F3", "F4", "F5", "F6", "F7",
            "F8", "F9", "FA", "FB", "FC", "FD", "FE", "FF"
    };

    /**
     * Create a byte array from a string of hexadecimal digits, assuming that the digits are a direct representation of
     * hexadecimal to dump into the array.
     *
     * The string must have even length.
     *
     * Only upper case letters are permitted.
     *
     * @param inString  The string to convert to a byte array.
     * @return byte[]   The output byte array.
     */
    public static byte[] stringHexToBytes(String inString) throws IOException {
        if (inString.length() % 2 != 0) {
            throw new IOException("Input has odd length.");
        }

        byte[] returnBytes = new byte[inString.length() / 2];

        for (int i = 0; i < inString.length(); i+=2) {
            Boolean validStringPair = false;
            for (int j = 0; j < byteStrings.length; j++) {
                if ( inString.substring(i, i+2).equals(byteStrings[j]) ) {
                    if (j >= 0) {
                        returnBytes[i/2] = (byte) j;
                        validStringPair = true;
                        break;
                    }
                    else {
                        returnBytes[i/2] = (byte) (j + 256);
                        validStringPair = true;
                        break;
                    }
                }
            }
            if (!validStringPair) {
                // Character s.charAt(i) not in byteStrings.
                String s = "Invalid character-pair in input: " + inString.substring(i, i+2);
                throw new IOException(s);
            }
        }

        return returnBytes;
    }

    /*
     * Example string output:
     *
     * (byte)0xa6, (byte)0x9e, (byte)0x98, (byte)0xb1,
     * (byte)0x30, (byte)0x7b, (byte)0x13, (byte)0x15,
     * (byte)0xdd, (byte)0xd6, (byte)0xbc, (byte)0xb7,
     * (byte)0x8c, (byte)0x20, (byte)0xb8, (byte)0x56,
     * (byte)0xbb, (byte)0xdd, (byte)0xb3, (byte)0xc3,
     * (byte)0x62, (byte)0xd1, (byte)0xa0, (byte)0x5f,
     * (byte)0xda, (byte)0xad, (byte)0x0c, (byte)0x6d,
     * (byte)0xc8, (byte)0xc2, (byte)0x06, (byte)0xea
     */
    private static String byteArrayToHexInput(byte[] bytesIn, int hexPerLine) {
        if (hexPerLine < 1) {
            System.out.println("Error: Cannot print fewer than 1 pair of hex digits per line.");
            return null;
        }

        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bytesIn.length; i++) {
            if (i == bytesIn.length - 1) {
                sb.append(String.format("(byte)0x%02x ", bytesIn[i]));
                break;
            }
            else {
                sb.append(String.format("(byte)0x%02x, ", bytesIn[i]));
            }

            if (i % hexPerLine == hexPerLine - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public static void printByteArrayAsHex(byte[] bytesIn, int hexPerLine) {
        System.out.println(byteArrayToHexInput(bytesIn, hexPerLine));
    }

    public static void printPublishedTextMembers(PublishedText pt) {
        String[] outStrings = {
                "\nPublishedText:",
                pt.getPublishedText(),
                "\nPlaintext:",
                byteArrayToHexInput(pt.getPlainText(), 8),
                "\nTime-out:",
                Long.toString(pt.getTimeOut()),
                "\nInitialisation vector:",
                byteArrayToHexInput(pt.getIv(), 8),
                "\nEncrypted plaintext (raw cipher text):",
                byteArrayToHexInput(pt.getRawCipherText(), 8),
                "\nEncrypted time-out and cipher text (cipher text):",
                byteArrayToHexInput(pt.getTimeOutCipherText(), 8),
                "\nGroup number:",
                String.valueOf(pt.getGroup().getNumber()),
                "\nGroup name:",
                pt.getGroup().getName(),
                "\nGroup description:",
                pt.getGroup().getDescription(),
                "\nGroup ID:",
                pt.getGroup().getId(),
                "\nGroup key:",
                byteArrayToHexInput(pt.getGroup().getKey(), 8),
                "\nGroup permissions:",
                pt.getGroup().getPermissions().toString(),
                "\nGroup open:",
                Boolean.toString(pt.getGroup().isOpen()),
                "\nGroup selected:",
                Boolean.toString(pt.getGroup().isSelected())
        };

        for (String s : outStrings) {
            try {
                System.out.println(s);
            }
            catch (NullPointerException e) {
                System.out.println("null");
            }
        }
    }

    /*
     * Call this before/after tests to clear all static lists so that future tests are not affected.
     */
    public static void clearStaticLists() {
        Alerts.setNullGroupShares();
        Group.setNullKnownGroups();
        Group.setNullSelectedGroup();
        Contact.setNullCurrentContact();
        Contact.setNullKnownContacts();
        Contact.setNullExtendedContacts();
        TempGlobalStatics.setNullTempGlobalStatics();
    }
}
