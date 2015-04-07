package celtech.utils.threed.exporters;

import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.modelcontrol.ModelContainer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.collections.ObservableFloatArray;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class AMFOutputConverter implements MeshFileOutputConverter
{

    private Stenographer steno = StenographerFactory.getStenographer(AMFOutputConverter.class.
        getName());

    void outputProject(Project project, XMLStreamWriter streamWriter) throws XMLStreamException
    {
        streamWriter.writeStartDocument();
        streamWriter.writeStartElement("amf");
        streamWriter.writeAttribute("unit", "inch");
        streamWriter.writeAttribute("version", "1.1");
        for (ModelContainer modelContainer : project.getLoadedModels())
        {
            outputModelContainer(modelContainer, 1, streamWriter);
        }
        outputMaterials(project, streamWriter);
        streamWriter.writeEndElement();
        streamWriter.writeEndDocument();
        streamWriter.flush();
        streamWriter.close();
    }

    void outputMaterials(Project project, XMLStreamWriter streamWriter) throws XMLStreamException
    {
        outputMaterial(project, 2, 0.1f, 0.1f, 0.1f, streamWriter);
        outputMaterial(project, 3, 0f, 0.9f, 0.9f, Optional.of(0.5f), streamWriter);
    }

    void outputMaterial(Project project, int materialId, float r, float g, float b,
        XMLStreamWriter streamWriter) throws XMLStreamException
    {
        outputMaterial(project, 2, 0.1f, 0.1f, 0.1f, Optional.empty(), streamWriter);
    }

    void outputMaterial(Project project, int materialId, float r, float g, float b,
        Optional<Float> a,
        XMLStreamWriter streamWriter) throws XMLStreamException
    {
        streamWriter.writeStartElement("material");
        streamWriter.writeAttribute("id", Integer.toString(materialId));
        streamWriter.writeStartElement("color");
        streamWriter.writeStartElement("r");
        streamWriter.writeCharacters(Float.toString(r));
        streamWriter.writeEndElement();
        streamWriter.writeStartElement("g");
        streamWriter.writeCharacters(Float.toString(g));
        streamWriter.writeEndElement();
        streamWriter.writeStartElement("b");
        streamWriter.writeCharacters(Float.toString(b));
        streamWriter.writeEndElement();
        if (a.isPresent())
        {
            streamWriter.writeStartElement("a");
            streamWriter.writeCharacters(Float.toString(a.get()));
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }

    void outputModelContainer(ModelContainer modelContainer, int modelId,
        XMLStreamWriter streamWriter) throws XMLStreamException
    {
        streamWriter.writeStartElement("object");
        streamWriter.writeAttribute("id", Integer.toString(modelId));
        streamWriter.writeStartElement("mesh");
        outputVertices(modelContainer, streamWriter);
        outputVolume(modelContainer, streamWriter);
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();

        streamWriter.flush();
    }

    void outputVolume(ModelContainer modelContainer, XMLStreamWriter streamWriter) throws XMLStreamException
    {
        streamWriter.writeStartElement("volume");
        streamWriter.writeAttribute("materialid", "2");

        for (Node node : modelContainer.getMeshGroupChildren())
        {
            if (node instanceof MeshView)
            {
                MeshView mesh = (MeshView) node;
                TriangleMesh triMesh = (TriangleMesh) mesh.getMesh();

                ObservableFaceArray faces = triMesh.getFaces();
                for (int i = 0; i < faces.size(); i += 6)
                {
                    outputFace(faces, i, streamWriter);
                }
            }
        }
        streamWriter.writeEndElement();
    }

    void outputVertices(ModelContainer modelContainer, XMLStreamWriter streamWriter) throws XMLStreamException
    {
        streamWriter.writeStartElement("vertices");

        for (Node node : modelContainer.getMeshGroupChildren())
        {
            if (node instanceof MeshView)
            {
                MeshView mesh = (MeshView) node;
                TriangleMesh triMesh = (TriangleMesh) mesh.getMesh();

                ObservableFloatArray points = triMesh.getPoints();
                for (int i = 0; i < points.size(); i += 3)
                {
                    outputVertex(points, i, streamWriter);
                }

            }
        }
        streamWriter.writeEndElement();
    }

    private void outputFace(ObservableFaceArray faces, int offset, XMLStreamWriter streamWriter) throws XMLStreamException
    {
        streamWriter.writeStartElement("triangle");
        streamWriter.writeStartElement("v1");
        streamWriter.writeCharacters(Integer.toString(faces.get(offset)));
        streamWriter.writeEndElement();
        streamWriter.writeStartElement("v2");
        streamWriter.writeCharacters(Integer.toString(faces.get(offset + 2)));
        streamWriter.writeEndElement();
        streamWriter.writeStartElement("v3");
        streamWriter.writeCharacters(Integer.toString(faces.get(offset + 4)));
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }

    private void outputVertex(ObservableFloatArray points, int offset, XMLStreamWriter streamWriter) throws XMLStreamException
    {
        streamWriter.writeStartElement("vertex");
        streamWriter.writeStartElement("coordinates");
        streamWriter.writeStartElement("x");
        streamWriter.writeCharacters(Float.toString(points.get(offset)));
        streamWriter.writeEndElement();
        streamWriter.writeStartElement("y");
        streamWriter.writeCharacters(Float.toString(points.get(offset + 1)));
        streamWriter.writeEndElement();
        streamWriter.writeStartElement("z");
        streamWriter.writeCharacters(Float.toString(points.get(offset + 2)));
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }

    @Override
    public void outputFile(Project project, String printJobUUID)
    {
        String tempModelFilenameWithPath = ApplicationConfiguration.getPrintSpoolDirectory()
            + printJobUUID
            + File.separator + printJobUUID + ApplicationConfiguration.amfTempFileExtension;

        try
        {
            FileWriter fileWriter = new FileWriter(tempModelFilenameWithPath);
            XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xmlStreamWriter
                = xmlOutputFactory.createXMLStreamWriter(fileWriter);

            PrettyPrintHandler handler = new PrettyPrintHandler(xmlStreamWriter);
            XMLStreamWriter prettyPrintWriter = (XMLStreamWriter) Proxy.newProxyInstance(
                XMLStreamWriter.class.getClassLoader(),
                new Class[]
                {
                    XMLStreamWriter.class
                },
                handler);

            outputProject(project, prettyPrintWriter);
            xmlStreamWriter.flush();
            xmlStreamWriter.close();
        } catch (IOException ex)
        {
            steno.error("Unable to write AMF file to given path: " + ex);
        } catch (XMLStreamException ex)
        {
            steno.error("Unable to write AMF file: " + ex);
        }

    }

    class PrettyPrintHandler implements InvocationHandler
    {

        private final XMLStreamWriter target;
        private int depth = 0;
        private final Map<Integer, Boolean> hasChildElement = new HashMap<Integer, Boolean>();
        private static final String INDENT_CHAR = " ";
        private static final String LINEFEED_CHAR = "\n";

        public PrettyPrintHandler(XMLStreamWriter target)
        {
            this.target = target;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            String m = method.getName();

            // Needs to be BEFORE the actual event, so that for instance the
            // sequence writeStartElem, writeAttr, writeStartElem, writeEndElem, writeEndElem
            // is correctly handled
            if ("writeStartElement".equals(m))
            {
                // update state of parent node
                if (depth > 0)
                {
                    hasChildElement.put(depth - 1, true);
                }
                // reset state of current node
                hasChildElement.put(depth, false);
                // indent for current depth
                target.writeCharacters(LINEFEED_CHAR);
                target.writeCharacters(repeat(depth, INDENT_CHAR));
                depth++;
            } else if ("writeEndElement".equals(m))
            {
                depth--;
                if (hasChildElement.get(depth) == true)
                {
                    target.writeCharacters(LINEFEED_CHAR);
                    target.writeCharacters(repeat(depth, INDENT_CHAR));
                }
            } else if ("writeEmptyElement".equals(m))
            {
                // update state of parent node
                if (depth > 0)
                {
                    hasChildElement.put(depth - 1, true);
                }
                // indent for current depth
                target.writeCharacters(LINEFEED_CHAR);
                target.writeCharacters(repeat(depth, INDENT_CHAR));
            }
            method.invoke(target, args);
            return null;
        }

        private String repeat(int d, String s)
        {
            String _s = "";
            while (d-- > 0)
            {
                _s += s;
            }
            return _s;
        }
    }

}