package gdx.diablo.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.badlogic.gdx.utils.IntMap;

import java.util.Arrays;
import java.util.Iterator;

import gdx.diablo.Diablo;
import gdx.diablo.entity.Entity;
import gdx.diablo.graphics.PaletteIndexedBatch;
import gdx.diablo.map.DT1.Tile;

public class MapRenderer2 {
  private static final String TAG = "MapRenderer";
  private static final boolean DEBUG          = true;
  private static final boolean DEBUG_MATH     = DEBUG && !true;
  private static final boolean DEBUG_BUFFER   = DEBUG && true;
  private static final boolean DEBUG_SUBTILE  = DEBUG && true;
  private static final boolean DEBUG_TILE     = DEBUG && true;
  private static final boolean DEBUG_CAMERA   = DEBUG && true;
  private static final boolean DEBUG_OVERSCAN = DEBUG && true;
  private static final boolean DEBUG_GRID     = DEBUG && true;
  private static final boolean DEBUG_WALKABLE = DEBUG && !true;
  private static final boolean DEBUG_SPECIAL  = DEBUG && true;
  private static final boolean DEBUG_MOUSE    = DEBUG && true;
  private static final boolean DEBUG_PATHS    = DEBUG && true;
  private static final boolean DEBUG_POPPADS  = DEBUG && true;

  public static boolean RENDER_DEBUG_SUBTILE  = DEBUG_SUBTILE;
  public static boolean RENDER_DEBUG_TILE     = DEBUG_TILE;
  public static boolean RENDER_DEBUG_CAMERA   = DEBUG_CAMERA;
  public static boolean RENDER_DEBUG_OVERSCAN = DEBUG_OVERSCAN;
  public static int     RENDER_DEBUG_GRID     = DEBUG_GRID ? 3 : 0;
  public static int     RENDER_DEBUG_WALKABLE = DEBUG_WALKABLE ? 1 : 0;
  public static boolean RENDER_DEBUG_SPECIAL  = DEBUG_SPECIAL;
  public static boolean RENDER_DEBUG_PATHS    = DEBUG_PATHS;

  private static final Color RENDER_DEBUG_GRID_COLOR_1 = new Color(0x3f3f3f3f);
  private static final Color RENDER_DEBUG_GRID_COLOR_2 = new Color(0x7f7f7f3f);
  private static final Color RENDER_DEBUG_GRID_COLOR_3 = new Color(0x0000ff3f);
  public static int DEBUG_GRID_MODES = 3;

  // Extra padding to ensure proper overscan, should be odd value
  private static final int TILES_PADDING_X = 3;
  private static final int TILES_PADDING_Y = 3;

  private final Vector3    tmpVec3  = new Vector3();
  private final Vector2    tmpVec2  = new Vector2();
  private final GridPoint2 tmpVec2i = new GridPoint2();

  PaletteIndexedBatch batch;
  OrthographicCamera  camera;
  Map                 map;
  int viewBuffer[];
  //int viewBuffer2[]; // lower walls
  //int viewBuffer3[]; // upper walls

  Entity  src;
  Vector3 currentPos = new Vector3();

  // sub-tile index in world-space
  int x, y;

  // sub-tile index in tile-space 2-D
  int stx, sty;

  // sub-tile index in tile-space 1-D
  int t;

  // pixel offset of sub-tile in world-space
  int spx, spy;

  // tile index in world-space
  int tx, ty;

  // pixel offset of tile in world-space
  int tpx, tpy;

  int width, height;
  int tilesX, tilesY;
  int renderWidth, renderHeight;

  // tile index of top right tile in render area
  int startX, startY;

  // tpx and tpy of startX, startY tile in world-space
  int startPx, startPy;

  // camera bounds
  int renderMinX, renderMinY;
  int renderMaxX, renderMaxY;

  // DT1 mainIndexes to not draw
  final Bits popped = new Bits();

  IntMap<? extends Entity> entities;

  public MapRenderer2(PaletteIndexedBatch batch) {
    this.batch  = batch;
    this.camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    camera.setToOrtho(false, 480f * 16 / 9, 480);
    setClipPlane(-1000, 1000);
  }

  // This adjusts clip plane for debugging purposes (some elements rotated to map grid)
  private void setClipPlane(float near, float far) {
    camera.near = near;
    camera.far  = far;
    camera.update();
  }

  public Map getMap() {
    return map;
  }

  public void setMap(Map map) {
    if (this.map != map) {
      this.map = map;
    }
  }

  public Entity getSrc() {
    return src;
  }

  public void setSrc(Entity src) {
    if (this.src != src) {
      this.src = src;
    }
  }

  public void setEntities(IntMap<? extends Entity> entities) {
    this.entities = entities;
  }

  public float zoom() {
    return camera.zoom;
  }

  public void zoom(float zoom) {
    if (camera.zoom != zoom) {
      camera.zoom = zoom;
      update(true);
    }
  }

  public Vector2 unproject(Vector2 dst) {
    return unproject(dst.x, dst.y, dst);
  }

  public Vector2 unproject(float x, float y) {
    return unproject(x, y, new Vector2());
  }

  public Vector2 unproject(float x, float y, Vector2 dst) {
    tmpVec3.set(x, y, 0);
    camera.unproject(tmpVec3);
    return dst.set(tmpVec3.x, tmpVec3.y);
  }

  public GridPoint2 coords() {
    return coords(new GridPoint2());
  }

  public GridPoint2 coords(GridPoint2 dst) {
    tmpVec2.set(Gdx.input.getX(), Gdx.input.getY());
    unproject(tmpVec2);
    return coords(tmpVec2.x, tmpVec2.y, dst);
  }

  public GridPoint2 coords(float x, float y) {
    return coords(x, y, new GridPoint2());
  }

  public GridPoint2 coords(float x, float y, GridPoint2 dst) {
    float adjustX = (int) x;
    float adjustY = (int) y - Tile.SUBTILE_HEIGHT50;
    float selectX = ( adjustX / Tile.SUBTILE_WIDTH50 - adjustY / Tile.SUBTILE_HEIGHT50) / 2;
    float selectY = (-adjustX / Tile.SUBTILE_WIDTH50 - adjustY / Tile.SUBTILE_HEIGHT50) / 2;
    if (selectX < 0) selectX--;
    if (selectY < 0) selectY--;
    return dst.set((int) selectX, (int) selectY);
  }

  public void resize() {
    updateBounds();
    final int viewBufferLen = tilesX + tilesY - 1;
    final int viewBufferMax = tilesY * 2 - 1; // was tilesX, but seems better if bound to height
    viewBuffer = new int[viewBufferLen];
    int x, y;
    for (x = 0, y = 1; y < viewBufferMax; x++, y += 2)
      viewBuffer[x] = viewBuffer[viewBufferLen - 1 - x] = y;
    while (viewBuffer[x] == 0)
      viewBuffer[x++] = viewBufferMax;
    if (DEBUG_BUFFER) Gdx.app.debug(TAG, "viewBuffer=" + Arrays.toString(viewBuffer));
  }

  private void updateBounds() {
    width  = (int) (camera.viewportWidth  * camera.zoom);
    height = (int) (camera.viewportHeight * camera.zoom);

    renderMinX = (int) camera.position.x - (width  >>> 1);
    renderMinY = (int) camera.position.y - (height >>> 1);
    renderMaxX = renderMinX + width;
    renderMaxY = renderMinY + height;

    int minTilesX = ((width  + Tile.WIDTH  - 1) / Tile.WIDTH);
    int minTilesY = ((height + Tile.HEIGHT - 1) / Tile.HEIGHT);
    if ((minTilesX & 1) == 1) minTilesX++;
    if ((minTilesY & 1) == 1) minTilesY++;
    tilesX = minTilesX + TILES_PADDING_X;
    tilesY = minTilesY + TILES_PADDING_Y;
    renderWidth  = tilesX * Tile.WIDTH;
    renderHeight = tilesY * Tile.HEIGHT;
    assert (tilesX & 1) == 1 && (tilesY & 1) == 1;
  }

  public void update() {
    update(false);
  }

  public void update(boolean force) {
    if (src == null) return;
    Vector3 pos = src.position();
    if (pos.epsilonEquals(currentPos) && !force) return;
    currentPos.set(pos);
    this.x = (int) pos.x;
    this.y = (int) pos.y;
    spx =  (x * Tile.SUBTILE_WIDTH50)  - (y * Tile.SUBTILE_WIDTH50);
    spy = -(x * Tile.SUBTILE_HEIGHT50) - (y * Tile.SUBTILE_HEIGHT50);
    float spxf =  (pos.x * Tile.SUBTILE_WIDTH50)  - (pos.y * Tile.SUBTILE_WIDTH50);
    float spyf = -(pos.x * Tile.SUBTILE_HEIGHT50) - (pos.y * Tile.SUBTILE_HEIGHT50);
    camera.position.set(spxf, spyf, 0);
    camera.update();

    // subtile index in tile-space
    stx = x < 0
        ? (x + 1) % Tile.SUBTILE_SIZE + (Tile.SUBTILE_SIZE - 1)
        : x % Tile.SUBTILE_SIZE;
    sty = y < 0
        ? (y + 1) % Tile.SUBTILE_SIZE + (Tile.SUBTILE_SIZE - 1)
        : y % Tile.SUBTILE_SIZE;
    t   = Tile.SUBTILE_INDEX[stx][sty];

    // pixel offset of subtile in world-space
    spx = -Tile.SUBTILE_WIDTH50  + (x * Tile.SUBTILE_WIDTH50)  - (y * Tile.SUBTILE_WIDTH50);
    spy = -Tile.SUBTILE_HEIGHT50 - (x * Tile.SUBTILE_HEIGHT50) - (y * Tile.SUBTILE_HEIGHT50);

    // tile index in world-space
    tx = x < 0
        ? ((x + 1) / Tile.SUBTILE_SIZE) - 1
        : (x / Tile.SUBTILE_SIZE);
    ty = y < 0
        ? ((y + 1) / Tile.SUBTILE_SIZE) - 1
        : (y / Tile.SUBTILE_SIZE);

    tpx = spx - Tile.SUBTILE_OFFSET[t][0];
    tpy = spy - Tile.SUBTILE_OFFSET[t][1];

    //updateBounds();
    renderMinX = (int) camera.position.x - (width  >>> 1);
    renderMinY = (int) camera.position.y - (height >>> 1);
    renderMaxX = renderMinX + width;
    renderMaxY = renderMinY + height;

    final int offX = tilesX >>> 1;
    final int offY = tilesY >>> 1;
    startX = tx + offX - offY;
    startY = ty - offX - offY;
    startPx = tpx + renderWidth  / 2 - Tile.WIDTH50;
    startPy = tpy + renderHeight / 2 - Tile.HEIGHT50;

    if (DEBUG_MATH) {
      Gdx.app.debug(TAG,
          String.format("(%2d,%2d){%d,%d}[%2d,%2d](%dx%d)[%dx%d] %d,%d",
              x, y, stx, sty, tx, ty, width, height, tilesX, tilesY, spx, spy));
    }

    map.updatePopPads(popped, x, y, tx, ty, stx, sty);
    if (DEBUG_POPPADS) {
      String popPads = getPopPads();
      if (!popPads.isEmpty()) Gdx.app.debug(TAG, "PopPad IDs: " + popPads);
    }
  }

  private String getPopPads() {
    StringBuilder builder = new StringBuilder();
    for (int i = popped.nextSetBit(0); i >= 0; i = popped.nextSetBit(i + 1)) {
      builder.append(i).append(',');
    }

    if (builder.length() > 0) {
      builder.setLength(builder.length() - 1);
    }

    return builder.toString();
  }

  public void draw(float delta) {
    batch.setProjectionMatrix(camera.combined);
    for (int i = 0, x, y; i < Map.MAX_LAYERS; i++) {
      int startX2 = startX;
      int startY2 = startY;
      int startPx2 = startPx;
      int startPy2 = startPy;
      for (y = 0; y < viewBuffer.length; y++) {
        int tx = startX2;
        int ty = startY2;
        int stx = tx * Tile.SUBTILE_SIZE;
        int sty = ty * Tile.SUBTILE_SIZE;
        int px = startPx2;
        int py = startPy2;
        int size = viewBuffer[y];
        for (x = 0; x < size; x++) {
          Map.Zone zone = map.getZone(stx, sty);
          if (zone != null) {
            //Map.Tile[][] tiles = zone.tiles[i];
            //if (tiles != null) {
              Map.Tile tile = zone.get(i, tx, ty);
              switch (i) {
                case Map.FLOOR_OFFSET: case Map.FLOOR_OFFSET + 1:
                  // TODO: Expand upon this idea... Good idea to limit upper/lower walls too!
                  if (px > renderMaxX || py > renderMaxY) break;
                  if (px + Tile.WIDTH < renderMinX || py + Tile.HEIGHT < renderMinY) break;
                  //drawWalls(batch, tile, px, py, false);
                  drawFloor(batch, tile, px, py);
                  break;
                case Map.SHADOW_OFFSET:
                  //batch.setBlendMode(BlendMode.SHADOW, SHADOW_TINT);
                  //drawShadows(batch, tx, ty, px, py);
                  //batch.resetBlendMode();
                  break;
                case Map.WALL_OFFSET:
                  drawEntities(batch, stx, sty);
                  drawObjects(batch, zone, stx, sty);
                  // fall-through
                case Map.WALL_OFFSET + 1: case Map.WALL_OFFSET + 2: case Map.WALL_OFFSET + 3:
                  drawWall(batch, tile, px, py);
                  break;
                case Map.TAG_OFFSET:
                  break;
                default:
                  //...
              }
            //}
          }

          tx++;
          stx += Tile.SUBTILE_SIZE;
          px += Tile.WIDTH50;
          py -= Tile.HEIGHT50;
        }

        startY2++;
        if (y >= tilesX - 1) {
          startX2++;
          startPy2 -= Tile.HEIGHT;
        } else {
          startX2--;
          startPx2 -= Tile.WIDTH;
        }
      }
    }
  }

  void drawFloor(PaletteIndexedBatch batch, Map.Tile tile, int px, int py) {
    if (tile == null) return;
    TextureRegion texture = tile.tile.texture;
    //if (texture.getTexture().getTextureObjectHandle() == 0) return;
    batch.draw(texture, px, py, texture.getRegionWidth() + 1, texture.getRegionHeight() + 1);
  }

  void drawEntities(PaletteIndexedBatch batch, int stx, int sty) {
    if (entities == null) return;
    for (Entity entity : entities.values()) {
      Vector3 position = entity.position();
      if ((stx <= position.x && position.x < stx + Tile.SUBTILE_SIZE)
       && (sty <= position.y && position.y < sty + Tile.SUBTILE_SIZE)) {
        entity.draw(batch);
      }
    }
  }

  void drawObjects(PaletteIndexedBatch batch, Map.Zone zone, int stx, int sty) {
    for (Entity entity : zone.entities) {
      Vector3 position = entity.position();
      if ((stx <= position.x && position.x < stx + Tile.SUBTILE_SIZE)
       && (sty <= position.y && position.y < sty + Tile.SUBTILE_SIZE)) {
        entity.draw(batch);
      }
    }
  }

  void drawWall(PaletteIndexedBatch batch, Map.Tile tile, int px, int py) {
    if (tile == null) return;
    if (Orientation.isSpecial(tile.cell.orientation)) {
      /*if (!RENDER_DEBUG_SPECIAL) return;
      batch.setShader(null);
      BitmapFont font = Diablo.fonts.consolas16;
      GlyphLayout layout = new GlyphLayout(font, Map.ID.getName(tile.cell.id));
      font.draw(batch, layout,
          px + Tile.WIDTH  / 2 - layout.width  / 2,
          py + Tile.HEIGHT / 2 - layout.height / 2);
      batch.setShader(Diablo.shader);*/
      return;
    }

    if (tile.tile.isFloor()) {
      return;
    }

    if (popped.get(tile.tile.mainIndex)) {
      return;
    }

    batch.draw(tile.tile.texture, px, tile.tile.orientation == Orientation.ROOF ? py + tile.tile.roofHeight : py);
    if (tile.tile.orientation == Orientation.RIGHT_NORTH_CORNER_WALL) {
      batch.draw(tile.sibling.texture, px, py);
    }
  }

  public void drawDebug(ShapeRenderer shapes) {
    shapes.setProjectionMatrix(camera.combined);
    if (RENDER_DEBUG_GRID > 0)
      drawDebugGrid(shapes);

    if (RENDER_DEBUG_WALKABLE > 0)
      drawDebugWalkable(shapes);


    if (RENDER_DEBUG_SPECIAL)
      drawDebugSpecial(shapes);

    if (RENDER_DEBUG_TILE) {
      shapes.setColor(Color.OLIVE);
      drawDiamond(shapes, tpx, tpy, Tile.WIDTH, Tile.HEIGHT);
    }

    if (RENDER_DEBUG_SUBTILE) {
      shapes.setColor(Color.WHITE);
      drawDiamond(shapes, spx, spy, Tile.SUBTILE_WIDTH, Tile.SUBTILE_HEIGHT);
    }

    if (RENDER_DEBUG_PATHS)
      drawDebugPaths(shapes);

    if (RENDER_DEBUG_CAMERA) {
      float viewportWidth  = width;
      float viewportHeight = height;
      shapes.setColor(Color.GREEN);
      shapes.rect(
          camera.position.x - MathUtils.ceil(viewportWidth  / 2),
          camera.position.y - MathUtils.ceil(viewportHeight / 2),
          viewportWidth + 2, viewportHeight + 2);
    }

    if (RENDER_DEBUG_OVERSCAN) {
      /*shapes.setColor(Color.LIGHT_GRAY);
      shapes.rect(
          tpx - renderWidth  / 2 + Tile.WIDTH50,
          tpy - renderHeight / 2 + Tile.HEIGHT50 + renderHeight,
          renderWidth, 96);
      shapes.setColor(Color.GRAY);*/
      shapes.rect(
          tpx - renderWidth  / 2 + Tile.WIDTH50,
          tpy - renderHeight / 2 + Tile.HEIGHT50,
          renderWidth, renderHeight);
      /*shapes.setColor(Color.DARK_GRAY);
      shapes.rect(
          tpx - renderWidth  / 2 + Tile.WIDTH50,
          tpy - renderHeight / 2 + Tile.HEIGHT50 - 96,
          renderWidth, 96);*/
    }

    if (DEBUG_MOUSE) {
      coords(tmpVec2i);
      int mx = -Tile.SUBTILE_WIDTH50  + (tmpVec2i.x * Tile.SUBTILE_WIDTH50)  - (tmpVec2i.y * Tile.SUBTILE_WIDTH50);
      int my = -Tile.SUBTILE_HEIGHT50 - (tmpVec2i.x * Tile.SUBTILE_HEIGHT50) - (tmpVec2i.y * Tile.SUBTILE_HEIGHT50);

      shapes.setColor(Color.VIOLET);
      drawDiamond(shapes, mx, my, Tile.SUBTILE_WIDTH, Tile.SUBTILE_HEIGHT);
    }
  }

  private void drawDebugGrid(ShapeRenderer shapes) {
    int x, y;
    switch (RENDER_DEBUG_GRID) {
      case 1:
        shapes.setColor(RENDER_DEBUG_GRID_COLOR_1);
        int startPx2 = startPx;
        int startPy2 = startPy;
        for (y = 0; y < viewBuffer.length; y++) {
          int px = startPx2;
          int py = startPy2;
          int size = viewBuffer[y];
          for (x = 0; x < size; x++) {
            for (int t = 0; t < Tile.NUM_SUBTILES; t++) {
              drawDiamond(shapes,
                  px + Tile.SUBTILE_OFFSET[t][0], py + Tile.SUBTILE_OFFSET[t][1],
                  Tile.SUBTILE_WIDTH, Tile.SUBTILE_HEIGHT);
            }

            px += Tile.WIDTH50;
            py -= Tile.HEIGHT50;
          }

          if (y >= tilesX - 1) {
            startPy2 -= Tile.HEIGHT;
          } else {
            startPx2 -= Tile.WIDTH;
          }
        }

      case 2:
        shapes.setColor(RENDER_DEBUG_GRID_COLOR_2);
        startPx2 = startPx;
        startPy2 = startPy;
        for (y = 0; y < viewBuffer.length; y++) {
          int px = startPx2;
          int py = startPy2;
          int size = viewBuffer[y];
          for (x = 0; x < size; x++) {
            drawDiamond(shapes, px, py, Tile.WIDTH, Tile.HEIGHT);
            px += Tile.WIDTH50;
            py -= Tile.HEIGHT50;
          }

          if (y >= tilesX - 1) {
            startPy2 -= Tile.HEIGHT;
          } else {
            startPx2 -= Tile.WIDTH;
          }
        }

      case 3:
        shapes.setColor(RENDER_DEBUG_GRID_COLOR_3);
        ShapeRenderer.ShapeType shapeType = shapes.getCurrentType();
        shapes.set(ShapeRenderer.ShapeType.Filled);
        final int LINE_WIDTH = 2;
        int startX2 = startX;
        int startY2 = startY;
        startPx2 = startPx;
        startPy2 = startPy;
        for (y = 0; y < viewBuffer.length; y++) {
          int tx = startX2;
          int ty = startY2;
          int px = startPx2;
          int py = startPy2;
          int size = viewBuffer[y];
          for (x = 0; x < size; x++) {
            Map.Zone zone = map.getZone(tx * Tile.SUBTILE_SIZE, ty * Tile.SUBTILE_SIZE);
            if (zone != null) {
              int modX = tx < 0
                  ? (tx + 1) % zone.gridSizeX + (zone.gridSizeX - 1)
                  : tx % zone.gridSizeX;
              if (modX == 0)
                shapes.rectLine(px, py + Tile.HEIGHT50, px + Tile.WIDTH50, py + Tile.HEIGHT, LINE_WIDTH);
              else if (modX == zone.gridSizeX - 1)
                shapes.rectLine(px + Tile.WIDTH, py + Tile.HEIGHT50, px + Tile.WIDTH50, py, LINE_WIDTH);

              int modY = ty < 0
                  ? (ty + 1) % zone.gridSizeY + (zone.gridSizeY - 1)
                  : ty % zone.gridSizeY;
              if (modY == 0)
                shapes.rectLine(px + Tile.WIDTH50, py + Tile.HEIGHT, px + Tile.WIDTH, py + Tile.HEIGHT50, LINE_WIDTH);
              else if (modY == zone.gridSizeY - 1)
                shapes.rectLine(px + Tile.WIDTH50, py, px, py + Tile.HEIGHT50, LINE_WIDTH);

              if (modX == 0 && modY == 0) {
                Map.Preset preset = zone.getGrid(tx, ty);
                StringBuilder sb = new StringBuilder(tx + "," + ty);
                if (preset != null)
                  sb.append('\n').append(preset.ds1Path);
                String desc = sb.toString();

                shapes.end();
                batch.getProjectionMatrix()
                    .translate(px + Tile.WIDTH50, py + Tile.HEIGHT - Tile.SUBTILE_HEIGHT, 0)
                    .rotate(Vector3.X,  60)
                    .rotate(Vector3.Z, -45);
                //batch.getProjectionMatrix()
                //    .translate(px + Tile.WIDTH50, py + Tile.HEIGHT - Tile.SUBTILE_HEIGHT, 0)
                //    .rotateRad(Vector3.Z, -0.463647609f)
                //    .shear;
                batch.begin();
                batch.setShader(null);
                BitmapFont font = Diablo.fonts.consolas16;
                GlyphLayout layout = new GlyphLayout(font, desc);
                font.draw(batch, layout, 0, 0);
                /*GlyphLayout layout = new GlyphLayout(font, desc, 0, desc.length(), font.getColor(), 0, Align.center, false, null);
                font.draw(batch, layout,
                    px + Tile.WIDTH50,
                    py + Tile.HEIGHT - font.getLineHeight());
                */
                batch.end();
                batch.setProjectionMatrix(camera.combined);
                shapes.begin(ShapeRenderer.ShapeType.Filled);
              }
            }

            tx++;
            px += Tile.WIDTH50;
            py -= Tile.HEIGHT50;
          }

          startY2++;
          if (y >= tilesX - 1) {
            startX2++;
            startPy2 -= Tile.HEIGHT;
          } else {
            startX2--;
            startPx2 -= Tile.WIDTH;
          }
        }
        shapes.set(shapeType);

      default:
    }
  }

  private void drawDebugWalkable(ShapeRenderer shapes) {
    final int[] WALKABLE_ID = {
        20, 21, 22, 23, 24,
        15, 16, 17, 18, 19,
        10, 11, 12, 13, 14,
        5, 6, 7, 8, 9,
        0, 1, 2, 3, 4
    };

    ShapeRenderer.ShapeType shapeType = shapes.getCurrentType();
    shapes.set(ShapeRenderer.ShapeType.Filled);

    int startX2 = startX;
    int startY2 = startY;
    int startPx2 = startPx;
    int startPy2 = startPy;
    int x, y;
    for (y = 0; y < viewBuffer.length; y++) {
      int tx = startX2;
      int ty = startY2;
      int px = startPx2;
      int py = startPy2;
      int size = viewBuffer[y];
      for (x = 0; x < size; x++) {
        Map.Zone zone = map.getZone(tx * Tile.SUBTILE_SIZE, ty * Tile.SUBTILE_SIZE);
        if (zone != null) {
          if (RENDER_DEBUG_WALKABLE == 1) {
            for (int sty = 0, t = 0; sty < Tile.SUBTILE_SIZE; sty++) {
              for (int stx = 0; stx < Tile.SUBTILE_SIZE; stx++, t++) {
                int flags = zone.flags[zone.getLocalTX(tx) * Tile.SUBTILE_SIZE + stx][zone.getLocalTY(ty) * Tile.SUBTILE_SIZE + sty] & 0xFF;
                if (flags == 0) continue;
                drawDebugWalkableTiles(shapes, px, py, t, flags);
              }
            }
          } else {
            //Map.Tile[][] tiles = zone.tiles[RENDER_DEBUG_WALKABLE - 1];
            //if (tiles != null) {
              Map.Tile tile = zone.get(RENDER_DEBUG_WALKABLE - 2, tx, ty);
              for (int t = 0; tile != null && tile.tile != null && t < Tile.NUM_SUBTILES; t++) {
                int flags = tile.tile.flags[WALKABLE_ID[t]] & 0xFF;
                if (flags == 0) continue;
                drawDebugWalkableTiles(shapes, px, py, t, flags);
              }
            //}
          }
        }

        tx++;
        px += Tile.WIDTH50;
        py -= Tile.HEIGHT50;
      }

      startY2++;
      if (y >= tilesX - 1) {
        startX2++;
        startPy2 -= Tile.HEIGHT;
      } else {
        startX2--;
        startPx2 -= Tile.WIDTH;
      }
    }

    shapes.set(shapeType);
  }

  private void drawDebugWalkableTiles(ShapeRenderer shapes, int px, int py, int t, int flags) {
    int offX = px + Tile.SUBTILE_OFFSET[t][0];
    int offY = py + Tile.SUBTILE_OFFSET[t][1];

    shapes.setColor(Color.CORAL);
    drawDiamond(shapes, offX, offY, Tile.SUBTILE_WIDTH, Tile.SUBTILE_HEIGHT);

    offY += Tile.SUBTILE_HEIGHT50;

    if ((flags & Tile.FLAG_BLOCK_WALK) != 0) {
      shapes.setColor(Color.FIREBRICK);
      shapes.triangle(
          offX + 16, offY,
          offX + 16, offY + 8,
          offX + 24, offY + 4);
    }
    if ((flags & Tile.FLAG_BLOCK_LIGHT_LOS) != 0) {
      shapes.setColor(Color.FOREST);
      shapes.triangle(
          offX + 16, offY,
          offX + 32, offY,
          offX + 24, offY + 4);
    }
    if ((flags & Tile.FLAG_BLOCK_JUMP) != 0) {
      shapes.setColor(Color.ROYAL);
      shapes.triangle(
          offX + 16, offY,
          offX + 32, offY,
          offX + 24, offY - 4);
    }
    if ((flags & Tile.FLAG_BLOCK_PLAYER_WALK) != 0) {
      shapes.setColor(Color.VIOLET);
      shapes.triangle(
          offX + 16, offY,
          offX + 16, offY - 8,
          offX + 24, offY - 4);
    }
    if ((flags & Tile.FLAG_BLOCK_UNKNOWN1) != 0) {
      shapes.setColor(Color.GOLD);
      shapes.triangle(
          offX + 16, offY,
          offX + 16, offY - 8,
          offX + 8, offY - 4);
    }
    if ((flags & Tile.FLAG_BLOCK_LIGHT) != 0) {
      shapes.setColor(Color.SKY);
      shapes.triangle(
          offX, offY,
          offX + 16, offY,
          offX + 8, offY - 4);
    }
    if ((flags & Tile.FLAG_BLOCK_UNKNOWN2) != 0) {
      shapes.setColor(Color.WHITE);
      shapes.triangle(
          offX, offY,
          offX + 16, offY,
          offX + 8, offY + 4);
    }
    if ((flags & Tile.FLAG_BLOCK_UNKNOWN3) != 0) {
      shapes.setColor(Color.SLATE);
      shapes.triangle(
          offX + 16, offY,
          offX + 16, offY + 8,
          offX + 8, offY + 4);
    }
  }

  private void drawDebugSpecial(ShapeRenderer shapes) {
    for (int i = Map.WALL_OFFSET, x, y; i < Map.WALL_OFFSET + Map.MAX_WALLS; i++) {
      int startX2 = startX;
      int startY2 = startY;
      int startPx2 = startPx;
      int startPy2 = startPy;
      for (y = 0; y < viewBuffer.length; y++) {
        int tx = startX2;
        int ty = startY2;
        int stx = tx * Tile.SUBTILE_SIZE;
        int sty = ty * Tile.SUBTILE_SIZE;
        int px = startPx2;
        int py = startPy2;
        int size = viewBuffer[y];
        for (x = 0; x < size; x++) {
          Map.Zone zone = map.getZone(stx, sty);
          if (zone != null) {
            Map.Tile tile = zone.get(i, tx, ty);
            if (tile != null && Orientation.isSpecial(tile.cell.orientation)) {
              if (Map.ID.POPPADS.contains(tile.cell.id)) {
                shapes.setColor(Map.ID.getColor(tile.cell));
                Map.Preset preset = zone.getGrid(tx, ty);
                Map.Preset.PopPad popPad = preset.popPads.get(tile.cell.id);
                if (popPad.startX == zone.getGridX(tx) && popPad.startY == zone.getGridY(ty)) {
                  int width  = popPad.endX - popPad.startX;
                  int height = popPad.endY - popPad.startY;
                  shapes.line(
                      px + Tile.WIDTH50, py + Tile.HEIGHT,
                      px + Tile.WIDTH50 + (width  * Tile.SUBTILE_WIDTH50),
                      py + Tile.HEIGHT  - (height * Tile.SUBTILE_HEIGHT50));
                  shapes.line(
                      px + Tile.WIDTH50, py + Tile.HEIGHT,
                      px + Tile.WIDTH50 - (height * Tile.SUBTILE_WIDTH50),
                      py + Tile.HEIGHT  - (height * Tile.SUBTILE_HEIGHT50));
                  shapes.line(
                      px + Tile.WIDTH50 + (width * Tile.SUBTILE_WIDTH50),
                      py + Tile.HEIGHT - (height * Tile.SUBTILE_HEIGHT50),
                      px + Tile.WIDTH50 + (width * Tile.SUBTILE_WIDTH50) - (height * Tile.SUBTILE_WIDTH50),
                      py + Tile.HEIGHT - (height * Tile.SUBTILE_HEIGHT50) - (height * Tile.SUBTILE_HEIGHT50));
                  shapes.line(
                      px + Tile.WIDTH50 - (height * Tile.SUBTILE_WIDTH50),
                      py + Tile.HEIGHT - (height * Tile.SUBTILE_HEIGHT50),
                      px + Tile.WIDTH50 - (height * Tile.SUBTILE_WIDTH50) + (width  * Tile.SUBTILE_WIDTH50),
                      py + Tile.HEIGHT - (height * Tile.SUBTILE_HEIGHT50) - (height * Tile.SUBTILE_HEIGHT50));
                }
              } else {
                shapes.setColor(Color.WHITE);
                drawDiamond(shapes, px, py, Tile.WIDTH, Tile.HEIGHT);
              }
              shapes.end();

              batch.begin();
              batch.setShader(null);
              BitmapFont font = Diablo.fonts.consolas16;
              String str = String.format(String.format("%s%n%08x", Map.ID.getName(tile.cell.id), tile.cell.value));
              GlyphLayout layout = new GlyphLayout(font, str, 0, str.length(), font.getColor(), 0, Align.center, false, null);
              font.draw(batch, layout,
                  px + Tile.WIDTH50,
                  py + Tile.HEIGHT50 + layout.height / 2);
              batch.end();
              batch.setShader(Diablo.shader);

              shapes.begin(ShapeRenderer.ShapeType.Line);
            }
          }

          tx++;
          stx += Tile.SUBTILE_SIZE;
          px += Tile.WIDTH50;
          py -= Tile.HEIGHT50;
        }

        startY2++;
        if (y >= tilesX - 1) {
          startX2++;
          startPy2 -= Tile.HEIGHT;
        } else {
          startX2--;
          startPx2 -= Tile.WIDTH;
        }
      }
    }
  }

  private void drawDebugPaths(ShapeRenderer shapes) {
    shapes.set(ShapeRenderer.ShapeType.Filled);
    int startX2 = startX;
    int startY2 = startY;
    int x, y;
    for (y = 0; y < viewBuffer.length; y++) {
      int tx = startX2;
      int ty = startY2;
      int stx = tx * Tile.SUBTILE_SIZE;
      int sty = ty * Tile.SUBTILE_SIZE;
      int size = viewBuffer[y];
      for (x = 0; x < size; x++) {
        Map.Zone zone = map.getZone(stx, sty);
        if (zone != null) {
          for (Entity entity : zone.entities) {
            Vector3 position = entity.position();
            if ((stx <= position.x && position.x < stx + Tile.SUBTILE_SIZE)
             && (sty <= position.y && position.y < sty + Tile.SUBTILE_SIZE)) {
              entity.drawDebugPath(shapes);
            }
          }
        }

        tx++;
        stx += Tile.SUBTILE_SIZE;
      }

      startY2++;
      if (y >= tilesX - 1) {
        startX2++;
      } else {
        startX2--;
      }
    }

    shapes.set(ShapeRenderer.ShapeType.Line);
  }

  private static void drawDiamond(ShapeRenderer shapes, float x, float y, int width, int height) {
    int hw = width  >>> 1;
    int hh = height >>> 1;
    shapes.line(x        , y + hh    , x + hw   , y + height);
    shapes.line(x + hw   , y + height, x + width, y + hh    );
    shapes.line(x + width, y + hh    , x + hw   , y         );
    shapes.line(x + hw   , y         , x        , y + hh    );
  }

  public void drawDebugPath(ShapeRenderer shapes, Path path) {
    if (path == null) return;
    shapes.setColor(Color.TAN);
    shapes.set(ShapeRenderer.ShapeType.Filled);
    for (Point2 dst : path) {
      float px = +(dst.x * Tile.SUBTILE_WIDTH50)  - (dst.y * Tile.SUBTILE_WIDTH50)  - Tile.SUBTILE_WIDTH50;
      float py = -(dst.x * Tile.SUBTILE_HEIGHT50) - (dst.y * Tile.SUBTILE_HEIGHT50) - Tile.SUBTILE_HEIGHT50;
      drawDiamondSolid(shapes, px, py, Tile.SUBTILE_WIDTH, Tile.SUBTILE_HEIGHT);
    }

    shapes.set(ShapeRenderer.ShapeType.Line);
  }

  public void drawDebugPath(ShapeRenderer shapes, MapGraph.MapGraphPath path) {
    if (path == null || path.getCount() < 2) return;
    shapes.setColor(Color.RED);
    shapes.set(ShapeRenderer.ShapeType.Line);
    Iterator<MapGraph.Point2> it = new Array.ArrayIterator<>(path.nodes);
    MapGraph.Point2 src = it.next();
    for (MapGraph.Point2 dst; it.hasNext(); src = dst) {
      dst = it.next();
      float px1 = +(src.x * Tile.SUBTILE_WIDTH50)  - (src.y * Tile.SUBTILE_WIDTH50);
      float py1 = -(src.x * Tile.SUBTILE_HEIGHT50) - (src.y * Tile.SUBTILE_HEIGHT50);
      float px2 = +(dst.x * Tile.SUBTILE_WIDTH50)  - (dst.y * Tile.SUBTILE_WIDTH50);
      float py2 = -(dst.x * Tile.SUBTILE_HEIGHT50) - (dst.y * Tile.SUBTILE_HEIGHT50);
      shapes.line(px1, py1, px2, py2);
    }
  }

  private static void drawDiamondSolid(ShapeRenderer shapes, float x, float y, int width, int height) {
    int hw = width  >>> 1;
    int hh = height >>> 1;
    shapes.triangle(x, y + hh, x + hw, y + height, x + width, y + hh);
    shapes.triangle(x, y + hh, x + hw, y         , x + width, y + hh);
  }
}