
package celtech.coreUI.components.tips;

import javafx.beans.binding.BooleanBinding;

/**
 *
 * @author Ian
 */
public class ConditionalText
{
    private BooleanBinding appearanceCondition;
    private String i18nText;

    public ConditionalText(String i18nText, BooleanBinding appearanceCondition)
    {
        this.i18nText = i18nText;
        this.appearanceCondition = appearanceCondition;
    }

    public BooleanBinding getAppearanceCondition()
    {
        return appearanceCondition;
    }

    public void setAppearanceCondition(BooleanBinding appearanceCondition)
    {
        this.appearanceCondition = appearanceCondition;
    }

    public String getI18nText()
    {
        return i18nText;
    }

    public void setI18nText(String i18nText)
    {
        this.i18nText = i18nText;
    }
}
