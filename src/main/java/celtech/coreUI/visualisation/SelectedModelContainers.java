/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.visualisation;

import celtech.appManager.Project;
import celtech.appManager.Project.ProjectChangesListener;
import celtech.coreUI.visualisation.metaparts.Part;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

/**
 * SelectedParts captures all required state about the currently selected Parts.
 *
 * @author tony
 */
public class SelectedModelContainers implements ProjectChangesListener
{

    private final ObservableSet<Part> parts;
    private final PrimarySelectedModelDetails primarySelectedModelDetails;
    private final IntegerProperty numModelsSelected = new SimpleIntegerProperty(0);
    private final Set<SelectedModelContainersListener> selectedPartsListeners;

    public SelectedModelContainers(Project project)
    {
        parts = FXCollections.observableSet();
        primarySelectedModelDetails = new PrimarySelectedModelDetails();
        selectedPartsListeners = new HashSet<>();
        project.addProjectChangesListener(this);
    }

    /**
     * Add the given part to the set of selected Parts.
     */
    public void addPart(Part part)
    {
        if (!parts.contains(part))
        {
            parts.add(part);
            part.setSelected(true);
            primarySelectedModelDetails.setTo(part);
            for (SelectedModelContainersListener selectedPartsListener : selectedPartsListeners)
            {
                selectedPartsListener.whenAdded(part);
            }
            numModelsSelected.set(numModelsSelected.get() + 1);
        }
    }

    /**
     * Remove the given part from the set of selected Parts.
     */
    public void removePart(Part part)
    {
        if (parts.contains(part))
        {
            parts.remove(part);
            numModelsSelected.set(numModelsSelected.get() - 1);
            part.setSelected(false);
            for (SelectedModelContainersListener selectedPartsListener : selectedPartsListeners)
            {
                selectedPartsListener.whenRemoved(part);
            }
        }

    }

    /**
     * Return if the given Part is selected or not.
     *
     * @param part
     * @return
     */
    public boolean isSelected(Part part)
    {
        return parts.contains(part);
    }

    /**
     * Deselect all Parts in the set of Parts.
     */
    public void deselectAllModels()
    {
        Set<Part> allSelectedParts = new HashSet<>(parts);
        for (Part part : allSelectedParts)
        {
            removePart(part);
        }
    }

    /**
     * Return a copy of the set of selected models.
     * @return 
     */
    public Set<Part> getSelectedModelsSnapshot()
    {
        return new HashSet<>(parts);
    }

    /**
     * Return the number of selected Parts as an observable number.
     * @return 
     */
    public ReadOnlyIntegerProperty getNumModelsSelectedProperty()
    {
        return numModelsSelected;
    }

    /**
     * Return the details of the primary selected Part.
     * @return 
     */
    public PrimarySelectedModelDetails getPrimarySelectedModelDetails()
    {
        return primarySelectedModelDetails;
    }

    /**
     * Call this method when the transformed geometry of the selected model have changed.
     */
    public void updateSelectedValues()
    {
        primarySelectedModelDetails.updateSelectedProperties();
    }

    @Override
    public void whenModelAdded(Part part)
    {
    }

    @Override
    public void whenModelRemoved(Part part)
    {
        removePart(part);
    }

    @Override
    public void whenAutoLaidOut()
    {
    }

    @Override
    public void whenModelsTransformed(Set<Part> parts)
    {
        updateSelectedValues();
    }

    @Override
    public void whenModelChanged(Part part, String propertyName)
    {
    }

    /**
     * Add a listener that will be notified whenever a Part is selected or deselected.
     */
    public void addListener(SelectedModelContainersListener selectedPartsListener)
    {
        selectedPartsListeners.add(selectedPartsListener);
    }

    /**
     * Remove a listener that will be notified whenever a Part is selected or deselected.
     */
    public void removeListener(SelectedModelContainersListener selectedPartsListener)
    {
        selectedPartsListeners.remove(selectedPartsListener);
    }

    public interface SelectedModelContainersListener
    {

        /**
         * Called when a Part is selected.
         */
        public void whenAdded(Part part);

        /**
         * Called when a Part is removed.
         */
        public void whenRemoved(Part part);
    }

    /**
     * PrimarySelectedModelDetails contains the details pertaining to the primary selected
     * Part.
     */
    public class PrimarySelectedModelDetails
    {

        Part boundPart;

        // initing values to -1 forces a change update when value first set to 0 (e.g. rotY)
        private final DoubleProperty width = new SimpleDoubleProperty(-1);
        private final DoubleProperty centreX = new SimpleDoubleProperty(-1);
        private final DoubleProperty centreZ = new SimpleDoubleProperty(-1);
        private final DoubleProperty height = new SimpleDoubleProperty(-1);
        private final DoubleProperty depth = new SimpleDoubleProperty(-1);
        private final DoubleProperty scale = new SimpleDoubleProperty(-1);
        private final DoubleProperty rotationY = new SimpleDoubleProperty(-1);

        private PrimarySelectedModelDetails()
        {
        }

        public DoubleProperty getWidth()
        {
            return width;
        }

        public void setTo(Part part)
        {
            boundPart = part;
            updateSelectedProperties();
        }

        private void updateSelectedProperties()
        {
            if (boundPart != null)
            {
                width.set(boundPart.getScaledWidth());
                centreX.set(boundPart.getTransformedCentreX());
                centreZ.set(boundPart.getTransformedCentreZ());
                height.set(boundPart.getScaledHeight());
                depth.set(boundPart.getScaledDepth());
                scale.set(boundPart.getScale());
                rotationY.set(boundPart.getRotationY());
            }
        }

        public DoubleProperty getCentreZ()
        {
            return centreZ;
        }

        public DoubleProperty getCentreX()
        {
            return centreX;
        }

        public DoubleProperty getHeight()
        {
            return height;
        }

        public DoubleProperty getDepth()
        {
            return depth;
        }

        public DoubleProperty getScale()
        {
            return scale;
        }

        public DoubleProperty getRotationY()
        {
            return rotationY;
        }
    }

}
