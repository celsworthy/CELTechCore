package celtech.coreUI.gcodepreview.entities;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Entity {
    
    private Vector3D position;
    
    private double rotationX;
    private double rotationY;
    private double rotationZ;
    
    private double scaleX;
    private double scaleY;
    private double scaleZ;
    
    private Vector3D colour = new Vector3D(1, 1, 1);

    public Entity(Vector3D position, double rotationX, double rotationY, double rotationZ, 
            double scaleX, double scaleY, double scaleZ) {
        this.position = position;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
    }
    
    public void increasePosition(double dx, double dy, double dz) {
        this.position = new Vector3D(position.getX() + dx, position.getY() + dy, position.getZ() + dz);
    }
    
    public void increaseRotation(double dx, double dy, double dz) {
        this.rotationX += dx;
        this.rotationY += dy;
        this.rotationZ += dz;
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

    public double getScaleX() {
        return scaleX;
    }

    public void setScaleX(double scaleX) {
        this.scaleX = scaleX;
    }
    
    public double getScaleY() {
        return scaleY;
    }

    public void setScaleY(double scaleY) {
        this.scaleY = scaleY;
    }
    
    public double getScaleZ() {
        return scaleZ;
    }

    public void setScaleZ(double scaleZ) {
        this.scaleZ = scaleZ;
    }

    public Vector3D getColour() {
        return colour;
    }

    public void setColour(Vector3D colour) {
        this.colour = colour;
    }
}
