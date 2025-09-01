package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ListEncoderDecoder {
    // Encoder
    public static String encode(List<String> list) {
        // Concatenate all strings
        StringBuilder concatenated = new StringBuilder();
        for (String s : list) {
            concatenated.append(s);
        }

        // Calculate log2 of the total length
        int length = concatenated.length();
        int delimiterLength = ((int) Math.ceil(Math.log(length) / Math.log(62)))+1;

        // Generate a random delimiter of the calculated length
        String delimiter = generateRandomString(delimiterLength);

        // Make sure the delimiter does not appear in the concatenated string
        while (concatenated.toString().contains(delimiter)) {
            delimiter = generateRandomString(delimiterLength);
        }

        // Prepend the length of the delimiter and the delimiter itself
        return delimiterLength + "#" + delimiter + concatenateWithDelimiter(list, delimiter);
    }

    // Decoder
    public static List<String> decode(String encoded) {
        int hashIndex = encoded.indexOf('#');

        int delimiterLength = Integer.parseInt(encoded.substring(0, hashIndex));

        String delimiter = encoded.substring(hashIndex + 1, hashIndex + 1 + delimiterLength);

        String content = encoded.substring(hashIndex + 1 + delimiterLength);
        // Split the content by the delimiter
        String[] splitStrings = content.split(delimiter);

        // Return the ArrayList of strings
        ArrayList<String> decodedList = new ArrayList<>();
        for (String s : splitStrings) {
            decodedList.add(s);
        }
        return decodedList;
    }

    // Helper method to concatenate strings with a specific delimiter
    private static String concatenateWithDelimiter(List<String> list, String delimiter) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            result.append(list.get(i));
            if (i < list.size() - 1) {
                result.append(delimiter);
            }
        }
        return result.toString();
    }

    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(chars.length());  // Get a random index from the chars string
            sb.append(chars.charAt(randomIndex));  // Append the character at that index to the StringBuilder
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        // Sample list of strings
        ArrayList<String> list = new ArrayList<>();
        list.add("apple");
        list.add("banana");
        list.add("cherry");

        // Encoding
        String encoded = encode(list);
        System.out.println("Encoded: " + encoded);

        // Decoding
        List<String> decoded = decode(encoded);
        System.out.println("Decoded: " + decoded);
    }
}
