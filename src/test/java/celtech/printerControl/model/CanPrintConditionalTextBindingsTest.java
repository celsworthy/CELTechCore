/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import celtech.roboxbase.printerControl.model.Printer;
import celtech.JavaFXConfiguredTest;
import celtech.Lookup;
import celtech.appManager.ModelContainerProject;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.HeadContainer;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import celtech.modelcontrol.ModelContainer;
import celtech.roboxbase.BaseLookup;
import java.io.File;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class CanPrintConditionalTextBindingsTest extends JavaFXConfiguredTest
{

    private Filament PURPLE;
    private Filament WHITE;
    private Filament GREEN;

    @Before
    public void setupFilaments()
    {
        PURPLE = BaseLookup.getFilamentContainer().getFilamentByID("RBX-ABS-PP156");
        WHITE = BaseLookup.getFilamentContainer().getFilamentByID("RBX-ABS-WH169");
        GREEN = BaseLookup.getFilamentContainer().getFilamentByID("RBX-ABS-GR499");
    }

    private ModelContainer makeModelContainer(boolean useExtruder0)
    {
        MeshView meshView = new MeshView(new Shape3DRectangle(2, 3));
        ModelContainer modelContainer = new ModelContainer(new File("testModel"), meshView);
        modelContainer.setUseExtruder0(useExtruder0);
        return modelContainer;
    }

    private Printer makeOneExtruderPrinter()
    {
        Printer printer = new TestPrinter(1);
        return printer;
    }

    private Printer makeOneExtruderSMHeadPrinter()
    {
        TestPrinter printer = new TestPrinter(1);
        printer.addHeadForHeadFile(HeadContainer.getHeadByID("RBX01-SM"));
        printer.overrideFilament(0, PURPLE);
        printer.loadFilament(0);
        return printer;
    }

    private Printer makeTwoExtruderPrinter()
    {
        Printer printer = new TestPrinter(2);
        return printer;
    }

    private Printer makeTwoExtruderDMHeadPrinter()
    {
        TestPrinter printer = new TestPrinter(2);
        printer.addHeadForHeadFile(HeadContainer.getHeadByID("RBX01-DM"));
        printer.overrideFilament(0, PURPLE);
        printer.overrideFilament(1, GREEN);
        printer.loadFilament(0);
        printer.loadFilament(1);
        return printer;
    }

    private ModelContainerProject makeOneModelProject(Filament projectFil0, Filament projectFil1)
    {
        ModelContainerProject project = new ModelContainerProject();
        ModelContainer modelContainer = makeModelContainer(true);
        project.addModel(modelContainer);

        project.setExtruder0Filament(projectFil0);
        project.setExtruder1Filament(projectFil1);

        return project;
    }
    
    private ModelContainerProject makeTwoModelProject(Filament projectFil0, Filament projectFil1)
    {
        ModelContainerProject project = new ModelContainerProject();
        ModelContainer modelContainer = makeModelContainer(true);
        project.addModel(modelContainer);
        
        ModelContainer modelContainer2 = makeModelContainer(false);
        project.addModel(modelContainer2);

        project.setExtruder0Filament(projectFil0);
        project.setExtruder1Filament(projectFil1);

        return project;
    }    

    /**
     * One printer extruder, PURPLE. One model on extruder 0. Project setting PURPLE, WHITE.
     */
    @Test
    public void testOneExtruderOneModelExtruder0_1()
    {

        ModelContainerProject project = makeOneModelProject(PURPLE, WHITE);
        Printer printer = makeOneExtruderSMHeadPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        assertTrue(filament0Reqd.get());
        assertFalse(filament1Reqd.get());
    }

    /**
     * One printer extruder, PURPLE. One model on extruder 0. Project setting GREEN, WHITE.
     */
    @Test
    public void testOneExtruderOneModelExtruder0_2()
    {

        ModelContainerProject project = makeOneModelProject(GREEN, WHITE);
        Printer printer = makeOneExtruderSMHeadPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();

        assertTrue(filament0Reqd.get());
        assertFalse(filament1Reqd.get());
    }

    /**
     * One printer extruder, PURPLE. One model on extruder 1. Project setting PURPLE, WHITE.
     */
    @Test
    public void testOneExtruderOneModelExtruder1_1()
    {

        ModelContainerProject project = makeOneModelProject(PURPLE, WHITE);
        Printer printer = makeOneExtruderSMHeadPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        ((ModelContainer)project.getTopLevelThings().get(0)).setUseExtruder0(true);
        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        assertTrue(filament0Reqd.get());
        assertFalse(filament1Reqd.get());
    }

    /**
     * Two printer extruders, PURPLE, WHITE. One model on extruder 1. Project setting PURPLE, WHITE.
     */
    @Test
    public void testTwoExtruderOneModelExtruder1_1()
    {

        ModelContainerProject project = makeOneModelProject(PURPLE, WHITE);
        Printer printer = makeTwoExtruderDMHeadPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);
        project.getPrinterSettings().setPrintSupportTypeOverride(SlicerParametersFile.SupportType.MATERIAL_2);

        ((ModelContainer)project.getTopLevelThings().get(0)).setUseExtruder0(false);
        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        // filament 0 not required because model is not on extruder 0
        assertFalse(filament0Reqd.get());
        assertTrue(filament1Reqd.get());
    }

    /**
     * Two printer extruders, PURPLE, WHITE. One model on extruder 0. Project setting PURPLE, WHITE.
     */
    @Test
    public void testTwoExtruderOneModelExtruder0_1()
    {

        ModelContainerProject project = makeOneModelProject(PURPLE, WHITE);
        Printer printer = makeTwoExtruderDMHeadPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        assertTrue(filament0Reqd.get());
        assertFalse(filament1Reqd.get());
    }
    
    /**
     * Two printer extruders, PURPLE, WHITE. One model on extruder 0, one on extruder 1.
     * Project setting PURPLE, WHITE.
     */
    @Test
    public void testTwoExtruderTwoModels_1()
    {

        ModelContainerProject project = makeTwoModelProject(PURPLE, WHITE);
        Printer printer = makeTwoExtruderDMHeadPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        assertTrue(filament0Reqd.get());
        assertTrue(filament1Reqd.get());
    }    
    
    /**
     * Two printer extruders, PURPLE, WHITE. One model on extruder 0, one on extruder 1.
     * Project setting GREEN, WHITE.
     */
    @Test
    public void testTwoExtruderTwoModels_2()
    {

        ModelContainerProject project = makeTwoModelProject(GREEN, WHITE);
        Printer printer = makeTwoExtruderDMHeadPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        assertTrue(filament0Reqd.get());
        assertTrue(filament1Reqd.get());
    }      
    
    /**
     * Two printer extruders, PURPLE, WHITE. One model on extruder 0, one on extruder 1.
     * Project setting PURPLE, GREEN.
     */
    @Test
    public void testTwoExtruderTwoModels_3()
    {

        ModelContainerProject project = makeTwoModelProject(PURPLE, GREEN);
        Printer printer = makeTwoExtruderDMHeadPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        assertTrue(filament0Reqd.get());
        assertTrue(filament1Reqd.get());
    }     

    class Shape3DRectangle extends TriangleMesh
    {

        public Shape3DRectangle(float Width, float Height)
        {
            float[] points =
            {
                -Width / 2, Height / 2, 0, // idx p0
                -Width / 2, -Height / 2, 0, // idx p1
                Width / 2, Height / 2, 0, // idx p2
                Width / 2, -Height / 2, 0  // idx p3
            };
            float[] texCoords =
            {
                1, 1, // idx t0
                1, 0, // idx t1
                0, 1, // idx t2
                0, 0  // idx t3
            };

            int[] faces =
            {
                2, 3, 0, 2, 1, 0,
                2, 3, 1, 0, 3, 1
            };

            this.getPoints().setAll(points);
            this.getTexCoords().setAll(texCoords);
            this.getFaces().setAll(faces);
        }
    }

}
