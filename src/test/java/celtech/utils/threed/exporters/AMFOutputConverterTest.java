/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed.exporters;

import celtech.JavaFXConfiguredTest;
import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.io.StringWriter;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import static org.codehaus.plexus.util.SelectorUtils.removeWhitespace;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class AMFOutputConverterTest extends JavaFXConfiguredTest
{

    private ModelContainer makeModelContainer(boolean useExtruder0)
    {
        MeshView meshView = new MeshView(new ShapePyramid(2, 3));
        ModelContainer modelContainer = new ModelContainer(new File("testModel"), meshView);
        modelContainer.setUseExtruder0(null, useExtruder0);
//        modelContainer.translateBy(-105, -75);
        return modelContainer;
    }

//    @Test
//    public void testOutputProjectWithOnePyramid() throws XMLStreamException
//    {
//        Project project = new Project();
//        ModelContainer modelContainer = makeModelContainer(true);
//        project.addModel(modelContainer);
//
//        AMFOutputConverter outputConverter = new AMFOutputConverter();
//        XMLOutputFactory factory = XMLOutputFactory.newInstance();
//        StringWriter stringWriter = new StringWriter();
//        XMLStreamWriter writer = factory.createXMLStreamWriter(stringWriter);
//
//        outputConverter.outputProject(project, writer);
//
//        String xmlOutput = stringWriter.toString();
//        System.out.println(xmlOutput);
//
//        String expectedOutput = "<?xml version=\'1.0\' encoding=\'UTF-8\'?>\n"
//            + "<amf unit=\"millimeter\" version=\"1.1\">\n"
//            + "<object id=\"0\">\n" + "    <mesh>\n" + "      <vertices>\n"
//            + "        <vertex><coordinates><x>0.0</x><y>0.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>1.0</x><y>0.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>0.0</x><y>1.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>1.0</x><y>1.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>0.5</x><y>0.5</y><z>1.0</z></coordinates></vertex>\n"
//            + "      </vertices>\n" + "      <volume materialid=\"2\">\n"
//            + "        <triangle><v1>2</v1><v2>1</v2><v3>0</v3></triangle>\n"
//            + "        <triangle><v1>0</v1><v2>1</v2><v3>4</v3></triangle>\n"
//            + "        <triangle><v1>4</v1><v2>1</v2><v3>2</v3></triangle>\n"
//            + "        <triangle><v1>0</v1><v2>4</v2><v3>2</v3></triangle>\n" + "      </volume>\n"
//            + "    </mesh>\n" + "  </object>\n" + " "
//            + "  <material id=\"2\">\n"
//            + "    <color><r>0.1</r><g>0.1</g><b>0.1</b></color>\n"
//            + "  </material>\n"
//            + "  <material id=\"3\">\n"
//            + "    <color><r>0.0</r><g>0.9</g><b>0.9</b><a>0.5</a></color>\n"
//            + "  </material>\n"
//            + "</amf>";
//
//        assertEquals(removeWhitespace(expectedOutput), removeWhitespace(xmlOutput));
//    }

//    @Test
//    public void testOutputOnePyramid() throws JsonProcessingException, XMLStreamException
//    {
//        ModelContainer modelContainer = makeModelContainer(true);
//        
//        AMFOutputConverter outputConverter = new AMFOutputConverter();
//
//        XMLOutputFactory factory = XMLOutputFactory.newInstance();
//        StringWriter stringWriter = new StringWriter();
//        XMLStreamWriter writer = factory.createXMLStreamWriter(stringWriter);
//        writer.writeStartElement("mesh");
//        writer.writeStartElement("vertices");
//        outputConverter.outputVertices(modelContainer, writer);
//        writer.writeEndElement();
//        outputConverter.outputVolume(modelContainer, 0, writer);
//        writer.writeEndElement();
//        writer.flush();
//        String xmlOutput = stringWriter.toString();
//        System.out.println(xmlOutput);
//
//        String expectedOutput = "    <mesh>\n" + "      <vertices>\n"
//            + "        <vertex><coordinates><x>0.0</x><y>0.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>1.0</x><y>0.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>0.0</x><y>0.0</y><z>-1.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>1.0</x><y>0.0</y><z>-1.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>0.5</x><y>1.0</y><z>-0.5</z></coordinates></vertex>\n"
//            + "      </vertices>\n" + "      <volume materialid=\"2\">\n"
//            + "        <triangle><v1>2</v1><v2>1</v2><v3>0</v3></triangle>\n"
//            + "        <triangle><v1>0</v1><v2>1</v2><v3>4</v3></triangle>\n"
//            + "        <triangle><v1>4</v1><v2>1</v2><v3>2</v3></triangle>\n"
//            + "        <triangle><v1>0</v1><v2>4</v2><v3>2</v3></triangle>\n" + "      </volume>\n"
//            + "    </mesh>\n";
//
//        assertEquals(removeWhitespace(expectedOutput), removeWhitespace(xmlOutput));
//    }

    class ShapePyramid extends TriangleMesh
    {

        public ShapePyramid(float Width, float Height)
        {
            float[] points =
            {
                0, 0, 0, // idx p0
                1, 0, 0, // idx p1
                0, 1, 0, // idx p2
                1, 1, 0, // idx p3
                0.5f, 0.5f, 1 // idx p4
            };
            float[] texCoords =
            {
                1, 1, // idx t0
                1, 0, // idx t1
                0, 1, // idx t2
                0, 0, // idx t3
                0.5f, 0.5f  // idx t4
            };

            int[] faces =
            {
                2, 3, 1, 2, 0, 0,
                0, 3, 1, 0, 4, 1,
                4, 3, 1, 2, 2, 0,
                0, 3, 4, 0, 2, 1,
            };

            this.getPoints().setAll(points);
            this.getTexCoords().setAll(texCoords);
            this.getFaces().setAll(faces);
        }
    }

//    @Test
//    public void testOutputProjectWithTwoPyramids() throws XMLStreamException
//    {
//        Project project = new Project();
//        ModelContainer modelContainer = makeModelContainer(true);
//        ModelContainer modelContainer2 = makeModelContainer(true);
//        project.addModel(modelContainer);
//        project.addModel(modelContainer2);
//
//        AMFOutputConverter outputConverter = new AMFOutputConverter();
//        XMLOutputFactory factory = XMLOutputFactory.newInstance();
//        StringWriter stringWriter = new StringWriter();
//        XMLStreamWriter writer = factory.createXMLStreamWriter(stringWriter);
//
//        outputConverter.outputProject(project, writer);
//
//        String xmlOutput = stringWriter.toString();
//        System.out.println(xmlOutput);
//
//        String expectedOutput = "<?xml version=\'1.0\' encoding=\'UTF-8\'?>\n"
//            + "<amf unit=\"millimeter\" version=\"1.1\">\n"
//            + "<object id=\"0\">\n" + "    <mesh>\n" + "      <vertices>\n"
//            + "        <vertex><coordinates><x>0.0</x><y>0.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>1.0</x><y>0.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>0.0</x><y>1.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>1.0</x><y>1.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>0.5</x><y>0.5</y><z>1.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>0.0</x><y>0.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>1.0</x><y>0.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>0.0</x><y>1.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>1.0</x><y>1.0</y><z>0.0</z></coordinates></vertex>\n"
//            + "        <vertex><coordinates><x>0.5</x><y>0.5</y><z>1.0</z></coordinates></vertex>\n"            
//            + "      </vertices>\n" + 
//              "       <volume materialid=\"2\">\n"
//            + "        <triangle><v1>2</v1><v2>1</v2><v3>0</v3></triangle>\n"
//            + "        <triangle><v1>0</v1><v2>1</v2><v3>4</v3></triangle>\n"
//            + "        <triangle><v1>4</v1><v2>1</v2><v3>2</v3></triangle>\n"
//            + "        <triangle><v1>0</v1><v2>4</v2><v3>2</v3></triangle>\n" + "      </volume>\n"
//            + "       <volume materialid=\"2\">\n"
//            + "        <triangle><v1>7</v1><v2>6</v2><v3>5</v3></triangle>\n"
//            + "        <triangle><v1>5</v1><v2>6</v2><v3>9</v3></triangle>\n"
//            + "        <triangle><v1>9</v1><v2>6</v2><v3>7</v3></triangle>\n"
//            + "        <triangle><v1>5</v1><v2>9</v2><v3>7</v3></triangle>\n" + "      </volume>\n"
//            + "    </mesh>\n" + "  </object>\n" + " "
//            + "  <material id=\"2\">\n"
//            + "    <color><r>0.1</r><g>0.1</g><b>0.1</b></color>\n"
//            + "  </material>\n"
//            + "  <material id=\"3\">\n"
//            + "    <color><r>0.0</r><g>0.9</g><b>0.9</b><a>0.5</a></color>\n"
//            + "  </material>\n"
//            + "</amf>";
//
//        assertEquals(removeWhitespace(expectedOutput), removeWhitespace(xmlOutput));
//    }

}
