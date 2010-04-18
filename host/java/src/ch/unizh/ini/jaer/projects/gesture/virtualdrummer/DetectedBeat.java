/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.unizh.ini.jaer.projects.gesture.virtualdrummer;

import com.sun.opengl.util.GLUT;
import java.awt.geom.Point2D;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;

/**
 * A detected drum beat. Includes countdown counter for rendering and method to render indication of beat.
 *
 * @author tobi
 */
public class DetectedBeat {

    private static GLUT glut = new GLUT();
    long sysTimeDetected = System.currentTimeMillis();
    BluringFilter2DTracker.Cluster cluster;
    final int FRAMES_TO_RENDER = 30;
    int framesLeftToRender = FRAMES_TO_RENDER;
    Point2D.Float location;

    public DetectedBeat(BluringFilter2DTracker.Cluster cluster) {
        this.cluster = cluster;
        location=(Point2D.Float)cluster.getLocation().clone();
    }

    public boolean isDoneRendering() {
        return framesLeftToRender <= 0;
    }

    /** Draws the beat on the cluster location.
     *
     * @param drawable the GL drawable to draw on.
     */
    public void draw(GLAutoDrawable drawable) {
        if (isDoneRendering()) {
            return;
        }
        GL gl = drawable.getGL(); // when we get this we are already set up with scale 1=1 pixel, at LL corner
        gl.glPushMatrix();
        gl.glColor3f(0, 1, 0); // green
        final int pos = 10;
        final String beatString = "BEAT";

        // render string at location of cluster. size of string is sized to match cluster size

        float sw = glut.glutStrokeLength(GLUT.STROKE_ROMAN, beatString); // length in model space
        float cw = cluster.getMaxRadius() ; // cluster size (actually radius, to make string half total width)
        float scale = cw / sw; // scaling to make string come out size of cluster /2

        // set origin to put string centered on cluster
        gl.glTranslatef(location.x - cw/2, location.y, 0);
        gl.glScalef(scale, scale, 1); // scale transform to make string right size

        glut.glutStrokeString(GLUT.STROKE_ROMAN, "Beat"); // stroke the string
        gl.glPopMatrix();
        framesLeftToRender--;  // decrease counter for rendering
    }
}