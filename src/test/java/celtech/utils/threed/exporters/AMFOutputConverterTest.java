/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed.exporters;

import celtech.modelcontrol.ModelContainer;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.io.FileWriter;
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
public class AMFOutputConverterTest
{

    private ModelContainer makeModelContainer(boolean useExtruder0)
    {
        MeshView meshView = new MeshView(new Shape3DRectangle(2, 3));
        ModelContainer modelContainer = new ModelContainer(new File("testModel"), meshView);
        modelContainer.setUseExtruder0Filament(useExtruder0);
        return modelContainer;
    }

    @Test
    public void testOutputOnePyramid() throws JsonProcessingException, XMLStreamException
    {
        ModelContainer modelContainer = makeModelContainer(true);
        AMFOutputConverter outputConverter = new AMFOutputConverter();

        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        StringWriter stringWriter = new StringWriter();
        XMLStreamWriter writer = factory.createXMLStreamWriter(stringWriter);
        outputConverter.outputModelContainer(modelContainer, 1, writer);
        String xmlOutput = stringWriter.toString();
        System.out.println(xmlOutput);

        String expectedOutput = "<object id=\"1\">\n" + "    <mesh>\n" + "      <vertices>\n"
            + "        <vertex><coordinates><x>0.0</x><y>0.0</y><z>0.0</z></coordinates></vertex>\n"
            + "        <vertex><coordinates><x>1.0</x><y>0.0</y><z>0.0</z></coordinates></vertex>\n"
            + "        <vertex><coordinates><x>0.0</x><y>1.0</y><z>0.0</z></coordinates></vertex>\n"
            + "        <vertex><coordinates><x>1.0</x><y>1.0</y><z>0.0</z></coordinates></vertex>\n"
            + "        <vertex><coordinates><x>0.5</x><y>0.5</y><z>1.0</z></coordinates></vertex>\n"
            + "      </vertices>\n" + "      <volume materialid=\"2\">\n"
            + "        <triangle><v1>2</v1><v2>1</v2><v3>0</v3></triangle>\n"
            + "        <triangle><v1>0</v1><v2>1</v2><v3>4</v3></triangle>\n"
            + "        <triangle><v1>4</v1><v2>1</v2><v3>2</v3></triangle>\n"
            + "        <triangle><v1>0</v1><v2>4</v2><v3>2</v3></triangle>\n" + "      </volume>\n"
            + "    </mesh>\n" + "  </object>";

        assertEquals(removeWhitespace(expectedOutput), removeWhitespace(xmlOutput));
    }

    class Shape3DRectangle extends TriangleMesh
    {

        public Shape3DRectangle(float Width, float Height)
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
                0, 0,  // idx t3
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

}
