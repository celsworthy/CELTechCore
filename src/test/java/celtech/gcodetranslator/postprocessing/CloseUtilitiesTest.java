package celtech.gcodetranslator.postprocessing;

import celtech.JavaFXConfiguredTest;
import celtech.appManager.Project;
import celtech.configuration.slicer.NozzleParameters;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.services.slicer.PrintQualityEnumeration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class CloseUtilitiesTest extends JavaFXConfiguredTest
{

    @Test
    public void testFindClosestMovementNode()
    {
        ToolSelectNode tool1 = setupToolNodeWithInnerAndOuterSquare();

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(0.003f);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        CloseUtilities closeUtilities = new CloseUtilities(testProject,
                testProject.getPrinterSettings().getSettings("RBX01-SM"),
                "RBX01-SM");

        List<SectionNode> sectionsToConsider = new ArrayList<>();
        sectionsToConsider.add((SectionNode) tool1.getChildren().get(0));

        Optional<IntersectionResult> result = closeUtilities.findClosestMovementNode(((ExtrusionNode) tool1.getChildren().get(1).getChildren().get(4)),
                sectionsToConsider);
        OutputUtilities output = new OutputUtilities();
        output.outputNodes(tool1, 0);
        assertTrue(result.isPresent());
        assertSame(tool1.getChildren().get(0).getChildren().get(4), result.get().getClosestNode());
        assertEquals(1, result.get().getIntersectionPoint().getX(), 0.01);
        assertEquals(2.5, result.get().getIntersectionPoint().getY(), 0.01);
    }

    @Test
    public void testFindClosestMovementNode_multiSection()
    {
        ToolSelectNode tool1 = setupToolNodeWithInnerAndOuterSquare();

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(0.003f);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        CloseUtilities closeUtilities = new CloseUtilities(testProject,
                testProject.getPrinterSettings().getSettings("RBX01-SM"),
                "RBX01-SM");

        FillSectionNode fill1 = new FillSectionNode();
        ExtrusionNode node1 = new ExtrusionNode();
        node1.getMovement().setX(100);
        node1.getMovement().setY(100);
        node1.getExtrusion().setE(3);
        tool1.addChildAtEnd(fill1);

        List<SectionNode> sectionsToConsider = new ArrayList<>();
        sectionsToConsider.add((SectionNode) tool1.getChildren().get(0));
        sectionsToConsider.add(fill1);

        Optional<IntersectionResult> result = closeUtilities.findClosestMovementNode(((ExtrusionNode) tool1.getChildren().get(1).getChildren().get(4)),
                sectionsToConsider);
        OutputUtilities output = new OutputUtilities();
        output.outputNodes(tool1, 0);
        assertTrue(result.isPresent());
        assertSame(tool1.getChildren().get(0).getChildren().get(4), result.get().getClosestNode());
        assertEquals(1, result.get().getIntersectionPoint().getX(), 0.01);
        assertEquals(2.5, result.get().getIntersectionPoint().getY(), 0.01);
    }

    private ToolSelectNode setupToolNodeWithInnerAndOuterSquare()
    {
        ToolSelectNode tool1 = new ToolSelectNode();

        OuterPerimeterSectionNode outer1 = new OuterPerimeterSectionNode();
        InnerPerimeterSectionNode inner1 = new InnerPerimeterSectionNode();

        TravelNode travel1 = new TravelNode();
        travel1.getMovement().setX(0);
        travel1.getMovement().setY(0);

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getMovement().setX(10);
        extrusionNode1.getMovement().setY(0);
        extrusionNode1.getExtrusion().setE(0.1f);
        extrusionNode1.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getMovement().setX(10);
        extrusionNode2.getMovement().setY(10);
        extrusionNode2.getExtrusion().setE(0.1f);
        extrusionNode2.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getMovement().setX(0);
        extrusionNode3.getMovement().setY(10);
        extrusionNode3.getExtrusion().setE(0.1f);
        extrusionNode3.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getMovement().setX(0);
        extrusionNode4.getMovement().setY(0);
        extrusionNode4.getExtrusion().setE(0.1f);
        extrusionNode4.getFeedrate().setFeedRate_mmPerMin(10);

        outer1.addChildAtEnd(travel1);
        outer1.addChildAtEnd(extrusionNode1);
        outer1.addChildAtEnd(extrusionNode2);
        outer1.addChildAtEnd(extrusionNode3);
        outer1.addChildAtEnd(extrusionNode4);

        TravelNode travel2 = new TravelNode();
        travel2.getMovement().setX(1);
        travel2.getMovement().setY(1);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getMovement().setX(9);
        extrusionNode5.getMovement().setY(1);
        extrusionNode5.getExtrusion().setE(0.1f);
        extrusionNode5.getFeedrate().setFeedRate_mmPerMin(20);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getMovement().setX(9);
        extrusionNode6.getMovement().setY(9);
        extrusionNode6.getExtrusion().setE(0.1f);
        extrusionNode6.getFeedrate().setFeedRate_mmPerMin(20);

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.getMovement().setX(1);
        extrusionNode7.getMovement().setY(9);
        extrusionNode7.getExtrusion().setE(0.1f);
        extrusionNode7.getFeedrate().setFeedRate_mmPerMin(20);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.getMovement().setX(1);
        extrusionNode8.getMovement().setY(1);
        extrusionNode8.getExtrusion().setE(0.1f);
        extrusionNode8.getFeedrate().setFeedRate_mmPerMin(20);

        inner1.addChildAtEnd(travel2);
        inner1.addChildAtEnd(extrusionNode5);
        inner1.addChildAtEnd(extrusionNode6);
        inner1.addChildAtEnd(extrusionNode7);
        inner1.addChildAtEnd(extrusionNode8);

        tool1.addChildAtEnd(inner1);
        tool1.addChildAtEnd(outer1);

        return tool1;
    }
}
