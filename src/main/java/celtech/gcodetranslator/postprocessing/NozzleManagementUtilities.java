package celtech.gcodetranslator.postprocessing;

import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.NozzleData;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SkinSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SupportInterfaceSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SupportSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Ian
 */
public class NozzleManagementUtilities
{

    private final List<NozzleProxy> nozzleProxies;
    private final SlicerParametersFile slicerParametersFile;
    private final HeadFile headFile;

    public NozzleManagementUtilities(List<NozzleProxy> nozzleProxies,
            SlicerParametersFile slicerParametersFile,
            HeadFile headFile)
    {
        this.nozzleProxies = nozzleProxies;
        this.slicerParametersFile = slicerParametersFile;
        this.headFile = headFile;
    }

    protected NozzleProxy chooseNozzleProxyByTask(final GCodeEventNode node) throws UnableToFindSectionNodeException
    {
        NozzleProxy nozzleProxy = null;

        //Go up through the parents until we either reach the top or find a section node
        GCodeEventNode foundNode = null;
        GCodeEventNode searchNode = node;

        do
        {
            if (searchNode instanceof SectionNode)
            {
                foundNode = searchNode;
                break;
            } else
            {
                if (searchNode.hasParent())
                {
                    searchNode = searchNode.getParent().get();
                }
            }
        } while (searchNode.hasParent());

        if (foundNode == null)
        {
            String outputMessage;
            if (node instanceof Renderable)
            {
                outputMessage = "Unable to find section parent of " + ((Renderable) node).renderForOutput();
            } else
            {
                outputMessage = "Unable to find section parent of " + node.toString();
            }
            throw new UnableToFindSectionNodeException(outputMessage);
        }

        if (foundNode instanceof FillSectionNode)
        {
            nozzleProxy = nozzleProxies.get(slicerParametersFile.getFillNozzle());
        } else if (foundNode instanceof OuterPerimeterSectionNode)
        {
            nozzleProxy = nozzleProxies.get(slicerParametersFile.getPerimeterNozzle());
        } else if (foundNode instanceof InnerPerimeterSectionNode)
        {
            nozzleProxy = nozzleProxies.get(slicerParametersFile.getPerimeterNozzle());
        } else if (foundNode instanceof SupportSectionNode)
        {
            nozzleProxy = nozzleProxies.get(slicerParametersFile.getSupportNozzle());
        } else if (foundNode instanceof SupportInterfaceSectionNode)
        {
            nozzleProxy = nozzleProxies.get(slicerParametersFile.getSupportInterfaceNozzle());
        } else if (foundNode instanceof SkinSectionNode)
        {
            nozzleProxy = nozzleProxies.get(slicerParametersFile.getFillNozzle());
        } else
        {
            nozzleProxy = nozzleProxies.get(slicerParametersFile.getFillNozzle());
        }
        return nozzleProxy;
    }

    protected Optional<NozzleProxy> chooseNozzleProxyByExtruderNumber(final int extruderNumber)
    {
        Optional<NozzleProxy> nozzleProxy = Optional.empty();

        String extruderLetter = "";

        switch (extruderNumber)
        {
            case 0:
                extruderLetter = "E";
                break;
            case 1:
                extruderLetter = "D";
                break;
        }

        for (int nozzleIndex = 0; nozzleIndex < headFile.getNozzles().size(); nozzleIndex++)
        {
            NozzleData nozzleData = headFile.getNozzles().get(nozzleIndex);
            if (nozzleData.getAssociatedExtruder().equals(extruderLetter))
            {
                nozzleProxy = Optional.of(nozzleProxies.get(nozzleIndex));
                break;
            }
        }

        return nozzleProxy;
    }

    protected Optional<NozzleProxy> determineNozzleStateAtEndOfLayer(LayerNode layerNode)
    {
        Optional<NozzleProxy> nozzleInUse = Optional.empty();

        Iterator<GCodeEventNode> layerIterator = layerNode.childBackwardsIterator();

        search:
        while (layerIterator.hasNext())
        {
            GCodeEventNode potentialToolSelectNode = layerIterator.next();

            if (potentialToolSelectNode instanceof ToolSelectNode)
            {
                ToolSelectNode lastToolSelect = (ToolSelectNode) potentialToolSelectNode;

                Iterator<GCodeEventNode> toolSelectChildIterator = lastToolSelect.childrenAndMeBackwardsIterator();
                while (toolSelectChildIterator.hasNext())
                {
                    GCodeEventNode potentialNozzleValvePositionNode = toolSelectChildIterator.next();

                    if (potentialNozzleValvePositionNode instanceof NozzleValvePositionNode)
                    {
                        NozzleValvePositionNode nozzleNode = (NozzleValvePositionNode) potentialNozzleValvePositionNode;
                        NozzleProxy proxy = nozzleProxies.get(lastToolSelect.getToolNumber());
                        proxy.setCurrentPosition(nozzleNode.getNozzlePosition().getB());
                        nozzleInUse = Optional.of(proxy);
                        break search;
                    }
                }
            }
        }

        return nozzleInUse;
    }
}
