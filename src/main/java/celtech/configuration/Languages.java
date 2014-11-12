/*
 * Copyright 2014 CEL UK
 */
package celtech.configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 *
 * @author tony
 */
public class Languages
{

    Set<Locale> locales;

    /**
     * Return a map of language_country to ResourceBundle. language_country should be either e.g.
     * "fr_CN" or "ru".
     *
     * @return
     */
    public Set<Locale> getLocales()
    {
        if (locales == null)
        {
            buildLocales();
        }

        return locales;
    }

    private void buildLocales()
    {
        locales = new HashSet<>();
        Pattern matchPattern = Pattern.compile(".*LanguageData_(.*)\\.properties");
        Collection<String> resources = ResourceList.getResources(matchPattern);
        for (String resource : resources)
        {
            Matcher matcher = matchPattern.matcher(resource);
            matcher.find();
            String languageCountry = matcher.group(1);
            Locale locale;
            if (languageCountry.contains("_"))
            {
                String language = languageCountry.split("_")[0];
                String country = languageCountry.split("_")[1];
                locale = new Locale(language, country);
            } else
            {
                locale = new Locale(languageCountry);
            }
            locales.add(locale);
        }
    }

    public Set<String> getlanguageNames()
    {
        Set<String> languageNames = new HashSet<>();
        for (Locale locale : getLocales())
        {
            languageNames.add(locale.getDisplayName());
        }
        return languageNames;
    }

    public static class ResourceList
    {

        /**
         * for all elements of java.class.path get a Collection of resources Pattern pattern =
         * Pattern.compile(".*"); gets all resources
         *
         * @param pattern the pattern to match
         * @return the resources in the order they are found
         */
        public static Collection<String> getResources(
            final Pattern pattern)
        {
            final ArrayList<String> retval = new ArrayList<String>();
            final String classPath = System.getProperty("java.class.path", ".");
            final String[] classPathElements = classPath.split(":");
            for (final String element : classPathElements)
            {
                retval.addAll(getResources(element, pattern));
            }
            return retval;
        }

        private static Collection<String> getResources(
            final String element,
            final Pattern pattern)
        {
            final ArrayList<String> retval = new ArrayList<String>();
            final File file = new File(element);
            if (file.isDirectory())
            {
                retval.addAll(getResourcesFromDirectory(file, pattern));
            } else
            {
                retval.addAll(getResourcesFromJarFile(file, pattern));
            }
            return retval;
        }

        private static Collection<String> getResourcesFromJarFile(
            final File file,
            final Pattern pattern)
        {
            final ArrayList<String> retval = new ArrayList<>();
            ZipFile zf;
            try
            {
                zf = new ZipFile(file);
            } catch (final ZipException e)
            {
                throw new Error(e);
            } catch (final IOException e)
            {
                throw new Error(e);
            }
            final Enumeration e = zf.entries();
            while (e.hasMoreElements())
            {
                final ZipEntry ze = (ZipEntry) e.nextElement();
                final String fileName = ze.getName();
                final boolean accept = pattern.matcher(fileName).matches();
                if (accept)
                {
                    retval.add(fileName);
                }
            }
            try
            {
                zf.close();
            } catch (final IOException e1)
            {
                throw new Error(e1);
            }
            return retval;
        }

        private static Collection<String> getResourcesFromDirectory(
            final File directory,
            final Pattern pattern)
        {
            final ArrayList<String> retval = new ArrayList<>();
            final File[] fileList = directory.listFiles();
            for (final File file : fileList)
            {
                if (file.isDirectory())
                {
                    retval.addAll(getResourcesFromDirectory(file, pattern));
                } else
                {
                    try
                    {
                        final String fileName = file.getCanonicalPath();
                        final boolean accept = pattern.matcher(fileName).matches();
                        if (accept)
                        {
                            retval.add(fileName);
                        }
                    } catch (final IOException e)
                    {
                        throw new Error(e);
                    }
                }
            }
            return retval;
        }

        /**
         * list the resources that match args[0]
         *
         * @param args args[0] is the pattern to match, or list all resources if there are no args
         */
        public static void main(final String[] args)
        {
            Pattern pattern;
            if (args.length < 1)
            {
                pattern = Pattern.compile(".*");
            } else
            {
                pattern = Pattern.compile(args[0]);
            }
            final Collection<String> list = ResourceList.getResources(pattern);
            for (final String name : list)
            {
                System.out.println(name);
            }
        }
    }

}
