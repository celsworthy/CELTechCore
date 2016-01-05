/*
 * Copyright 2014 CEL UK
 */

package celtech.services.modelLoader;

import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
import java.util.List;

/**
 *
 * @author tony
 */
public class ModelLoadResults
{
    private List<ModelLoadResult> results;

    public ModelLoadResults(List<ModelLoadResult> results)
    {
        this.results = results;
    }

    public List<ModelLoadResult> getResults()
    {
        return results;
    }

}
