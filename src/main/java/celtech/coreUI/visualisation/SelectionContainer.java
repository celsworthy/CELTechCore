/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation;

import celtech.modelcontrol.ModelContainer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
    private final ObservableList<ModelContainer> selectedModels = FXCollections.observableArrayList();

    public void addSelectedModel(ModelContainer model)
    {
        selectedModels.add(model);
    }

    public void removeSelectedModel(ModelContainer model)
    {
        selectedModels.remove(model);
    }

    public void clearSelectedModels()
    {
        selectedModels.clear();
    }

    public ObservableList<ModelContainer> selectedModelsProperty()
    {
        return selectedModels;
    }

    public void setCentreX(double value)
    {
        centreX.set(value);
    }

    public double getCentreX()
    {
        return centreX.get();
    }

    public DoubleProperty centreXProperty()
    {
        return centreX;
    }

    public void setCentreY(double value)
    {
        centreY.set(value);
    }

    public double getCentreY()
    {
        return centreY.get();
    }

    public DoubleProperty centreYProperty()
    {
        return centreY;
    }

    public void setCentreZ(double value)
    {
        centreZ.set(value);
    }

    public double getCentreZ()
    {
        return centreZ.get();
    }

    public DoubleProperty centreZProperty()
    {
        return centreZ;
    }

    public void setWidth(double value)
    {
        width.set(value);
    }

    public double getWidth()
    {
        return width.get();
    }

    public DoubleProperty widthProperty()
    {
        return width;
    }

    public void setDepth(double value)
    {
        depth.set(value);
    }

    public double getDepth()
    {
        return depth.get();
    }

    public DoubleProperty depthProperty()
    {
        return depth;
    }

    public void setHeight(double value)
    {
        height.set(value);
    }

    public double getHeight()
    {
        return height.get();
    }

    public DoubleProperty heightProperty()
    {
        return height;
    }

    public void setScale(double value)
    {
        scale.set(value);
    }

    public double getScale()
    {
        return scale.get();
    }

    public DoubleProperty scaleProperty()
    {
        return scale;
    }

    public void setRotationX(double value)
    {
        rotationX.set(value);
    }

    public double getRotationX()
    {
        return rotationX.get();
    }

    public DoubleProperty rotationXProperty()
    {
        return rotationX;
    }

    public void setRotationY(double value)
    {
        rotationY.set(value);
    }

    public double getRotationY()
    {
        return rotationY.get();
    }

    public DoubleProperty rotationYProperty()
    {
        return rotationY;
    }

    public void setRotationZ(double value)
    {
        rotationZ.set(value);
    }

    public double getRotationZ()
    {
        return rotationZ.get();
    }

    public DoubleProperty rotationZProperty()
    {
        return rotationZ;
    }

    public void setMinX(double value)
    {
        minX.set(value);
    }

    public double getMinX()
    {
        return minX.get();
    }

    public DoubleProperty minXProperty()
    {
        return minX;
    }

    public void setMinY(double value)
    {
        minY.set(value);
    }

    public double getMinY()
    {
        return minY.get();
    }

    public DoubleProperty minYProperty()
    {
        return minY;
    }

    public void setMinZ(double value)
    {
        minZ.set(value);
    }

    public double getMinZ()
    {
        return minZ.get();
    }

    public DoubleProperty minZProperty()
    {
        return minZ;
    }
}
