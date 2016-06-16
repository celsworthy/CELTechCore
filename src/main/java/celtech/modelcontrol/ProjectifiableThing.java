package celtech.modelcontrol;

import celtech.Lookup;
import celtech.coreUI.visualisation.ScreenExtents;
import celtech.coreUI.visualisation.ScreenExtentsProviderTwoD;
import celtech.coreUI.visualisation.ShapeProviderTwoD;
import celtech.roboxbase.configuration.datafileaccessors.PrinterContainer;
import celtech.roboxbase.configuration.fileRepresentation.PrinterDefinitionFile;
import celtech.roboxbase.printerControl.model.Printer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;

/**
 *
 * @author ianhudson
 */
public abstract class ProjectifiableThing extends Group implements ScreenExtentsProviderTwoD, ShapeProviderTwoD
{

    private File modelFile;
    protected boolean isCollided = false;
    protected BooleanProperty isSelected;
    protected BooleanProperty isOffBed;
    protected ScreenExtents extents = null;
    private List<ShapeProviderTwoD.ShapeChangeListener> shapeChangeListeners;
    private List<ScreenExtentsProviderTwoD.ScreenExtentsListener> screenExtentsChangeListeners;
    protected double printVolumeWidth = 0;
    protected double printVolumeDepth = 0;
    protected double printVolumeHeight = 0;

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
        initialise();
    }

    private void initialise()
    {
        isSelected = new SimpleBooleanProperty(false);
        isOffBed = new SimpleBooleanProperty(false);
        shapeChangeListeners = new ArrayList<>();
        screenExtentsChangeListeners = new ArrayList<>();

        Lookup.getSelectedPrinterProperty().addListener((ObservableValue<? extends Printer> ov, Printer t, Printer t1) ->
        {
            updatePrintVolumeBounds(t1);
        });

        updatePrintVolumeBounds(Lookup.getSelectedPrinterProperty().get());
    }

    public int getModelId()
    {
        return modelId;
    }

    public abstract ItemState getState();

    public abstract void setState(ItemState state);

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

    protected abstract boolean recalculateScreenExtents();

    @Override
    public final ScreenExtents getScreenExtents()
    {
        if (extents == null)
        {
            recalculateScreenExtents();
        }
        return extents;
    }

    @Override
    public final void addScreenExtentsChangeListener(ScreenExtentsProviderTwoD.ScreenExtentsListener listener)
    {
        recalculateScreenExtents();
        screenExtentsChangeListeners.add(listener);
    }

    @Override
    public final void removeScreenExtentsChangeListener(
            ScreenExtentsProviderTwoD.ScreenExtentsListener listener)
    {
        screenExtentsChangeListeners.remove(listener);
    }

    public final void notifyScreenExtentsChange()
    {
        if (recalculateScreenExtents())
        {
            for (ScreenExtentsProviderTwoD.ScreenExtentsListener screenExtentsListener : screenExtentsChangeListeners)
            {
                screenExtentsListener.screenExtentsChanged(this);
            }
        }
    }

    @Override
    public final void addShapeChangeListener(ShapeProviderTwoD.ShapeChangeListener listener)
    {
        shapeChangeListeners.add(listener);
    }

    @Override
    public final void removeShapeChangeListener(ShapeProviderTwoD.ShapeChangeListener listener)
    {
        shapeChangeListeners.remove(listener);
    }

    /**
     * This method must be called at the end of any operation that changes one
     * or more of the transforms.
     */
    public final void notifyShapeChange()
    {
        for (ShapeProviderTwoD.ShapeChangeListener shapeChangeListener : shapeChangeListeners)
        {
            shapeChangeListener.shapeChanged(this);
        }
    }

    private void updatePrintVolumeBounds(Printer printer)
    {
        if (printer != null
                && printer.printerConfigurationProperty().get() != null)
        {
            printVolumeWidth = printer.printerConfigurationProperty().get().getPrintVolumeWidth();
            printVolumeDepth = printer.printerConfigurationProperty().get().getPrintVolumeDepth();
            printVolumeHeight = printer.printerConfigurationProperty().get().getPrintVolumeHeight();
        } else
        {
            PrinterDefinitionFile defaultPrinterConfiguration = PrinterContainer.getPrinterByID(PrinterContainer.defaultPrinterID);
            printVolumeWidth = defaultPrinterConfiguration.getPrintVolumeWidth();
            printVolumeDepth = defaultPrinterConfiguration.getPrintVolumeDepth();
            printVolumeHeight = defaultPrinterConfiguration.getPrintVolumeHeight();
        }
        printVolumeBoundsUpdated();
    }
    
    protected abstract void printVolumeBoundsUpdated();
}
