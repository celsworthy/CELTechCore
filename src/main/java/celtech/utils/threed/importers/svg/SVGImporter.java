package celtech.utils.threed.importers.svg;

import celtech.utils.threed.importers.stl.*;
import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.coreUI.visualisation.metaparts.FloatArrayList;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;

import celtech.modelcontrol.ModelContainer;
import celtech.services.modelLoader.ModelLoaderTask;
import celtech.utils.threed.importers.svg.metadata.dragknife.DragKnifeMetaPart;
import celtech.utils.threed.importers.svg.metadata.svg.SVGMetaPart;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.beans.property.DoubleProperty;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.parser.PathParser;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SVGImporter
{

    private final Stenographer steno = StenographerFactory.getStenographer(SVGImporter.class.getName());
    private TriangleMesh meshToOutput = null;
    private ModelLoaderTask parentTask = null;
    private DoubleProperty percentProgressProperty = null;
    private final String spacePattern = "[ ]+";


    public ModelLoadResult loadFile(ModelLoaderTask parentTask, File modelFile,
        DoubleProperty percentProgressProperty)
    {
        this.parentTask = parentTask;
        this.percentProgressProperty = percentProgressProperty;
        boolean fileIsBinary;

//        steno.debug("Starting SVG load");
//
//          SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
////            String uri = "file:///Users/ianhudson/Downloads/svgcuts-120214-snow-flake/snowflake_doily_bottom.svg";
//            String inputFilename = "/Users/ianhudson/Downloads/svgcuts-120214-snow-flake/snowflake_doily_bottom.svg";
//            Document doc = f.createDocument("file://" + inputFilename);
//
//            UserAgent userAgent = new UserAgentAdapter();
//            DocumentLoader loader = new DocumentLoader(userAgent);
//            BridgeContext bridgeContext = new BridgeContext(userAgent, loader);
//            bridgeContext.setDynamicState(BridgeContext.DYNAMIC);
//
//            // Enable CSS- and SVG-specific enhancements.
//            (new GVTBuilder()).build(bridgeContext, doc);
//
//            NodeList svgData = doc.getElementsByTagName("svg");
//            NamedNodeMap svgAttributes = svgData.item(0).getAttributes();
//            String widthString = svgAttributes.getNamedItem("width").getNodeValue();
//            int width = Integer.valueOf(widthString.replaceAll("\\..*[a-z]+", ""));
//            String heightString = svgAttributes.getNamedItem("height").getNodeValue();
//            int height = Integer.valueOf(heightString.replaceAll("\\..*[a-z]+", ""));
//
//            NodeList paths = doc.getElementsByTagName("path");
//
//            PathParserThing parserThing = new PathParserThing();
////            parserThing.setDimensions(width, height);
////            parserThing.setScaleBy(0.1);
//
//            PathParser pathParser = new PathParser();
//            pathParser.setPathHandler(parserThing);
//
//            for (int pathIndex = 0; pathIndex < paths.getLength(); pathIndex++)
//            {
//                Node pathNode = paths.item(pathIndex);
//                NamedNodeMap nodeMap = pathNode.getAttributes();
//                Node dNode = nodeMap.getNamedItem("d");
//                System.out.println(dNode.getNodeValue());
//                pathParser.parse(dNode.getNodeValue());
//            }
//            
//            List<SVGMetaPart> metaParts = parserThing.getMetaParts();
//
//            SVGToDragKnifeMetaEngine svgToDragKnifeEngine = new SVGToDragKnifeMetaEngine();
//            List<DragKnifeMetaPart> dragknifeMetaparts = svgToDragKnifeEngine.convertToDragKnifeMetaParts(metaParts);
//            
//            DragKnifeCompensator compensator = new DragKnifeCompensator();
//            List<DragKnifeMetaPart> compensatedParts = compensator.doCompensation(dragknifeMetaparts, 0.1);        
//
//            DragKnifeToGCodeEngine svgToGCodeEngine = new DragKnifeToGCodeEngine(inputFilename + ".gcode", compensatedParts);
//            svgToGCodeEngine.generateGCode();
//       
//        if (parentTask == null || (!parentTask.isCancelled()))
//        {
//            MeshView meshView = new MeshView();
//
//            meshView.setMesh(meshToOutput);
//            meshView.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
//            meshView.setCullFace(CullFace.BACK);
//            meshView.setId(modelFile.getName() + "_mesh");
//
//            Set<ModelContainer> modelContainers = new HashSet<>();
//            ModelContainer modelContainer = new ModelContainer(modelFile, meshView);
//            modelContainers.add(modelContainer);
//
//            ModelLoadResult result = new ModelLoadResult(
//                                                         modelFile.getAbsolutePath(),
//                                                         modelFile.getName(), 
//                                                         modelContainers);
//            return result;
//        } else
//        {
            return null;
//        }
    }
//
//    @SuppressWarnings("empty-statement")
//    private int getLines(File aFile)
//    {
//        LineNumberReader reader = null;
//        try
//        {
//            reader = new LineNumberReader(new FileReader(aFile));
//            while ((reader.readLine()) != null);
//            return reader.getLineNumber();
//        } catch (Exception ex)
//        {
//            return -1;
//        } finally
//        {
//            if (reader != null)
//            {
//                try
//                {
//                    reader.close();
//                } catch (IOException ex)
//                {
//                    steno.error("Failed to close file during line number read: " + ex);
//                }
//            }
//        }
//    }
//
//    private boolean isFileBinary(File stlFile)
//    {
//        boolean fileIsBinary = false;
//        BufferedInputStream inputFileStream;
//        ByteBuffer dataBuffer;
//        byte[] facetBytes = new byte[4];     // Holds the number of faces
//
//        try
//        {
//            inputFileStream = new BufferedInputStream(new FileInputStream(stlFile));
//            inputFileStream.mark(4000);
//            byte[] asciiHeaderBytes = new byte[80];
//            int bytesRead = inputFileStream.read(asciiHeaderBytes);
//            String asciiHeader = new String(asciiHeaderBytes, "UTF-8");
//            steno.debug("The header was: " + asciiHeader);
//
//            bytesRead = inputFileStream.read(facetBytes);                      // We get the 4 bytes
//            dataBuffer = ByteBuffer.wrap(facetBytes);   // ByteBuffer for reading correctly the int
//            dataBuffer.order(ByteOrder.nativeOrder());    // Set the right order
//            int numberOfFacets = dataBuffer.getInt();
//
//            int filesize = (numberOfFacets * 50) + 84;
//            inputFileStream.reset();
//
//            if (stlFile.length() == filesize)
//            {
//                fileIsBinary = true;
//            }
//        } catch (IOException ex)
//        {
//            steno.error("Failed to determine whether " + stlFile.getName() + " was binary or ascii."
//                + ex.toString());
//        }
//
//        return fileIsBinary;
//    }
//
//
//    public TriangleMesh processBinarySTLData(File stlFile) throws STLFileParsingException
//    {
//        ByteBuffer dataBuffer;
//        byte[] facetBytes = new byte[4];     // Holds the number of faces
//        byte[] facetData = new byte[50]; // Each face has 50 bytes of data
//        int progressPercent = 0;
//
//        steno.debug("Processing binary STL");
//
//        TriangleMesh triangleMesh = new TriangleMesh();
//        HashMap<Vector3D, Integer> graph = new HashMap<>();
//        try
//        {
//            try (DataInputStream inputFileStream = new DataInputStream(new FileInputStream(stlFile)))
//            {
//                byte[] asciiHeaderBytes = new byte[80];
//                inputFileStream.read(asciiHeaderBytes);
//                String asciiHeader = new String(asciiHeaderBytes, "UTF-8");
//                steno.debug("The header was: " + asciiHeader);
//
//                inputFileStream.read(facetBytes);                      // We get the 4 bytes
//                dataBuffer = ByteBuffer.wrap(facetBytes);   // ByteBuffer for reading correctly the int
//                dataBuffer.order(ByteOrder.nativeOrder());    // Set the right order
//                int numberOfFacets = dataBuffer.getInt();
//
//                steno.debug("There are " + numberOfFacets + " faces");
//
//                int[] faceIndexArray = new int[6];
//
//                int vertexCounter = 0;
//
//                for (int facetNum = 0; facetNum < numberOfFacets; facetNum++)
//                {
//                    if ((parentTask != null) && parentTask.isCancelled())
//                    {
//                        break;
//                    }
//
//                    int progressUpdate = (int) (((double) facetNum / (double) numberOfFacets) * 100);
//                    if (progressUpdate != progressPercent)
//                    {
//                        progressPercent = progressUpdate;
//                        if (percentProgressProperty != null)
//                        {
//                            percentProgressProperty.set(progressPercent);
//                        }
//                    }
//
//                    inputFileStream.read(facetData);              // We get the rest of the file
//                    dataBuffer = ByteBuffer.wrap(facetData);      // Now we have all the data in this ByteBuffer
//                    dataBuffer.order(ByteOrder.nativeOrder());
//
//                    // Read the Normal and place it 3 times (one for each vertex)
//                    dataBuffer.getFloat();
//                    dataBuffer.getFloat();
//                    dataBuffer.getFloat();
//
//                    for (int vertexNumber = 0; vertexNumber < 3; vertexNumber++)
//                    {
//                        float inputVertexX, inputVertexY, inputVertexZ;
//
//                        inputVertexX = dataBuffer.getFloat();
//                        inputVertexY = dataBuffer.getFloat();
//                        inputVertexZ = dataBuffer.getFloat();
//                        
//                        Vector3D generatedVertex = new Vector3D(inputVertexX,
//                                                                -inputVertexZ,
//                                                                inputVertexY);
//
//                        if (!graph.containsKey(generatedVertex))
//                        {
//                            graph.put(generatedVertex, vertexCounter);
//                            faceIndexArray[vertexNumber * 2] = vertexCounter;
//                            vertexCounter++;
//                        } else
//                        {
//                            faceIndexArray[vertexNumber * 2] = graph.get(generatedVertex);
//                        }
//                    }
//
//                    // Add the face to the triangle mesh
//                    triangleMesh.getFaces().addAll(faceIndexArray, 0, 6);
//
//                // After each facet there are 2 bytes without information
//                    // In the last iteration we dont have to skip those bytes..
//                    if (facetNum != numberOfFacets - 1)
//                    {
//                        dataBuffer.get();
//                        dataBuffer.get();
//                    }
//                }
//
//                steno.debug("Started with " + numberOfFacets * 3 + " vertices and now have "
//                    + graph.size());
//
//                float[] tempVertexPointArray = new float[3];
//                graph.entrySet()
//                    .stream()
//                    .sorted((s1, s2) ->
//                        {
//                            if (s1.getValue() == s2.getValue())
//                            {
//                                return 0;
//                            } else if (s1.getValue() > s2.getValue())
//                            {
//                                return 1;
//                            } else
//                            {
//                                return -1;
//                            }
//                    })
//                    .forEach(vertexEntry ->
//                        {
//                            tempVertexPointArray[0] = (float) vertexEntry.getKey().getX();
//                            tempVertexPointArray[1] = (float) vertexEntry.getKey().getY();
//                            tempVertexPointArray[2] = (float) vertexEntry.getKey().getZ();
//
//                            triangleMesh.getPoints().addAll(tempVertexPointArray, 0, 3);
//                    });
//
//                FloatArrayList texCoords = new FloatArrayList();
//                texCoords.add(0f);
//                texCoords.add(0f);
//                triangleMesh.getTexCoords().addAll(texCoords.toFloatArray());
//
//                int[] smoothingGroups = new int[numberOfFacets];
//                for (int i = 0; i < smoothingGroups.length; i++)
//                {
//                    smoothingGroups[i] = 0;
//                }
//                triangleMesh.getFaceSmoothingGroups().addAll(smoothingGroups);
//                steno.debug("The mesh contains " + triangleMesh.getPoints().size() / 3
//                    + " points, " + triangleMesh.getTexCoords().size() / 2 + " tex coords and "
//                    + triangleMesh.getFaces().size() / 6 + " faces");
//            }
//
//        } catch (FileNotFoundException ex)
//        {
//            steno.error(ex.toString());
//        } catch (IOException ex)
//        {
//            steno.error(ex.toString());
//        }
//
//        return triangleMesh;
//    }
//
//    private TriangleMesh processAsciiSTLData(File modelFile)
//    {
//        TriangleMesh triangleMesh = new TriangleMesh();
//
//        int linesInFile = getLines(modelFile);
//
//        int progressPercent = 0;
//        int lineNumber = 0;
//
//        HashMap<Vector3D, Integer> graph = new HashMap<>();
//
//        try
//        {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
//                modelFile)));
//
//            String line = null;
//
//            int[] faceIndexArray = new int[6];
//            int vertexCounter = 0;
//            int facetCounter = 0;
//
//            while ((line = reader.readLine()) != null
//                && !parentTask.isCancelled())
//            {
//
//                if (line.trim().startsWith("vertex"))
//                {
//                    facetCounter++;
//
//                    for (int vertexNumber = 0; vertexNumber < 3; vertexNumber++)
//                    {
//                        String[] lineBits = line.trim().split(spacePattern);
//
//                        Vector3D generatedVertex = new Vector3D(
//                            Float.valueOf(lineBits[1]),
//                            -Float.valueOf(lineBits[3]),
//                            Float.valueOf(lineBits[2]));
//
//                        if (!graph.containsKey(generatedVertex))
//                        {
//                            graph.put(generatedVertex, vertexCounter);
//                            faceIndexArray[vertexNumber * 2] = vertexCounter;
//                            vertexCounter++;
//                        } else
//                        {
//                            faceIndexArray[vertexNumber * 2] = graph.get(generatedVertex);
//                        }
//
//                        lineNumber++;
//
//                        if (vertexNumber < 2)
//                        {
//                            line = reader.readLine();
//                        }
//                    }
//
//                    // Add the face to the triangle mesh
//                    triangleMesh.getFaces().addAll(faceIndexArray, 0, 6);
//
//                } else
//                {
//                    lineNumber++;
//                }
//
//                int progressUpdate = (int) (((double) lineNumber / (double) linesInFile) * 100);
//                if (progressUpdate != progressPercent)
//                {
//                    progressPercent = progressUpdate;
//                    percentProgressProperty.set(progressPercent);
//                }
//            }
//
//            reader.close();
//
//            steno.debug("Started with " + facetCounter * 3 + " vertices and now have "
//                + graph.size());
//
//            float[] tempVertexPointArray = new float[3];
//            graph.entrySet()
//                .stream()
//                .sorted((s1, s2) ->
//                    {
//                        if (s1.getValue() == s2.getValue())
//                        {
//                            return 0;
//                        } else if (s1.getValue() > s2.getValue())
//                        {
//                            return 1;
//                        } else
//                        {
//                            return -1;
//                        }
//                })
//                .forEach(vertexEntry ->
//                    {
//                        tempVertexPointArray[0] = (float) vertexEntry.getKey().getX();
//                        tempVertexPointArray[1] = (float) vertexEntry.getKey().getY();
//                        tempVertexPointArray[2] = (float) vertexEntry.getKey().getZ();
//
//                        triangleMesh.getPoints().addAll(tempVertexPointArray, 0, 3);
//                });
//
//            FloatArrayList texCoords = new FloatArrayList();
//            texCoords.add(0f);
//            texCoords.add(0f);
//            triangleMesh.getTexCoords().addAll(texCoords.toFloatArray());
//
//            int[] smoothingGroups = new int[triangleMesh.getFaces().size() / 6];
//            for (int i = 0; i < smoothingGroups.length; i++)
//            {
//                smoothingGroups[i] = 0;
//            }
//            triangleMesh.getFaceSmoothingGroups().addAll(smoothingGroups);
//            steno.debug("The mesh contains " + triangleMesh.getPoints().size()
//                + " points, " + triangleMesh.getTexCoords().size() + " tex coords and "
//                + triangleMesh.getFaces().size() + " faces");
//
//        } catch (FileNotFoundException ex)
//        {
//            steno.error("Failed to open STL file " + modelFile.getAbsolutePath() + " for reading");
//        } catch (IOException ex)
//        {
//            steno.error("IO Exception on line " + lineNumber + " when reading STL file "
//                + modelFile.getAbsolutePath());
//        }
//
//        return triangleMesh;
//    }
}
