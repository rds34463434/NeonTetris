import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;
import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

@SuppressWarnings("unused")
public class NeonTetris extends JFrame {
    private static final int BOARD_WIDTH = 12;
    private static final int BOARD_HEIGHT = 20;
    private static final int TILE_SIZE = 30; 

    private static final Color BG_DARK = new Color(7, 7, 13);
    private static final Color GRID_COLOR = new Color(0, 240, 255, 20);
    private static final Color NEON_CYAN = new Color(0, 240, 255);
    private static final Color NEON_MAGENTA = new Color(255, 0, 119);
    private static final Color SIDEBAR_BG = new Color(15, 15, 30, 200);

    private static final Color[] COLORS = {
        null,
        new Color(255, 0, 85),   // T (Розовый)
        new Color(0, 240, 255),  // O (Бирюзовый)
        new Color(0, 255, 102),  // L (Салатовый)
        new Color(255, 153, 0),  // J (Оранжевый)
        new Color(157, 0, 255),  // I (Фиолетовый)
        new Color(255, 230, 0),  // S (Желтый)
        new Color(255, 0, 170)   // Z (Маджента)
    };

    private static final int[][][] PIECES = {
        {{0,1,0}, {1,1,1}, {0,0,0}}, // T
        {{2,2}, {2,2}},             // O
        {{0,0,3}, {3,3,3}, {0,0,0}}, // L
        {{4,0,0}, {4,4,4}, {0,0,0}}, // J
        {{0,0,0,0}, {5,5,5,5}, {0,0,0,0}, {0,0,0,0}}, // I
        {{0,6,6}, {6,6,0}, {0,0,0}}, // S
        {{7,7,0}, {0,7,7}, {0,0,0}}  // Z
    };
    private static final String Image = null;

    private enum GameState { MENU, PLAYING, GAME_OVER }
    private GameState currentState = GameState.MENU;

    private int[][] arena = new int[BOARD_HEIGHT][BOARD_WIDTH];
    private int[][] currentPiece;
    private int currentTypeIndex;
    private int pieceX, pieceY;
    
    private int nextTypeIndex;
    private Integer holdTypeIndex = null; 
    private boolean hasHeldThisTurn = false;

    private int score = 0;
    private int combo = 0;
    private int level = 1;
    private int highScore = 0;

    private Timer gameTimer;
    private int dropInterval = 750; 
    private GamePanel gamePanel;
    private Rectangle startButtonRect;

// Метод загружает рекорд из файла при старте игры
private void loadHighScore() {
    try {
        File file = new File("highscore.txt");
        if (file.exists()) {
            Scanner scanner = new Scanner(file);
            if (scanner.hasNextInt()) {
                highScore = scanner.nextInt();
            }
            scanner.close();
        }
    } catch (Exception e) {
        System.out.println("Ошибка загрузки рекорда: " + e.getMessage());
    }
}

// Метод сохраняет новый рекорд в файл
private void saveHighScore() {
    try {
        PrintWriter writer = new PrintWriter("highscore.txt");
        writer.print(highScore);
        writer.close();
    } catch (Exception e) {
        System.out.println("Ошибка сохранения рекорда: " + e.getMessage());
    }
}

    public NeonTetris() {
  


        setTitle("NEON TETRIS // JAVA EDITION");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
loadHighScore();

        int panelWidth = BOARD_WIDTH * TILE_SIZE + 180;
        int panelHeight = BOARD_HEIGHT * TILE_SIZE;
        startButtonRect = new Rectangle(panelWidth / 2 - 100, panelHeight / 2, 200, 50);

        nextTypeIndex = getRandomPieceIndex();
        spawnPiece();

        gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        setLocationRelativeTo(null); 

        gameTimer = new Timer(dropInterval, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentState == GameState.PLAYING) {
                    playerDrop();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (currentState == GameState.MENU) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                        startGame();
                    }
                    return;
                }
                if (currentState == GameState.GAME_OVER) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                        currentState = GameState.MENU;
                        gamePanel.repaint();
                    }
                    return;
                }
                
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> playerMove(-1);
                    case KeyEvent.VK_RIGHT -> playerMove(1);
                    case KeyEvent.VK_DOWN -> playerDrop();
                    case KeyEvent.VK_UP -> playerRotate();
                    case KeyEvent.VK_SHIFT -> playerHold();
                }
            }
        });

        gamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentState == GameState.MENU && startButtonRect.contains(e.getPoint())) {
                    startGame();
                }
            }
        });
    }

    private int getRandomPieceIndex() {
        return new Random().nextInt(PIECES.length);
    }

    private void startGame() {
        resetGameData();
        currentState = GameState.PLAYING;
        gameTimer.start();
        gamePanel.repaint();
    }

    private void spawnPiece() {
        currentTypeIndex = nextTypeIndex;
        currentPiece = deepCopy(PIECES[currentTypeIndex]);
        pieceY = 0;
        pieceX = (BOARD_WIDTH / 2) - (currentPiece.length / 2);
        hasHeldThisTurn = false;

        nextTypeIndex = getRandomPieceIndex();

        if (checkCollision(pieceX, pieceY, currentPiece)) {
            currentState = GameState.GAME_OVER;
            gameTimer.stop();
        }
    }

    private void playerDrop() {
        pieceY++;
        if (checkCollision(pieceX, pieceY, currentPiece)) {
            pieceY--;
            mergePiece();
            checkLines();
            spawnPiece();
        }
        gamePanel.repaint();
    }

    private void playerMove(int dir) {
        pieceX += dir;
        if (checkCollision(pieceX, pieceY, currentPiece)) {
            pieceX -= dir;
        }
        gamePanel.repaint();
    }

    private void playerRotate() {
        int[][] rotated = rotateMatrix(currentPiece);
        int oldX = pieceX;
        int offset = 1;
        
        while (checkCollision(pieceX, pieceY, rotated)) {
            pieceX += offset;
            offset = -(offset + (offset > 0 ? 1 : -1));
            if (Math.abs(offset) > rotated.length) {
                pieceX = oldX;
                return; 
            }
        }
        currentPiece = rotated;
        gamePanel.repaint();
    }

    private void playerHold() {
        if (hasHeldThisTurn) return;

        if (holdTypeIndex == null) {
            holdTypeIndex = currentTypeIndex;
            spawnPiece();
        } else {
            int temp = currentTypeIndex;
            currentTypeIndex = holdTypeIndex;
            currentPiece = deepCopy(PIECES[currentTypeIndex]);
            pieceY = 0;
            pieceX = (BOARD_WIDTH / 2) - (currentPiece.length / 2);
            holdTypeIndex = temp;
        }
        hasHeldThisTurn = true;
        gamePanel.repaint();
    }

    private boolean checkCollision(int nx, int ny, int[][] matrix) {
        for (int y = 0; y < matrix.length; y++) {
            for (int x = 0; x < matrix[y].length; x++) {
                if (matrix[y][x] != 0) {
                    int ax = nx + x;
                    int ay = ny + y;
                    if (ax < 0 || ax >= BOARD_WIDTH || ay >= BOARD_HEIGHT) return true;
                    if (ay >= 0 && arena[ay][ax] != 0) return true;
                }
            }
        }
        return false;
    }

    private void mergePiece() {
        for (int y = 0; y < currentPiece.length; y++) {
            for (int x = 0; x < currentPiece[y].length; x++) {
                if (currentPiece[y][x] != 0) {
                    if (pieceY + y >= 0) {
                        arena[pieceY + y][pieceX + x] = currentPiece[y][x];
                    }
                }
            }
        }
    }

    private void checkLines() {
        int linesCleared = 0;
        for (int y = BOARD_HEIGHT - 1; y >= 0; y--) {
            boolean isFull = true;
            for (int x = 0; x < BOARD_WIDTH; x++) {
                if (arena[y][x] == 0) {
                    isFull = false;
                    break;
                }
            }
            if (isFull) {
                linesCleared++;
                // Сдвигаем верхние строки вниз
                for (int ly = y; ly > 0; ly--) {
                    System.arraycopy(arena[ly - 1], 0, arena[ly], 0, BOARD_WIDTH);
                }
                // Полностью очищаем самую верхнюю строку
                arena[0] = new int[BOARD_WIDTH]; 
                y++; 
            }
        }

        if (linesCleared > 0) {
            combo++;
            score += (linesCleared * 100) * combo;
            level = (score / 500) + 1;
            
            dropInterval = Math.max(750 - (level - 1) * 70, 100);
            gameTimer.setDelay(dropInterval);

            if (score > highScore) {
    highScore = score;
    saveHighScore(); // Добавляем вызов сохранения в файл
}

        } else {
            combo = 0;
        }
    }

    private int getGhostY() {
        int ghostY = pieceY;
        while (!checkCollision(pieceX, ghostY + 1, currentPiece)) {
            ghostY++;
        }
        return ghostY;
    }

    private int[][] rotateMatrix(int[][] matrix) {
        int n = matrix.length;
        int[][] rotated = new int[n][n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                rotated[x][n - 1 - y] = matrix[y][x];
            }
        }
        return rotated;
    }

    private int[][] deepCopy(int[][] original) {
        int[][] result = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = original[i].clone();
        }
        return result;
    }

    private void resetGameData() {
        arena = new int[BOARD_HEIGHT][BOARD_WIDTH];
        score = 0; combo = 0; level = 1; dropInterval = 750;
        holdTypeIndex = null;
        gameTimer.setDelay(dropInterval);
        spawnPiece();
    }

    private class GamePanel extends JPanel {
        public GamePanel() {
            setPreferredSize(new Dimension(BOARD_WIDTH * TILE_SIZE + 180, BOARD_HEIGHT * TILE_SIZE));
            setBackground(BG_DARK);
        }

        @Override
protected void paintComponent(Graphics g) {super.paintComponent(g);Graphics2D g2d = (Graphics2D) g;g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);if (currentState == GameState.MENU) {drawMainMenu(g2d);} else {drawGrid(g2d);drawArena(g2d);if (currentState == GameState.PLAYING) {drawGhost(g2d);drawCurrentPiece(g2d);}drawSidebar(g2d);if (currentState == GameState.GAME_OVER) {drawGameOver(g2d);}}}private void drawMainMenu(Graphics2D g) {g.setColor(GRID_COLOR);for (int i = 0; i <= getWidth(); i += TILE_SIZE) g.drawLine(i, 0, i, getHeight());for (int i = 0; i <= getHeight(); i += TILE_SIZE) g.drawLine(0, i, getWidth(), i);g.setFont(new Font("Segoe UI", Font.BOLD, 42));g.setColor(NEON_CYAN);g.drawString("NEON TETRIS", getWidth() / 2 - 130, getHeight() / 2 - 80);g.setFont(new Font("Segoe UI", Font.ITALIC, 14));g.setColor(NEON_MAGENTA);g.drawString("JAVA CYBERPUNK EDITION", getWidth() / 2 - 90, getHeight() / 2 - 50);g.setColor(NEON_CYAN);g.setStroke(new BasicStroke(2));g.drawRect(startButtonRect.x, startButtonRect.y, startButtonRect.width, startButtonRect.height);g.setColor(new Color(0, 240, 255, 30));g.fillRect(startButtonRect.x, startButtonRect.y, startButtonRect.width, startButtonRect.height);g.setFont(new Font("Segoe UI", Font.BOLD, 18));g.setColor(Color.WHITE);g.drawString("START GAME", startButtonRect.x + 40, startButtonRect.y + 32);g.setFont(new Font("Segoe UI", Font.PLAIN, 12));g.setColor(Color.GRAY);g.drawString("Или нажмите Пробел / Enter для старта", getWidth() / 2 - 110, getHeight() / 2 + 100);}private void drawGrid(Graphics2D g) {g.setColor(GRID_COLOR);for (int i = 0; i <= BOARD_WIDTH; i++) {g.drawLine(i * TILE_SIZE, 0, i * TILE_SIZE, BOARD_HEIGHT * TILE_SIZE);}for (int i = 0; i <= BOARD_HEIGHT; i++) {g.drawLine(0, i * TILE_SIZE, BOARD_WIDTH * TILE_SIZE, i * TILE_SIZE);}g.setColor(NEON_CYAN);g.setStroke(new BasicStroke(3));g.drawLine(BOARD_WIDTH * TILE_SIZE, 0, BOARD_WIDTH * TILE_SIZE, BOARD_HEIGHT * TILE_SIZE);}private void drawArena(Graphics2D g) {for (int y = 0; y < BOARD_HEIGHT; y++) {for (int x = 0; x < BOARD_WIDTH; x++) {if (arena[y][x] != 0) {drawBlock(g, x, y, COLORS[arena[y][x]], false);}}}}private void drawCurrentPiece(Graphics2D g) {if (currentPiece == null) return;for (int y = 0; y < currentPiece.length; y++) {for (int x = 0; x < currentPiece[y].length; x++) {if (currentPiece[y][x] != 0) {drawBlock(g, pieceX + x, pieceY + y, COLORS[currentPiece[y][x]], false);}}}}private void drawGhost(Graphics2D g) {if (currentPiece == null) return;int gy = getGhostY();for (int y = 0; y < currentPiece.length; y++) {for (int x = 0; x < currentPiece[y].length; x++) {if (currentPiece[y][x] != 0) {drawBlock(g, pieceX + x, gy + y, COLORS[currentPiece[y][x]], true);}}}}private void drawBlock(Graphics2D g, int x, int y, Color color, boolean isGhost) {int px = x * TILE_SIZE;int py = y * TILE_SIZE;if (isGhost) {g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));g.fillRect(px + 2, py + 2, TILE_SIZE - 4, TILE_SIZE - 4);g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));g.drawRect(px + 2, py + 2, TILE_SIZE - 4, TILE_SIZE - 4);} else {g.setColor(color);g.fillRect(px + 1, py + 1, TILE_SIZE - 2, TILE_SIZE - 2);g.setColor(new Color(255, 255, 255, 100));g.fillRect(px + 4, py + 4, 5, 5);}}private void drawSidebar(Graphics2D g) {int sx = BOARD_WIDTH * TILE_SIZE + 15;g.setColor(Color.WHITE);g.setFont(new Font("Segoe UI", Font.BOLD, 12));g.drawString("HOLD", sx, 30);g.setColor(SIDEBAR_BG);g.fillRect(sx, 40, 140, 80);if (holdTypeIndex != null) {drawPreviewPiece(g, sx + 40, 60, holdTypeIndex);}g.setColor(Color.WHITE);g.drawString("NEXT", sx, 160);g.setColor(SIDEBAR_BG);g.fillRect(sx, 170, 140, 80);drawPreviewPiece(g, sx + 40, 190, nextTypeIndex);g.setColor(Color.WHITE);g.drawString("SCORE", sx, 290);g.setFont(new Font("Segoe UI", Font.BOLD, 22));g.setColor(NEON_CYAN);g.drawString(String.valueOf(score), sx, 315);g.setFont(new Font("Segoe UI", Font.BOLD, 12));g.setColor(Color.WHITE);g.drawString("LEVEL // COMBO", sx, 350);g.setFont(new Font("Segoe UI", Font.BOLD, 16));g.setColor(NEON_MAGENTA);g.drawString("LVL " + level + "   X" + combo, sx, 375);g.setFont(new Font("Segoe UI", Font.BOLD, 12));g.setColor(Color.WHITE);g.drawString("TOP SCORE", sx, 410);g.setFont(new Font("Segoe UI", Font.BOLD, 16));g.setColor(Color.YELLOW);g.drawString(String.valueOf(highScore), sx, 432);g.setFont(new Font("Segoe UI", Font.PLAIN, 10));g.setColor(Color.GRAY);g.drawString("← → : ДВИЖЕНИЕ", sx, 520);g.drawString("↑ : ПОВОРОТ", sx, 540);g.drawString("↓ : УСКОРИТЬ", sx, 560);g.drawString("SHIFT : ЗАПАС", sx, 580);}private void drawPreviewPiece(Graphics2D g, int x, int y, int typeIndex) {int[][] matrix = PIECES[typeIndex];for (int r = 0; r < matrix.length; r++) {for (int c = 0; c < matrix[r].length; c++) {if (matrix[r][c] != 0) {g.setColor(COLORS[matrix[r][c]]);g.fillRect(x + c * 20, y + r * 20, 18, 18);}}}}private void drawGameOver(Graphics2D g) {g.setColor(new Color(7, 7, 13, 230));g.fillRect(0, 0, BOARD_WIDTH * TILE_SIZE, getHeight());g.setColor(NEON_MAGENTA);g.setFont(new Font("Segoe UI", Font.BOLD, 36));g.drawString("GAME OVER", BOARD_WIDTH * TILE_SIZE / 2 - 100, BOARD_HEIGHT * TILE_SIZE / 2 - 20);g.setColor(Color.WHITE);g.setFont(new Font("Segoe UI", Font.PLAIN, 12));g.drawString("Нажмите Пробел для выхода в меню", BOARD_WIDTH * TILE_SIZE / 2 - 105, BOARD_HEIGHT * TILE_SIZE / 2 + 20);}}public static void main(String[] args) {SwingUtilities.invokeLater(() -> {NeonTetris game = new NeonTetris();game.setVisible(true);});}}