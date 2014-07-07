/*
 * Copyright 2014 CEL UK
 */
package celtech;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.ApplicationEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

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
        ResourceBundle i18nBundle = ResourceBundle.getBundle("celtech.resources.i18n.LanguageData", appLocale, new UTF8Control());
        applicationEnvironment = new ApplicationEnvironment(i18nBundle, appLocale);
    }

    public static void initialise()
    {
        instance = new Lookup();
    }

    private class UTF8Control extends Control
    {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException
        {
            // The below is a copy of the default implementation.
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload)
            {
                URL url = loader.getResource(resourceName);
                if (url != null)
                {
                    URLConnection connection = url.openConnection();
                    if (connection != null)
                    {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else
            {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null)
            {
                try
                {
                    // Only this line is changed to make it to read properties files as UTF-8.
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
                } finally
                {
                    stream.close();
                }
            }
            return bundle;
        }
    }
}
