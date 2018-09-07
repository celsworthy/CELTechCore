package celtech.coreUI.gcodepreview.entities;

import java.util.List;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class SequenceEntity {
    
    private List<Vector3D> sequence;
    private double radius;
    private Vector3D colour = new Vector3D(1, 1, 1);

    public SequenceEntity(List<Vector3D> sequence, double radius, Vector3D colour) {
        this.sequence = sequence;
        this.radius = radius;
        this.colour = colour;
    }
    
    public List<Vector3D> getSequence() {
        return sequence;
    }

    public void setSequence(List<Vector3D> sequence) {
        this.sequence = sequence;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public Vector3D getColour() {
        return colour;
    }

    public void setColour(Vector3D colour) {
        this.colour = colour;
    }
}
