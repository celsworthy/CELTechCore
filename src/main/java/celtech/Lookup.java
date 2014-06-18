/*
 * Copyright 2014 CEL UK
 */
package celtech;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.ApplicationEnvironment;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 *
 * @author tony
 */
public class Lookup
{

    private static Lookup instance;
    private static ApplicationEnvironment applicationEnvironment;

    /**
     * @return the applicationEnvironment
     */
    public static ApplicationEnvironment getApplicationEnvironment()
    {
        return applicationEnvironment;
    }

    /**
     * @param applicationEnvironment the applicationEnvironment to set
     */
    public static void setApplicationEnvironment(ApplicationEnvironment applicationEnvironment)
    {
        applicationEnvironment = applicationEnvironment;
    }

    private Lookup()
    {
        Locale appLocale = Locale.forLanguageTag(ApplicationConfiguration.getApplicationLanguage());
        ResourceBundle i18nBundle = ResourceBundle.getBundle("celtech.resources.i18n.LanguageData", appLocale);
        applicationEnvironment = new ApplicationEnvironment(i18nBundle, appLocale);
        
    }

    public static void initialise()
    {
        instance = new Lookup();
    }
}
