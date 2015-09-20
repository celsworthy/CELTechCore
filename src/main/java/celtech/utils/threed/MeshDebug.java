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
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.triangulation.TriangulationPoint;

/**
 *
 * @author tony
 */
public class MeshDebug {

    static ModelContainer node;

    static void showFaceCentres(List<Integer> cutFaces, TriangleMesh mesh) {
        for (Integer faceIndex : cutFaces) {
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
            if (node != null) {
                node.addChildNode(sphere);
                node.addChildNode(text);
            }
        }
    }

    static void showFace(TriangleMesh mesh, int faceIndex) {
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
        if (node != null) {
            node.addChildNode(meshView);
        }
    }

    static void showSphere(double x, double y, double z) {
        Sphere sphere = new Sphere(0.5);
        sphere.translateXProperty().set(x);
        sphere.translateYProperty().set(y);
        sphere.translateZProperty().set(z);
        sphere.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
        if (node != null) {
            node.addChildNode(sphere);
        }
    }

    public static void setDebuggingNode(ModelContainer node) {
        MeshDebug.node = node;
    }

    static void showNewVertices(List<Integer> newVertices, TriangleMesh mesh) {
        if (node != null) {
            for (Integer newVertex : newVertices) {
                Sphere sphere = new Sphere(0.5);
                sphere.translateXProperty().set(mesh.getPoints().get(newVertex * 3));
                sphere.translateYProperty().set(mesh.getPoints().get(newVertex * 3 + 1));
                sphere.translateZProperty().set(mesh.getPoints().get(newVertex * 3 + 2));
                sphere.setMaterial(ApplicationMaterials.getOffBedModelMaterial());
                node.addChildNode(sphere);
            }
        }
    }

    static void showIncomingMesh(TriangleMesh mesh) {
        System.out.println(mesh.getVertexFormat());
        System.out.println(mesh.getVertexFormat().getVertexIndexSize());
        System.out.println(mesh.getVertexFormat().getPointIndexOffset());
        for (int i = 0; i < mesh.getPoints().size() / 3; i++) {
            System.out.println("point " + i + " is " + mesh.getPoints().get(i * 3) + " "
                    + mesh.getPoints().get(i * 3 + 1) + " " + mesh.getPoints().get(i * 3 + 2));
            showSphere(mesh.getPoints().get(i * 3), mesh.getPoints().get(i * 3 + 1),
                    mesh.getPoints().get(i * 3 + 2));
        }
        for (int i = 0; i < mesh.getFaces().size() / 6; i++) {
            System.out.println("face " + i + " is " + mesh.getFaces().get(i * 6) + " "
                    + mesh.getFaces().get(i * 6 + 2) + " " + mesh.getFaces().get(i * 6 + 4));
        }
    }

    static boolean close = false;

    static void close() {
        close = true;
    }

    static void visualiseEdgeLoops(Set<ManifoldEdge> nonManifoldEdges, Set<List<ManifoldEdge>> loops) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f = new JFrame("Edge Loops");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                MyPanel panel = new MyPanel();
                f.add(panel);
                JButton button = new JButton("Quit");
                panel.add(button);
                panel.showLoops(loops);
                panel.showNonManifoldEdges(nonManifoldEdges);
                button.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        close();
                    }

                });
                f.pack();
                f.setSize(1200, 1200);
                f.setVisible(true);
            }

        });
        while (true) {
            if (close) {
                break;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(MeshDebug.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    static void visualiseDLPolygon(Polygon outerPolygon) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f = new JFrame("Edge Loops");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                MyPanel panel = new MyPanel();
                f.add(panel);
                JButton button = new JButton("Quit");
                panel.add(button);
                panel.showPolygon(outerPolygon);
                button.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        close();
                    }

                });
                f.pack();
                f.setSize(1200, 1200);
                f.setVisible(true);
            }

        });
        while (true) {
            if (close) {
                break;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(MeshDebug.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}

class MyPanel extends JPanel {

    Set<List<ManifoldEdge>> loops;
    private Set<ManifoldEdge> nonManifoldEdges;
    private Polygon outerPolygon;

    public MyPanel() {
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    public Dimension getPreferredSize() {
        return new Dimension(250, 200);
    }

    public void paintComponent(Graphics g1) {
        final Graphics2D g = (Graphics2D) g1.create();
        super.paintComponent(g);

        double scale = 30;
        int xOffset = 100;
        int yOffset = 100;

        g.drawOval(xOffset - 15, yOffset - 15, 30, 30);

        if (nonManifoldEdges != null) {

            double minX = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            for (ManifoldEdge edge : nonManifoldEdges) {
                if (edge.vertex0.x < minX) {
                    minX = edge.vertex0.x;
                }
                if (edge.vertex0.x > maxX) {
                    maxX = edge.vertex0.x;
                }
                if (edge.vertex1.x < minX) {
                    minX = edge.vertex1.x;
                }
                if (edge.vertex1.x > maxX) {
                    maxX = edge.vertex1.x;
                }
                if (edge.vertex0.z < minZ) {
                    minZ = edge.vertex0.z;
                }
                if (edge.vertex0.z > maxZ) {
                    maxZ = edge.vertex0.z;
                }
                if (edge.vertex1.z < minZ) {
                    minZ = edge.vertex1.z;
                }
                if (edge.vertex1.z > maxZ) {
                    maxZ = edge.vertex1.z;
                }
            }

            double width = getWidth();
            scale = width / (maxX - minX);
            scale /= 1.5;
            System.out.println("scale is " + scale);

            xOffset -= minX * scale;
            yOffset -= minZ * scale;

            g.setColor(Color.green);
            for (ManifoldEdge edge : nonManifoldEdges) {
                g.drawLine(xOffset + (int) (edge.vertex0.x * scale),
                        yOffset + (int) (edge.vertex0.z * scale),
                        xOffset + (int) (edge.vertex1.x * scale),
                        yOffset + (int) (edge.vertex1.z * scale));
                g.drawOval(xOffset - 5 + (int) ((edge.vertex0.x + edge.vertex1.x) / 2d * scale),
                        yOffset - 5 + (int) ((edge.vertex0.z + edge.vertex1.z) / 2d * scale),
                        10, 10);
            }
        }

        if (loops != null) {
            g.setColor(Color.red);
            for (List<ManifoldEdge> loop : loops) {
//                System.out.println("draw loop");
                for (ManifoldEdge edge : loop) {
//                    System.out.println("draw edge " + edge);
//                    System.out.println(edge.vertex0.x + "," + edge.vertex0.z);
                    g.drawLine(xOffset + (int) (edge.vertex0.x * scale),
                            yOffset + (int) (edge.vertex0.z * scale),
                            xOffset + (int) (edge.vertex1.x * scale),
                            yOffset + (int) (edge.vertex1.z * scale));
                }
            }
        }

        if (outerPolygon != null) {
            g.setColor(Color.blue);
            double minX = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            for (TriangulationPoint point : outerPolygon.getPoints()) {
                double x = point.getX();
                double z = point.getY();
                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (z < minZ) {
                    minZ = z;
                }
                if (z > maxZ) {
                    maxZ = z;
                }
            }
            double width = getWidth();
            scale = width / (maxX - minX);
            scale /= 1.5;
//            System.out.println("scale is " + scale);

            xOffset -= minX * scale;
            yOffset -= minZ * scale;
//            System.out.println("offsets " + xOffset + "," + yOffset);
            drawPolygon(outerPolygon, g, xOffset, yOffset, scale);
        }
    }

    private void drawPolygon(Polygon polygon, final Graphics2D g,
            int xOffset, int yOffset, double scale) {
        TriangulationPoint startPoint = outerPolygon.getPoints().get(0);
        double startX = startPoint.getX() * scale;
        double startY = startPoint.getY() * scale;
        double beginX = startX;
        double beginY = startY;
        double endX = 0, endY = 0;
        for (TriangulationPoint point : polygon.getPoints()) {
            endX = point.getX() * scale;
            endY = point.getY() * scale;
            g.drawLine(xOffset + (int) (startX),
                    yOffset + (int) (startY),
                    xOffset + (int) (endX),
                    yOffset + (int) (endY));
//            System.out.println("draw " + startX + "," + startY + " " + endX + "," + endY);
            startX = endX;
            startY = endY;
        }
        g.drawLine(xOffset + (int) (endX),
                yOffset + (int) (endY),
                xOffset + (int) (beginX),
                yOffset + (int) (beginY));
        if (polygon.getHoles() != null) {
            for (Polygon hole : polygon.getHoles()) {
                drawPolygon(hole, g, xOffset, yOffset, scale);
            }
        }
    }

    void showLoops(Set<List<ManifoldEdge>> loops) {
        this.loops = loops;
    }

    void showNonManifoldEdges(Set<ManifoldEdge> nonManifoldEdges) {
        this.nonManifoldEdges = nonManifoldEdges;
    }

    void showPolygon(Polygon outerPolygon) {
        this.outerPolygon = outerPolygon;
    }
}
