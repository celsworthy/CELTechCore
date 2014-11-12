package celtech.crypto;

import celtech.configuration.ApplicationConfiguration;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Ian
 */
public class CryptoFileStore
{

    private final Stenographer steno = StenographerFactory.getStenographer(CryptoFileStore.class.getName());
    private final String storeFileName;
    private final int keySize = 128;
    private final String password = "ab54vi'vSDDAS5r433jjk's#a";
    private String salt = "a5h4*jkhda'#:L";
    private final int passwordIterations = 5;
    private SecretKeyFactory factory = null;
    private SecretKey secretKey = null;
    private SecretKeySpec secret = null;
    private Cipher cipher = null;
    private byte[] ivBytes = null;
    private boolean initialised = false;
    private File storeFile = null;
    private final String cipherType = "AES/CBC/PKCS5Padding";

    public CryptoFileStore(String storeFileName)
    {
        this.storeFileName = storeFileName;
        salt = salt + storeFileName;
    }

    public String readFile()
    {
        String decryptedText = null;

        try
        {
            String encryptedBase64Text = FileUtils.readFileToString(storeFile, "UTF-8");

            decryptedText = decrypt(encryptedBase64Text);
        } catch (IOException ex)
        {
            steno.error("Error decrypting file " + storeFileName);
            ex.printStackTrace();
        }

        return decryptedText;
    }

    public void writeFile(String dataToEncrypt)
    {
        String encryptedData = encrypt(dataToEncrypt);

        if (encryptedData != null)
        {
            try
            {
                FileUtils.writeStringToFile(storeFile, encryptedData, "UTF-8", false);
            } catch (IOException ex)
            {
                steno.error("Error writing crypto file " + storeFileName);
            }
        }
    }

    protected String encrypt(String plainText)
    {
        String encryptedText = null;

        try
        {
            //get salt
            byte[] saltBytes = salt.getBytes("UTF-8");

            // Derive the key
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                saltBytes,
                passwordIterations,
                keySize
            );

            SecretKey secretKey = factory.generateSecret(spec);
            SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

            //encrypt the message
            Cipher cipher = Cipher.getInstance(cipherType);
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            AlgorithmParameters params = cipher.getParameters();
            ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
            byte[] encryptedTextBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
            encryptedText = new Base64().encodeAsString(encryptedTextBytes);
        } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException | InvalidKeySpecException | InvalidParameterSpecException | NoSuchAlgorithmException | NoSuchPaddingException | UnsupportedEncodingException ex)
        {
            steno.error("Error encrypting");
            ex.printStackTrace();
        }

        return encryptedText;
    }

    protected String decrypt(String encryptedText)
    {
        String decryptedText = null;

        try
        {
            byte[] saltBytes = salt.getBytes("UTF-8");
            byte[] encryptedTextBytes = new Base64().decodeBase64(encryptedText);

            // Derive the key
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                saltBytes,
                passwordIterations,
                keySize
            );

            SecretKey secretKey = factory.generateSecret(spec);
            SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

            // Decrypt the message
            Cipher cipher = Cipher.getInstance(cipherType);
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));

            byte[] decryptedTextBytes = null;
            try
            {
                decryptedTextBytes = cipher.doFinal(encryptedTextBytes);
            } catch (IllegalBlockSizeException e)
            {
                e.printStackTrace();
            } catch (BadPaddingException e)
            {
                e.printStackTrace();
            }

            decryptedText = new String(decryptedTextBytes);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException | UnsupportedEncodingException ex)
        {
            steno.error("Error decrypting");
            ex.printStackTrace();
        }

        return decryptedText;
    }
}
