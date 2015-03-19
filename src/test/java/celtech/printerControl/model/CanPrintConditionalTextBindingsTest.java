/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import celtech.JavaFXConfiguredTest;
import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.modelcontrol.ModelContainer;
import celtech.utils.TestPrinter;
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
        PURPLE = FilamentContainer.getFilamentByID("RBX-ABS-PP156");
        WHITE = FilamentContainer.getFilamentByID("RBX-ABS-WH169");
        GREEN = FilamentContainer.getFilamentByID("RBX-ABS-GR499");
    }

    private ModelContainer makeModelContainer(boolean useExtruder0)
    {
        MeshView meshView = new MeshView(new Shape3DRectangle(2, 3));
        ModelContainer modelContainer = new ModelContainer("testModel", meshView);
        modelContainer.setUseExtruder0Filament(useExtruder0);
        return modelContainer;
    }

    private Printer makeOneExtruderPrinter()
    {
        Printer printer = new TestPrinter(1);
        return printer;
    }

    private Printer makeTwoExtruderPrinter()
    {
        Printer printer = new TestPrinter(2);
        return printer;
    }

    private Project makeOneModelProject(Filament projectFil0, Filament projectFil1)
    {
        Project project = new Project();
        ModelContainer modelContainer = makeModelContainer(true);
        project.addModel(modelContainer);

        project.setExtruder0Filament(projectFil0);
        project.setExtruder1Filament(projectFil1);

        project.getPrinterSettings().setFilament0(PURPLE);
        project.getPrinterSettings().setFilament1(WHITE);

        return project;
    }
    
    private Project makeTwoModelProject(Filament projectFil0, Filament projectFil1)
    {
        Project project = new Project();
        ModelContainer modelContainer = makeModelContainer(true);
        project.addModel(modelContainer);
        
        ModelContainer modelContainer2 = makeModelContainer(false);
        project.addModel(modelContainer2);

        project.setExtruder0Filament(projectFil0);
        project.setExtruder1Filament(projectFil1);

        project.getPrinterSettings().setFilament0(PURPLE);
        project.getPrinterSettings().setFilament1(WHITE);

        return project;
    }    

    /**
     * One printer extruder, PURPLE. One model on extruder 0. Project setting PURPLE, WHITE.
     */
    @Test
    public void testOneExtruderOneModelExtruder0_1()
    {

        Project project = makeOneModelProject(PURPLE, WHITE);
        Printer printer = makeOneExtruderPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        BooleanBinding extruder0FilamentMismatch = conditionalTextBindings.getExtruder0FilamentMismatch();
        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        assertFalse(extruder0FilamentMismatch.get());
        assertTrue(filament0Reqd.get());
        assertFalse(filament1Reqd.get());
    }

    /**
     * One printer extruder, PURPLE. One model on extruder 0. Project setting GREEN, WHITE.
     */
    @Test
    public void testOneExtruderOneModelExtruder0_2()
    {

        Project project = makeOneModelProject(GREEN, WHITE);
        Printer printer = makeOneExtruderPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        BooleanBinding extruder0FilamentMismatch = conditionalTextBindings.getExtruder0FilamentMismatch();
        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();

        assertTrue(extruder0FilamentMismatch.get());
        assertTrue(filament0Reqd.get());
        assertFalse(filament1Reqd.get());
    }

    /**
     * One printer extruder, PURPLE. One model on extruder 1. Project setting PURPLE, WHITE.
     */
    @Test
    public void testOneExtruderOneModelExtruder1_1()
    {

        Project project = makeOneModelProject(PURPLE, WHITE);
        Printer printer = makeOneExtruderPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        project.getLoadedModels().get(0).setUseExtruder0Filament(false);
        BooleanBinding extruder0FilamentMismatch = conditionalTextBindings.getExtruder0FilamentMismatch();
        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        assertTrue(extruder0FilamentMismatch.get());
        assertTrue(filament0Reqd.get());
        assertFalse(filament1Reqd.get());
    }

    /**
     * Two printer extruders, PURPLE, WHITE. One model on extruder 1. Project setting PURPLE, WHITE.
     */
    @Test
    public void testTwoExtruderOneModelExtruder1_1()
    {

        Project project = makeOneModelProject(PURPLE, WHITE);
        Printer printer = makeTwoExtruderPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        project.getLoadedModels().get(0).setUseExtruder0Filament(false);
        BooleanBinding extruder0FilamentMismatch = conditionalTextBindings.getExtruder0FilamentMismatch();
        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        assertTrue(extruder0FilamentMismatch.get());
        assertFalse(filament0Reqd.get());
        assertTrue(filament1Reqd.get());
    }

    /**
     * Two printer extruders, PURPLE, WHITE. One model on extruder 0. Project setting PURPLE, WHITE.
     */
    @Test
    public void testTwoExtruderOneModelExtruder0_1()
    {

        Project project = makeOneModelProject(PURPLE, WHITE);
        Printer printer = makeTwoExtruderPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        BooleanBinding extruder0FilamentMismatch = conditionalTextBindings.getExtruder0FilamentMismatch();
        BooleanBinding extruder1FilamentMismatch = conditionalTextBindings.getExtruder1FilamentMismatch();
        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        assertFalse(extruder0FilamentMismatch.get());
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

        Project project = makeTwoModelProject(PURPLE, WHITE);
        Printer printer = makeTwoExtruderPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        BooleanBinding extruder0FilamentMismatch = conditionalTextBindings.getExtruder0FilamentMismatch();
        BooleanBinding extruder1FilamentMismatch = conditionalTextBindings.getExtruder1FilamentMismatch();
        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        assertFalse(extruder0FilamentMismatch.get());
        assertFalse(extruder1FilamentMismatch.get());
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

        Project project = makeTwoModelProject(GREEN, WHITE);
        Printer printer = makeTwoExtruderPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        BooleanBinding extruder0FilamentMismatch = conditionalTextBindings.getExtruder0FilamentMismatch();
        BooleanBinding extruder1FilamentMismatch = conditionalTextBindings.getExtruder1FilamentMismatch();
        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        assertTrue(extruder0FilamentMismatch.get());
        assertFalse(extruder1FilamentMismatch.get());
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

        Project project = makeTwoModelProject(PURPLE, GREEN);
        Printer printer = makeTwoExtruderPrinter();
        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);

        BooleanBinding extruder0FilamentMismatch = conditionalTextBindings.getExtruder0FilamentMismatch();
        BooleanBinding extruder1FilamentMismatch = conditionalTextBindings.getExtruder1FilamentMismatch();
        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();
        assertFalse(extruder0FilamentMismatch.get());
        assertTrue(extruder1FilamentMismatch.get());
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
