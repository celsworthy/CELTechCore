/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.modelcontrol.ModelContainer;
import static celtech.utils.threed.MeshSeparator.addPointToMesh;
import static celtech.utils.threed.MeshSeparator.setTextureAndSmoothing;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


/**
 *
 * @author tony
 */
public class MeshDebug
{

    static ModelContainer node;

    static void showFaceCentres(List<Integer> cutFaces, TriangleMesh mesh)
    {
        for (Integer faceIndex : cutFaces)
        {
            int v0 = mesh.getFaces().get(faceIndex * 6);
            int v1 = mesh.getFaces().get(faceIndex * 6 + 2);
            int v2 = mesh.getFaces().get(faceIndex * 6 + 4);
            double x0 = mesh.getPoints().get(v0 * 3);
            double y0 = mesh.getPoints().get(v0 * 3 + 1);
            double z0 = mesh.getPoints().get(v0 * 3 + 2);
            double x1 = mesh.getPoints().get(v1 * 3);
            double y1 = mesh.getPoints().get(v1 * 3 + 1);
            double z1 = mesh.getPoints().get(v1 * 3 + 2);
            double x2 = mesh.getPoints().get(v2 * 3);
            double y2 = mesh.getPoints().get(v2 * 3 + 1);
            double z2 = mesh.getPoints().get(v2 * 3 + 2);
            double xMin = Math.min(x0, Math.min(x1, x2));
            double xMax = Math.max(x0, Math.max(x1, x2));
            double x = (xMin + xMax) / 2;
            double yMin = Math.min(y0, Math.min(y1, y2));
            double yMax = Math.max(y0, Math.max(y1, y2));
            double y = (yMin + yMax) / 2;
            double zMin = Math.min(z0, Math.min(z1, z2));
            double zMax = Math.max(z0, Math.max(z1, z2));
            double z = (zMin + zMax) / 2;
            Sphere sphere = new Sphere(0.5);
            sphere.translateXProperty().set((x0 + x1 + x2) / 3.0);
            sphere.translateYProperty().set((y0 + y1 + y2) / 3.0);
            sphere.translateZProperty().set((z0 + z1 + z2) / 3.0);
            sphere.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
            Text text = new Text(Integer.toString(faceIndex));
            text.translateXProperty().set((x0 + x1 + x2) / 3.0);
            text.translateYProperty().set((y0 + y1 + y2) / 3.0);
            text.translateZProperty().set((z0 + z1 + z2) / 3.0);
            Font font = new Font("Source Sans Pro Regular", 8);
            text.setFont(font);
            if (node != null)
            {
                node.addChildNode(sphere);
                node.addChildNode(text);
            }
        }
    }

    static void showFace(TriangleMesh mesh, int faceIndex)
    {
        TriangleMesh triangle = new TriangleMesh();
        int[] vertices = new int[6];
        vertices[0] = mesh.getFaces().get(faceIndex * 6);
        vertices[2] = mesh.getFaces().get(faceIndex * 6 + 2);
        vertices[4] = mesh.getFaces().get(faceIndex * 6 + 4);
        triangle.getFaces().addAll(vertices);
        addPointToMesh(mesh, vertices[0], triangle);
        addPointToMesh(mesh, vertices[2], triangle);
        addPointToMesh(mesh, vertices[4], triangle);
        setTextureAndSmoothing(triangle, triangle.getFaces().size() / 6);
        MeshView meshView = new MeshView(triangle);
        meshView.setMaterial(ApplicationMaterials.pickedGCodeMaterial);
        if (node != null)
        {
            node.addChildNode(meshView);
        }
    }

    static void showSphere(double x, double y, double z)
    {
        Sphere sphere = new Sphere(0.5);
        sphere.translateXProperty().set(x);
        sphere.translateYProperty().set(y);
        sphere.translateZProperty().set(z);
        sphere.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
        if (node != null)
        {
            node.addChildNode(sphere);
        }
    }

    public static void setDebuggingNode(ModelContainer node)
    {
        MeshDebug.node = node;
    }

    static void showNewVertices(List<Integer> newVertices, TriangleMesh mesh)
    {
        if (node != null)
        {
            for (Integer newVertex : newVertices)
            {
                Sphere sphere = new Sphere(0.5);
                sphere.translateXProperty().set(mesh.getPoints().get(newVertex * 3));
                sphere.translateYProperty().set(mesh.getPoints().get(newVertex * 3 + 1));
                sphere.translateZProperty().set(mesh.getPoints().get(newVertex * 3 + 2));
                sphere.setMaterial(ApplicationMaterials.getOffBedModelMaterial());
                node.addChildNode(sphere);
            }
        }
    }

    static void showIncomingMesh(TriangleMesh mesh)
    {
        System.out.println(mesh.getVertexFormat());
        System.out.println(mesh.getVertexFormat().getVertexIndexSize());
        System.out.println(mesh.getVertexFormat().getPointIndexOffset());
        for (int i = 0; i < mesh.getPoints().size() / 3; i++)
        {
            System.out.println("point " + i + " is " + mesh.getPoints().get(i * 3) + " "
                + mesh.getPoints().get(i * 3 + 1) + " " + mesh.getPoints().get(i * 3 + 2));
            showSphere(mesh.getPoints().get(i * 3), mesh.getPoints().get(i * 3 + 1),
                       mesh.getPoints().get(i * 3 + 2));
        }
        for (int i = 0; i < mesh.getFaces().size() / 6; i++)
        {
            System.out.println("face " + i + " is " + mesh.getFaces().get(i * 6) + " "
                + mesh.getFaces().get(i * 6 + 2) + " " + mesh.getFaces().get(i * 6 + 4));
        }
    }

    static boolean close = false;

    static void close()
    {
        close = true;
    }

    static void visualiseEdgeLoops(Set<List<ManifoldEdge>> loops)
    {

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                JFrame f = new JFrame("Edge Loops");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                MyPanel panel = new MyPanel();
                f.add(panel);
                JButton button = new JButton("Quit");
                panel.add(button);
                panel.showLoops(loops);
                button.addActionListener(new ActionListener()
                {

                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        close();
                    }

                });
                f.pack();
                f.setSize(300, 300);
                f.setVisible(true);
            }

        });
        while (true)
        {
            if (close)
            {
                break;
            }
            try
            {
                Thread.sleep(200);
            } catch (InterruptedException ex)
            {
                Logger.getLogger(MeshDebug.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}


class MyPanel extends JPanel
{
    
    Set<List<ManifoldEdge>> loops;

    public MyPanel()
    {
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(250, 200);
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        // Draw Text
        g.drawString("This is my custom Panel!", 10, 20);
        int scale = 20;
        int xOffset = 100;
        int yOffset = 100;
        
        if (loops != null) {
            for (List<ManifoldEdge> loop : loops)
            {
                for (ManifoldEdge edge : loop)
                {
                    g.drawLine(xOffset + (int) edge.vertex0.x * scale,
                               yOffset + (int) edge.vertex0.y * scale,
                               xOffset + (int)edge.vertex1.x * scale, 
                               yOffset + (int) edge.vertex1.y * scale);
                }
            }
        }
        
    }

    void showLoops(Set<List<ManifoldEdge>> loops)
    {
        this.loops = loops;
    }
}
