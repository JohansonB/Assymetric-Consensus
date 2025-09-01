package utils;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class MACUtils {



    // Generate ECDSA key pair
    public static KeyPair generateKeyPair(){
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(256);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Sign message using private key
    public static String signMessage(String message, PrivateKey privateKey){
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(message.getBytes());
            byte[] signedBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signedBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Verify message signature using public key
    public static boolean verifySignature(String message, String signature, PublicKey publicKey){
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(publicKey);
            sig.update(message.getBytes());
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return sig.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    // Decode public key from Base64 string
    public static PublicKey decodePublicKey(String publicKeyStr){
        byte[] decodedBytes = Base64.getDecoder().decode(publicKeyStr);
        KeyFactory keyFactory = null; // Change to "RSA" if using RSA
        try {
            keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePublic(new X509EncodedKeySpec(decodedBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Convert HashMap to a sorted & concatenated string
    public static String hashMapToSortedString(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();

        // Sort keys in natural (lexicographical) order
        List<String> sortedKeys = new ArrayList<>(map.keySet());
        Collections.sort(sortedKeys);

        // Concatenate key-value pairs in sorted order
        for (String key : sortedKeys) {
            sb.append(key).append("=").append(map.get(key)).append("&"); // "key=value&"
        }

        // Remove trailing "&" if present
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    }