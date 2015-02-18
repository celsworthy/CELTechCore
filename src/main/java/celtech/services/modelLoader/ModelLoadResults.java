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
    private boolean relayout = false;

    public ModelLoadResults(List<ModelLoadResult> results, boolean relayout)
    {
        this.results = results;
        this.relayout = relayout;
    }

    /**
     * @return the results
     */
    public List<ModelLoadResult> getResults()
    {
        return results;
    }

    /**
     * @return the relayout
     */
    public boolean isRelayout()
    {
        return relayout;
    }
    
    
}
