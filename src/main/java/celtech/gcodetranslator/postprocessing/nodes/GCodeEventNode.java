package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.IteratorWithOrigin;
import celtech.gcodetranslator.postprocessing.nodes.providers.Comment;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Ian
 */
public abstract class GCodeEventNode
{

    private final Comment comment = new Comment();
    private Optional<GCodeEventNode> parent = Optional.empty();
    protected final LinkedList<GCodeEventNode> children = new LinkedList<>();

    public GCodeEventNode()
    {
    }

    public boolean isLeaf()
    {
        return children.isEmpty();
    }

    public boolean hasParent()
    {
        return parent.isPresent();
    }

    public IteratorWithOrigin<GCodeEventNode> childrenAndMeBackwardsIterator()
    {
        IteratorWithOrigin<GCodeEventNode> it = new IteratorWithOrigin<GCodeEventNode>()
        {
            private GCodeEventNode originNode;
            private int currentIndex = children.size() - 1;
            private Iterator<GCodeEventNode> childIterator = null;

            @Override
            public void setOriginNode(GCodeEventNode originNode)
            {
                this.originNode = originNode;
            }

            @Override
            public boolean hasNext()
            {
                return currentIndex >= -1;
            }

            @Override
            public GCodeEventNode next()
            {
                if (currentIndex >= 0)
                {
                    if (childIterator != null
                            && childIterator.hasNext())
                    {
                        return childIterator.next();
                    } else if (childIterator != null)
                    {
                        childIterator = null;
                        GCodeEventNode child = children.get(currentIndex);
                        currentIndex--;
                        return child;
                    } else
                    {
                        GCodeEventNode child = children.get(currentIndex);
                        if (child.isLeaf())
                        {
                            currentIndex--;
                            return child;
                        } else
                        {
                            childIterator = child.children.descendingIterator();
                            return childIterator.next();
                        }
                    }

                } else
                {
                    currentIndex--;
                    return originNode;
                }
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };

        it.setOriginNode(this);
        return it;
    }

    public IteratorWithOrigin<GCodeEventNode> meAndSiblingsBackwardsIterator()
    {
        IteratorWithOrigin<GCodeEventNode> it = new IteratorWithOrigin<GCodeEventNode>()
        {
            private GCodeEventNode originNode;
            private int currentIndex;

            @Override
            public void setOriginNode(GCodeEventNode originNode)
            {
                this.originNode = originNode;
                currentIndex = parent.get().children.indexOf(originNode);
            }

            @Override
            public boolean hasNext()
            {
                return currentIndex >= 0;
            }

            @Override
            public GCodeEventNode next()
            {
                return parent.get().children.get(currentIndex--);
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };

        it.setOriginNode(this);
        return it;
    }

    public IteratorWithOrigin<GCodeEventNode> siblingsBackwardsIterator()
    {
        IteratorWithOrigin<GCodeEventNode> it = new IteratorWithOrigin<GCodeEventNode>()
        {
            private GCodeEventNode originNode;
            private int currentIndex;

            @Override
            public void setOriginNode(GCodeEventNode originNode)
            {
                this.originNode = originNode;
                currentIndex = parent.get().children.indexOf(originNode) - 1;
            }

            @Override
            public boolean hasNext()
            {
                return currentIndex >= 0;
            }

            @Override
            public GCodeEventNode next()
            {
                return parent.get().children.get(currentIndex--);
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };

        it.setOriginNode(this);
        return it;
    }

    public IteratorWithOrigin<GCodeEventNode> siblingsIterator()
    {
        IteratorWithOrigin<GCodeEventNode> it = new IteratorWithOrigin<GCodeEventNode>()
        {
            private GCodeEventNode originNode;
            private int currentIndex;

            @Override
            public void setOriginNode(GCodeEventNode originNode)
            {
                this.originNode = originNode;
                currentIndex = parent.get().children.indexOf(originNode) + 1;
            }

            @Override
            public boolean hasNext()
            {
                return currentIndex >= 0 && currentIndex < parent.get().children.size();
            }

            @Override
            public GCodeEventNode next()
            {
                return parent.get().children.get(currentIndex++);
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };

        it.setOriginNode(this);
        return it;
    }

    public Iterator<GCodeEventNode> treeSpanningIterator()
    {
        Iterator<GCodeEventNode> it = new Iterator<GCodeEventNode>()
        {
            private int currentIndex = 0;
            private Iterator<GCodeEventNode> childIterator = null;

            @Override
            public boolean hasNext()
            {
                return currentIndex < children.size()
                        || (childIterator != null && childIterator.hasNext());
            }

            @Override
            public GCodeEventNode next()
            {
                if (childIterator != null
                        && childIterator.hasNext())
                {
                    return childIterator.next();
                } else
                {
                    childIterator = null;
                    GCodeEventNode child = children.get(currentIndex++);
                    if (!child.isLeaf())
                    {
                        childIterator = child.treeSpanningIterator();
                    }
                    return child;
                }
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };

        return it;
    }

    public IteratorWithOrigin<GCodeEventNode> treeSpanningBackwardsIterator()
    {
        IteratorWithOrigin<GCodeEventNode> it = new IteratorWithOrigin<GCodeEventNode>()
        {
            private GCodeEventNode originNode;
            private int currentIndex;
            private IteratorWithOrigin<GCodeEventNode> parentIterator = null;

            @Override
            public void setOriginNode(GCodeEventNode originNode)
            {
                this.originNode = originNode;
                if (parent.isPresent())
                {
                    currentIndex = parent.get().children.indexOf(originNode) - 1;
                } else
                {
                    currentIndex = -1;
                }
            }

            @Override
            public boolean hasNext()
            {
                return currentIndex >= 0
                        || (parentIterator != null && parentIterator.hasNext()
                        || (currentIndex < 0 && originNode.hasParent()));
            }

            @Override
            public GCodeEventNode next()
            {
                if (parentIterator != null
                        && parentIterator.hasNext())
                {
                    return parentIterator.next();
                } else if (currentIndex >= 0)
                {
                    parentIterator = null;

                    GCodeEventNode child = children.get(currentIndex--);
                    return child;
                } else
                {
                    //Look upwards from the origin node
                    parentIterator = originNode.parent.get().treeSpanningBackwardsIterator();
                    parentIterator.setOriginNode(originNode.parent.get());
                    return originNode.parent.get();
                }
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };

        it.setOriginNode(this);
        return it;
    }

//    public static <T> List<T> reversedView(final List<T> list)
//    {
//        return new AbstractList<T>()
//        {
//            @Override
//            public T get(int index)
//            {
//                return list.get(list.size() - 1 - index);
//            }
//
//            @Override
//            public int size()
//            {
//                return list.size();
//            }
//        };
//    }
//    public Stream<GCodeEventNode> stream()
//    {
//        if (isLeaf())
//        {
//            return Stream.of(this);
//        } else
//        {
//            return getChildren().stream()
//                    .map(childNode -> childNode.stream())
//                    .reduce(Stream.of(this), (s1, s2) -> Stream.concat(s1, s2));
//        }
//    }
//
//    public Stream<GCodeEventNode> streamChildrenAndMe()
//    {
//        return Stream.concat(
//                Stream.of(this),
//                getChildren().stream()
//                .map(childNode -> childNode.streamChildrenAndMe())
//                .reduce(Stream.empty(), (s1, s2) -> Stream.concat(s1, s2)));
//    }
//
//    public Stream<GCodeEventNode> streamChildrenAndMeBackwards()
//    {
//        List<GCodeEventNode> reversedChildList = reversedView(getChildren());
//
//        return Stream.concat(
//                reversedChildList.stream()
//                .map(childNode -> childNode.streamChildrenAndMeBackwards())
//                .reduce(Stream.empty(), (s1, s2) -> Stream.concat(s1, s2)),
//                Stream.of(this));
//    }
//
//    public Stream<GCodeEventNode> streamFromHere() throws NodeProcessingException
//    {
//        Stream<GCodeEventNode> finalStream = null;
//
//        //Add me to the stream, with any of my children and their children too
//        finalStream = stream();
//
//        if (hasParent())
//        {
//            finalStream = Stream.concat(finalStream, getParent().streamFromHere(this));
//        }
//
//        return finalStream;
//    }
//
//    public Stream<GCodeEventNode> streamSiblingsFromHere() throws NodeProcessingException
//    {
//        if (getParent() == null)
//        {
//            throw new NodeProcessingException("No parent", this);
//        }
//
//        int startingChildIndex = getParent().getChildren().indexOf(this) + 1;
//        int maxIndex = getParent().getChildren().size();
//
//        return getParent().getChildren().subList(startingChildIndex, maxIndex).stream();
//    }
//
//    public Stream<GCodeEventNode> streamSiblingsBackwardsFromHere() throws NodeProcessingException
//    {
//        if (getParent() == null)
//        {
//            throw new NodeProcessingException("No parent", this);
//        }
//
//        int startingChildIndex = getParent().getChildren().indexOf(this);
//
//        List<GCodeEventNode> reversedChildList = reversedView(getParent().getChildren().subList(0, startingChildIndex));
//
//        return reversedChildList.stream();
//    }
//
//    public Stream<GCodeEventNode> streamSiblingsAndMeFromHere() throws NodeProcessingException
//    {
//        if (getParent() == null)
//        {
//            throw new NodeProcessingException("No parent", this);
//        }
//
//        int startingChildIndex = getParent().getChildren().indexOf(this);
//        int maxIndex = getParent().getChildren().size();
//
//        return getParent().getChildren().subList(startingChildIndex, maxIndex).stream();
//    }
//
//    public Stream<GCodeEventNode> streamSiblingsAndMeBackwardsFromHere() throws NodeProcessingException
//    {
//        if (getParent() == null)
//        {
//            throw new NodeProcessingException("No parent", this);
//        }
//
//        int startingChildIndex = getParent().getChildren().indexOf(this);
//
//        List<GCodeEventNode> reversedChildList = reversedView(getParent().getChildren().subList(0, startingChildIndex + 1));
//
//        return reversedChildList.stream();
//    }
//
//    private Stream<GCodeEventNode> streamFromHere(GCodeEventNode sourceNode) throws NodeProcessingException
//    {
//        Stream<GCodeEventNode> finalStream = Stream.empty();
//
//        if (sourceNode == null)
//        {
//            throw new NodeProcessingException("Source node cannot be null", this);
//        }
//
//        int startingChildIndex = getChildren().indexOf(sourceNode) + 1;
//        int maxIndex = getChildren().size();
//
//        // Only output some of our children, but output all of their children
//        // Don't output us, since we've been called by a child
//        finalStream = getChildren().subList(startingChildIndex, maxIndex).stream()
//                .map(childNode -> childNode.stream())
//                .reduce(finalStream, (s1, s2) -> Stream.concat(s1, s2));
//
//        //Now output children of our parent
//        if (hasParent())
//        {
//            finalStream = Stream.concat(finalStream, getParent().streamFromHere(this));
//        }
//
//        return finalStream;
//    }
//
//    /**
//     * This method traverses the tree backwards
//     *
//     * @return
//     * @throws NodeProcessingException
//     */
//    public Stream<GCodeEventNode> streamBackwardsFromHere() throws NodeProcessingException
//    {
//        //Add me to the stream
//        Stream<GCodeEventNode> finalStream = null;
//
//        //Add my children to the stream
//        finalStream = streamChildrenAndMeBackwards();
//
//        if (hasParent())
//        {
//            finalStream = Stream.concat(finalStream, getParent().streamBackwardsFromHere(this));
//        }
//
//        return finalStream;
//    }
//
//    private Stream<GCodeEventNode> streamBackwardsFromHere(GCodeEventNode sourceNode) throws NodeProcessingException
//    {
//        Stream<GCodeEventNode> finalStream = Stream.empty();
//
//        if (sourceNode == null)
//        {
//            throw new NodeProcessingException("Source node cannot be null", this);
//        }
//
//        int startingChildIndex = getChildren().indexOf(sourceNode);
//
//        // Only output some of our children, but output all of their children in reverse order
//        // Don't output us, since we've been called by a child
//        List<GCodeEventNode> reversedChildList = reversedView(getChildren().subList(0, startingChildIndex));
//
//        finalStream = reversedChildList.stream()
//                .map(childNode -> childNode.streamChildrenAndMeBackwards())
//                .reduce(finalStream, (s1, s2) -> Stream.concat(s1, s2));
//
//        //Add me too
//        finalStream = Stream.concat(finalStream, Stream.of(this));
//
//        //Now output children of our parent
//        if (hasParent())
//        {
//            finalStream = Stream.concat(finalStream, getParent().streamBackwardsFromHere(this));
//        }
//
//        return finalStream;
////
////        if (!isLeaf())
////        {
////            if (sourceNode == null)
////            {
////                finalStream = getChildren().stream()
////                        .map(childNode -> childNode.stream())
////                        .reduce(null, (s1, s2) -> Stream.concat(s1, s2)).collect(null);
////            } else
////            {
////                int childIndex = getChildren().indexOf(sourceNode);
////                int maxIndex = getChildren().size() - 1;
////
////                if (childIndex > 0 && childIndex < maxIndex)
////                {
////                    Stream.Builder<GCodeEventNode> builder = Stream.builder();
////                    getChildren().subList(childIndex, maxIndex)
////                            .forEach(child -> builder.add(child));
////                    finalStream = builder.build();
////                }
////            }
////        }
////
////        if (parent != null)
////        {
////            Stream<GCodeEventNode> parentStream = parent.streamFromHere(this);
////
////            if (parentStream != null && finalStream != null)
////            {
////                finalStream = Stream.concat(finalStream, parentStream);
////            } else if (parentStream != null)
////            {
////                finalStream = parentStream;
////            }
////        }
//
////        return finalStream;
//    }
//    public void setParent(GCodeEventNode parentNode)
//    {
//        if (parentNode == null)
//        {
//            parent = Optional.empty();
//        } else
//        {
//            parent = Optional.of(parentNode);
//        }
//    }
//
//    private void setPrior(GCodeEventNode priorNode)
//    {
//        if (priorNode == null)
//        {
//            priorSibling = Optional.empty();
//        } else
//        {
//            priorSibling = Optional.of(priorNode);
//        }
//    }
//
//    private void setNext(GCodeEventNode nextNode)
//    {
//        if (nextNode == null)
//        {
//            nextSibling = Optional.empty();
//        } else
//        {
//            nextSibling = Optional.of(nextNode);
//        }
//    }
//
//    public void addChild(GCodeEventNode childNode)
//    {
//        children.add(childNode);
//    }
//
//    public void removeChild(GCodeEventNode childNode)
//    {
//        children.remove(childNode);
//    }
//
    public void addSiblingBefore(GCodeEventNode newNode)
    {
        if (parent.isPresent())
        {
            GCodeEventNode parentNode = parent.get();
            int myIndex = parentNode.children.indexOf(this);
            parentNode.children.add(myIndex, newNode);
        }
    }

    public void addSiblingAfter(GCodeEventNode newNode)
    {
        if (parent.isPresent())
        {
            GCodeEventNode parentNode = parent.get();
            int myIndex = parentNode.children.indexOf(this);
            parentNode.children.add(myIndex + 1, newNode);
        }
    }
//
//    public void removeFromParentAndFixup()
//    {
//        if (parent.isPresent())
//        {
//            parent.get().removeChild(this);
//            parent = Optional.empty();
//
//            if (priorSibling.isPresent()
//                    && nextSibling.isPresent())
//            {
//                priorSibling.get().setNext(nextSibling.get());
//                nextSibling.get().setPrior(priorSibling.get());
//            } else if (priorSibling.isPresent())
//            {
//                priorSibling.get().setNext(null);
//            } else if (nextSibling.isPresent())
//            {
//                nextSibling.get().setPrior(null);
//            }
//        }
//    }
//    /**
//     * Adds a child node at the end of the list of this node's children
//     *
//     * @param node
//     */
//    public void addChildAtEnd(GCodeEventNode node)
//    {
//        addChild(getChildren().size(), node);
//    }

    public Optional<GCodeEventNode> getParent()
    {
        return parent;
    }

    /**
     *
     * @return
     */
    public Optional<GCodeEventNode> getSiblingBefore()
    {
        Optional<GCodeEventNode> returnValue = Optional.empty();

        if (parent.isPresent())
        {
            GCodeEventNode parentNode = parent.get();
            int myIndex = parentNode.children.indexOf(this);
            if (myIndex > 0)
            {
                return Optional.of(parentNode.children.get(myIndex - 1));
            }
        }

        return returnValue;
    }

    /**
     *
     * @return
     */
    public Optional<GCodeEventNode> getSiblingAfter()
    {
        Optional<GCodeEventNode> returnValue = Optional.empty();

        if (parent.isPresent())
        {
            GCodeEventNode parentNode = parent.get();
            int myIndex = parentNode.children.indexOf(this);
            if (myIndex < parentNode.children.size() - 1)
            {
                return Optional.of(parentNode.children.get(myIndex + 1));
            }
        }

        return returnValue;
    }

    /**
     *
     * @param newNode
     */
    public void addChildAtEnd(GCodeEventNode newNode)
    {
        children.addLast(newNode);
        newNode.parent = Optional.of(this);
    }

    public String getCommentText()
    {
        return comment.renderComments();
    }

    public void setCommentText(String commentText)
    {
        comment.setComment(commentText);
    }

    public void appendCommentText(String commentText)
    {
        comment.setComment(comment.getComment() + " " + commentText);
    }
}
