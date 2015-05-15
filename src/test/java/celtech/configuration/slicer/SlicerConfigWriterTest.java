/*
 * Copyright 2015 CEL UK
 */
package celtech.configuration.slicer;

import celtech.JavaFXConfiguredTest;
import celtech.configuration.SlicerType;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.services.slicer.PrintQualityEnumeration;
import java.io.File;
import java.io.IOException;
import java.util.List;
import static org.apache.commons.io.FileUtils.readLines;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author tony
 */
public class SlicerConfigWriterTest extends JavaFXConfiguredTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testGenerateConfigForRaftOnCura() throws IOException
    {
        String TEMPFILENAME = "output.roboxprofile";
        SlicerConfigWriter configWriter = SlicerConfigWriterFactory.getConfigWriter(
            SlicerType.Cura);
        PrinterSettings printerSettings = new PrinterSettings();
        printerSettings.setPrintQuality(PrintQualityEnumeration.DRAFT);
        printerSettings.setRaftOverride(true);
        String destinationFile = temporaryFolder.getRoot().getAbsolutePath() + File.separator + TEMPFILENAME;
        configWriter.generateConfigForSlicer(printerSettings.getSettings(), destinationFile);
        List<String> outputData = readLines(new File(destinationFile));
        assertTrue(outputData.contains("raftBaseThickness=300"));
        assertTrue(outputData.contains("raftInterfaceThickness=280"));
    }
    
}
