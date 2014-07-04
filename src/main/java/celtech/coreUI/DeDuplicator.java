/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI;

import java.util.Collection;

/**
 *
 * @author tony
 */
public class DeDuplicator
{

    /**
     * If the given name is not already in the collection of currentNames then
     * return the given name unchanged. If the name exists in 
     * currentNames then return a changed name (based on the original name) that
     * does not exist in currentNames.
     *
     * @param name
     * @param currentNames
     * @return
     */
    public static String suggestNonDuplicateName(String name,
        Collection<String> currentNames)
    {
        int suffix = 0;
        String suggestedName = name;
        while (currentNames.contains(suggestedName))
        {
            suffix++;
            suggestedName =  name + suffix;
        }
        return suggestedName;
    }

}
