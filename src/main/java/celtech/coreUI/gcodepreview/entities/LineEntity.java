package celtech.coreUI.gcodepreview.entities;

import celtech.coreUI.gcodepreview.VectorUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author George Salter
 */
public class LineEntity {
    
    private Vector3D start;
    private Vector3D end;
    private Vector3D direction;
    
    private Vector3D position;
    
    private double rotationX;
    private double rotationY;
    private double rotationZ;
    
    private double length;
    
    private Vector3D colour = new Vector3D(1, 1, 1);
    
    public LineEntity(Vector3D start, Vector3D end) {
        this.start = start;
        this.end = end;
        this.direction =  new Vector3D(end.getX() - start.getX(), end.getY() - start.getY(), end.getZ() - start.getZ()).normalize();
        position = calculatePosition();
        rotationX = 0;
        rotationY = calculateRotationAroundY();
        rotationZ = calculateRotationAroundZ();
        length = calculateLength();
    }
    
    private Vector3D calculatePosition() {
        return VectorUtils.calculateCenterBetweenVectors(start, end);
    }
    
    private double calculateRotationAroundY() {
        return VectorUtils.calculateRotationAroundYOfVectors(start, end);
    }
    
    private double calculateRotationAroundZ() {
        return VectorUtils.calculateRotationAroundZOfVectors(start, end);
    }
    
    private double calculateLength() {
        return VectorUtils.calculateLengthBetweenVectors(start, end);
    }

    public Vector3D getStart() {
        return start;
    }

    public void setStart(Vector3D start) {
        this.start = start;
    }

    public Vector3D getEnd() {
        return end;
    }

    public void setEnd(Vector3D end) {
        this.end = end;
    }

    public Vector3D getPosition() {
        return position;
    }

    public void setPosition(Vector3D position) {
        this.position = position;
    }

    public double getRotationX() {
        return rotationX;
    }

    public void setRotationX(double rotationX) {
        this.rotationX = rotationX;
    }

    public double getRotationY() {
        return rotationY;
    }

    public void setRotationY(double rotationY) {
        this.rotationY = rotationY;
    }

    public double getRotationZ() {
        return rotationZ;
    }

    public void setRotationZ(double rotationZ) {
        this.rotationZ = rotationZ;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public Vector3D getColour() {
        return colour;
    }

    public void setColour(Vector3D colour) {
        this.colour = colour;
    }
    
    public Vector3D getDirection() {
        return direction;
    }
}
