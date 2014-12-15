package celtech.configuration.datafileaccessors;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.HeadFileFilter;
import celtech.configuration.fileRepresentation.HeadFile;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/**
 *
 * @author ianhudson
 */
public class HeadContainer
{

    private static final Stenographer steno = StenographerFactory.getStenographer(HeadContainer.class.getName());
    private static HeadContainer instance = null;
    private static final ObservableList<HeadFile> completeHeadList = FXCollections.observableArrayList();
    private static final ObservableMap<String, HeadFile> completeHeadMap = FXCollections.observableHashMap();
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String defaultHeadID = "RBX01-SM";

    private HeadContainer()
    {
        File applicationHeadDirHandle = new File(ApplicationConfiguration.getApplicationHeadDirectory());
        File[] applicationheads = applicationHeadDirHandle.listFiles(new HeadFileFilter());
        ArrayList<HeadFile> heads = ingestHeads(applicationheads);
        completeHeadList.addAll(heads);
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    }

    private ArrayList<HeadFile> ingestHeads(File[] userheads)
    {
        ArrayList<HeadFile> headList = new ArrayList<>();

        for (File headFile : userheads)
        {
            try
            {
                HeadFile headFileData = mapper.readValue(headFile, HeadFile.class);

                headList.add(headFileData);
                completeHeadMap.put(headFileData.getTypeCode(), headFileData);

            } catch (IOException ex)
            {
                steno.error("Error loading head " + headFile.getAbsolutePath());
            }
        }

        return headList;
    }

    /**
     *
     * @return
     */
    public static HeadContainer getInstance()
    {
        if (instance == null)
        {
            instance = new HeadContainer();
        }

        return instance;
    }

    /**
     *
     * @param headID
     * @return
     */
    public static HeadFile getHeadByID(String headID)
    {
        if (instance == null)
        {
            HeadContainer.getInstance();
        }

        HeadFile returnedHead = completeHeadMap.get(headID);
        return returnedHead;
    }

    /**
     *
     * @return
     */
    public static ObservableList<HeadFile> getCompleteHeadList()
    {
        if (instance == null)
        {
            instance = new HeadContainer();
        }

        return completeHeadList;
    }
}
