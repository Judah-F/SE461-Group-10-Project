package org.psnbtech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// Tests for SnakeGame. Reflection used to reach private members
public class SnakeGameTest {

    private static final int DOCUMENTED_MIN_SNAKE_LENGTH = 5;
    private static final int COL_COUNT = BoardPanel.COL_COUNT;
    private static final int ROW_COUNT = BoardPanel.ROW_COUNT;

    private SnakeGame game;

    @Before
    public void setUp() throws Exception {
        Constructor<SnakeGame> ctor = SnakeGame.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        game = ctor.newInstance();
        game.setVisible(false);

        // Fields normally initialized in startGame() — set them manually.
        setField("random", new Random());
        setField("snake", new LinkedList<Point>());
        setField("directions", new LinkedList<Direction>());
        setField("logicTimer", new Clock(9.0f));

        invoke("resetGame");

        // Clear the random fruit so movement tests are deterministic.
        BoardPanel board = (BoardPanel) getField("board");
        board.clearBoard();
        board.setTile(snakeList().peekFirst(), TileType.SnakeHead);
    }

    @After
    public void tearDown() {
        if (game != null) {
            game.dispose();
        }
    }

    // ============== P13 — tail removal, CACC (2-clause AND) ==============
    // p = (old != Fruit) && (snake.size() > MIN_SNAKE_LENGTH)
    // Rows 1, 2, 3 cover CACC. Row 2 demonstrates bug B1.

    @Test
    public void p13_row1_longSnake_emptyTile_removesTail() throws Exception {
        driveTicks(5);
        int sizeBefore = snakeList().size();
        driveTicks(1);
        assertEquals(sizeBefore, snakeList().size());
    }

    // B1: predicate uses > instead of >= -> stable size is 6, not 5.
    @Test
    public void p13_row2_atMinLength_noFruit_shouldNotGrow_butDoes() throws Exception {
        driveTicks(4);
        assertEquals(DOCUMENTED_MIN_SNAKE_LENGTH, snakeList().size());
        driveTicks(1);
        assertEquals(
                "Bug B1: predicate uses > instead of >=.",
                DOCUMENTED_MIN_SNAKE_LENGTH,
                snakeList().size()
        );
    }

    @Test
    public void p13_row3_longSnake_eatsFruit_doesNotRemoveTail() throws Exception {
        driveTicks(5);
        int sizeBefore = snakeList().size();
        int fruitsBefore = game.getFruitsEaten();

        Point head = snakeList().peekFirst();
        boardPanel().setTile(new Point(head.x, head.y - 1), TileType.Fruit);

        driveTicks(1);
        assertEquals(sizeBefore + 1, snakeList().size());
        assertEquals(fruitsBefore + 1, game.getFruitsEaten());
    }

    // ============== P1 — W key handler, CACC (5-clause AND) ==============
    // p = !isPaused && !isGameOver && size<MAX && last!=South && last!=North
    // 6 unique rows: all-true + each clause flipped false in turn.

    @Test
    public void p1_keyW_allClausesTrue_addsNorth() throws Exception {
        seedDirections(Direction.West);
        pressKey(KeyEvent.VK_W);
        assertEquals(2, directionsList().size());
        assertEquals(Direction.North, directionsList().peekLast());
    }

    @Test
    public void p1_keyW_whenPaused_doesNotAddNorth() throws Exception {
        seedDirections(Direction.West);
        setBooleanField("isPaused", true);
        pressKey(KeyEvent.VK_W);
        assertEquals(Direction.West, directionsList().peekLast());
    }

    @Test
    public void p1_keyW_whenGameOver_doesNotAddNorth() throws Exception {
        seedDirections(Direction.West);
        setBooleanField("isGameOver", true);
        pressKey(KeyEvent.VK_W);
        assertEquals(Direction.West, directionsList().peekLast());
    }

    @Test
    public void p1_keyW_whenQueueFull_doesNotAddNorth() throws Exception {
        seedDirections(Direction.East, Direction.South, Direction.West);
        pressKey(KeyEvent.VK_W);
        assertEquals(3, directionsList().size());
        assertEquals(Direction.West, directionsList().peekLast());
    }

    @Test
    public void p1_keyW_whenLastIsSouth_doesNotAddNorth() throws Exception {
        seedDirections(Direction.West, Direction.South);
        pressKey(KeyEvent.VK_W);
        assertEquals(Direction.South, directionsList().peekLast());
    }

    @Test
    public void p1_keyW_whenLastIsNorth_doesNotAddNorth() throws Exception {
        seedDirections(Direction.North);
        pressKey(KeyEvent.VK_W);
        assertEquals(1, directionsList().size());
        assertEquals(Direction.North, directionsList().peekLast());
    }

    // ============== P2/P3/P4 — S/A/D handlers, all-true sanity ==============
    // CACC argument follows P1 by symmetry; full false-case PC tests below.

    @Test
    public void p2_keyS_allClausesTrue_addsSouth() throws Exception {
        seedDirections(Direction.West);
        pressKey(KeyEvent.VK_S);
        assertEquals(Direction.South, directionsList().peekLast());
    }

    @Test
    public void p3_keyA_allClausesTrue_addsWest() throws Exception {
        seedDirections(Direction.North);
        pressKey(KeyEvent.VK_A);
        assertEquals(Direction.West, directionsList().peekLast());
    }

    @Test
    public void p4_keyD_allClausesTrue_addsEast() throws Exception {
        seedDirections(Direction.North);
        pressKey(KeyEvent.VK_D);
        assertEquals(Direction.East, directionsList().peekLast());
    }

    // ============== P2 — S key, PC false cases ==============

    @Test
    public void p2_keyS_whenPaused_doesNotAddSouth() throws Exception {
        seedDirections(Direction.West);
        setBooleanField("isPaused", true);
        pressKey(KeyEvent.VK_S);
        assertEquals(Direction.West, directionsList().peekLast());
    }

    @Test
    public void p2_keyS_whenGameOver_doesNotAddSouth() throws Exception {
        seedDirections(Direction.West);
        setBooleanField("isGameOver", true);
        pressKey(KeyEvent.VK_S);
        assertEquals(Direction.West, directionsList().peekLast());
    }

    @Test
    public void p2_keyS_whenQueueFull_doesNotAddSouth() throws Exception {
        seedDirections(Direction.West, Direction.North, Direction.East);
        pressKey(KeyEvent.VK_S);
        assertEquals(3, directionsList().size());
        assertEquals(Direction.East, directionsList().peekLast());
    }

    @Test
    public void p2_keyS_whenLastIsNorth_doesNotAddSouth() throws Exception {
        seedDirections(Direction.North);
        pressKey(KeyEvent.VK_S);
        assertEquals(Direction.North, directionsList().peekLast());
    }

    @Test
    public void p2_keyS_whenLastIsSouth_doesNotAddSouth() throws Exception {
        seedDirections(Direction.West, Direction.South);
        pressKey(KeyEvent.VK_S);
        assertEquals(2, directionsList().size());
        assertEquals(Direction.South, directionsList().peekLast());
    }

    // ============== P3 — A key, PC false cases ==============

    @Test
    public void p3_keyA_whenPaused_doesNotAddWest() throws Exception {
        seedDirections(Direction.North);
        setBooleanField("isPaused", true);
        pressKey(KeyEvent.VK_A);
        assertEquals(Direction.North, directionsList().peekLast());
    }

    @Test
    public void p3_keyA_whenGameOver_doesNotAddWest() throws Exception {
        seedDirections(Direction.North);
        setBooleanField("isGameOver", true);
        pressKey(KeyEvent.VK_A);
        assertEquals(Direction.North, directionsList().peekLast());
    }

    @Test
    public void p3_keyA_whenQueueFull_doesNotAddWest() throws Exception {
        seedDirections(Direction.North, Direction.East, Direction.South);
        pressKey(KeyEvent.VK_A);
        assertEquals(3, directionsList().size());
        assertEquals(Direction.South, directionsList().peekLast());
    }

    @Test
    public void p3_keyA_whenLastIsEast_doesNotAddWest() throws Exception {
        seedDirections(Direction.North, Direction.East);
        pressKey(KeyEvent.VK_A);
        assertEquals(Direction.East, directionsList().peekLast());
    }

    @Test
    public void p3_keyA_whenLastIsWest_doesNotAddWest() throws Exception {
        seedDirections(Direction.North, Direction.West);
        pressKey(KeyEvent.VK_A);
        assertEquals(2, directionsList().size());
        assertEquals(Direction.West, directionsList().peekLast());
    }

    // ============== P4 — D key, PC false cases ==============

    @Test
    public void p4_keyD_whenPaused_doesNotAddEast() throws Exception {
        seedDirections(Direction.North);
        setBooleanField("isPaused", true);
        pressKey(KeyEvent.VK_D);
        assertEquals(Direction.North, directionsList().peekLast());
    }

    @Test
    public void p4_keyD_whenGameOver_doesNotAddEast() throws Exception {
        seedDirections(Direction.North);
        setBooleanField("isGameOver", true);
        pressKey(KeyEvent.VK_D);
        assertEquals(Direction.North, directionsList().peekLast());
    }

    @Test
    public void p4_keyD_whenQueueFull_doesNotAddEast() throws Exception {
        seedDirections(Direction.North, Direction.West, Direction.South);
        pressKey(KeyEvent.VK_D);
        assertEquals(3, directionsList().size());
        assertEquals(Direction.South, directionsList().peekLast());
    }

    @Test
    public void p4_keyD_whenLastIsWest_doesNotAddEast() throws Exception {
        seedDirections(Direction.North, Direction.West);
        pressKey(KeyEvent.VK_D);
        assertEquals(Direction.West, directionsList().peekLast());
    }

    @Test
    public void p4_keyD_whenLastIsEast_doesNotAddEast() throws Exception {
        seedDirections(Direction.North, Direction.East);
        pressKey(KeyEvent.VK_D);
        assertEquals(2, directionsList().size());
        assertEquals(Direction.East, directionsList().peekLast());
    }

    // ============== P5 (P key) and P6 (Enter key) — PC ==============

    @Test
    public void p5_keyP_togglesPaused() throws Exception {
        assertFalse(game.isPaused());
        pressKey(KeyEvent.VK_P);
        assertTrue(game.isPaused());
        pressKey(KeyEvent.VK_P);
        assertFalse(game.isPaused());
    }

    @Test
    public void p5_keyP_whenGameOver_doesNotToggle() throws Exception {
        setBooleanField("isGameOver", true);
        pressKey(KeyEvent.VK_P);
        assertFalse(game.isPaused());
    }

    @Test
    public void p6_keyEnter_whenNewGame_resetsGame() throws Exception {
        setBooleanField("isNewGame", true);
        setIntField("score", 999);
        pressKey(KeyEvent.VK_ENTER);
        assertEquals(0, game.getScore());
        assertFalse(game.isNewGame());
    }

    @Test
    public void p6_keyEnter_whenGameOver_resetsGame() throws Exception {
        setBooleanField("isGameOver", true);
        setIntField("score", 999);
        pressKey(KeyEvent.VK_ENTER);
        assertEquals(0, game.getScore());
        assertFalse(game.isGameOver());
    }

    @Test
    public void p6_keyEnter_midGame_doesNotReset() throws Exception {
        setIntField("score", 42);
        pressKey(KeyEvent.VK_ENTER);
        assertEquals(42, game.getScore());
    }

    // ============== P12 — wall collision, CACC (4-clause OR) ==============
    // p = head.x<0 || head.x>=COL_COUNT || head.y<0 || head.y>=ROW_COUNT
    // 5 unique rows: all-false + each clause true alone.

    @Test
    public void p12_row1_allFalse_noWallCollision() throws Exception {
        positionSnakeAt(COL_COUNT / 2, ROW_COUNT / 2, Direction.North);
        invoke("updateGame");
        assertFalse(game.isGameOver());
    }

    @Test
    public void p12_row2_xLessThanZero_leftWallHit() throws Exception {
        positionSnakeAt(0, ROW_COUNT / 2, Direction.West);
        invoke("updateGame");
        assertTrue(game.isGameOver());
    }

    @Test
    public void p12_row3_xAtRightWall_rightWallHit() throws Exception {
        positionSnakeAt(COL_COUNT - 1, ROW_COUNT / 2, Direction.East);
        invoke("updateGame");
        assertTrue(game.isGameOver());
    }

    @Test
    public void p12_row4_yLessThanZero_topWallHit() throws Exception {
        positionSnakeAt(COL_COUNT / 2, 0, Direction.North);
        invoke("updateGame");
        assertTrue(game.isGameOver());
    }

    @Test
    public void p12_row5_yAtBottomWall_bottomWallHit() throws Exception {
        positionSnakeAt(COL_COUNT / 2, ROW_COUNT - 1, Direction.South);
        invoke("updateGame");
        assertTrue(game.isGameOver());
    }

    // ============== Public getter sanity (PC) ==============

    @Test public void getter_isGameOver_isFalseAfterReset()  { assertFalse(game.isGameOver()); }
    @Test public void getter_isPaused_isFalseAfterReset()    { assertFalse(game.isPaused()); }
    @Test public void getter_isNewGame_isFalseAfterReset()   { assertFalse(game.isNewGame()); }
    @Test public void getter_getScore_isZeroAfterReset()     { assertEquals(0, game.getScore()); }
    @Test public void getter_getFruitsEaten_isZeroAfterReset() { assertEquals(0, game.getFruitsEaten()); }
    @Test public void getter_getDirection_isNorthAfterReset() { assertEquals(Direction.North, game.getDirection()); }

    // ============== B7 — fruit-score floors at 10 (ISP) ==============

    @Test
    public void b7_fruitScoreFloorsAtTenAndGameDoesNotEnd() throws Exception {
        setIntField("nextFruitScore", 11);

        safeTick();
        assertEquals(10, game.getNextFruitScore());

        for (int i = 0; i < 10; i++) {
            safeTick();
        }
        assertEquals("B7: floors at 10", 10, game.getNextFruitScore());
        assertFalse("B7: no game-over at floor", game.isGameOver());
    }

    // ============== Helpers ==============

    private void driveTicks(int n) throws Exception {
        for (int i = 0; i < n; i++) {
            invoke("updateGame");
        }
    }

    // Recenters the snake before ticking so score-decay tests don't crash into a wall.
    private void safeTick() throws Exception {
        LinkedList<Point> snake = snakeList();
        snake.clear();
        snake.add(new Point(COL_COUNT / 2, ROW_COUNT / 2));
        boardPanel().clearBoard();
        boardPanel().setTile(snake.peekFirst(), TileType.SnakeHead);
        invoke("updateGame");
    }

    private void positionSnakeAt(int x, int y, Direction facing) throws Exception {
        LinkedList<Point> snake = snakeList();
        snake.clear();
        snake.add(new Point(x, y));
        seedDirections(facing);
        boardPanel().clearBoard();
        boardPanel().setTile(snake.peekFirst(), TileType.SnakeHead);
    }

    private void seedDirections(Direction... newDirections) throws Exception {
        LinkedList<Direction> dirs = directionsList();
        dirs.clear();
        for (Direction d : newDirections) {
            dirs.addLast(d);
        }
    }

    // Dispatches a synthetic KEY_PRESSED to every registered KeyListener.
    private void pressKey(int keyCode) {
        KeyEvent event = new KeyEvent(game, KeyEvent.KEY_PRESSED, 0L, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
        for (KeyListener listener : game.getKeyListeners()) {
            listener.keyPressed(event);
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedList<Point> snakeList() throws Exception {
        return (LinkedList<Point>) getField("snake");
    }

    @SuppressWarnings("unchecked")
    private LinkedList<Direction> directionsList() throws Exception {
        return (LinkedList<Direction>) getField("directions");
    }

    private BoardPanel boardPanel() throws Exception {
        return (BoardPanel) getField("board");
    }

    // ============== Reflection helpers ==============

    private Object getField(String name) throws Exception {
        Field f = SnakeGame.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(game);
    }

    private void setField(String name, Object value) throws Exception {
        Field f = SnakeGame.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(game, value);
    }

    private void setBooleanField(String name, boolean value) throws Exception {
        Field f = SnakeGame.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setBoolean(game, value);
    }

    private void setIntField(String name, int value) throws Exception {
        Field f = SnakeGame.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(game, value);
    }

    private Object invoke(String method) throws Exception {
        Method m = SnakeGame.class.getDeclaredMethod(method);
        m.setAccessible(true);
        return m.invoke(game);
    }
}
