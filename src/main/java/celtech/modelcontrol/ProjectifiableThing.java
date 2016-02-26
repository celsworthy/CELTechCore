package celtech.modelcontrol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;

/**
 *
 * @author ianhudson
 */
public abstract class ProjectifiableThing extends Group implements Scaleable, Translateable, Resizeable
{

    private File modelFile;
    protected boolean isCollided = false;
    protected BooleanProperty isSelected;
    protected BooleanProperty isOffBed;

    /**
     * The modelId is only guaranteed unique at the project level because it
     * could be reloaded with duplicate values from saved models into other
     * projects.
     */
    protected int modelId;
    private SimpleStringProperty modelName;

    public ProjectifiableThing()
    {
        initialise();

    }

    public ProjectifiableThing(File modelFile)
    {
        this.modelFile = modelFile;
    }

    private void initialise()
    {
        isSelected = new SimpleBooleanProperty(false);
        isOffBed = new SimpleBooleanProperty(false);
    }

    public int getModelId()
    {
        return modelId;
    }

    public abstract ProjectifiableThing.State getState();

    public abstract void setState(ProjectifiableThing.State state);

    /**
     * Make a copy of this ModelContainer and return it.
     *
     * @return
     */
    public abstract ProjectifiableThing makeCopy();

    public abstract void clearElements();

    public void setModelFile(File modelFile)
    {
        this.modelFile = modelFile;
    }

    public File getModelFile()
    {
        return modelFile;
    }

    public abstract void addChildNodes(ObservableList<Node> nodes);

    public abstract void addChildNode(Node node);

    public abstract ObservableList<Node> getChildNodes();

    public void setSelected(boolean selected)
    {
        isSelected.set(selected);
        selectedAction();
    }

    public final boolean isSelected()
    {
        return isSelected.get();
    }

    public abstract void selectedAction();

    public final void setModelName(String modelName)
    {
        if (this.modelName == null)
        {
            this.modelName = new SimpleStringProperty();
        }
        this.modelName.set(modelName);
    }

    public final String getModelName()
    {
        return modelName.get();
    }

    public final void setCollided(boolean collided)
    {
        this.isCollided = collided;
    }
    
    public final boolean isCollided()
    {
        return isCollided;
    }

    /**
     * State captures the state of all the transforms being applied to this
     * ModelContainer. It is used as an efficient way of applying Undo and Redo
     * to changes to a Set of ModelContainers.
     */
    public static class State
    {

        public int modelId;
        public double x;
        public double y;
        public double z;
        public double preferredXScale;
        public double preferredYScale;
        public double preferredZScale;
        public double preferredRotationTwist;
        public double preferredRotationTurn;
        public double preferredRotationLean;

        public State()
        {
        }

        @JsonCreator
        public State(
                @JsonProperty("modelId") int modelId,
                @JsonProperty("x") double x,
                @JsonProperty("y") double y,
                @JsonProperty("z") double z,
                @JsonProperty("preferredXScale") double preferredXScale,
                @JsonProperty("preferredYScale") double preferredYScale,
                @JsonProperty("preferredZScale") double preferredZScale,
                @JsonProperty("preferredRotationTwist") double preferredRotationTwist,
                @JsonProperty("preferredRotationTurn") double preferredRotationTurn,
                @JsonProperty("preferredRotationLean") double preferredRotationLean)
        {
            this.modelId = modelId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.preferredXScale = preferredXScale;
            this.preferredYScale = preferredYScale;
            this.preferredZScale = preferredZScale;
            this.preferredRotationTwist = preferredRotationTwist;
            this.preferredRotationTurn = preferredRotationTurn;
            this.preferredRotationLean = preferredRotationLean;
        }

        /**
         * The assignment operator.
         *
         * @param fromState
         */
        public void assignFrom(State fromState)
        {
            this.x = fromState.x;
            this.y = fromState.y;
            this.z = fromState.z;
            this.preferredXScale = fromState.preferredXScale;
            this.preferredYScale = fromState.preferredYScale;
            this.preferredZScale = fromState.preferredZScale;
            this.preferredRotationTwist = fromState.preferredRotationTwist;
            this.preferredRotationTurn = fromState.preferredRotationTurn;
            this.preferredRotationLean = fromState.preferredRotationLean;
        }
    }
}
