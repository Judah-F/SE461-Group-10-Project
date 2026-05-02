package org.psnbtech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BoardPanelTest {

    private BoardPanel board;

    @Before
    public void setUp() {
        board = new BoardPanel(null);
    }

    // ============== Storage methods — PC + boundary ==============

    @Test
    public void newBoardHasAllTilesNull() {
        for (int x = 0; x < BoardPanel.COL_COUNT; x++) {
            for (int y = 0; y < BoardPanel.ROW_COUNT; y++) {
                assertNull(board.getTile(x, y));
            }
        }
    }

    @Test
    public void setTileWithCoordinatesIsRoundTrippable() {
        board.setTile(5, 10, TileType.Fruit);
        assertEquals(TileType.Fruit, board.getTile(5, 10));
    }

    @Test
    public void setTileWithPointIsRoundTrippable() {
        board.setTile(new Point(7, 12), TileType.SnakeHead);
        assertEquals(TileType.SnakeHead, board.getTile(7, 12));
    }

    @Test
    public void setTileDoesNotAffectOtherCells() {
        board.setTile(5, 10, TileType.Fruit);
        board.setTile(8, 3, TileType.SnakeBody);
        assertEquals(TileType.Fruit, board.getTile(5, 10));
        assertEquals(TileType.SnakeBody, board.getTile(8, 3));
        assertNull(board.getTile(0, 0));
        assertNotEquals(board.getTile(5, 10), board.getTile(8, 3));
    }

    @Test
    public void setTileOverwritesPreviousValue() {
        board.setTile(2, 2, TileType.Fruit);
        board.setTile(2, 2, TileType.SnakeHead);
        assertEquals(TileType.SnakeHead, board.getTile(2, 2));
    }

    @Test
    public void clearBoardResetsEveryCellToNull() {
        board.setTile(0, 0, TileType.SnakeHead);
        board.setTile(5, 10, TileType.Fruit);
        board.setTile(BoardPanel.COL_COUNT - 1, BoardPanel.ROW_COUNT - 1, TileType.SnakeBody);

        board.clearBoard();

        for (int x = 0; x < BoardPanel.COL_COUNT; x++) {
            for (int y = 0; y < BoardPanel.ROW_COUNT; y++) {
                assertNull(board.getTile(x, y));
            }
        }
    }

    @Test
    public void setTileAtMaxValidCoordinatesRoundTrips() {
        int maxX = BoardPanel.COL_COUNT - 1;
        int maxY = BoardPanel.ROW_COUNT - 1;
        board.setTile(maxX, maxY, TileType.Fruit);
        assertEquals(TileType.Fruit, board.getTile(maxX, maxY));
    }

    @Test
    public void setTileAtOriginRoundTrips() {
        board.setTile(0, 0, TileType.SnakeBody);
        assertEquals(TileType.SnakeBody, board.getTile(0, 0));
    }

    // ============== paintComponent / drawTile smoke tests ==============
    // Render against an off-screen BufferedImage.

    private static final int PANEL_W = BoardPanel.COL_COUNT * BoardPanel.TILE_SIZE;
    private static final int PANEL_H = BoardPanel.ROW_COUNT * BoardPanel.TILE_SIZE;

    private SnakeGame smokeGame;
    private BoardPanel smokeBoard;

    @After
    public void tearDownSmokeGame() {
        if (smokeGame != null) {
            smokeGame.dispose();
            smokeGame = null;
        }
    }

    private void initSmokeGame() throws Exception {
        Constructor<SnakeGame> ctor = SnakeGame.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        smokeGame = ctor.newInstance();
        smokeGame.setVisible(false);

        setSnakeGameField("random", new Random());
        setSnakeGameField("snake", new LinkedList<>());
        setSnakeGameField("directions", new LinkedList<>());
        setSnakeGameField("logicTimer", new Clock(9.0f));

        Field boardField = SnakeGame.class.getDeclaredField("board");
        boardField.setAccessible(true);
        smokeBoard = (BoardPanel) boardField.get(smokeGame);
        smokeBoard.setSize(new Dimension(PANEL_W, PANEL_H));

        java.lang.reflect.Method m = SnakeGame.class.getDeclaredMethod("resetGame");
        m.setAccessible(true);
        m.invoke(smokeGame);
    }

    private void paint() {
        BufferedImage img = new BufferedImage(PANEL_W, PANEL_H, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        try {
            smokeBoard.paintComponent(g);
        } finally {
            g.dispose();
        }
    }

    @Test
    public void paintComponent_freshGame_renders() throws Exception {
        initSmokeGame();
        paint();
    }

    @Test
    public void paintComponent_newGameOverlay_renders() throws Exception {
        initSmokeGame();
        setSnakeGameBool("isNewGame", true);
        paint();
    }

    @Test
    public void paintComponent_gameOverOverlay_renders() throws Exception {
        initSmokeGame();
        setSnakeGameBool("isGameOver", true);
        paint();
    }

    @Test
    public void paintComponent_pausedOverlay_renders() throws Exception {
        initSmokeGame();
        setSnakeGameBool("isPaused", true);
        paint();
    }

    @Test
    public void paintComponent_allTileTypesRendered() throws Exception {
        initSmokeGame();
        smokeBoard.clearBoard();
        smokeBoard.setTile(2, 2, TileType.Fruit);
        smokeBoard.setTile(5, 5, TileType.SnakeBody);
        smokeBoard.setTile(10, 10, TileType.SnakeHead);
        paint();
    }

    @Test
    public void paintComponent_headDirectionBranches_allRender() throws Exception {
        initSmokeGame();
        for (Direction d : Direction.values()) {
            @SuppressWarnings("unchecked")
            LinkedList<Direction> dirs = (LinkedList<Direction>) getSnakeGameField("directions");
            dirs.clear();
            dirs.add(d);
            smokeBoard.setTile(12, 12, TileType.SnakeHead);
            paint();
        }
    }

    // ============== Reflection helpers ==============

    private void setSnakeGameField(String name, Object value) throws Exception {
        Field f = SnakeGame.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(smokeGame, value);
    }

    private void setSnakeGameBool(String name, boolean value) throws Exception {
        Field f = SnakeGame.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setBoolean(smokeGame, value);
    }

    private Object getSnakeGameField(String name) throws Exception {
        Field f = SnakeGame.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(smokeGame);
    }
}
