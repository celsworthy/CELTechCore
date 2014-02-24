/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation.importers.gcode;

import celtech.services.gcodeLoader.GCodeLoadState;
import celtech.services.gcodeLoader.GCodeEventType;
import celtech.services.gcodeLoader.GCodeParseException;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ProjectTab;
import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.coreUI.visualisation.Xform;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.modelcontrol.ModelContainer;
import celtech.services.modelLoader.ModelLoaderTask;
import celtech.utils.Math.MathUtils;
import celtech.utils.Math.PolarCoordinate;
import celtech.utils.gcode.representation.CompoundMovement;
import celtech.utils.gcode.representation.ExtrusionMode;
import celtech.utils.gcode.representation.GCodeElement;
import celtech.utils.gcode.representation.GCodeFile;
import celtech.utils.gcode.representation.GCodeMeshData;
import celtech.utils.gcode.representation.Layer;
import celtech.utils.gcode.representation.Movement;
import celtech.utils.gcode.representation.MovementMode;
import celtech.utils.gcode.representation.MovementType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Point3D;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Shape3D;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class GCodeImporter
{

    private final Stenographer steno = StenographerFactory.getStenographer(GCodeImporter.class.getName());
    private final String G0_TOKEN = "G0";
    private final String G1_TOKEN = "G1";
    private Point3D currentPositionWorldCoords = new Point3D(0, 0, 0);
    private NumberFormat numberFormatter = NumberFormat.getNumberInstance();
    private ArrayList<String> fileLines = new ArrayList();
    private float zLevel = 0;
    private int layerNumber = 0;
    private int lineNumber = 0;
    private GCodeMeshData gcodeMeshData = null;

    private Group gcodeParts = new Group();
    private HashMap<Integer, GCodeElement> referencedElements = new HashMap<>();
    private HashMap<Integer, Group> referencedLayers = new HashMap<>();
    /*
     * GCode meta representation
     */
    private GCodeFile gcodeMetaFile = new GCodeFile();
    private CompoundMovement currentMovement = null;
    private ArrayList<Shape> currentLayer = null;
    private Group layerNode = new Group();

    private Point3D lastPoint = new Point3D(0, 0, 0);

    private ModelLoaderTask parentTask = null;
    private DoubleProperty progressProperty = null;

    private ArrayList<Xform> partHolder = new ArrayList<Xform>();

    public ModelLoadResult loadFile(ModelLoaderTask parentTask, String modelFileToLoad, ProjectTab targetProjectTab, DoubleProperty progressProperty)
    {
        this.parentTask = parentTask;
        this.progressProperty = progressProperty;

        File fFile = new File(modelFileToLoad);
        fileLines.clear();
        int linesInFile = getNumberOfLines(fFile);

        //Note that FileReader is used, not File, since File is not Closeable
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(fFile));

//            Scanner scanner = new Scanner(new FileReader(fFile));
            lineNumber = 1;

            try
            {
                //first use a Scanner to get each line
                String line;
                while ((line = reader.readLine()) != null && !parentTask.isCancelled())
                {
                    processLine(line, lineNumber);
                    fileLines.add(lineNumber + " " + line);
                    lineNumber++;
                    if (lineNumber < linesInFile)
                    {
                        progressProperty.set(((double) lineNumber / (double) linesInFile) * 100);
                    }
                }
                reader.close();
            } catch (GCodeParseException ex)
            {
                steno.error("File parsing exception whilst processing " + fFile.getName() + " : " + ex + " on line " + lineNumber);
            } catch (IOException ex)
            {
                steno.error("IO Exception whilst processing " + fFile.getName() + " : " + ex + " on line " + lineNumber);
            } finally
            {
            }

        } catch (FileNotFoundException ex)
        {
            steno.error("Couldn't find or open " + fFile.getName());
        }

//        ArrayList<BatchNode> nodeList = null;
//        if (!parentTask.isCancelled())
//        {
//            parentTask.updateMessageText(DisplayManager.getLanguageBundle().getString("dialogs.gcodeLoadProcessing"));
////            gcodeMeshData = GCodeMeshGenerator.generateMesh(gcodeMetaFile, progressProperty);
//        }
        if (!parentTask.isCancelled())
        {
//            fxtojmeInterface.sendGCodeModel(fFile.getName(), nodeList);
            DisplayManager.getInstance().setLayersInGCode(layerNumber);
//            currentProject.setGCodeModelLoaded(fFile.getAbsolutePath());
            steno.info("Loaded gcode " + fFile.getName());

            ModelContainer modelContainer = new ModelContainer(fFile.getName(), new GCodeMeshData(gcodeParts, referencedElements, referencedLayers), fileLines);

            // Not sure if we should put the check for oversized GCode in or not...
//            BoundingBox bounds = (BoundingBox) modelContainer.getBoundsInLocal();
//            modelIsTooLarge = PrintBed.isBiggerThanPrintVolume(bounds);
            ModelLoadResult result = new ModelLoadResult(false, modelFileToLoad, fFile.getName(), targetProjectTab, modelContainer);
            result.setFileLines(fileLines);
            return result;
        } else
        {
            return null;
        }

    }

    private String quote(String aText)
    {
        String QUOTE = "'";
        return QUOTE + aText + QUOTE;
    }

    private int getNumberOfLines(File aFile)
    {
        LineNumberReader reader = null;
        try
        {
            reader = new LineNumberReader(new FileReader(aFile));
            while ((reader.readLine()) != null);
            return reader.getLineNumber();
        } catch (Exception ex)
        {
            return -1;
        } finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                } catch (IOException ex)
                {
                    steno.error("Failed to close file during line number read: " + ex);
                }
            }
        }
    }

    protected void processLine(String aLine, int currentLineNumber) throws GCodeParseException
    {
        GCodeLoadState loadState = GCodeLoadState.IDLE;
        boolean gotXPos = false;
        float xPos = 0;
        float yPos = 0;
        boolean gotYPos = false;
        float zPos = 0;
        boolean gotZPos = false;
        float eValue = 0;
        boolean gotEValue = false;
        boolean validPositionData = false;
        boolean parseFailure = false;
        boolean retractDetected = false;
        boolean unretractDetected = false;
        boolean supportDetected = false;

        // Get rid of extraneous junk
        aLine = aLine.trim();

        // Split the content and the comments
        String[] segregatedData = aLine.split(";");

//        Scanner scanner = new Scanner(segregatedData[0]);
        if (segregatedData.length > 1)
        {
            // We have a line that has both content and comments -- use this as a retract/unretract cue
            if (segregatedData[1].toLowerCase().contains("unretract"))
            {
                unretractDetected = true;
            } else if (segregatedData[1].toLowerCase().contains("retract"))
            {
                retractDetected = true;
            }

            if (segregatedData[1].toLowerCase().contains("support"))
            {
                supportDetected = true;
            }

        }

//        scanner.useDelimiter(" +");
        String[] lineParts = segregatedData[0].split(" +");

        if (lineParts[0].length() > 0)
        {
            for (int partCounter = 0; partCounter < lineParts.length; partCounter++)
            {
                if (parentTask.isCancelled() || parseFailure)
                {
                    break;
                }
//        while (scanner.hasNext() && parseFailure == false && !parentTask.isCancelled())
//        {
                // Only look for G1 Xn Yn lines for the moment

                String part = lineParts[partCounter];

                if (loadState == GCodeLoadState.IDLE && (part.equals(G0_TOKEN) || part.equals(G1_TOKEN)))
                {
                    loadState = GCodeLoadState.FOUND_G0_G1;

                    String token = part;
//                    steno.trace("Got " + token);
                } else if (loadState == GCodeLoadState.FOUND_G0_G1 && part.startsWith("X"))
                {
//                    steno.trace("Got an X co-ord");
                    String valueBit = part.substring(1);
                    try
                    {
                        xPos = numberFormatter.parse(valueBit).floatValue();
//                        steno.trace("Got " + xPos);
                        gotXPos = true;
                        validPositionData = true;
                    } catch (ParseException ex)
                    {
                        steno.error("Error whilst parsing X value (" + valueBit + ")");
                        parseFailure = true;
                    }
                } else if (loadState == GCodeLoadState.FOUND_G0_G1 && part.startsWith("Y"))
                {
//                    steno.trace("Got a Y co-ord");
                    String valueBit = part.substring(1);
                    try
                    {
                        yPos = numberFormatter.parse(valueBit).floatValue();
//                        steno.trace("Got " + yPos);
                        gotYPos = true;
                        validPositionData = true;
                    } catch (ParseException ex)
                    {
                        steno.error("Error whilst parsing Y value (" + valueBit + ")");
                        parseFailure = true;
                    }
                } else if (loadState == GCodeLoadState.FOUND_G0_G1 && part.startsWith("Z"))
                {
//                    steno.trace("Got a Z co-ord");
                    String valueBit = part.substring(1);
                    try
                    {
                        zPos = numberFormatter.parse(valueBit).floatValue();
//                        steno.trace("Got " + zPos);
                        gotZPos = true;
                        validPositionData = true;
                    } catch (ParseException ex)
                    {
                        steno.error("Error whilst parsing Z value (" + valueBit + ")");
                        parseFailure = true;
                    }
                } else if (loadState == GCodeLoadState.FOUND_G0_G1 && part.startsWith("E"))
                {
//                    steno.trace("Got an E value");
                    String valueBit = part.substring(1);
                    try
                    {
                        eValue = numberFormatter.parse(valueBit).floatValue();
//                        steno.trace("Got " + eValue);
                        gotEValue = true;
                        validPositionData = true;
                    } catch (ParseException ex)
                    {
                        steno.error("Error whilst parsing E value (" + valueBit + ")");
                        parseFailure = true;
                    }
                } else if (loadState == GCodeLoadState.IDLE && part.equals("M82"))
                {
                    //Absolute extrusion
                    gcodeMetaFile.setExtrusionMode(ExtrusionMode.ABSOLUTE);
                } else if (loadState == GCodeLoadState.IDLE && part.equals("M83"))
                {
                    //Relative extrusion
                    gcodeMetaFile.setExtrusionMode(ExtrusionMode.RELATIVE);
                } else if (loadState == GCodeLoadState.IDLE && part.equals("G90"))
                {
                    //Absolute move
                    gcodeMetaFile.setMovementMode(MovementMode.ABSOLUTE);
                } else if (loadState == GCodeLoadState.IDLE && part.equals("G91"))
                {
                    //Relative move
                    gcodeMetaFile.setMovementMode(MovementMode.RELATIVE);
                } else if (loadState == GCodeLoadState.IDLE)
                {
                    parseFailure = true;
//                    steno.debug("Discarding line " + aLine);
                    break;
                } else
                {
//                    steno.trace("Discarding this token " + part);
                }
            }

            if (parseFailure == false && validPositionData == true)
            {
                GCodeEventType eventType = null;

//                steno.trace("Got valid position data");
                MovementType latestMovementType = null;

                if (supportDetected)
                {
                    latestMovementType = MovementType.EXTRUDE_SUPPORT;
                } else if (retractDetected)
                {
                    latestMovementType = MovementType.RETRACT;
                } else if (unretractDetected)
                {
                    latestMovementType = MovementType.UNRETRACT;
                } else if (gotEValue)
                {
                    latestMovementType = MovementType.EXTRUDE;
                } else
                {
                    latestMovementType = MovementType.TRAVEL;
                }

                if (gotXPos == false)
                {
                    xPos = (float) currentPositionWorldCoords.getX();
                }
                if (gotYPos == false)
                {
                    yPos = (float) currentPositionWorldCoords.getY();
                }
                if (gotZPos == false)
                {
                    zPos = (float) currentPositionWorldCoords.getZ();
                }

                if (zPos != zLevel)
                {
                    if (layerNode.getChildren().size() > 0)
                    {
                        gcodeParts.getChildren().add(layerNode);
                        layerNode.setId(String.valueOf(layerNumber));
                        referencedLayers.put(layerNumber, layerNode);
                        layerNode = new Group();
                        layerNumber++;
                    }
//                steno.info("Layer change from " + zLevel + " to " + zPos);
//                layerNumber++;
                    zLevel = zPos;
                }

//            steno.info("Got x" + xPos + " y " + (-zPos) + " z " + yPos);
                currentPositionWorldCoords = new Point3D(xPos, yPos, zPos);

                Point3D transformedCurrent = new Point3D(xPos, -zPos, yPos);
                Point3D reorient = transformedCurrent.subtract(lastPoint);
                PolarCoordinate polarCoord = MathUtils.cartesianToSphericalLocalSpaceUnadjusted(reorient);

                Box newCyl = new Box(.2, reorient.magnitude(), .2);
//            newCyl.setDrawMode(DrawMode.LINE);

//                        Cylinder newCyl = new Cylinder(.25, reorient.magnitude(), 4);
                newCyl.setUserData(layerNumber);
                newCyl.setId(String.valueOf(lineNumber));
                newCyl.setRotationAxis(MathUtils.zAxis);
                newCyl.setRotate(90);
                newCyl.setTranslateX(reorient.magnitude() / 2);

                Xform cylZRotXform = new Xform();
                cylZRotXform.getChildren().add(newCyl);
                cylZRotXform.setRz(360 - (MathUtils.RAD_TO_DEG * polarCoord.getTheta()));

                Xform cylYRotXForm = new Xform();
                cylYRotXForm.getChildren().add(cylZRotXform);
                cylYRotXForm.setRy(360 - (MathUtils.RAD_TO_DEG * polarCoord.getPhi()));

                cylYRotXForm.setTx(lastPoint.getX());
                cylYRotXForm.setTz(lastPoint.getZ());
                cylYRotXForm.setTy(lastPoint.getY());

                newCyl.setMaterial(ApplicationMaterials.getGCodeMaterial(latestMovementType, false));
                newCyl.setCache(true);
                newCyl.setCacheHint(CacheHint.SPEED);
                newCyl.setCullFace(CullFace.BACK);

                layerNode.getChildren().add(cylYRotXForm);

//                referencedElements.put(lineNumber, new GCodeElement(newCyl, latestMovementType));
                lastPoint = transformedCurrent;

            }
        }
    }

        boolean testNextTokenBeginsWith
        (Scanner scanner, String beginsWith
        
            )
    {
        boolean tokenPresent = false;

            if (scanner.hasNext(beginsWith + ".*"))
            {
                tokenPresent = true;
                //log("Next token is confirmed as : " + tokenToFind);
            }
            return tokenPresent;
        }

        boolean testNextStringToken
        (String part, String tokenToFind
        
            )
    {
        boolean tokenPresent = false;

            if (part.equals(tokenToFind))
            {
                tokenPresent = true;
            }

            return tokenPresent;
        }

        boolean testNextStringToken
        (Scanner scanner, String tokenToFind
        
            )
    {
        boolean tokenPresent = false;

            if (scanner.hasNext(tokenToFind))
            {
                tokenPresent = true;
                //log("Next token is confirmed as : " + tokenToFind);
            }
            return tokenPresent;
        }
    }
