/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.gcodetranslator;

import celtech.configuration.SlicerType;

/**
 *
 * @author Ian
 */
public enum ExtrusionTask
{

    Skirt("skirt", "skirt", "TYPE:SKIRT"),
    Perimeter("perimeter", "perimeter", "TYPE:WALL-INNER"),
    ExternalPerimeter("externalPerimeter", "N/A-not-included-in-slic3r", "TYPE:WALL-OUTER"),
    Support("support", "support", "TYPE:SUPPORT"),
    Support_Interface("supportInterface", "N/A-not-included-in-slic3r", "N/A-not-included-in-cura"),
    Fill("fill", "infill", "TYPE:FILL");

    private String genericLabelInGCode;
    private String slic3rLabelInGCode;
    private String curaLabelInGCode;

    private ExtrusionTask(String genericLabelInGCode, String slic3rLabelInGCode, String curaLabelInGCode)
    {
        this.genericLabelInGCode = genericLabelInGCode;
        this.slic3rLabelInGCode = slic3rLabelInGCode;
        this.curaLabelInGCode = curaLabelInGCode;
    }

    /**
     *
     * @return
     */
    public String getGenericLabel()
    {
        return genericLabelInGCode;
    }

    /**
     *
     * @param slicerType
     * @return
     */
    public String getSlicerSpecificLabel(SlicerType slicerType)
    {
        String label = null;

        switch (slicerType)
        {
            case Cura:
                label = curaLabelInGCode;
                break;
            case Slic3r:
                label = slic3rLabelInGCode;
                break;
        }

        return label;
    }

    /**
     *
     * @param slicerType - if null then the generic string is searched for
     * @param stringToSearch
     * @return
     */
    public static ExtrusionTask lookupExtrusionTaskFromComment(SlicerType slicerType, String stringToSearch)
    {
        ExtrusionTask foundExtrusionTask = null;

        if (stringToSearch != null)
        {
            for (ExtrusionTask task : ExtrusionTask.values())
            {
                String labelToSearchFor = null;

                if (slicerType != null)
                {
                    labelToSearchFor = task.getSlicerSpecificLabel(slicerType);
                } else
                {
                    labelToSearchFor = task.getGenericLabel();
                }

                if (stringToSearch.contains(labelToSearchFor))
                {
                    foundExtrusionTask = task;
                    break;
                }
            }
        }

        return foundExtrusionTask;
    }
}
