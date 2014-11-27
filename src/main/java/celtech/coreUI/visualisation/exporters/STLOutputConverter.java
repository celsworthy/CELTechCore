/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation.exporters;

import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.modelcontrol.ModelContainer;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Transform;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class STLOutputConverter
{

    private File fFile;
    private Stenographer steno = null;
    private Project project = null;
    private String printJobUUID = null;
    private String tempModelFilenameWithPath = null;
    
    /**
     *
     * @param project
     * @param printJobUUID
     */
    public STLOutputConverter(Project project, String printJobUUID)
    {
        steno = StenographerFactory.getStenographer(this.getClass().getName());

        this.project = project;
        this.printJobUUID = printJobUUID;
        tempModelFilenameWithPath = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator + printJobUUID + ApplicationConfiguration.stlTempFileExtension;
               
        fFile = new File(tempModelFilenameWithPath);
    }

    /**
     *
     */
    public void outputSTLFile()
    {
        DataOutputStream dataOutput = null;
        int totalNumberOfFacets = 0;
        byte blankByte = 0;

        try
        {
            dataOutput = new DataOutputStream(new FileOutputStream(fFile));
            ByteBuffer headerByteBuffer = null;
            ArrayList<float[]> listOfFacetSets = new ArrayList<float[]>();

            Iterator<ModelContainer> printableIter = project.getLoadedModels().iterator();
            while (printableIter.hasNext())
            {
                int numberOfFacets = 0;
                ModelContainer printable = printableIter.next();
//                Node parentNode = printable.getParent();
//                steno.debug(parentNode.getName());
//                steno.debug("Rotation: " + parentNode.getLocalRotation());
//                steno.debug("Scale: " + parentNode.getLocalScale());
//                steno.debug("Translation: " + parentNode.getLocalTranslation());
//                steno.debug(printable.getName());
//                steno.debug("Rotation: " + printable.getLocalRotation());
//                steno.debug("Scale: " + printable.getLocalScale());
//                steno.debug("Translation: " + printable.getLocalTranslation());
//
//                Transform worldTrans = printable.getWorldTransform();

                MeshView meshview = printable.getMeshView();
  
                TriangleMesh triangles = (TriangleMesh)meshview.getMesh();
                
                int[] faces = triangles.getFaces().toArray(null);
                float[] vertexArray = triangles.getPoints().toArray(null);
                
                // Every other 
                numberOfFacets = faces.length / 6;
                
                totalNumberOfFacets += numberOfFacets;
                ByteBuffer facetDataBuffer = ByteBuffer.allocate(50 * numberOfFacets);

                steno.debug("Processing " + printable.getModelName());
                steno.debug("Facets: " + numberOfFacets);

//                float[] normalArray = new float[normalData.capacity()];
//                normalData.clear();
//                normalData.get(normalArray);

//                for (int facetNum = 0; facetNum < numberOfFacets; facetNum = facetNum + 1)
//                {
//                }

                float[] outputArray = new float[numberOfFacets * 12];

                for (int facetNum = 0; facetNum < numberOfFacets; facetNum = facetNum + 1)
                {

//                    float normalX = normalArray[(facetNum * 9)];
//                    float normalY = normalArray[(facetNum * 9) + 1];
//                    float normalZ = normalArray[(facetNum * 9) + 2];
                    
                    float normalX = 0;
                    float normalY = 0;
                    float normalZ = 0;
//                    System.out.println("Normal x " + normalX + " y " + (-normalZ) + " z " + normalY);

//                    Vector3f normal = new Vector3f(normalX, normalY, normalZ);
//                    Vector3f transformedNormal = new Vector3f();
//                    worldTrans.transformVector(normal, transformedNormal);

                    outputArray[facetNum * 12] = normalX;
                    outputArray[(facetNum * 12) + 1] = -normalZ;
                    outputArray[(facetNum * 12) + 2] = normalY;
//                    outputArray[facetNum * 12] = transformedNormal.x;
//                    outputArray[(facetNum * 12) + 1] = -transformedNormal.z;
//                    outputArray[(facetNum * 12) + 2] = transformedNormal.y;

                    for (int vertexNum = 0; vertexNum < 3; vertexNum++)
                    {
//                        Triangle tri = new Triangle();
//                        printable.getMesh().getTriangle(facetNum + i, tri);
//                        System.out.println("Normal " + tri.getNormal().toString());
//                        System.out.println("V1 " + tri.get1().toString());
//                        System.out.println("V2 " + tri.get2().toString());
//                        System.out.println("V3 " + tri.get3().toString());
//
                        int vertexIndex = faces[(facetNum * 6) + (vertexNum * 2)];
                                
                        float vertexX = vertexArray[vertexIndex * 3];
                        float vertexY = vertexArray[(vertexIndex * 3) + 1];
                        float vertexZ = vertexArray[(vertexIndex * 3) + 2];
//                        Vector3f vertex = new Vector3f(vertexX, vertexY, vertexZ);
//                        Vector3f transformedVertex = new Vector3f();
//                        worldTrans.transformVector(vertex, transformedVertex);
//                        steno.info("Vertex was " + vertexNum + " x " + vertexX + " y " + vertexY + " z " + vertexZ);

                        Point3D parentVertex = printable.transformMeshToRealWorldCoordinates(vertexX, vertexY, vertexZ);
//                        steno.info("parent v " + parentVertex);

                        outputArray[(facetNum * 12) + (vertexNum * 3) + 3] = (float)parentVertex.getX();
                        outputArray[(facetNum * 12) + (vertexNum * 3) + 4] = (float)parentVertex.getZ();
                        outputArray[(facetNum * 12) + (vertexNum * 3) + 5] = (float)-parentVertex.getY();
//                        steno.info("Outputting " + vertexX + ":" + vertexZ + ":" + (-vertexY));

//                        outputArray[(facetNum * 12) + (vertexNum * 3) + 3] = transformedVertex.x;
//                        outputArray[(facetNum * 12) + (vertexNum * 3) + 4] = -transformedVertex.z;
//                        outputArray[(facetNum * 12) + (vertexNum * 3) + 5] = transformedVertex.y;

                    }

                }

                listOfFacetSets.add(outputArray);
            }


            //File consists of:
            // 80 byte ascii header
            // Int containing number of facets
            // Then for each facet:
            //  3 floats for facet normals
            //  3 x 3 floats for vertices (x,y,z * 3)
            //  2 byte spacer


            ByteBuffer outputBuffer = ByteBuffer.allocate(80 + (totalNumberOfFacets * 50) + 4);

            ByteBuffer headerBuffer = ByteBuffer.allocate(80);
            headerBuffer.put(("Generated by " + ApplicationConfiguration.getTitleAndVersion()).getBytes("UTF-8"));

            dataOutput.write(headerBuffer.array());

            byte outputByte = (byte) (totalNumberOfFacets & 0xff);
            dataOutput.write(outputByte);

            outputByte = (byte) ((totalNumberOfFacets >>> 8) & 0xff);
            dataOutput.write(outputByte);

            outputByte = (byte) ((totalNumberOfFacets >>> 16) & 0xff);
            dataOutput.write(outputByte);

            outputByte = (byte) ((totalNumberOfFacets >>> 24) & 0xff);
            dataOutput.write(outputByte);

            ByteBuffer outputBuf = ByteBuffer.allocate(totalNumberOfFacets * 50);
            outputBuf.order(ByteOrder.LITTLE_ENDIAN);

            for (float[] facetData : listOfFacetSets)
            {
                for (int floatCount = 0; floatCount < facetData.length; floatCount++)
                {
                    outputBuf.putFloat(facetData[floatCount]);
//                    dataOutput.writeFloat(facetData[floatCount]);
//                    System.out.println("Output " + facetData[floatCount]);

                    if (((floatCount + 1) % 12) == 0)
                    {
//                        System.out.println("Mod " + floatCount);
                        outputBuf.put(blankByte);
                        outputBuf.put(blankByte);
//                        dataOutput.writeByte(blankByte);
//                        dataOutput.writeByte(blankByte);
                    }
                }
            }
            dataOutput.write(outputBuf.array());
        } catch (FileNotFoundException ex)
        {
            steno.error("Error opening file " + fFile + " :" + ex.toString());
        } catch (IOException ex)
        {
            steno.error("Error writing to file " + fFile + " :" + ex.toString());

        } finally
        {
            try
            {
                if (dataOutput != null)
                {
                    dataOutput.flush();
                    dataOutput.close();
                }
            } catch (IOException ex)
            {
                steno.error("Error closing file " + fFile + " :" + ex.toString());
            }
        }



    }
}
