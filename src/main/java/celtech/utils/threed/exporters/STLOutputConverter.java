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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Transform;
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

            project.getTopLevelModels().stream().forEach(mc -> meshViewsToOutput.addAll(
                    mc.descendentMeshViews()));

            String tempModelFilenameWithPath = printJobDirectory + printJobUUID
                    + ApplicationConfiguration.stlTempFileExtension;

            createdFiles.add(tempModelFilenameWithPath);

            outputMeshViewsInSingleFile(tempModelFilenameWithPath, meshViewsToOutput);
        } else
        {
            //Collate a list of models for each extruder
            Map<Integer, List<MeshView>> modelsAgainstExtruders = new HashMap<>();
            List<MeshView> extruder0Models = new ArrayList<>();
            List<MeshView> extruder1Models = new ArrayList<>();
            modelsAgainstExtruders.put(0, extruder0Models);
            modelsAgainstExtruders.put(1, extruder1Models);

            for (ModelContainer modelContainer : project.getTopLevelModels())
            {
                if (!(modelContainer instanceof ModelGroup))
                {
                    modelsAgainstExtruders.get(modelContainer.getAssociateWithExtruderNumberProperty().get()).add(modelContainer.getMeshView());
                }

                Set<ModelContainer> descendents = modelContainer.getDescendentModelContainers();
                descendents.stream().forEach(descendent ->
                {
                    if (!(descendent instanceof ModelGroup))
                    {
                        modelsAgainstExtruders.get(descendent.getAssociateWithExtruderNumberProperty().get()).add(descendent.getMeshView());
                    }
                });
            }

            String extruder0ModelFilename = printJobDirectory + printJobUUID
                    + "_E" + ApplicationConfiguration.stlTempFileExtension;
            createdFiles.add(extruder0ModelFilename);
            outputMeshViewsInSingleFile(extruder0ModelFilename, extruder0Models);

            String extruder1ModelFilename = printJobDirectory + printJobUUID
                    + "_D" + ApplicationConfiguration.stlTempFileExtension;
            createdFiles.add(extruder1ModelFilename);
            outputMeshViewsInSingleFile(extruder1ModelFilename, extruder1Models);
        }

        return createdFiles;
    }

    private void outputAllMeshesFromContainer(ModelContainer container,
            List<String> createdFiles,
            String printJobUUID, String printJobDirectory)
    {
        for (MeshView meshView : container.descendentMeshViews())
        {
            String tempModelFilenameWithPath = printJobDirectory + printJobUUID
                    + "-" + modelFileCount + ApplicationConfiguration.stlTempFileExtension;
            List<MeshView> meshViewToOutput = new ArrayList<>();
            meshViewToOutput.add(meshView);
            outputMeshViewsInSingleFile(tempModelFilenameWithPath, meshViewToOutput);
            createdFiles.add(tempModelFilenameWithPath);
            modelFileCount++;
        }
    }

    private void outputMeshViewsInSingleFile(final String tempModelFilenameWithPath,
            List<MeshView> meshViews)
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

                for (MeshView meshView : meshViews)
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
                for (MeshView meshView : meshViews)
                {
                    TriangleMesh triangles = (TriangleMesh) meshView.getMesh();
                    int[] faceArray = triangles.getFaces().toArray(null);
                    float[] pointArray = triangles.getPoints().toArray(null);
                    int numberOfFacets = faceArray.length / 6;
                    
                    for (int facetNumber = 0; facetNumber < numberOfFacets; facetNumber++)
                    {
                        dataBuffer.rewind();
                        // Output zero normals
                        dataBuffer.putFloat(0);
                        dataBuffer.putFloat(0);
                        dataBuffer.putFloat(0);

                        for (int vertexNumber = 0; vertexNumber < 3; vertexNumber++)
                        {
                            int vertexIndex = faceArray[(facetNumber * 6) + (vertexNumber * 2)];

                            Point3D vertex = ((ModelContainer) meshView.getParent()).
                                    transformMeshToRealWorldCoordinates(
                                            pointArray[vertexIndex * 3],
                                            pointArray[(vertexIndex * 3) + 1],
                                            pointArray[(vertexIndex * 3) + 2]);

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
