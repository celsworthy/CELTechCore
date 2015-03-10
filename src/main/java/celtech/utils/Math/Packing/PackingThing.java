package celtech.utils.Math.Packing;

import celtech.coreUI.visualisation.metaparts.Part;
import celtech.modelcontrol.ModelContainer;
import java.util.ArrayList;
import javafx.collections.ObservableList;

/**
 *
 * @author Ian
 */
public class PackingThing
{

    private final ArrayList<Block> blocks = new ArrayList<>();
    private BinNode rootNode = new BinNode(0, 0, 0, 0);

    /**
     *
     * @param availableWidth
     * @param availableHeight
     */
    public PackingThing(int availableWidth, int availableHeight)
    {
        rootNode.setW(availableWidth);
        rootNode.setH(availableHeight);
    }

    /**
     *
     * @param root
     * @param w
     * @param h
     * @return
     */
    public BinNode findNode(BinNode root, int w, int h)
    {
        if (root.used)
        {
            BinNode result = findNode(root.right, w, h);
            if (result == null)
            {
                result = findNode(root.down, w, h);
            }
            return result;
        } else if ((w <= root.w) && (h <= root.h))
        {
            return root;
        } else
        {
            return null;
        }
    }

    /**
     *
     * @param node
     * @param w
     * @param h
     * @return
     */
    public BinNode splitNode(BinNode node, int w, int h)
    {
        node.used = true;
        node.down = new BinNode(node.x, node.y + h, node.w, node.h - h);
        node.right = new BinNode(node.x + w, node.y, node.w - w, h);
        return node;
    }

    /**
     *
     */
    public void pack()
    {
        blocks.stream().forEach((block) ->
        {
            BinNode node = findNode(rootNode, block.w, block.h);
            if (node != null)
            {
                block.fit = splitNode(node, block.w, block.h);
            }
        });
    }

    /**
     *
     * @param loadedParts
     * @param padding
     */
    public void reference(ObservableList<Part> loadedParts, int padding)
    {
        blocks.clear();
        loadedParts.stream().forEach((model) ->
        {
            blocks.add(new Block(model, padding));
        });
    }

    /**
     *
     */
    public void relocateBlocks()
    {
        blocks.stream().forEach(Block::relocate);
    }
}
