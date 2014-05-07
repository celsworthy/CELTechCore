/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation.importers.stl;

import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.configuration.PrintBed;
import celtech.coreUI.components.ProjectTab;
import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.coreUI.visualisation.importers.FloatArrayList;
import celtech.coreUI.visualisation.importers.IntegerArrayList;
import celtech.coreUI.visualisation.importers.MetaTriangle;
import celtech.coreUI.visualisation.modelDisplay.ModelBounds;
import celtech.modelcontrol.ModelContainer;
import celtech.services.modelLoader.ModelLoaderTask;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point3D;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class STLImporter
{

    private final Stenographer steno = StenographerFactory.getStenographer(STLImporter.class.getName());
    private STLLoadState loadState = STLLoadState.IDLE;
    private TriangleMesh meshToOutput = null;
    private ModelLoaderTask parentTask = null;
    private int vertexGroupCount = 0;
    private DoubleProperty percentProgressProperty = null;
    private StringBuilder hashStringHolder = new StringBuilder();
    private final int VERTICES_PER_TRIANGLE = 3;
    private final int POINTS_PER_VERTEX = 3;
    private final int POINTS_PER_TRIANGLE = POINTS_PER_VERTEX * VERTICES_PER_TRIANGLE;
    private final int FACE_INDICES_PER_VERTEX = 2;
    private final int FACE_INDICES_PER_TRIANGLE = FACE_INDICES_PER_VERTEX * POINTS_PER_VERTEX;
    private final String spacePattern = "[ ]+";

    public ModelLoadResult loadFile(ModelLoaderTask parentTask, String modelFileToLoad, ProjectTab targetProjectTab, DoubleProperty percentProgressProperty)
    {
        this.parentTask = parentTask;
        this.percentProgressProperty = percentProgressProperty;
        boolean fileIsBinary;
        boolean modelIsTooLarge = false;
        File modelFile = new File(modelFileToLoad);

        steno.info("Starting STL load");

        //Note that FileReader is used, not File, since File is not Closeable
        try
        {
            Scanner scanner = new Scanner(new FileReader(modelFile));
            fileIsBinary = isFileBinary(modelFile);
            int lineNumber = 1;

            if (!fileIsBinary)
            {
                steno.debug("I have an ASCII file");
            } else
            {
                steno.debug("I'm guessing I have a binary file");
                fileIsBinary = true;
            }

            try
            {
                if (fileIsBinary)
                {
                    meshToOutput = processBinarySTLData(modelFile);

                } else
                {

                    meshToOutput = processAsciiSTLData(modelFile);

//                    if (!parentTask.isCancelled())
//                    {
//                        meshToOutput = simplifyMesh(inputTriangles, hashedVertices);
//                    }
                }
            } catch (STLFileParsingException ex)
            {
                steno.error("File parsing exception whilst processing " + modelFile.getName() + " : " + ex + " on line " + lineNumber);
            } finally
            {
                //ensure the underlying stream is always closed
                //this only has any effect if the item passed to the Scanner
                //constructor implements Closeable (which it does in this case).
                scanner.close();
            }

        } catch (FileNotFoundException ex)
        {
            steno.error("Couldn't find or open " + modelFile.getName());
        }

        steno.info("loaded and processing mesh");

        if (!parentTask.isCancelled())
        {
            MeshView meshView = new MeshView();

//            float[] pointArray = meshToOutput.getPoints().toArray(null);
//            int[] faceArray = meshToOutput.getFaces().toArray(null);
//
//            for (int i = 0; i < pointArray.length; i++)
//            {
//                steno.info("Point " + i + ":" + pointArray[i]);
//            }
//
//            for (int i = 0; i < faceArray.length; i++)
//            {
//                steno.info("Face " + i + ":" + faceArray[i]);
//            }
            meshView.setMesh(meshToOutput);
            meshView.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
            meshView.setCullFace(CullFace.BACK);
            meshView.setId(modelFile.getName() + "_mesh");

            ModelContainer modelContainer = new ModelContainer(modelFile.getName(), meshView);

            BoundingBox bounds = (BoundingBox) modelContainer.getBoundsInLocal();
            steno.info("Model bounds are : " + bounds);
            ModelBounds originalBounds = modelContainer.getOriginalModelBounds();
            steno.info("Model orig bounds are : " + originalBounds);
            modelIsTooLarge = PrintBed.isBiggerThanPrintVolume(originalBounds);

            ModelLoadResult result = new ModelLoadResult(modelIsTooLarge, modelFileToLoad, modelFile.getName(), targetProjectTab, modelContainer);
            steno.info("Done");
            return result;
        } else
        {
            return null;
        }
    }

    private String quote(String aText)
    {
        String QUOTE = "'";
        return QUOTE + aText + QUOTE;
    }

    @SuppressWarnings("empty-statement")
    private int getLines(File aFile)
    {
        LineNumberReader reader = null;
        try
        {
            reader = new LineNumberReader(new FileReader(aFile));
            while ((reader.readLine()) != null);
            return reader.getLineNumber();
        } catch (Exception ex)
        {
            return -1;
        } finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                } catch (IOException ex)
                {
                    steno.error("Failed to close file during line number read: " + ex);
                }
            }
        }
    }

    private boolean isFileBinary(File stlFile)
    {
        boolean fileIsBinary = false;
        BufferedInputStream inputFileStream;
        ByteBuffer dataBuffer;
        byte[] facetBytes = new byte[4];     // Holds the number of faces

        try
        {
            inputFileStream = new BufferedInputStream(new FileInputStream(stlFile));
            inputFileStream.mark(4000);
            byte[] asciiHeaderBytes = new byte[80];
            int bytesRead = inputFileStream.read(asciiHeaderBytes);
            String asciiHeader = new String(asciiHeaderBytes, "UTF-8");
            steno.debug("The header was: " + asciiHeader);

            bytesRead = inputFileStream.read(facetBytes);                      // We get the 4 bytes
            dataBuffer = ByteBuffer.wrap(facetBytes);   // ByteBuffer for reading correctly the int
            dataBuffer.order(ByteOrder.nativeOrder());    // Set the right order
            int numberOfFacets = dataBuffer.getInt();

            int filesize = (numberOfFacets * 50) + 84;
            inputFileStream.reset();

            if (stlFile.length() == filesize)
            {
                fileIsBinary = true;
            }
        } catch (IOException ex)
        {
            steno.error("Failed to determine whether " + stlFile.getName() + " was binary or ascii." + ex.toString());
        }

        return fileIsBinary;
    }

    protected TriangleMesh processBinarySTLData(File stlFile) throws STLFileParsingException
    {
        DataInputStream inputFileStream;
        ByteBuffer dataBuffer;
        byte[] facetBytes = new byte[4];     // Holds the number of faces
        byte[] facetData;
        int progressPercent = 0;

        TriangleMesh triangleMesh = new TriangleMesh();
        try
        {
            inputFileStream = new DataInputStream(new FileInputStream(stlFile));
            byte[] asciiHeaderBytes = new byte[80];
            inputFileStream.read(asciiHeaderBytes);
            String asciiHeader = new String(asciiHeaderBytes, "UTF-8");
            steno.debug("The header was: " + asciiHeader);

            inputFileStream.read(facetBytes);                      // We get the 4 bytes
            dataBuffer = ByteBuffer.wrap(facetBytes);   // ByteBuffer for reading correctly the int
            dataBuffer.order(ByteOrder.nativeOrder());    // Set the right order
            int numberOfFacets = dataBuffer.getInt();

            steno.debug("There are " + numberOfFacets + " facets");

            facetData = new byte[50 * numberOfFacets];        // Each face has 50 bytes of data
            inputFileStream.read(facetData);                         // We get the rest of the file
            dataBuffer = ByteBuffer.wrap(facetData);      // Now we have all the data in this ByteBuffer
            dataBuffer.order(ByteOrder.nativeOrder());

            float[] points = new float[numberOfFacets * VERTICES_PER_TRIANGLE * POINTS_PER_VERTEX];
            int[] faces = new int[numberOfFacets * FACE_INDICES_PER_TRIANGLE];

            float[] vertexXArray = new float[3];
            float[] vertexYArray = new float[3];
            float[] vertexZArray = new float[3];

            for (int facetNum = 0; facetNum < numberOfFacets; facetNum++)
            {
                if (parentTask.isCancelled())
                {
                    break;
                }

                int progressUpdate = (int) (((double) facetNum / (double) numberOfFacets) * 100);
                if (progressUpdate != progressPercent)
                {
                    progressPercent = progressUpdate;
                    percentProgressProperty.set(progressPercent);
                }
                // Read the Normal and place it 3 times (one for each vertex)
                dataBuffer.getFloat();
                dataBuffer.getFloat();
                dataBuffer.getFloat();

                for (int vertexNumber = 0; vertexNumber < 3; vertexNumber++)
                {
                    vertexXArray[vertexNumber] = dataBuffer.getFloat();
                    vertexYArray[vertexNumber] = dataBuffer.getFloat();
                    vertexZArray[vertexNumber] = dataBuffer.getFloat();
                }

                for (int vertexNumber = 0; vertexNumber < 3; vertexNumber++)
                {
                    float inputVertexX, inputVertexY, inputVertexZ;

                    inputVertexX = vertexXArray[vertexNumber];
                    inputVertexY = vertexYArray[vertexNumber];
                    inputVertexZ = vertexZArray[vertexNumber];

                    int baseIndex = (facetNum * POINTS_PER_TRIANGLE) + (vertexNumber * POINTS_PER_VERTEX);

                    points[baseIndex] = inputVertexX;
                    points[baseIndex + 1] = -inputVertexZ;
                    points[baseIndex + 2] = inputVertexY;

                    int faceIndex = (facetNum * FACE_INDICES_PER_TRIANGLE) + (vertexNumber * FACE_INDICES_PER_VERTEX);
                    faces[faceIndex] = (facetNum * POINTS_PER_VERTEX) + vertexNumber;
                    faces[faceIndex + 1] = 0;
                }

                // After each facet there are 2 bytes without information
                // In the last iteration we dont have to skip those bytes..
                if (facetNum != numberOfFacets - 1)
                {
                    dataBuffer.get();
                    dataBuffer.get();
                }
            }

            FloatArrayList texCoords = new FloatArrayList();
            texCoords.add(0f);
            texCoords.add(0f);

            triangleMesh.getPoints().addAll(points);
            triangleMesh.getTexCoords().addAll(texCoords.toFloatArray());
            triangleMesh.getFaces().addAll(faces);
            int[] smoothingGroups = new int[faces.length / 6];
            for (int i = 0; i < smoothingGroups.length; i++)
            {
                smoothingGroups[i] = 0;
            }
            triangleMesh.getFaceSmoothingGroups().addAll(smoothingGroups);

            steno.info("The mesh contains " + triangleMesh.getPoints().size()
                    + " points, " + triangleMesh.getTexCoords().size() + " tex coords and "
                    + triangleMesh.getFaces().size() + " faces");

        } catch (FileNotFoundException ex)
        {
            steno.error(ex.toString());
        } catch (IOException ex)
        {
            steno.error(ex.toString());
        }

        return triangleMesh;
    }

    protected void processASCIILine(String aLine, ArrayList<MetaTriangle> inputTriangles, HashMap<Long, Point3D> hashedVertices, Point3D[] facetVertices) throws STLFileParsingException
    {
        int vertexCounter = 0;
        //Get rid of the leading and trailing spaces
        aLine = aLine.trim();

        //use a second Scanner to parse the content of each line 
        Scanner scanner = new Scanner(aLine);
        scanner.useDelimiter(" +");
        if (scanner.hasNext())
        {
            /*
             solid CATIA STL
             facet normal -3.135569e-002 -9.972335e-001  6.739524e-002
             outer loop
             vertex -5.647915e+000 -1.535569e+002  3.085470e+000
             vertex -7.534769e-012 -1.539431e+002  4.116263e-012
             vertex -4.773464e+000 -1.533782e+002  6.137561e+000
             endloop
             endfacet
             endsolid CATIA STL
             */
            switch (loadState)
            {
                case IDLE:
                    enforceNextStringToken(scanner, "solid", STLLoadState.ASCII_FILE_STARTED);
                    break;

                case ASCII_FILE_STARTED:
                    enforceNextStringToken(scanner, "facet", STLLoadState.FACET_STARTED);
                    enforceNextStringToken(scanner, "normal", STLLoadState.FACET_STARTED);
                    scanner.nextFloat();
                    scanner.nextFloat();
                    scanner.nextFloat();
                    break;
                case FACET_ENDED:
                    if (testNextStringToken(scanner, "endsolid"))
                    {
                        loadState = STLLoadState.FILE_ENDED_OK;

//                        meshToOutput = simplifyMesh(inputTriangles, hashedVertices);
                    } else
                    {
                        //Store the last facet
//                        String vertex1Hash = createVertexHash(facetVertices[0]);
//                        hashedVertices.put(vertex1Hash, facetVertices[0]);
//                        Long vertex1Hash = getHashValue(facetVertices[0]);
//                        hashedVertices.put(vertex1Hash, facetVertices[0]);

//                        String vertex2Hash = createVertexHash(facetVertices[1]);
//                        hashedVertices.put(vertex2Hash, facetVertices[1]);
//                        Long vertex2Hash = getHashValue(facetVertices[1]);
//                        hashedVertices.put(vertex2Hash, facetVertices[1]);
//                        String vertex3Hash = createVertexHash(facetVertices[2]);
//                        hashedVertices.put(vertex3Hash, facetVertices[2]);
//                        Long vertex3Hash = getHashValue(facetVertices[2]);
//                        hashedVertices.put(vertex3Hash, facetVertices[2]);
//                        MetaTriangle metaTriangle = new MetaTriangle(vertex1Hash, vertex2Hash, vertex3Hash);
//                        inputTriangles.add(metaTriangle);
                        enforceNextStringToken(scanner, "facet", STLLoadState.FACET_STARTED);
                        enforceNextStringToken(scanner, "normal", STLLoadState.FACET_STARTED);
                        scanner.nextFloat();
                        scanner.nextFloat();
                        scanner.nextFloat();
                        //Ignore the normals - javafx will calculate them for us
//                        normalList.add(new Point3D(x, z, -y));
//                        normalList.add(new Point3D(x, z, -y));
//                        normalList.add(new Point3D(x, z, -y));
                    }
                    break;

                case FACET_STARTED:
                    enforceNextStringToken(scanner, "outer", STLLoadState.LOOP_STARTED);
                    break;

                case LOOP_STARTED:
                    if (testNextStringToken(scanner, "endloop"))
                    {
                        if (vertexGroupCount != 3)
                        {
                            throw new STLFileParsingException("Loop ended with less than 3 vertices - not a triangle!");
                        } else
                        {
                            loadState = STLLoadState.LOOP_ENDED;
                        }
                    } else
                    {

                        enforceNextStringToken(scanner, "vertex", STLLoadState.LOOP_STARTED);

                        if (vertexGroupCount > 2)
                        {
                            throw new STLFileParsingException("Found shape with more than 3 vertices - not a triangle!");
                        }

                        float vertexX = scanner.nextFloat();
                        float vertexY = scanner.nextFloat();
                        float vertexZ = scanner.nextFloat();
                        Point3D vertex = new Point3D(vertexX, -vertexZ, vertexY);
                        facetVertices[vertexGroupCount] = vertex;
                        vertexGroupCount++;
                        vertexCounter++;
                    }
                    break;

                case LOOP_ENDED:
                    enforceNextStringToken(scanner, "endfacet", STLLoadState.FACET_ENDED);
                    vertexGroupCount = 0;
                    break;

                default:
                    steno.error("unrecognised state");
                    throw new STLFileParsingException("Parser in unknown state");
            }
        } else
        {
            steno.warning("Empty or invalid line. Unable to process.");
        }

        //no need to call scanner.close(), since the source is a String
    }

    boolean enforceNextStringToken(Scanner scanner, String tokenToFind, STLLoadState nextState) throws STLFileParsingException
    {
        boolean tokenPresent = false;

        if (scanner.next().equalsIgnoreCase(tokenToFind))
        {
            tokenPresent = true;
            //log("Got token: " + tokenToFind);
            loadState = nextState;
        } else
        {
            throw new STLFileParsingException("Error in file - expected " + tokenToFind);
        }
        return tokenPresent;
    }

    boolean testNextStringToken(Scanner scanner, String tokenToFind)
    {
        boolean tokenPresent = false;

        if (scanner.hasNext(tokenToFind))
        {
            tokenPresent = true;
            //log("Next token is confirmed as : " + tokenToFind);
        }
        return tokenPresent;
    }

    private TriangleMesh processAsciiSTLData(File modelFile)
    {
        TriangleMesh triangleMesh = new TriangleMesh();

        int linesInFile = getLines(modelFile);

        int progressPercent = 0;
        int lineNumber = 0;

        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(modelFile)));

            FloatArrayList points = new FloatArrayList();
            IntegerArrayList faces = new IntegerArrayList();

            int pointCounter = 0;

            String line = null;

            while ((line = reader.readLine()) != null
                    && !parentTask.isCancelled())
            {

                if (line.trim().startsWith("vertex"))
                {
                    String[] lineBits = line.trim().split(spacePattern);
                    points.add(Float.valueOf(lineBits[1]));
                    points.add(-Float.valueOf(lineBits[3]));
                    points.add(Float.valueOf(lineBits[2]));
                    faces.add(pointCounter);
                    faces.add(0);
                    pointCounter++;
                    lineNumber++;

                    line = reader.readLine();
                    lineBits = line.trim().split(spacePattern);
                    points.add(Float.valueOf(lineBits[1]));
                    points.add(-Float.valueOf(lineBits[3]));
                    points.add(Float.valueOf(lineBits[2]));
                    faces.add(pointCounter);
                    faces.add(0);
                    pointCounter++;
                    lineNumber++;

                    line = reader.readLine();
                    lineBits = line.trim().split(spacePattern);
                    points.add(Float.valueOf(lineBits[1]));
                    points.add(-Float.valueOf(lineBits[3]));
                    points.add(Float.valueOf(lineBits[2]));
                    faces.add(pointCounter);
                    faces.add(0);
                    pointCounter++;
                    lineNumber++;
                } else
                {
                    lineNumber++;
                }

                int progressUpdate = (int) (((double) lineNumber / (double) linesInFile) * 100);
                if (progressUpdate != progressPercent)
                {
                    progressPercent = progressUpdate;
                    percentProgressProperty.set(progressPercent);
                }
            }

            reader.close();

            FloatArrayList texCoords = new FloatArrayList();
            texCoords.add(0f);
            texCoords.add(0f);

            triangleMesh.getPoints().addAll(points.toFloatArray());
            triangleMesh.getTexCoords().addAll(texCoords.toFloatArray());
            triangleMesh.getFaces().addAll(faces.toIntArray());
            int[] smoothingGroups = new int[faces.size() / 6];
            for (int i = 0; i < smoothingGroups.length; i++)
            {
                smoothingGroups[i] = 0;
            }
            triangleMesh.getFaceSmoothingGroups().addAll(smoothingGroups);

            steno.info("The mesh contains " + triangleMesh.getPoints().size()
                    + " points, " + triangleMesh.getTexCoords().size() + " tex coords and "
                    + triangleMesh.getFaces().size() + " faces");

        } catch (FileNotFoundException ex)
        {
            steno.error("Failed to open STL file " + modelFile.getAbsolutePath() + " for reading");
        } catch (IOException ex)
        {
            steno.error("IO Exception on line " + lineNumber + " when reading STL file " + modelFile.getAbsolutePath());
        }

        return triangleMesh;
    }
}
