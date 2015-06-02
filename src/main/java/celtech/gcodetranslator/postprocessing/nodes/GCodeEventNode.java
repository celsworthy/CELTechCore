package celtech.gcodetranslator.postprocessing.nodes;

import java.util.AbstractList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.parboiled.trees.MutableTreeNodeImpl;

/**
 *
 * @author Ian
 */
public abstract class GCodeEventNode extends MutableTreeNodeImpl<GCodeEventNode>
{

    public GCodeEventNode()
    {
    }

    /**
     *
     * @return
     */
    public abstract String renderForOutput();

    public boolean isLeaf()
    {
        return getChildren().isEmpty();
    }

    public boolean hasParent()
    {
        return getParent() != null;
    }

    public static <T> List<T> reversedView(final List<T> list)
    {
        return new AbstractList<T>()
        {
            @Override
            public T get(int index)
            {
                return list.get(list.size() - 1 - index);
            }

            @Override
            public int size()
            {
                return list.size();
            }
        };
    }

    public Stream<GCodeEventNode> stream()
    {
        if (isLeaf())
        {
            return Stream.of(this);
        } else
        {
            return getChildren().stream()
                    .map(childNode -> childNode.stream())
                    .reduce(Stream.of(this), (s1, s2) -> Stream.concat(s1, s2));
        }
    }

    public Stream<GCodeEventNode> streamChildrenAndMeBackwards()
    {
        List<GCodeEventNode> reversedChildList = reversedView(getChildren());

        return Stream.concat(
                reversedChildList.stream()
                .map(childNode -> childNode.streamChildrenAndMeBackwards())
                .reduce(Stream.empty(), (s1, s2) -> Stream.concat(s1, s2)),
                Stream.of(this));
    }

    public Stream<GCodeEventNode> streamFromHere() throws NodeProcessingException
    {
        Stream<GCodeEventNode> finalStream = null;

        //Add me to the stream, with any of my children and their children too
        finalStream = stream();

        if (hasParent())
        {
            finalStream = Stream.concat(finalStream, getParent().streamFromHere(this));
        }

        return finalStream;
    }

    private Stream<GCodeEventNode> streamFromHere(GCodeEventNode sourceNode) throws NodeProcessingException
    {
        Stream<GCodeEventNode> finalStream = Stream.empty();

        if (sourceNode == null)
        {
            throw new NodeProcessingException("Source node cannot be null", this);
        }

        int startingChildIndex = getChildren().indexOf(sourceNode) + 1;
        int maxIndex = getChildren().size();

        // Only output some of our children, but output all of their children
        // Don't output us, since we've been called by a child
        finalStream = getChildren().subList(startingChildIndex, maxIndex).stream()
                .map(childNode -> childNode.stream())
                .reduce(finalStream, (s1, s2) -> Stream.concat(s1, s2));

        //Now output children of our parent
        if (hasParent())
        {
            finalStream = Stream.concat(finalStream, getParent().streamFromHere(this));
        }

        return finalStream;
    }

    public Stream<GCodeEventNode> streamBackwardsFromHere() throws NodeProcessingException
    {
        //Add me to the stream
        Stream<GCodeEventNode> finalStream = null;

        //Add my children to the stream
        finalStream = streamChildrenAndMeBackwards();

        if (hasParent())
        {
            finalStream = Stream.concat(finalStream, getParent().streamBackwardsFromHere(this));
        }

        return finalStream;
    }

    private Stream<GCodeEventNode> streamBackwardsFromHere(GCodeEventNode sourceNode) throws NodeProcessingException
    {
        Stream<GCodeEventNode> finalStream = Stream.empty();

        if (sourceNode == null)
        {
            throw new NodeProcessingException("Source node cannot be null", this);
        }

        int startingChildIndex = getChildren().indexOf(sourceNode);

        // Only output some of our children, but output all of their children in reverse order
        // Don't output us, since we've been called by a child
        List<GCodeEventNode> reversedChildList = reversedView(getChildren().subList(0, startingChildIndex));

        finalStream = reversedChildList.stream()
                .map(childNode -> childNode.streamChildrenAndMeBackwards())
                .reduce(finalStream, (s1, s2) -> Stream.concat(s1, s2));

        //Add me too
        finalStream = Stream.concat(finalStream, Stream.of(this));

        //Now output children of our parent
        if (hasParent())
        {
            finalStream = Stream.concat(finalStream, getParent().streamBackwardsFromHere(this));
        }

        return finalStream;
//
//        if (!isLeaf())
//        {
//            if (sourceNode == null)
//            {
//                finalStream = getChildren().stream()
//                        .map(childNode -> childNode.stream())
//                        .reduce(null, (s1, s2) -> Stream.concat(s1, s2)).collect(null);
//            } else
//            {
//                int childIndex = getChildren().indexOf(sourceNode);
//                int maxIndex = getChildren().size() - 1;
//
//                if (childIndex > 0 && childIndex < maxIndex)
//                {
//                    Stream.Builder<GCodeEventNode> builder = Stream.builder();
//                    getChildren().subList(childIndex, maxIndex)
//                            .forEach(child -> builder.add(child));
//                    finalStream = builder.build();
//                }
//            }
//        }
//
//        if (parent != null)
//        {
//            Stream<GCodeEventNode> parentStream = parent.streamFromHere(this);
//
//            if (parentStream != null && finalStream != null)
//            {
//                finalStream = Stream.concat(finalStream, parentStream);
//            } else if (parentStream != null)
//            {
//                finalStream = parentStream;
//            }
//        }

//        return finalStream;
    }

    public void addSiblingBefore(GCodeEventNode newNode)
    {
        GCodeEventNode parent = getParent();
        int myIndex = parent.getChildren().indexOf(this);
        parent.addChild(myIndex, newNode);
    }

    public void addSiblingAfter(GCodeEventNode newNode)
    {
        GCodeEventNode parent = getParent();
        int myIndex = parent.getChildren().indexOf(this);
        parent.addChild(myIndex + 1, newNode);
    }

    public void removeFromParent()
    {
        GCodeEventNode parent = getParent();
        int myIndex = parent.getChildren().indexOf(this);
        parent.removeChild(myIndex);
    }
    
    public void addChildAtEnd(GCodeEventNode node)
    {
        addChild(getChildren().size(), node);
    }
    
    public Optional<GCodeEventNode> getSiblingBefore()
    {
        Optional<GCodeEventNode> siblingBefore = Optional.empty();
        GCodeEventNode parent = getParent();
        int myIndex = parent.getChildren().indexOf(this);
        
        if (myIndex > 0)
        {
            siblingBefore = Optional.of(parent.getChildren().get(myIndex - 1));
        }
        
        return siblingBefore;
    }
    
    public Optional<GCodeEventNode> getSiblingAfter()
    {
        Optional<GCodeEventNode> siblingAfter = Optional.empty();
        GCodeEventNode parent = getParent();
        int myIndex = parent.getChildren().indexOf(this);
        
        if (myIndex < parent.getChildren().size() - 1)
        {
            siblingAfter = Optional.of(parent.getChildren().get(myIndex + 1));
        }
        
        return siblingAfter;
    }
}
