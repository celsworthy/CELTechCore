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
    private ApplicationEnvironment applicationEnvironment;

    /**
     * @return the applicationEnvironment
     */
    public static ApplicationEnvironment getApplicationEnvironment()
    {
        return instance.applicationEnvironment;
    }

    /**
     * @param applicationEnvironment the applicationEnvironment to set
     */
    public static void setApplicationEnvironment(ApplicationEnvironment applicationEnvironment)
    {
        instance.applicationEnvironment = applicationEnvironment;
    }

    private Lookup()
    {
        Locale appLocale = Locale.forLanguageTag(ApplicationConfiguration.getApplicationInstallationLanguage());
        ResourceBundle i18nBundle = ResourceBundle.getBundle("celtech.resources.i18n.LanguageData", appLocale);
        applicationEnvironment = new ApplicationEnvironment(i18nBundle, appLocale);
        
    }

    public static void initialise()
    {
        instance = new Lookup();
    }
}
