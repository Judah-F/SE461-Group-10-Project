package org.psnbtech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SidePanelTest {

    private SnakeGame game;
    private SidePanel side;

    @Before
    public void setUp() throws Exception {
        Constructor<SnakeGame> ctor = SnakeGame.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        game = ctor.newInstance();
        game.setVisible(false);

        setField("random", new Random());
        setField("snake", new LinkedList<>());
        setField("directions", new LinkedList<>());
        setField("logicTimer", new Clock(9.0f));

        invokeReset();

        side = new SidePanel(game);
        side.setSize(new Dimension(300, 500));
    }

    @After
    public void tearDown() {
        if (game != null) {
            game.dispose();
        }
    }

    // ============== Constructor PC ==============

    @Test
    public void newSidePanelHasCorrectPreferredSize() {
        Dimension expected = new Dimension(300, BoardPanel.ROW_COUNT * BoardPanel.TILE_SIZE);
        assertEquals(expected, side.getPreferredSize());
    }

    @Test
    public void newSidePanelHasBlackBackground() {
        assertEquals(Color.BLACK, side.getBackground());
    }

    @Test
    public void paintComponentRunsWithoutThrowingInFreshGame() {
        BufferedImage img = new BufferedImage(300, 500, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        try {
            side.paintComponent(g);
        } finally {
            g.dispose();
        }
        assertNotNull(img);
    }

    @Test
    public void paintComponentRunsWithoutThrowingWithGameProgressed() throws Exception {
        setIntField("score", 537);
        setIntField("fruitsEaten", 4);
        setIntField("nextFruitScore", 42);

        BufferedImage img = new BufferedImage(300, 500, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        try {
            side.paintComponent(g);
        } finally {
            g.dispose();
        }
    }

    // ============== Reflection helpers ==============

    private void setField(String name, Object value) throws Exception {
        Field f = SnakeGame.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(game, value);
    }

    private void setIntField(String name, int value) throws Exception {
        Field f = SnakeGame.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(game, value);
    }

    private void invokeReset() throws Exception {
        java.lang.reflect.Method m = SnakeGame.class.getDeclaredMethod("resetGame");
        m.setAccessible(true);
        m.invoke(game);
    }
}
