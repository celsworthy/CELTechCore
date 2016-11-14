package celtech.appManager;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.ModelContainerProjectFile;
import celtech.configuration.fileRepresentation.ProjectFile;
import celtech.configuration.fileRepresentation.ProjectFileDeserialiser;
import celtech.configuration.fileRepresentation.ShapeContainerProjectFile;
import celtech.modelcontrol.Groupable;
import celtech.modelcontrol.ItemState;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.modelcontrol.ResizeableThreeD;
import celtech.modelcontrol.ResizeableTwoD;
import celtech.modelcontrol.RotatableThreeD;
import celtech.modelcontrol.RotatableTwoD;
import celtech.modelcontrol.ScaleableThreeD;
import celtech.modelcontrol.ScaleableTwoD;
import celtech.modelcontrol.Translateable;
import celtech.modelcontrol.TranslateableThreeD;
import celtech.modelcontrol.TranslateableTwoD;
import celtech.roboxbase.configuration.datafileaccessors.PrinterContainer;
import celtech.roboxbase.configuration.fileRepresentation.PrinterDefinitionFile;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
import celtech.roboxbase.utils.Math.packing.core.Bin;
import celtech.roboxbase.utils.Math.packing.core.BinPacking;
import celtech.roboxbase.utils.Math.packing.primitives.MArea;
import celtech.roboxbase.utils.RectangularBounds;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public abstract class Project
{

    public static class ProjectLoadException extends Exception
    {

        public ProjectLoadException(String message)
        {
            super(message);
        }
    }

    private int version = -1;

    private static final Stenographer steno = StenographerFactory.getStenographer(Project.class.getName());

    protected Set<ProjectChangesListener> projectChangesListeners;

    protected BooleanProperty canPrint;

    protected final StringProperty projectNameProperty;
    protected ObjectProperty<Date> lastModifiedDate;

    protected boolean suppressProjectChanged = false;

    protected ObjectProperty<ProjectMode> mode = new SimpleObjectProperty<>(ProjectMode.NONE);

    protected ObservableList<ProjectifiableThing> topLevelThings;

    protected String lastPrintJobID = "";

    public Project()
    {
        topLevelThings = FXCollections.observableArrayList();

        initialise();

        canPrint = new SimpleBooleanProperty(true);
        lastModifiedDate = new SimpleObjectProperty<>();
        projectChangesListeners = new HashSet<>();

        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("-hhmmss-ddMMYY");
        projectNameProperty = new SimpleStringProperty(Lookup.i18n("projectLoader.untitled")
                + formatter.format(now));
        lastModifiedDate.set(now);

        Lookup.getUserPreferences().getSlicerTypeProperty().addListener(
                (ObservableValue<? extends SlicerType> observable, SlicerType oldValue, SlicerType newValue) ->
                {
                    projectModified();
                });

    }

    protected abstract void initialise();

    public final void setProjectName(String value)
    {
        projectNameProperty.set(value);
    }

    public final String getProjectName()
    {
        return projectNameProperty.get();
    }

    public final StringProperty projectNameProperty()
    {
        return projectNameProperty;
    }

    public final String getAbsolutePath()
    {
        return ApplicationConfiguration.getProjectDirectory() + File.separator
                + projectNameProperty.get()
                + ApplicationConfiguration.projectFileExtension;
    }

    protected abstract void load(ProjectFile projectFile, String basePath) throws ProjectLoadException;

    public static final Project loadProject(String basePath)
    {
        Project project = null;
        File file = new File(basePath + ApplicationConfiguration.projectFileExtension);

        try
        {
            ProjectFileDeserialiser deserializer
                    = new ProjectFileDeserialiser();
            SimpleModule module
                    = new SimpleModule("LegacyProjectFileDeserialiserModule",
                            new Version(1, 0, 0, null));
            module.addDeserializer(ProjectFile.class, deserializer);

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(module);
            ProjectFile projectFile = mapper.readValue(file, ProjectFile.class);

            if (projectFile instanceof ModelContainerProjectFile)
            {
                project = new ModelContainerProject();
                project.load(projectFile, basePath);
            } else if (projectFile instanceof ShapeContainerProjectFile)
            {
                project = new ShapeContainerProject();
                project.load(projectFile, basePath);
            }
        } catch (Exception ex)
        {
            steno.exception("Unable to load project file at " + basePath, ex);
        }
        return project;
    }

    protected abstract void save(String basePath);

    public static final void saveProject(Project project)
    {
        if (project != null)
        {
            String basePath = ApplicationConfiguration.getProjectDirectory() + File.separator
                    + project.getProjectName();
            project.save(basePath);
        }
    }

    @Override
    public String toString()
    {
        return projectNameProperty.get();
    }

    public abstract void addModel(ProjectifiableThing projectifiableThing);

    public abstract void removeModels(Set<ProjectifiableThing> projectifiableThings);

    public final void addProjectChangesListener(ProjectChangesListener projectChangesListener)
    {
        projectChangesListeners.add(projectChangesListener);
    }

    public final void removeProjectChangesListener(ProjectChangesListener projectChangesListener)
    {
        projectChangesListeners.remove(projectChangesListener);
    }

    public final ObjectProperty<Date> getLastModifiedDate()
    {
        return lastModifiedDate;
    }

    public final BooleanProperty canPrintProperty()
    {
        return canPrint;
    }

    /**
     * ProjectChangesListener allows other objects to observe when models are
     * added or removed etc to the project.
     */
    public interface ProjectChangesListener
    {

        /**
         * This should be fired when a model is added to the project.
         *
         * @param projectifiableThing
         */
        void whenModelAdded(ProjectifiableThing projectifiableThing);

        /**
         * This should be fired when a model is removed from the project.
         *
         * @param projectifiableThing
         */
        void whenModelsRemoved(Set<ProjectifiableThing> projectifiableThing);

        /**
         * This should be fired when the project is auto laid out.
         */
        void whenAutoLaidOut();

        /**
         * This should be fired when one or more models have been moved, rotated
         * or scaled etc. If possible try to fire just once for any given group
         * change.
         *
         * @param projectifiableThing
         */
        void whenModelsTransformed(Set<ProjectifiableThing> projectifiableThing);

        /**
         * This should be fired when certain details of the model change.
         * Currently this is only: - associatedExtruder
         *
         * @param modelContainer
         * @param propertyName
         */
        void whenModelChanged(ProjectifiableThing modelContainer, String propertyName);

        /**
         * This should be fired whenever the PrinterSettings of the project
         * changes.
         *
         * @param printerSettings
         */
        void whenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings);
    }

    public abstract void autoLayout();

    //This carries out the same function but leaves the existing things in place
    public void autoLayout(List<ProjectifiableThing> thingsToLayout)
    {
        double printVolumeWidth = 0;
        double printVolumeDepth = 0;

        if (Lookup.getSelectedPrinterProperty().get() != null
                && Lookup.getSelectedPrinterProperty().get().printerConfigurationProperty().get() != null)
        {
            printVolumeWidth = Lookup.getSelectedPrinterProperty().get().printerConfigurationProperty().get().getPrintVolumeWidth();
            printVolumeDepth = Lookup.getSelectedPrinterProperty().get().printerConfigurationProperty().get().getPrintVolumeDepth();
        } else
        {
            PrinterDefinitionFile defaultPrinterConfiguration = PrinterContainer.getPrinterByID(PrinterContainer.defaultPrinterID);
            printVolumeWidth = defaultPrinterConfiguration.getPrintVolumeWidth();
            printVolumeDepth = defaultPrinterConfiguration.getPrintVolumeDepth();
        }

        Dimension binDimension = new Dimension((int) printVolumeWidth, (int) printVolumeDepth);
        Bin layoutBin = null;

        final double spacing = 2.5;
        final double halfSpacing = spacing / 2.0;

        Map<Integer, ProjectifiableThing> partMap = new HashMap<>();

        int numberOfPartsNotToLayout = topLevelThings.size() - thingsToLayout.size();

        if (numberOfPartsNotToLayout > 0)
        {
            MArea[] existingPieces = new MArea[numberOfPartsNotToLayout];
            int existingPartCounter = 0;
            for (ProjectifiableThing thingToConsider : topLevelThings)
            {
                if (!thingsToLayout.contains(thingToConsider))
                {
                    //We need to stop this part from being laid out
                    RectangularBounds pieceBounds = thingToConsider.calculateBoundsInBedCoordinateSystem();
                    Rectangle2D.Double rectangle = new Rectangle2D.Double(pieceBounds.getMinX() - halfSpacing,
                            printVolumeDepth - pieceBounds.getMaxZ() - halfSpacing,
                            pieceBounds.getWidth() + halfSpacing,
                            pieceBounds.getDepth() + halfSpacing);

                    double currentRotation = 0;
                    if (thingToConsider instanceof RotatableTwoD)
                    {
                        currentRotation = ((RotatableTwoD) thingToConsider).getRotationTurn();
                    }

                    MArea piece = new MArea(rectangle, existingPartCounter, currentRotation);
                    existingPieces[existingPartCounter] = piece;
                    partMap.put(existingPartCounter, thingToConsider);
                    existingPartCounter++;
                }
            }
            layoutBin = new Bin(binDimension, existingPieces);
        }

        int startingIndexForPartsToLayout = (numberOfPartsNotToLayout == 0) ? 0 : numberOfPartsNotToLayout - 1;

        MArea[] partsToLayout = new MArea[thingsToLayout.size()];
        int partsToLayoutCounter = 0;
        for (ProjectifiableThing thingToLayout : thingsToLayout)
        {
            RectangularBounds pieceBounds = thingToLayout.calculateBoundsInBedCoordinateSystem();
            double width = pieceBounds.getWidth();
            double depth = 0;

            if (thingToLayout instanceof TranslateableThreeD)
            {
                depth = pieceBounds.getDepth();
            } else
            {
                depth = pieceBounds.getHeight();
            }
            //Change the coords so that every part is in the bottom left corner
            Rectangle2D.Double rectangle = new Rectangle2D.Double(0, 0,
                    width + spacing,
                    depth + spacing);
//            Rectangle2D.Double rectangle = new Rectangle2D.Double(pieceBounds.getMinX() - halfSpacing,
//                    printVolumeDepth - pieceBounds.getMaxZ() - halfSpacing,
//                    pieceBounds.getWidth() + halfSpacing,
//                    pieceBounds.getDepth() + halfSpacing);
            MArea piece = new MArea(rectangle, partsToLayoutCounter + startingIndexForPartsToLayout, 0);
            partsToLayout[partsToLayoutCounter] = piece;
            partMap.put(partsToLayoutCounter + startingIndexForPartsToLayout, thingToLayout);
            partsToLayoutCounter++;
        }
        steno.info("started with");
        for (MArea area : partsToLayout)
        {
            steno.info("Piece " + area.getID()
                    + " X" + area.getBoundingBox2D().getX()
                    + " Y" + area.getBoundingBox2D().getY()
                    + " W" + area.getBoundingBox2D().getWidth()
                    + " H" + area.getBoundingBox2D().getHeight()
                    + " R" + area.getRotation()
            );

        }

        if (layoutBin != null)
        {
            MArea[] unplacedParts = null;
            unplacedParts = layoutBin.BBCompleteStrategy(partsToLayout);
            steno.info("Unplaced = " + unplacedParts.length);
        } else
        {
            Bin[] bins = BinPacking.BinPackingStrategy(partsToLayout, binDimension, binDimension);
            layoutBin = bins[0];
        }

        int numberOfPartsInTotal = layoutBin.getPlacedPieces().length;
        double newXPosition[] = new double[numberOfPartsInTotal];
        double newDepthPosition[] = new double[numberOfPartsInTotal];
        double newRotation[] = new double[numberOfPartsInTotal];
        double minLayoutX = 999, maxLayoutX = -999, minLayoutY = 999, maxLayoutY = -999;

        for (int pieceNumber = 0; pieceNumber < numberOfPartsInTotal; pieceNumber++)
        {
            MArea area = layoutBin.getPlacedPieces()[pieceNumber];

            steno.info("Piece " + area.getID()
                    + " X" + area.getBoundingBox2D().getX()
                    + " Y" + area.getBoundingBox2D().getY()
                    + " W" + area.getBoundingBox2D().getWidth()
                    + " H" + area.getBoundingBox2D().getHeight()
                    + " R" + area.getRotation()
            );
            newRotation[pieceNumber] = area.getRotation();

            newDepthPosition[pieceNumber] = printVolumeDepth - area.getBoundingBox2D().getMaxY() + area.getBoundingBox2D().getHeight() / 2.0;
            newXPosition[pieceNumber] = area.getBoundingBox2D().getMinX() + (area.getBoundingBox2D().getWidth() / 2.0);

            maxLayoutX = Math.max(maxLayoutX, area.getBoundingBox2D().getMaxX());
            minLayoutX = Math.min(minLayoutX, area.getBoundingBox2D().getMinX());
            maxLayoutY = Math.max(maxLayoutY, area.getBoundingBox2D().getMaxY());
            minLayoutY = Math.min(minLayoutY, area.getBoundingBox2D().getMinY());
        }

        steno.info("minx " + minLayoutX + " maxX " + maxLayoutX);
        steno.info("miny " + minLayoutY + " maxY " + maxLayoutY);
        double xCentringOffset = (printVolumeWidth - (maxLayoutX - minLayoutX)) / 2.0;
        double yCentringOffset = (printVolumeDepth - (maxLayoutY - minLayoutY)) / 2.0;

        steno.info("Centring offset x  " + xCentringOffset + " y " + yCentringOffset);
        for (int pieceNumber = 0; pieceNumber < layoutBin.getPlacedPieces().length; pieceNumber++)
        {
            MArea area = layoutBin.getPlacedPieces()[pieceNumber];
            ProjectifiableThing container = (ProjectifiableThing) partMap.get(area.getID());

            if (thingsToLayout.contains(container))
            {
                if (container instanceof RotatableTwoD)
                {
                    double rotation = newRotation[pieceNumber] + ((RotatableTwoD) container).getRotationTurn();
                    if (rotation >= 360.0)
                    {
                        rotation -= 360.0;
                    }
                    ((RotatableTwoD) container).setRotationTurn(rotation);
                }

                //Only auto centre if we're laying out all of the parts
                if (container instanceof TranslateableTwoD)
                {
//                    if (numberOfPartsNotToLayout == 0)
//                    {
//                        ((TranslateableTwoD) container).translateTo(newXPosition[pieceNumber] + xCentringOffset, newDepthPosition[pieceNumber] + yCentringOffset);
//                    } else
                    {
                        ((TranslateableTwoD) container).translateTo(newXPosition[pieceNumber], newDepthPosition[pieceNumber]);
                    }
                }
//                steno.info("Thing " + area.getID() + " is at cX" + container.getTransformedCentreX() + " cY" + container.getTransformedCentreDepth() + " r" + container.getRotationTurn());
            }
        }

        projectModified();
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenAutoLaidOut();
        }
    }

    /**
     * Scale X, Y and Z by the given factor, apply the given ratio to the given
     * scale. I.e. the ratio is not an absolute figure to be applied to the
     * models but a ratio to be applied to the current scale.
     *
     * @param projectifiableThings
     * @param ratio
     */
    public final void scaleXYZRatioSelection(Set<ScaleableThreeD> projectifiableThings, double ratio)
    {
        for (ScaleableThreeD projectifiableThing : projectifiableThings)
        {
            projectifiableThing.setXScale(projectifiableThing.getXScale() * ratio);
            projectifiableThing.setYScale(projectifiableThing.getYScale() * ratio);
            projectifiableThing.setZScale(projectifiableThing.getZScale() * ratio);
        }
        projectModified();
        fireWhenModelsTransformed((Set) projectifiableThings);
    }

    /**
     * Scale X, Y and Z by the given factor, apply the given ratio to the given
     * scale. I.e. the ratio is not an absolute figure to be applied to the
     * models but a ratio to be applied to the current scale.
     *
     * @param projectifiableThings
     * @param ratio
     */
    public final void scaleXYRatioSelection(Set<ScaleableTwoD> projectifiableThings, double ratio)
    {
        for (ScaleableTwoD projectifiableThing : projectifiableThings)
        {
            projectifiableThing.setXScale(projectifiableThing.getXScale() * ratio);
            projectifiableThing.setYScale(projectifiableThing.getYScale() * ratio);
        }
        projectModified();
        fireWhenModelsTransformed((Set) projectifiableThings);
    }

    public final void scaleXModels(Set<ScaleableTwoD> projectifiableThings, double newScale,
            boolean preserveAspectRatio)
    {
        if (preserveAspectRatio)
        {
            // this only happens for non-multiselect
            assert (projectifiableThings.size() == 1);
            ScaleableTwoD projectifiableThing = projectifiableThings.iterator().next();
            double ratio = newScale / projectifiableThing.getXScale();
            if (projectifiableThing instanceof ScaleableThreeD)
            {
                scaleXYZRatioSelection((Set) projectifiableThings, ratio);
            } else
            {
                scaleXYRatioSelection(projectifiableThings, ratio);
            }
        } else
        {
            for (ScaleableTwoD projectifiableThing : projectifiableThings)
            {
                {
                    projectifiableThing.setXScale(newScale);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed((Set) projectifiableThings);
    }

    public final void scaleYModels(Set<ScaleableTwoD> projectifiableThings, double newScale,
            boolean preserveAspectRatio)
    {
        if (preserveAspectRatio)
        {
            // this only happens for non-multiselect
            assert (projectifiableThings.size() == 1);
            ScaleableTwoD projectifiableThing = projectifiableThings.iterator().next();
            double ratio = newScale / projectifiableThing.getYScale();

            if (projectifiableThing instanceof ScaleableThreeD)
            {
                scaleXYZRatioSelection((Set) projectifiableThings, ratio);
            } else
            {
                scaleXYRatioSelection(projectifiableThings, ratio);
            }
        } else
        {
            for (ScaleableTwoD projectifiableThing : projectifiableThings)
            {
                {
                    projectifiableThing.setYScale(newScale);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed((Set) projectifiableThings);
    }

    public final void scaleZModels(Set<ScaleableThreeD> projectifiableThings, double newScale,
            boolean preserveAspectRatio)
    {
        if (preserveAspectRatio)
        {
            // this only happens for non-multiselect
            assert (projectifiableThings.size() == 1);
            ScaleableThreeD projectifiableThing = projectifiableThings.iterator().next();
            double ratio = newScale / projectifiableThing.getZScale();
            scaleXYZRatioSelection(projectifiableThings, ratio);
        } else
        {
            for (ScaleableThreeD projectifiableThing : projectifiableThings)
            {
                {
                    projectifiableThing.setZScale(newScale);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed((Set) projectifiableThings);
    }

    public void translateModelsBy(Set<TranslateableTwoD> modelContainers, double x, double y)
    {
        for (TranslateableTwoD model : modelContainers)
        {
            model.translateBy(x, y);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }

    public void translateModelsTo(Set<TranslateableTwoD> modelContainers, double x, double y)
    {
        for (TranslateableTwoD model : modelContainers)
        {
            model.translateTo(x, y);
        }
        projectModified();
        fireWhenModelsTransformed((Set) modelContainers);
    }

    public void translateModelsXTo(Set<TranslateableTwoD> modelContainers, double x)
    {
        for (TranslateableTwoD model : modelContainers)
        {
            model.translateXTo(x);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }

    public void translateModelsDepthPositionTo(Set<Translateable> modelContainers, double z)
    {
        for (Translateable model : modelContainers)
        {
            model.translateDepthPositionTo(z);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }

    public void resizeModelsDepth(Set<ResizeableThreeD> modelContainers, double depth)
    {
        for (ResizeableThreeD model : modelContainers)
        {
            model.resizeDepth(depth);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }

    public void resizeModelsHeight(Set<ResizeableTwoD> modelContainers, double height)
    {
        for (ResizeableTwoD model : modelContainers)
        {
            model.resizeHeight(height);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }

    public void resizeModelsWidth(Set<ResizeableTwoD> modelContainers, double width)
    {
        for (ResizeableTwoD model : modelContainers)
        {
            model.resizeWidth(width);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }

    public abstract Set<ProjectifiableThing> getAllModels();

    public final Set<ItemState> getModelStates()
    {
        Set<ItemState> states = new HashSet<>();
        for (ProjectifiableThing model : getAllModels())
        {
            states.add(model.getState());
        }
        return states;
    }

    public final void setModelStates(Set<ItemState> modelStates)
    {
        Set<ProjectifiableThing> modelContainers = new HashSet<>();
        for (ItemState modelState : modelStates)
        {
            for (ProjectifiableThing model : getAllModels())
            {
                if (model.getModelId() == modelState.modelId)
                {
                    model.setState(modelState);
                    model.updateOriginalModelBounds();
                    modelContainers.add(model);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public final ReadOnlyObjectProperty<ProjectMode> getModeProperty()
    {
        return mode;
    }

    public ProjectMode getMode()
    {
        return mode.get();
    }

    public final void setMode(ProjectMode mode)
    {
        this.mode.set(mode);
    }

    protected final void projectModified()
    {
        if (!suppressProjectChanged)
        {
            lastPrintJobID = "";
            lastModifiedDate.set(new Date());
        }
    }

    abstract protected void fireWhenModelsTransformed(Set<ProjectifiableThing> projectifiableThings);

    abstract protected void fireWhenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings);

    public int getNumberOfProjectifiableElements()
    {
        return getAllModels().size();
    }

    public ObservableList<ProjectifiableThing> getTopLevelThings()
    {
        return topLevelThings;
    }

    public void setLastPrintJobID(String lastPrintJobID)
    {
        this.lastPrintJobID = lastPrintJobID;
    }

    public String getLastPrintJobID()
    {
        return lastPrintJobID;
    }

    public ModelGroup group(Set<Groupable> modelContainers)
    {
        Set<ProjectifiableThing> projectifiableThings = (Set) modelContainers;

        removeModels(projectifiableThings);
        ModelGroup modelGroup = createNewGroup(modelContainers);
        addModel(modelGroup);
        return modelGroup;
    }

    public ModelGroup group(Set<Groupable> modelContainers, int groupModelId)
    {
        Set<ProjectifiableThing> projectifiableThings = (Set) modelContainers;

        removeModels(projectifiableThings);
        ModelGroup modelGroup = createNewGroup(modelContainers, groupModelId);
        addModel(modelGroup);
        return modelGroup;
    }

    /**
     * Create a new group from models that are not yet in the project.
     *
     * @param modelContainers
     * @param groupModelId
     * @return
     */
    public ModelGroup createNewGroup(Set<Groupable> modelContainers, int groupModelId)
    {
        checkNotAlreadyInGroup(modelContainers);
        ModelGroup modelGroup = new ModelGroup((Set) modelContainers, groupModelId);
        modelGroup.checkOffBed();
        modelGroup.notifyScreenExtentsChange();
        return modelGroup;
    }

    /**
     * Create a new group from models that are not yet in the project.
     *
     * @param modelContainers
     * @return
     */
    public ModelGroup createNewGroup(Set<Groupable> modelContainers)
    {
        checkNotAlreadyInGroup(modelContainers);

        ModelGroup modelGroup = new ModelGroup((Set) modelContainers);
        modelGroup.checkOffBed();
        modelGroup.notifyScreenExtentsChange();
        return modelGroup;
    }

    public void ungroup(Set<? extends ModelContainer> modelContainers)
    {
        List<ProjectifiableThing> ungroupedModels = new ArrayList<>();

        for (ModelContainer modelContainer : modelContainers)
        {
            if (modelContainer instanceof ModelGroup)
            {
                ModelGroup modelGroup = (ModelGroup) modelContainer;
                Set<ProjectifiableThing> modelGroups = new HashSet<>();
                modelGroups.add(modelGroup);
                removeModels(modelGroups);
                for (ModelContainer childModelContainer : modelGroup.getChildModelContainers())
                {
                    addModel(childModelContainer);
                    childModelContainer.setBedCentreOffsetTransform();
                    childModelContainer.applyGroupTransformToThis(modelGroup);
                    childModelContainer.updateLastTransformedBoundsInParent();
                    ungroupedModels.add(childModelContainer);
                }
                Set<ProjectifiableThing> changedModels = new HashSet<>(modelGroup.getChildModelContainers());
                fireWhenModelsTransformed(changedModels);
            }
        }
    }

    protected abstract void checkNotAlreadyInGroup(Set<Groupable> modelContainers);

    /**
     * Create a new group from models that are not yet in the project, and add
     * model listeners to all descendent children.
     *
     * @param modelContainers
     * @return
     */
    public abstract ModelGroup createNewGroupAndAddModelListeners(Set<Groupable> modelContainers);

    @JsonIgnore
    public void invalidate()
    {
        projectModified();
    }

    public void rotateTurnModels(Set<RotatableTwoD> modelContainers, double rotation)
    {
        for (RotatableTwoD model : modelContainers)
        {
            model.setRotationTurn(rotation);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }
}
