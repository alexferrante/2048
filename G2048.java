import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class G2048 extends JPanel {
  private static final Color BG_COLOR = new Color(0xbbada0);
  private static final String FONT_NAME = "Helvetica Neue";
  private static final int TILE_SIZE = 64;
  private static final int TILES_MARGIN = 16;

  private Tile[] tiles;
  boolean won = false;
  boolean lost = false;
  int moveCount = 0;

  boolean initExit = false;
  boolean initRestart = false;

/*
Initialize game, set up JPanel and add key listeners for user input 
*/
  public G2048() {
    setPreferredSize(new Dimension(340, 400));
    setFocusable(true);
    addKeyListener(new KeyAdapter() {
      // Allow user to quit or restart (with confirmation)
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_R) {
          if (won || lost) {
            resetGame();
          } else if (!won && !lost) {
            if (initRestart == true) {
              resetGame();
            } else {
              initRestart = true;
            }
          }
        }
        
        if (e.getKeyCode() == KeyEvent.VK_Q) {
          if (won || lost) {
            System.exit(0);
          }
          if (initExit == true) {
            System.exit(0);
          } else {
            initExit = true;
          } 
        }
        if (!canMove()) {
          lost = true;
        }

        // Manage input used for movement 
        if (!won && !lost) {
          int keycode = e.getKeyCode();
          if ((keycode == KeyEvent.VK_LEFT) || (keycode == KeyEvent.VK_RIGHT) 
             || (keycode == KeyEvent.VK_DOWN) || (keycode == KeyEvent.VK_UP)) {
            switch (keycode) {
              case KeyEvent.VK_LEFT:
                moveLeft();
                break;
              case KeyEvent.VK_RIGHT:
                moveRight();
                break;
              case KeyEvent.VK_DOWN:
                moveDown();
                break;
              case KeyEvent.VK_UP:
                moveUp();
                break;
            }
            initExit = false;
            initRestart = false;
          }
        }

        if (!won && !canMove()) {
          lost = true;
        }
        repaint();
      }
    });
    resetGame();
  }

  // Allow game to be restarted
  public void resetGame() {
    tiles = new Tile[4 * 4];
    moveCount = 0;
    won = false;
    lost = false;
    for (int i = 0; i < tiles.length; i++) {
      tiles[i] = new Tile();
    }
    addTile();
    addTile();
  }

  // Manage movement to the left, update tiles appropriately, validate attempted moves and log game state information
  public void moveLeft() {
    boolean needAddTile = false;
    for (int i = 0; i < 4; i++) {
      Tile[] line = getTiles(i);
      Tile[] merged = combineTile(modTile(line));
      setLine(i, merged);
      if (!needAddTile && !compare(line, merged)) {
        needAddTile = true;
      } 
    }
    if (needAddTile) {
      moveCount++;
      System.out.println("Valid move" + "\tMax number: " +getMaxTile()+ "\tValid moves: "+moveCount);
      addTile();
    } else {
      System.out.println("Invalid move");
    }
  }

  // Manage movement to the right, validate moves and update board via moveLeft
  public void moveRight() {
    tiles = rotate(180);
    moveLeft();
    tiles = rotate(180);
  }

  // Manage upward movement, validate moves and update board via moveLeft
  public void moveUp() {
    tiles = rotate(270);
    moveLeft();
    tiles = rotate(90);
  }

  // Manage downward movement, validate moves and update board via moveLeft
  public void moveDown() {
    tiles = rotate(90);
    moveLeft();
    tiles = rotate(270);
  }

  // Check and return list of empty tiles
  private List<Tile> spaceLeft() {
    final List<Tile> list = new ArrayList<Tile>(16);
    for (Tile t : tiles) {
      if (t.isEmpty()) {
        list.add(t);
      }
    }
    return list;
  }

  // Check if board is full
  private boolean isFull() {
    return spaceLeft().size() == 0;
  }
  boolean canMove() {
    if (!isFull()) {
      return true;
    }
    for (int x = 0; x < 4; x++) {
      for (int y = 0; y < 4; y++) {
        Tile t = tileAt(x, y);
        if ((x < 3 && t.value == tileAt(x + 1, y).value)
          || ((y < 3) && t.value == tileAt(x, y + 1).value)) {
          return true;
        }
      }
    }
    return false;
  }

  // Compare tiles, check if their values are equivalent 
  private boolean compare(Tile[] a, Tile[] b) {
    if (a == b) {
      return true;
    } else if (a.length != b.length) {
      return false;
    }

    for (int i = 0; i < a.length; i++) {
      if (a[i].value != b[i].value) {
        return false;
      }
    }
    return true;
  }

  // Create tiles and rotate for movement in a given direction, calculate appropriate coordinates 
  private Tile[] rotate(int angle) {
    Tile[] newTiles = new Tile[4 * 4];
    int offsetX = 3, offsetY = 3;
    if (angle == 90) {
      offsetY = 0;
    } else if (angle == 270) {
      offsetX = 0;
    }
    double rad = Math.toRadians(angle);
    int cos = (int) Math.cos(rad);
    int sin = (int) Math.sin(rad);
    for (int x = 0; x < 4; x++) {
      for (int y = 0; y < 4; y++) {
        int newX = (x * cos) - (y * sin) + offsetX;
        int newY = (x * sin) + (y * cos) + offsetY;
        newTiles[(newX) + (newY) * 4] = tileAt(x, y);
      }
    }
    return newTiles;
  }

  // Modify tile positioning to update board for moves 
  private Tile[] modTile(Tile[] prevLine) {
    LinkedList<Tile> l = new LinkedList<Tile>();
    for (int i = 0; i < 4; i++) {
      if (!prevLine[i].isEmpty())
        l.addLast(prevLine[i]);
    }
    if (l.size() == 0) {
      return prevLine;
    } else {
      Tile[] newLine = new Tile[4];
      ensureSize(l, 4);
      for (int i = 0; i < 4; i++) {
        newLine[i] = l.removeFirst();
      }
      return newLine;
    }
  }

  // Combine appropriate tiles (tiles of the same value) and update the board 
  private Tile[] combineTile(Tile[] prevLine) {
    LinkedList<Tile> list = new LinkedList<Tile>();
    for (int i = 0; i < 4 && !prevLine[i].isEmpty(); i++) {
      int num = prevLine[i].value;
      if (i < 3 && prevLine[i].value == prevLine[i + 1].value) {
        num *= 2;
        int ourTarget = 2048;
        if (num == ourTarget) {
          won = true;
        }
        i++;
      }
      list.add(new Tile(num));
    }
    if (list.size() == 0) {
      return prevLine;
    } else {
      ensureSize(list, 4);
      return list.toArray(new Tile[4]);
    }
  }

  // Get tiles
  private Tile[] getTiles(int index) {
    Tile[] result = new Tile[4];
    for (int i = 0; i < 4; i++) {
      result[i] = tileAt(i, index);
    }
    return result;
  }

  // Set line of tiles 
  private void setLine(int index, Tile[] re) {
    System.arraycopy(re, 0, tiles, index * 4, 4);
  }

  // Get tile with the maximum value on the board 
  private int getMaxTile() {
    int max = tiles[0].value;
    for (int i = 1; i < tiles.length; i++) {
      if (tiles[i].value > max) {
        max = tiles[i].value;
      }
    }
    return max;
  }

  // Get tile at a specified position 
  private Tile tileAt(int x, int y) {
    return tiles[x + y * 4];
  }

  // Add tile to the main list of tiles, make sure number of tiles does not exceed max
  private static void ensureSize(java.util.List<Tile> l, int s) {
    while (l.size() != s) {
      l.add(new Tile());
    }
  }

  // Add tile to the board
  private void addTile() {
    List<Tile> list = spaceLeft();
    if (!spaceLeft().isEmpty()) {
      int index = (int) (Math.random() * list.size()) % list.size();
      Tile emptyTime = list.get(index);
      emptyTime.value = Math.random() < 0.8 ? 2 : 4;
    }
  }

  // Paint method for updating the board every move
  @Override
  public void paint(Graphics g) {
    super.paint(g);
    g.setColor(BG_COLOR);
    g.fillRect(0, 0, this.getSize().width, this.getSize().height);
    for (int y = 0; y < 4; y++) {
      for (int x = 0; x < 4; x++) {
        drawTile(g, tiles[x + y * 4], x, y);
      }
    }
  }

  // Utility method used by paint to style, display, and update tiles and display game state messages to user 
  private void drawTile(Graphics g2, Tile tile, int x, int y) {
    Graphics2D g = ((Graphics2D) g2);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

    int value = tile.value;
    int xOffset = modCords(x);
    int yOffset = modCords(y);
    g.setColor(tile.tileBackground());
    g.fillRoundRect(xOffset, yOffset, TILE_SIZE, TILE_SIZE, 14, 14);
    g.setColor(tile.getTileVal());
    final int size = value < 100 ? 36 : value < 1000 ? 32 : 24;
    final Font font = new Font(FONT_NAME, Font.BOLD, size);
    g.setFont(font);

    String st = String.valueOf(value);
    final FontMetrics finfo = getFontMetrics(font);
    final int wd = finfo.stringWidth(st);
    final int ht = -(int) finfo.getLineMetrics(st, g).getBaselineOffsets()[2];

    if (value != 0)
      g.drawString(st, xOffset + (TILE_SIZE - wd) / 2, yOffset + TILE_SIZE - (TILE_SIZE - ht) / 2 - 2);

    if ((initExit || initRestart) && (!won) && (!lost)) {
      g.setColor(new Color(255, 255, 255, 30));
      g.fillRect(0, 0, getWidth(), getHeight());
      g.setColor(new Color(0, 0, 0));
      g.setFont(new Font(FONT_NAME, Font.BOLD, 20));
      if (initExit) {
        g.drawString("Are you sure?", 20, 130);
        g.drawString("Press q again to quit", 20, 200);
      }
      if (initRestart) {
        g.drawString("Are you sure?", 50, 130);
        g.drawString("Press r again to restart", 64, 200);
      }
    }

    if (won || lost) {
      g.setColor(new Color(255, 255, 255, 30));
      g.fillRect(0, 0, getWidth(), getHeight());
      g.setColor(new Color(0, 0, 0));
      g.setFont(new Font(FONT_NAME, Font.BOLD, 48));
      if (won) {
        g.drawString("You won!", 60, 150);
      }
      if (lost) {
        g.drawString("Game over!", 50, 130);
        g.drawString("You lose!", 60, 200);
      }
      if (won || lost) {
        g.setFont(new Font(FONT_NAME, Font.PLAIN, 16));
        g.setColor(new Color(128, 128, 128, 128));
        g.drawString("Press r to play again", 100, getHeight() - 20);
      }
    }
    g.setFont(new Font(FONT_NAME, Font.PLAIN, 18));
  }

  // Offset coordinates for properly displaying in frame / panel
  private static int modCords(int arg) {
    return arg * (TILES_MARGIN + TILE_SIZE) + TILES_MARGIN;
  }

  // Utility class to represent tiles and organize the colors associated with their values
  static class Tile {
    int value;

    public Tile() {
      this(0);
    }

    public Tile(int num) {
      value = num;
    }

    public Color getTileVal() {
      return value < 16 ? new Color(0x776e65) :  new Color(0xf9f6f2);
    }

    public boolean isEmpty() {
      return value == 0;
    }

    public Color tileBackground() {
      switch (value) {
        case 2:    return new Color(0xeee4da);
        case 4:    return new Color(0xede0c8);
        case 8:    return new Color(0xf2b179);
        case 16:   return new Color(0xf59563);
        case 32:   return new Color(0xf67c5f);
        case 64:   return new Color(0xf65e3b);
        case 128:  return new Color(0xedcf72);
        case 256:  return new Color(0xedcc61);
        case 512:  return new Color(0xedc850);
        case 1024: return new Color(0xedc53f);
        case 2048: return new Color(0xedc22e);
      }
      return new Color(0xcdc1b4);
    }
  }

  // Start game
  public static void main(String[] args) {
    JFrame game = new JFrame();
    game.setTitle("2048");
    game.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    game.setSize(340, 400);
    game.setResizable(false);

    game.add(new G2048());

    game.setLocationRelativeTo(null);
    game.setVisible(true);
  }
}