/*
 * Copyright 2014 CEL UK
 */
package celtech.services.printing;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.appManager.ProjectMode;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintProfileContainer;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.coreUI.visualisation.importers.stl.STLImporter;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.Printer;
import celtech.printerControl.PrinterStatusEnumeration;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class PrintQueueTest
{

    static final int WAIT_INTERVAL = 500;
    static final int MAX_WAIT_INTERVAL = 3000;

    @Rule
    public JavaFXThreadingRule jfxRule = new JavaFXThreadingRule();

    /**
     * Test that progressProperty is 0 at start of print
     */
    @Test
    public void testProgressPropertyIsZeroAtStartOfPrint()
    {
        Properties testProperties = new Properties();
        testProperties.setProperty("language", "UK");
        ApplicationConfiguration.setInstallationProperties(testProperties);
        Lookup.initialise();

//        RoboxCommsManager singletonCommsManager = RoboxCommsManager.getInstance("");
        Printer testPrinter = new TestPrinter();
        PrintQueue printQueue = new PrintQueue(testPrinter);
        ReadOnlyDoubleProperty result = printQueue.progressProperty();
        assertEquals(0d, result.get(), 0.001);
    }

    /**
     * Test that progressProperty is 1 at end of print
     */
    @Test
    public void testProgressPropertyIsOneAtEndOfPrint() throws IOException, InterruptedException
    {
        Properties testProperties = new Properties();
        testProperties.setProperty("language", "UK");
        ApplicationConfiguration.setInstallationProperties(testProperties);
        Lookup.initialise();

        // force initialisation
        System.setProperty("libertySystems.configFile",
                           "/home/tony/CEL/AutoMaker/AutoMaker.configFile.xml");
        String installDir = ApplicationConfiguration.getApplicationInstallDirectory(Lookup.class);
        PrintProfileContainer.getInstance();

        Printer testPrinter = new TestPrinter();
        TestNotificationsHandler testNotificationsHandler = new TestNotificationsHandler();
        PrintQueue printQueue = new PrintQueue(testPrinter, testNotificationsHandler);

        STLImporter stlImporter = new STLImporter();
        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
        ModelLoadResult modelLoadResult = stlImporter.loadFile(null, "/home/tony/Downloads/pyramid1.stl", null, progressProperty);
        ObservableList<ModelContainer> modelList = FXCollections.observableArrayList(modelLoadResult.getModelContainer());
        Project project = new Project("abcdef", "Pyramid", modelList);
        project.setProjectMode(ProjectMode.MESH);

        RoboxProfile roboxProfile = PrintProfileContainer.getCompleteProfileList().get(0);
        printQueue.printProject(project, PrintQualityEnumeration.DRAFT, roboxProfile);

        int totalWaitTime = 0;
        while (true)
        {
            System.out.println("STATUS " + printQueue.getPrintStatus());
            if (PrinterStatusEnumeration.PRINTING.equals(printQueue.getPrintStatus()))
            {
                break;
            }
            Thread.sleep(WAIT_INTERVAL);
            totalWaitTime += WAIT_INTERVAL;
            if (totalWaitTime > MAX_WAIT_INTERVAL)
            {
                fail("Test print took too long");
            }
        }
        
        testPrinter.setPrintJobLineNumber(0);
        Thread.sleep(2000);
        testPrinter.setPrintJobLineNumber(1);
        ReadOnlyStringProperty etcFormatted = printQueue.progressAndETCProperty();
        System.out.println("ETCF " + etcFormatted);

        testPrinter.setPrintJobLineNumber(1633);

        ReadOnlyStringProperty result = printQueue.progressAndETCProperty();
        assertEquals("100% Elapsed Time (HH:MM) 00:00", result.get());
        ReadOnlyDoubleProperty progress = printQueue.progressProperty();
        assertEquals(1.0d, progress.get(), 0.001);
    }

}
