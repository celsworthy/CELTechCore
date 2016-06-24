package celtech.coreUI.components;

import celtech.JavaFXConfiguredTest;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;
import static org.hamcrest.CoreMatchers.is;
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
public class HyperlinkedLabelTest extends JavaFXConfiguredTest
{
    public HyperlinkedLabelTest()
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
     * Test of replaceText method, of class HyperlinkedLabel.
     */
    @Test
    public void testReplaceText_plaintextOnly()
    {
        System.out.println("replaceText_plaintextOnly");
        String newText = "Some plain text";
        HyperlinkedLabel instance = new HyperlinkedLabel();
        instance.replaceText(newText);

        assertEquals(1, instance.getChildren().size());
        assertTrue(instance.getChildren().get(0) instanceof Text);
        assertThat(((Text) instance.getChildren().get(0)).getText(), is(newText));
    }

    /**
     * Test of replaceText method, of class HyperlinkedLabel.
     */
    @Test
    public void testReplaceText_plaintextAndHyperlink()
    {
        System.out.println("replaceText_plaintextAndHyperlink");
        String newText = "Robox firmware update <a href=\"https://robox.freshdesk.com/solution/categories/1000090870/folders/1000214277/articles/1000180224-the-filament-isn-t-moving-as-expected\">Other article</a>";
        String expectedTextContent = "Robox firmware update ";
        String expectedHyperlinkContent = "Other article";
        HyperlinkedLabel instance = new HyperlinkedLabel();
        instance.replaceText(newText);

        assertEquals(2, instance.getChildren().size());
        assertTrue(instance.getChildren().get(0) instanceof Text);
        assertTrue(instance.getChildren().get(1) instanceof Hyperlink);
        assertThat(((Text) instance.getChildren().get(0)).getText(), is(expectedTextContent));
        assertThat(((Hyperlink) instance.getChildren().get(1)).getText(), is(expectedHyperlinkContent));
    }

    /**
     * Test of replaceText method, of class HyperlinkedLabel.
     */
    @Test
    public void testReplaceText_plaintextAndTwoHyperlinks()
    {
        System.out.println("replaceText_plaintextAndTwoHyperlinks");
        String newText = "Robox firmware update <a href=\"https://robox.freshdesk.com/support/home\">Robox solutions</a>more text<a href=\"https://robox.freshdesk.com/solution/categories/1000090870/folders/1000214277/articles/1000180224-the-filament-isn-t-moving-as-expected\">Other article</a>";

        String expectedTextContent1 = "Robox firmware update ";
        String expectedTextContent2 = "more text";
        String expectedHyperlinkContent1 = "Robox solutions";
        String expectedHyperlinkContent2 = "Other article";
        HyperlinkedLabel instance = new HyperlinkedLabel();
        instance.replaceText(newText);

        assertEquals(4, instance.getChildren().size());
        assertTrue(instance.getChildren().get(0) instanceof Text);
        assertTrue(instance.getChildren().get(1) instanceof Hyperlink);
        assertTrue(instance.getChildren().get(2) instanceof Text);
        assertTrue(instance.getChildren().get(3) instanceof Hyperlink);
        assertThat(((Text) instance.getChildren().get(0)).getText(), is(expectedTextContent1));
        assertThat(((Hyperlink) instance.getChildren().get(1)).getText(), is(expectedHyperlinkContent1));
        assertThat(((Text) instance.getChildren().get(2)).getText(), is(expectedTextContent2));
        assertThat(((Hyperlink) instance.getChildren().get(3)).getText(), is(expectedHyperlinkContent2));
    }

    /**
     * Test of replaceText method, of class HyperlinkedLabel.
     */
    @Test
    public void testReplaceText_HyperlinkOnly()
    {
        System.out.println("replaceText_HyperlinkOnly");
        String newText = "<a href=\"https://robox.freshdesk.com/solution/categories/1000090870/folders/1000214277/articles/1000180224-the-filament-isn-t-moving-as-expected\">Other article</a>";
        String expectedHyperlinkContent = "Other article";
        HyperlinkedLabel instance = new HyperlinkedLabel();
        instance.replaceText(newText);

        assertEquals(1, instance.getChildren().size());
        assertTrue(instance.getChildren().get(0) instanceof Hyperlink);
        assertThat(((Hyperlink) instance.getChildren().get(0)).getText(), is(
                   expectedHyperlinkContent));
    }
}
