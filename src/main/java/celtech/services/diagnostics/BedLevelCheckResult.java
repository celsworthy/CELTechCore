/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.services.diagnostics;

import java.util.ArrayList;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
class BedLevelCheckResult
{
    private final ArrayList<Double> levelOffsets = new ArrayList<>();
    private final ArrayList<Vector2D> measuringPoints = new ArrayList<>();

    public ArrayList<Double> getLevelOffsets()
    {
        return levelOffsets;
    }

    public ArrayList<Vector2D> getMeasuringPoints()
    {
        return measuringPoints;
    }

    public void addOffset(Vector2D position, double value)
    {
        levelOffsets.add(value);
        measuringPoints.add(position);
    }
}
