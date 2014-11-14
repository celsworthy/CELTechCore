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
    public void testEncryptDecrypt() throws Exception
    {
        System.out.println("decrypt");
        
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
}
