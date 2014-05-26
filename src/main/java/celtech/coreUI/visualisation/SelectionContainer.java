/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation;

import celtech.modelcontrol.ModelContainer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SelectionContainer
{

    private final DoubleProperty minX = new SimpleDoubleProperty(0);
    private final DoubleProperty minY = new SimpleDoubleProperty(0);
    private final DoubleProperty minZ = new SimpleDoubleProperty(0);
    private final DoubleProperty centreX = new SimpleDoubleProperty(0);
    private final DoubleProperty centreY = new SimpleDoubleProperty(0);
    private final DoubleProperty centreZ = new SimpleDoubleProperty(0);
    private final DoubleProperty width = new SimpleDoubleProperty(0);
    private final DoubleProperty depth = new SimpleDoubleProperty(0);
    private final DoubleProperty height = new SimpleDoubleProperty(0);
    private final DoubleProperty scale = new SimpleDoubleProperty(0);
    private final DoubleProperty rotationX = new SimpleDoubleProperty(0);
    private final DoubleProperty rotationY = new SimpleDoubleProperty(0.1);
    private final DoubleProperty rotationZ = new SimpleDoubleProperty(0);
    private final DoubleProperty screenX = new SimpleDoubleProperty(0);
    private final DoubleProperty screenY = new SimpleDoubleProperty(0);
    private final ObservableList<ModelContainer> selectedModels = FXCollections.observableArrayList();

    /**
     *
     * @param model
     */
    public void addSelectedModel(ModelContainer model)
    {
        selectedModels.add(model);
    }

    /**
     *
     * @param model
     */
    public void removeSelectedModel(ModelContainer model)
    {
        selectedModels.remove(model);
    }

    /**
     *
     */
    public void clearSelectedModels()
    {
        selectedModels.clear();
    }

    /**
     *
     * @return
     */
    public ObservableList<ModelContainer> selectedModelsProperty()
    {
        return selectedModels;
    }

    /**
     *
     * @param value
     */
    public void setCentreX(double value)
    {
        centreX.set(value);
    }

    /**
     *
     * @return
     */
    public double getCentreX()
    {
        return centreX.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty centreXProperty()
    {
        return centreX;
    }

    /**
     *
     * @param value
     */
    public void setCentreY(double value)
    {
        centreY.set(value);
    }

    /**
     *
     * @return
     */
    public double getCentreY()
    {
        return centreY.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty centreYProperty()
    {
        return centreY;
    }

    /**
     *
     * @param value
     */
    public void setCentreZ(double value)
    {
        centreZ.set(value);
    }

    /**
     *
     * @return
     */
    public double getCentreZ()
    {
        return centreZ.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty centreZProperty()
    {
        return centreZ;
    }

    /**
     *
     * @param value
     */
    public void setScreenX(double value)
    {
        screenX.set(value);
    }

    /**
     *
     * @return
     */
    public double getScreenX()
    {
        return screenX.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty screenXProperty()
    {
        return screenX;
    }

    /**
     *
     * @param value
     */
    public void setScreenY(double value)
    {
        screenY.set(value);
    }

    /**
     *
     * @return
     */
    public double getScreenY()
    {
        return screenY.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty screenYProperty()
    {
        return screenY;
    }

    /**
     *
     * @param value
     */
    public void setWidth(double value)
    {
        width.set(value);
    }

    /**
     *
     * @return
     */
    public double getWidth()
    {
        return width.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty widthProperty()
    {
        return width;
    }

    /**
     *
     * @param value
     */
    public void setDepth(double value)
    {
        depth.set(value);
    }

    /**
     *
     * @return
     */
    public double getDepth()
    {
        return depth.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty depthProperty()
    {
        return depth;
    }

    /**
     *
     * @param value
     */
    public void setHeight(double value)
    {
        height.set(value);
    }

    /**
     *
     * @return
     */
    public double getHeight()
    {
        return height.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty heightProperty()
    {
        return height;
    }

    /**
     *
     * @param value
     */
    public void setScale(double value)
    {
        scale.set(value);
    }

    /**
     *
     * @return
     */
    public double getScale()
    {
        return scale.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty scaleProperty()
    {
        return scale;
    }

    /**
     *
     * @param value
     */
    public void setRotationX(double value)
    {
        rotationX.set(value);
    }

    /**
     *
     * @return
     */
    public double getRotationX()
    {
        return rotationX.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty rotationXProperty()
    {
        return rotationX;
    }

    /**
     *
     * @param value
     */
    public void setRotationY(double value)
    {
        rotationY.set(value);
    }

    /**
     *
     * @return
     */
    public double getRotationY()
    {
        return rotationY.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty rotationYProperty()
    {
        return rotationY;
    }

    /**
     *
     * @param value
     */
    public void setRotationZ(double value)
    {
        rotationZ.set(value);
    }

    /**
     *
     * @return
     */
    public double getRotationZ()
    {
        return rotationZ.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty rotationZProperty()
    {
        return rotationZ;
    }

    /**
     *
     * @param value
     */
    public void setMinX(double value)
    {
        minX.set(value);
    }

    /**
     *
     * @return
     */
    public double getMinX()
    {
        return minX.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty minXProperty()
    {
        return minX;
    }

    /**
     *
     * @param value
     */
    public void setMinY(double value)
    {
        minY.set(value);
    }

    /**
     *
     * @return
     */
    public double getMinY()
    {
        return minY.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty minYProperty()
    {
        return minY;
    }

    /**
     *
     * @param value
     */
    public void setMinZ(double value)
    {
        minZ.set(value);
    }

    /**
     *
     * @return
     */
    public double getMinZ()
    {
        return minZ.get();
    }

    /**
     *
     * @return
     */
    public DoubleProperty minZProperty()
    {
        return minZ;
    }
}
