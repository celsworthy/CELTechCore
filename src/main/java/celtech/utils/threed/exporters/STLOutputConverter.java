/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils.threed.exporters;

import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class STLOutputConverter implements MeshFileOutputConverter
{

    private Stenographer steno = null;
    private int modelFileCount = 0;

    public STLOutputConverter()
    {
        steno = StenographerFactory.getStenographer(this.getClass().getName());
    }

    @Override
    public List<String> outputFile(Project project, String printJobUUID, boolean outputAsSingleFile)
    {
        return outputFile(project, printJobUUID, ApplicationConfiguration.getPrintSpoolDirectory()
                + printJobUUID + File.separator, outputAsSingleFile);
    }

    @Override
    public List<String> outputFile(Project project, String printJobUUID, String printJobDirectory,
            boolean outputAsSingleFile)
    {
        List<String> createdFiles = new ArrayList<>();
        modelFileCount = 0;

        if (outputAsSingleFile)
        {
            List<MeshView> meshViewsToOutput = new ArrayList<>();

            project.getLoadedModels().stream()
                    .map(ModelContainer::getMeshViews)
                    .forEach(meshViewList -> meshViewList
                            .forEach(meshView -> meshViewsToOutput.add(meshView)));

            String tempModelFilenameWithPath = printJobDirectory + printJobUUID
                    + ApplicationConfiguration.stlTempFileExtension;

            createdFiles.add(tempModelFilenameWithPath);

//            outputMeshViewsInSingleFile(tempModelFilenameWithPath, meshViewsToOutput, project.);
        } else
        {

            for (ModelContainer modelContainer : project.getLoadedModels())
            {
                if (modelContainer instanceof ModelGroup)
                {
                    ModelGroup group = (ModelGroup) modelContainer;
                    for (ModelContainer container : group.getChildModelContainers())
                    {
                        outputAllMeshesFromContainer(container, createdFiles, printJobUUID, printJobDirectory, group);
                    }
                } else
                {
                    outputAllMeshesFromContainer(modelContainer, createdFiles, printJobUUID, printJobDirectory);
                }
            }
        }

        return createdFiles;
    }

    private void outputAllMeshesFromContainer(ModelContainer container,
            List<String> createdFiles,
            String printJobUUID, String printJobDirectory)
    {
        outputAllMeshesFromContainer(container, createdFiles, printJobUUID, printJobDirectory, container);
    }
    
    private void outputAllMeshesFromContainer(ModelContainer container,
            List<String> createdFiles,
            String printJobUUID, String printJobDirectory,
            ModelContainer containerWithWorldTransform)
    {
        if (container instanceof ModelGroup)
        {
            for (ModelContainer subModel : ((ModelGroup)container).getChildModelContainers())
            {
                outputAllMeshesFromContainer(container, createdFiles, printJobUUID, printJobDirectory, container);
            }
        } else
        {
            for (MeshView meshView
                    : container.getMeshViews())
            {
                String tempModelFilenameWithPath = printJobDirectory + printJobUUID
                        + "-" + modelFileCount + ApplicationConfiguration.stlTempFileExtension;

                List<MeshView> meshViewsToOutput = new ArrayList<>();
                meshViewsToOutput.add(meshView);
                outputMeshViewsInSingleFile(tempModelFilenameWithPath, meshViewsToOutput, containerWithWorldTransform);
                createdFiles.add(tempModelFilenameWithPath);
                modelFileCount++;
            }
        }
    }

    private void outputMeshViewsInSingleFile(final String tempModelFilenameWithPath,
            List<MeshView> meshViewsToOutput,
            ModelContainer containerWithWorldTransforms)
    {
        File fFile = new File(tempModelFilenameWithPath);

        final short blankSpace = (short) 0;

        try
        {
            final DataOutputStream dataOutput = new DataOutputStream(new FileOutputStream(fFile));

            try
            {
                int totalNumberOfFacets = 0;
                ByteBuffer headerByteBuffer = null;

                for (MeshView meshView : meshViewsToOutput)
                {
                    TriangleMesh triangles = (TriangleMesh) meshView.getMesh();
                    ObservableFaceArray faceArray = triangles.getFaces();
                    int numberOfFacets = faceArray.size() / 6;
                    totalNumberOfFacets += numberOfFacets;
                }

                //File consists of:
                // 80 byte ascii header
                // Int containing number of facets
                ByteBuffer headerBuffer = ByteBuffer.allocate(80);
                headerBuffer.put(("Generated by " + ApplicationConfiguration.getTitleAndVersion()).
                        getBytes("UTF-8"));

                dataOutput.write(headerBuffer.array());

                byte outputByte = (byte) (totalNumberOfFacets & 0xff);
                dataOutput.write(outputByte);

                outputByte = (byte) ((totalNumberOfFacets >>> 8) & 0xff);
                dataOutput.write(outputByte);

                outputByte = (byte) ((totalNumberOfFacets >>> 16) & 0xff);
                dataOutput.write(outputByte);

                outputByte = (byte) ((totalNumberOfFacets >>> 24) & 0xff);
                dataOutput.write(outputByte);

                ByteBuffer dataBuffer = ByteBuffer.allocate(50);
                //Binary STL files are always assumed to be little endian
                dataBuffer.order(ByteOrder.LITTLE_ENDIAN);

                // Then for each facet:
                //  3 floats for facet normals
                //  3 x 3 floats for vertices (x,y,z * 3)
                //  2 byte spacer
                for (MeshView meshView : meshViewsToOutput)
                {
                    TriangleMesh triangles = (TriangleMesh) meshView.getMesh();
                    ObservableFaceArray faceArray = triangles.getFaces();
                    ObservableFloatArray pointArray = triangles.getPoints();
                    int numberOfFacets = faceArray.size() / 6;

                    for (int facetNumber = 0; facetNumber < numberOfFacets; facetNumber++)
                    {
                        dataBuffer.rewind();
                        // Output zero normals
                        dataBuffer.putFloat(0);
                        dataBuffer.putFloat(0);
                        dataBuffer.putFloat(0);

                        for (int vertexNumber = 0; vertexNumber < 3; vertexNumber++)
                        {
                            int vertexIndex = faceArray.get((facetNumber * 6) + (vertexNumber * 2));

                            Point3D vertex = containerWithWorldTransforms
                                    .transformMeshToRealWorldCoordinates(
                                            pointArray.get(vertexIndex * 3),
                                            pointArray.get((vertexIndex * 3) + 1),
                                            pointArray.get((vertexIndex * 3) + 2));

                            dataBuffer.putFloat((float) vertex.getX());
                            dataBuffer.putFloat((float) vertex.getZ());
                            dataBuffer.putFloat(-(float) vertex.getY());

                        }
                        dataBuffer.putShort(blankSpace);

                        dataOutput.write(dataBuffer.array());
                    }
                }
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
        } catch (FileNotFoundException ex)
        {
            steno.error("Error opening STL output file " + fFile + " :" + ex.toString());
        }

    }
}
