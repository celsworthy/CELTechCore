/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import celtech.roboxbase.printerControl.model.Printer;
import celtech.appManager.ModelContainerProject;
import java.util.Set;
import javafx.beans.binding.BooleanBinding;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class CanPrintConditionalTextBindings
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            CanPrintConditionalTextBindings.class.getName());

    private final ModelContainerProject project;
    private final Printer printer;

    public CanPrintConditionalTextBindings(ModelContainerProject project, Printer printer)
    {
        this.project = project;
        this.printer = printer;
    }

    /**
     * Binding to detect when filament 0 is required. Filament 0 is only NOT
     * required for two-extruder printers where all models are on extruder 1.
     */
    public BooleanBinding getFilament0Required()
    {
        return new BooleanBinding()
        {

            {
                super.bind(project.getExtruder0FilamentProperty(),
                        project.getExtruder1FilamentProperty(),
                        project.getTopLevelThings(),
                        project.getModelColourChanged() // USE USEEXTRUDERS ? as observable
                );
            }

            @Override
            protected boolean computeValue()
            {
                Set<Integer> usedExtruders = project.getUsedExtruders(printer);
                return usedExtruders.contains(0);
            }
        };
    }

    /**
     * Binding to detect when filament 1 is required.
     */
    public BooleanBinding getFilament1Required()
    {
        return new BooleanBinding()
        {

            {
                super.bind(project.getExtruder0FilamentProperty(),
                        project.getExtruder1FilamentProperty(),
                        project.getTopLevelThings(),
                        project.getModelColourChanged() // USE USEEXTRUDERS ? as observable
                );
            }

            @Override
            protected boolean computeValue()
            {
                Set<Integer> usedExtruders = project.getUsedExtruders(printer);
                return usedExtruders.contains(1);
            }
        };
    }
}
