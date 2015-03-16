/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import celtech.appManager.Project;
import celtech.configuration.Filament;
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
    
    private final Project project;
    private final Printer printer;

    public CanPrintConditionalTextBindings(Project project, Printer printer)
    {
        this.project = project;
        this.printer = printer;
    }
    
    /**
     * Create a binding for when project filament 0 does not match printer filament 0.
     */
    public BooleanBinding getExtruder0FilamentMismatch() {
        return new BooleanBinding()
        {
            {
                // We don't need to include printer in the bindings dependencies because
                // this method is called whenever the printer reel is added or removed
                //TODO check for programming reel
                super.bind(project.getExtruder0FilamentProperty(),
                           project.getExtruder1FilamentProperty(),
                           project.getLoadedModels(),
                           project.getModelColourChanged(), // USE USEEXTRUDERS ? as observable
                           project.getPrinterSettings().getFilament0Property(),
                           project.getPrinterSettings().getFilament1Property());
            }

            @Override
            protected boolean computeValue()
            {
                steno.debug("Recompute conditional text reqd binding");
                boolean filamentMismatch = true;

                Filament printerFilament = project.getPrinterSettings().getFilament0Property().get();
                Set<Integer> usedExtruders = project.getUsedExtruders();

                if (!printer.extrudersProperty().get(1).isFittedProperty().get())
                {
                    // only one extruder on the printer
                    steno.debug("One Extruder Only");
                    steno.debug("Printer Settings filament 0 is "
                        + project.getPrinterSettings().getFilament0Property());

                    if (usedExtruders.contains(0))
                    {
                        steno.debug("extruder 0 is being used");
                        Filament usedFilament = project.getExtruder0FilamentProperty().get();
                        if (usedFilament.equals(printerFilament))
                        {
                            steno.debug("used filament 0 matches printer filament 0");
                            filamentMismatch = false;
                        }
                    }

                    if (usedExtruders.contains(1))
                    {
                        steno.debug("extruder 1 is being used");
                        Filament usedFilament = project.getExtruder1FilamentProperty().get();
                        if (usedFilament.equals(printerFilament))
                        {
                            steno.debug(
                                "used filament 1 matches printer filament 0 for single extruder printer");
                            filamentMismatch = false;
                        }
                    }
                } else
                {
                    // two extruders on printer, just check extruder 0
                    if (usedExtruders.contains(0))
                    {
                        steno.debug("extruder 0 is being used");
                        Filament usedFilament = project.getExtruder0FilamentProperty().get();
                        if (usedFilament.equals(printerFilament))
                        {
                            steno.debug("used filament 0 matches printer filament 0");
                            filamentMismatch = false;
                        }
                    }
                }
                steno.debug("mismatch on 0 detected: " + filamentMismatch);
                return filamentMismatch;
            }
        };

    }
    
    /**
     * Create a binding for when project filament 1 does not match printer filament 1.
     */    
    public BooleanBinding getExtruder1FilamentMismatch() {
        return new BooleanBinding()
        {
            {
                // We don't need to include printer in the bindings dependencies because
                // this method is called whenever the printer reel is added or removed
                //TODO check for programming reel
                super.bind(project.getExtruder0FilamentProperty(),
                           project.getExtruder1FilamentProperty(),
                           project.getLoadedModels(),
                           project.getModelColourChanged(), // USE USEEXTRUDERS ? as observable
                           project.getPrinterSettings().getFilament0Property(),
                           project.getPrinterSettings().getFilament1Property());
            }

            @Override
            protected boolean computeValue()
            {
                steno.debug("Recompute conditional text reqd binding (1)");
                boolean filamentMismatch = true;
                Filament printerFilament = project.getPrinterSettings().getFilament1Property().get();
                Set<Integer> usedExtruders = project.getUsedExtruders();

                if (usedExtruders.contains(1))
                {
                    steno.debug("extruder 1 is being used");
                    Filament usedFilament = project.getExtruder1FilamentProperty().get();
                    steno.debug("printer 1 filament " + printerFilament);
                    steno.debug("project 1 filament " + usedFilament);
                    if (usedFilament.equals(printerFilament))
                    {
                        steno.debug("used filament 1 matches printer filament 1");
                        filamentMismatch = false;
                    }
                }
                steno.debug("mismatch on 1 detected: " + filamentMismatch);
                return filamentMismatch;
            }
        };
    }
    
    /**
     * Binding to detect when filament 0 is required. Filament 0 is only NOT required
     * for two-extruder printers where all models are on extruder 1.
     */
    public BooleanBinding getFilament0Required() {
        return new BooleanBinding()
        {

            {
                super.bind(project.getExtruder0FilamentProperty(),
                           project.getExtruder1FilamentProperty(),
                           project.getLoadedModels(),
                           project.getModelColourChanged(), // USE USEEXTRUDERS ? as observable
                           project.getPrinterSettings().getFilament0Property(),
                           project.getPrinterSettings().getFilament1Property());
            }

            @Override
            protected boolean computeValue()
            {
                Set<Integer> usedExtruders = project.getUsedExtruders();
                if (printer.extrudersProperty().get(1).isFittedProperty().get() && 
                    ! usedExtruders.contains(0)) {
                    return false;
                }
                return true;
            }
        };        
    }
    
     /**
     * Binding to detect when filament 1 is required.
     */
    public BooleanBinding getFilament1Required() {
        return new BooleanBinding()
        {

            {
                super.bind(project.getExtruder0FilamentProperty(),
                           project.getExtruder1FilamentProperty(),
                           project.getLoadedModels(),
                           project.getModelColourChanged(), // USE USEEXTRUDERS ? as observable
                           project.getPrinterSettings().getFilament0Property(),
                           project.getPrinterSettings().getFilament1Property());
            }

            @Override
            protected boolean computeValue()
            {
                Set<Integer> usedExtruders = project.getUsedExtruders();
                if (!printer.extrudersProperty().get(1).isFittedProperty().get()) {
                    return false;
                }
                return usedExtruders.contains(1);
            }
        };
    }
}
