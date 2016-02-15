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
    private boolean shouldCentre = true;

    public ModelLoadResults()
    {
    }

    public ModelLoadResults(List<ModelLoadResult> results)
    {
        this.results = results;
    }

    public void setResults(List<ModelLoadResult> results)
    {
        this.results = results;
    }

    public List<ModelLoadResult> getResults()
    {
        return results;
    }

    public void setShouldCentre(boolean shouldCentre)
    {
        this.shouldCentre = shouldCentre;
    }

    public boolean isShouldCentre()
    {
        return shouldCentre;
    }

}
