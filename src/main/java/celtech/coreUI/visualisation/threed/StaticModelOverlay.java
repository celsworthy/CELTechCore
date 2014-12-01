package celtech.coreUI.visualisation.threed;

import celtech.CoreTest;
import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintBed;
import celtech.coreUI.visualisation.Xform;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.coreUI.visualisation.importers.obj.ObjImporter;
import celtech.modelcontrol.ModelContainer;
import celtech.services.modelLoader.ModelLoadResults;
import celtech.services.modelLoader.ModelLoaderService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class StaticModelOverlay
{

    private final Stenographer steno = StenographerFactory.getStenographer(StaticModelOverlay.class.getName());
    private final Group root3D = new Group();
    private SubScene subScene = null;
    private final Xform bedTranslateXform = new Xform(Xform.RotateOrder.YXZ, "BedXForm");
    private Group bed;
    private final double bedXOffsetFromCameraZero;
    private final double bedZOffsetFromCameraZero;
    private Group models = new Group();
    private final PrintBed printBedData = PrintBed.getInstance();

    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private double demandedCameraRotationX = 0;
    private double demandedCameraRotationY = 0;
    private ModelLoaderService modelLoaderService = new ModelLoaderService();

    public StaticModelOverlay(double width, double height)
    {
        root3D.setPickOnBounds(false);
        root3D.getChildren().add(camera);

        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);

        subScene = new SubScene(root3D, 500, 500, true,
                                SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(camera);

        bed = buildBed();

        bedTranslateXform.getChildren().addAll(bed, models);
        root3D.getChildren().add(bedTranslateXform);

        bedXOffsetFromCameraZero = -printBedData.getPrintVolumeBounds().getWidth() / 2;
        bedZOffsetFromCameraZero = -printBedData.getPrintVolumeBounds().getDepth() / 2;

        bedTranslateXform.setTx(bedXOffsetFromCameraZero);
        bedTranslateXform.setTz(bedZOffsetFromCameraZero - 350);
        bedTranslateXform.setPivot(-bedXOffsetFromCameraZero, 0, -bedZOffsetFromCameraZero);

        rotateCameraAroundAxes(-30, 0);

        modelLoaderService.setOnSucceeded((WorkerStateEvent t) ->
        {
            whenModelLoadSucceeded();
        });
    }

    public void showModelForPrintJob(String printjobID)
    {
        String modelPath = ApplicationConfiguration.getPrintSpoolDirectory() + printjobID + File.separator + printjobID + ".stl";
        List<File> modelsToLoad = new ArrayList<>();
        modelsToLoad.add(new File(modelPath));

        modelLoaderService.reset();
        modelLoaderService.setModelFilesToLoad(modelsToLoad, false);
        modelLoaderService.start();
    }

    public void clearModel()
    {
        models.getChildren().clear();
    }

    private void whenModelLoadSucceeded()
    {
        ModelLoadResults loadResults = modelLoaderService.getValue();
        if (loadResults.getResults().isEmpty())
        {
            return;
        }

        for (ModelLoadResult loadResult : loadResults.getResults())
        {
            if (loadResult != null)
            {
                ModelContainer modelContainer = loadResult.getModelContainer();

                models.getChildren().add(modelContainer);
            } else
            {
                steno.error("Error whilst attempting to load model");
            }
        }
    }

    public SubScene getSubScene()
    {
        return subScene;
    }

    public void resize(double newWidth, double newHeight)
    {
        subScene.resize(newWidth, newHeight);
    }

    private Group buildBed()
    {
        String bedOuterURL = CoreTest.class
            .getResource(ApplicationConfiguration.modelResourcePath + "bedOuter.obj").toExternalForm();
        String bedInnerURL = CoreTest.class.getResource(ApplicationConfiguration.modelResourcePath
            + "bedInner.obj").toExternalForm();

        PhongMaterial bedOuterMaterial = new PhongMaterial(Color.rgb(65, 65, 65));

        bedOuterMaterial.setSpecularColor(Color.WHITE);

        bedOuterMaterial.setSpecularPower(5.0);

        PhongMaterial bedInnerMaterial = new PhongMaterial(Color.GREY);

        bedInnerMaterial.setSpecularColor(Color.WHITE);

        bedInnerMaterial.setSpecularPower(.1);

        Group bed = new Group();
        bed.setId("Bed");

        ObjImporter bedOuterImporter = new ObjImporter();
        ModelLoadResult bedOuterLoadResult = bedOuterImporter.loadFile(null, bedOuterURL, null);

        bed.getChildren().addAll(bedOuterLoadResult.getModelContainer().getMeshes());

        ObjImporter bedInnerImporter = new ObjImporter();
        ModelLoadResult bedInnerLoadResult = bedInnerImporter.loadFile(null, bedInnerURL, null);

        bed.getChildren().addAll(bedInnerLoadResult.getModelContainer().getMeshes());

        final Image roboxLogoImage = new Image(CoreTest.class.getResource(
            ApplicationConfiguration.imageResourcePath + "roboxLogo.png").toExternalForm());
        final ImageView roboxLogoView = new ImageView();

        roboxLogoView.setImage(roboxLogoImage);

        final Xform roboxLogoTransformNode = new Xform();

        roboxLogoTransformNode.setRotateX(-90);

        final double logoSide_mm = 100;
        double logoScale = logoSide_mm / roboxLogoImage.getWidth();

        roboxLogoTransformNode.setScale(logoScale);

        roboxLogoTransformNode.setTz(logoSide_mm
            + PrintBed.getPrintVolumeCentre().getZ() / 2);
        roboxLogoTransformNode.setTy(-.25);
        roboxLogoTransformNode.setTx(PrintBed.getPrintVolumeCentre().getX() / 2);
        roboxLogoTransformNode.getChildren().add(roboxLogoView);
        roboxLogoTransformNode.setId("LogoImage");

        bed.getChildren().add(roboxLogoTransformNode);
        bed.setMouseTransparent(true);

        return bed;
    }

    public void rotateCameraAroundAxes(double xangle, double yangle)
    {
        double yAxisRotation = demandedCameraRotationY - yangle;

        if (yAxisRotation > 360)
        {
            yAxisRotation = yAxisRotation - 360;
        } else if (yAxisRotation < 0)
        {
            yAxisRotation = yAxisRotation + 360;
        }
        demandedCameraRotationY = yAxisRotation;

        double xAxisRotation = demandedCameraRotationX - xangle;
        if (xAxisRotation > 89)
        {
            xAxisRotation = 89;
        } else if (xAxisRotation < 0)
        {
            xAxisRotation = 0;
        }
        demandedCameraRotationX = xAxisRotation;

        bedTranslateXform.setRotateY(yAxisRotation);
        bedTranslateXform.setRotateX(xAxisRotation);

    }

}
