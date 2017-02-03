package celtech.coreUI.components.material;

import celtech.JavaFXConfiguredTest;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.datafileaccessors.FilamentContainer;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class FilamentMenuButtonTest extends JavaFXConfiguredTest
{

    @Test
    public void testCategoryComparator()
    {
        FilamentMenuButton componentUnderTest = new FilamentMenuButton();
        Filament roboxCategory = FilamentContainer.UNKNOWN_FILAMENT.clone();
        roboxCategory.setCategory("Robox");
        Filament aCategory = FilamentContainer.UNKNOWN_FILAMENT.clone();
        aCategory.setCategory("A Category");
        Filament zCategory = FilamentContainer.UNKNOWN_FILAMENT.clone();
        zCategory.setCategory("Z Category");
        Filament customCategory = FilamentContainer.UNKNOWN_FILAMENT.clone();
        customCategory.setCategory("Custom");

        //Basic alpha sort check
        assertTrue(componentUnderTest.byCategory.compare(aCategory, zCategory) < 0);
        assertTrue(componentUnderTest.byCategory.compare(aCategory, aCategory) == 0);
        assertTrue(componentUnderTest.byCategory.compare(zCategory, aCategory) > 0);
        //Check Robox always comes first
        assertTrue(componentUnderTest.byCategory.compare(roboxCategory, zCategory) < 0);
        assertTrue(componentUnderTest.byCategory.compare(roboxCategory, aCategory) < 0);
        assertTrue(componentUnderTest.byCategory.compare(zCategory, roboxCategory) > 0);
        assertTrue(componentUnderTest.byCategory.compare(aCategory, roboxCategory) > 0);
        assertTrue(componentUnderTest.byCategory.compare(customCategory, roboxCategory) > 0);
        assertTrue(componentUnderTest.byCategory.compare(roboxCategory, roboxCategory) == 0);
        //Check Custom always comes last
        assertTrue(componentUnderTest.byCategory.compare(customCategory, zCategory) > 0);
        assertTrue(componentUnderTest.byCategory.compare(customCategory, aCategory) > 0);
        assertTrue(componentUnderTest.byCategory.compare(zCategory, customCategory) < 0);
        assertTrue(componentUnderTest.byCategory.compare(aCategory, customCategory) < 0);
        assertTrue(componentUnderTest.byCategory.compare(customCategory, customCategory) == 0);
    }

    @Test
    public void testCategoryByNameComparator()
    {
        FilamentMenuButton componentUnderTest = new FilamentMenuButton();
        String roboxCategory = "Robox";
        String aCategory = "A Category";
        String zCategory = "Z Category";
        String customCategory = "Custom";

        //Basic alpha sort check
        assertTrue(componentUnderTest.byCategoryName.compare(aCategory, zCategory) < 0);
        assertTrue(componentUnderTest.byCategoryName.compare(aCategory, aCategory) == 0);
        assertTrue(componentUnderTest.byCategoryName.compare(zCategory, aCategory) > 0);
        //Check Robox always comes first
        assertTrue(componentUnderTest.byCategoryName.compare(roboxCategory, zCategory) < 0);
        assertTrue(componentUnderTest.byCategoryName.compare(roboxCategory, aCategory) < 0);
        assertTrue(componentUnderTest.byCategoryName.compare(zCategory, roboxCategory) > 0);
        assertTrue(componentUnderTest.byCategoryName.compare(aCategory, roboxCategory) > 0);
        assertTrue(componentUnderTest.byCategoryName.compare(customCategory, roboxCategory) > 0);
        assertTrue(componentUnderTest.byCategoryName.compare(roboxCategory, roboxCategory) == 0);
        //Check Custom always comes last
        assertTrue(componentUnderTest.byCategoryName.compare(customCategory, zCategory) > 0);
        assertTrue(componentUnderTest.byCategoryName.compare(customCategory, aCategory) > 0);
        assertTrue(componentUnderTest.byCategoryName.compare(zCategory, customCategory) < 0);
        assertTrue(componentUnderTest.byCategoryName.compare(aCategory, customCategory) < 0);
        assertTrue(componentUnderTest.byCategoryName.compare(customCategory, customCategory) == 0);
    }

}
