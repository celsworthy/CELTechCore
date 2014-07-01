/*
 * Copyright 2014 CEL UK
 */
package celtech.services.printing;

import celtech.JavaFXConfiguredTest;
import celtech.appManager.Project;
import celtech.appManager.ProjectMode;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintProfileContainer;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.coreUI.visualisation.importers.stl.STLImporter;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.PrinterStatusEnumeration;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;

/**
 *
 * @author tony
 */
public class PrintQueueTest extends JavaFXConfiguredTest
{

    static final int WAIT_INTERVAL = 500;
    static final int MAX_WAIT_INTERVAL = 3000;
    static final String DRAFT_SETTINGS = "DraftSettings";

    TestPrinter testPrinter;
    PrintQueue printQueue;
    Project project;

//    @Rule
//    public JavaFXThreadingRule jfxRule = new JavaFXThreadingRule();

    /**
     * Test that progressProperty is 0 at start of print
     */
//    @Test
    public void testProgressPropertyIsZeroAtStartOfPrint()
    {
        testPrinter = new TestPrinter();
        printQueue = new PrintQueue(testPrinter);
        ReadOnlyDoubleProperty result = printQueue.progressProperty();
        assertEquals(0d, result.get(), 0.001);
    }

    @Before
    public void setupPrintQueue()
    {
        testPrinter = new TestPrinter();
        testPrinter.setBedTargetTemperature(120);
        testPrinter.setBedTemperature(120);

        TestListFilesResponse listFilesResponse = new TestListFilesResponse();
        testPrinter.setListFilesResonse(listFilesResponse);

        TestSlicerService testSlicerService = new TestSlicerService();
        TestNotificationsHandler testNotificationsHandler = new TestNotificationsHandler();
        printQueue = new PrintQueue(testPrinter, testNotificationsHandler,
                                    testSlicerService);

        STLImporter stlImporter = new STLImporter();
        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
        URL pyramidSTLURL = this.getClass().getResource("/pyramid1.stl");
        ModelLoadResult modelLoadResult = stlImporter.loadFile(null,
                                                               pyramidSTLURL.getFile(),
                                                               null,
                                                               progressProperty);
        ObservableList<ModelContainer> modelList = FXCollections.observableArrayList(
            modelLoadResult.getModelContainer());
        project = new Project("abcdef", "Pyramid", modelList);
        project.setProjectMode(ProjectMode.MESH);
    }

//    @Test
    public void testETCCalculatorCreatedForReprint() throws
        InterruptedException, IOException, URISyntaxException
    {
        RoboxProfile roboxProfile = PrintProfileContainer.getSettingsByProfileName(
            DRAFT_SETTINGS);
        String TEST_JOB_ID = "asdxyz";
        project.addPrintJobID(TEST_JOB_ID);
        File printJobDirectory = new File(
            ApplicationConfiguration.getPrintSpoolDirectory() + TEST_JOB_ID);
        printJobDirectory.mkdir();
        System.out.println("PJD " + printJobDirectory);
        URL pyramidURL = this.getClass().getResource("/pyramid.statistics");
        Path statisticsFile = Paths.get(pyramidURL.toURI());
        Path destinationStatisticsFile = Paths.get(printJobDirectory
            + File.separator + TEST_JOB_ID
            + ApplicationConfiguration.statisticsFileExtension);
        Files.copy(statisticsFile, destinationStatisticsFile);

        TestListFilesResponse listFilesResponse = new TestListFilesResponse(
            TEST_JOB_ID);
        testPrinter.setListFilesResonse(listFilesResponse);

        printQueue.printProject(project, PrintQualityEnumeration.DRAFT,
                                roboxProfile);

        int totalWaitTime = 0;
        while (true)
        {
            System.out.println("STATUS " + printQueue.getPrintStatus());
            if (PrinterStatusEnumeration.PRINTING.equals(
                printQueue.getPrintStatus()))
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

        testPrinter.setPrintJobLineNumber(1702);
        ReadOnlyDoubleProperty progress = printQueue.progressProperty();
        assertEquals(1.0d, progress.get(), 0.001);

    }

    /**
     * Test that progressProperty is 1 at end of print
     *
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    @Test
    public void testProgressPropertyIsOneAtEndOfPrint() throws IOException, InterruptedException
    {
        return;
//        RoboxProfile roboxProfile = PrintProfileContainer.getSettingsByProfileName(
//            DRAFT_SETTINGS);
//        printQueue.printProject(project, PrintQualityEnumeration.DRAFT,
//                                roboxProfile);
//
//        int totalWaitTime = 0;
//        while (true)
//        {
//            System.out.println("STATUS " + printQueue.getPrintStatus());
//            if (PrinterStatusEnumeration.PRINTING.equals(
//                printQueue.getPrintStatus()))
//            {
//                break;
//            }
//            Thread.sleep(WAIT_INTERVAL);
//            totalWaitTime += WAIT_INTERVAL;
//            if (totalWaitTime > MAX_WAIT_INTERVAL)
//            {
//                fail("Test print took too long");
//            }
//        }
//
//        testPrinter.setPrintJobLineNumber(0);
//        Thread.sleep(2000);
//        testPrinter.setPrintJobLineNumber(1);
//
//        testPrinter.setPrintJobLineNumber(1702);
//
//        int ETC = printQueue.progressETCProperty().get();
//        assertEquals(0, ETC);
//        ReadOnlyDoubleProperty progress = printQueue.progressProperty();
//        assertEquals(1.0d, progress.get(), 0.001);
    }

//    @Test
    public void testCurrentLayerAtEndOfPrint() throws IOException, InterruptedException
    {
        RoboxProfile roboxProfile = PrintProfileContainer.getSettingsByProfileName(
            DRAFT_SETTINGS);
        printQueue.printProject(project, PrintQualityEnumeration.DRAFT,
                                roboxProfile);

        int totalWaitTime = 0;
        while (true)
        {
            System.out.println("STATUS " + printQueue.getPrintStatus());
            if (PrinterStatusEnumeration.PRINTING.equals(
                printQueue.getPrintStatus()))
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
        Thread.sleep(1000);
        testPrinter.setPrintJobLineNumber(1);

        testPrinter.setPrintJobLineNumber(1702);
        int currentLayer = printQueue.progressCurrentLayerProperty().get();
        assertEquals(66, currentLayer);
    }

//    @Test
    public void testCurrentLayerAtStartOfPrint() throws IOException, InterruptedException
    {
        RoboxProfile roboxProfile = PrintProfileContainer.getSettingsByProfileName(
            DRAFT_SETTINGS);
        printQueue.printProject(project, PrintQualityEnumeration.DRAFT,
                                roboxProfile);

        int totalWaitTime = 0;
        while (true)
        {
            System.out.println("STATUS " + printQueue.getPrintStatus());
            if (PrinterStatusEnumeration.PRINTING.equals(
                printQueue.getPrintStatus()))
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
        Thread.sleep(1000);
        testPrinter.setPrintJobLineNumber(1);
        int currentLayer = printQueue.progressCurrentLayerProperty().get();
        assertEquals(0, currentLayer);
    }

}
