package celtech.crypto;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class CryptoStoreTest
{

    public CryptoStoreTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

    /**
     * Test of decrypt method, of class CryptoFileStore.
     */
    @Test
    public void testEncryptDecryptIntegrity() throws Exception
    {
        System.out.println("Encrypt<->Decrypt integrity");

        CryptoFileStore instance = new CryptoFileStore("fred.dat");

        String stringToEncrypt = "hello world!";

        String encryptedString = instance.encrypt(stringToEncrypt);
        String secondAttemptAtencryptedString = instance.encrypt(stringToEncrypt);
        String decryptedString = instance.decrypt(encryptedString);

        String expectedEncryptionResult = "FiQvWsvqupGbOyDrf+/fxg==";
        assertEquals(encryptedString, secondAttemptAtencryptedString);
        assertEquals(expectedEncryptionResult, encryptedString);
        assertEquals(stringToEncrypt, decryptedString);
    }

    /**
     * Test that two different filenames cause two different encryptions to take place
     */
    @Test
    public void testEncryptDecryptUniqueness() throws Exception
    {
        System.out.println("Encrypt uniqueness");
        
        CryptoFileStore firstCryptoStore = new CryptoFileStore("fred.dat");
        CryptoFileStore secondCryptoStore = new CryptoFileStore("fred2.dat");

        String stringToEncrypt = "hello world!";

        String firstEncryption = firstCryptoStore.encrypt(stringToEncrypt);
        String secondEncryption = secondCryptoStore.encrypt(stringToEncrypt);
        String firstDecryptedString = firstCryptoStore.decrypt(firstEncryption);
        String secondDecryptedString = secondCryptoStore.decrypt(firstEncryption);

        assertNotSame(firstEncryption, secondEncryption);
        assertEquals(stringToEncrypt, firstDecryptedString);
        assertNotSame(stringToEncrypt, secondDecryptedString);
    }
}
