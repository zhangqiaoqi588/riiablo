package com.gmail.collinsmith70.unifi.unifi;

import android.support.annotation.CallSuper;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.gmail.collinsmith70.unifi.unifi.math.Boundary;
import com.gmail.collinsmith70.unifi.unifi.util.LengthUnit;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

public abstract class WidgetGroup extends Widget
        implements WidgetParent, Marginable {

  public static final class LayoutParams {

    /**
     * Determines the width of a {@code Widget} within its parent. Width should be given as one of:
     * <ul>
     *   <li>{@code wrap_content} - (default) {@code Widget} is only large enough to fit its
     *                              children</li>
     *   <li>{@code match_parent} - {@code Widget} will be the same size as its parent (excluding
     *                              padding)</li>
     *   <li>{@code [0-9]+\w*(px|dp|mm|cm|m)} - {@code Widget} will be sized according to the
     *                                          {@linkplain LengthUnit#toPixels(String) specified
     *                                          value}</li>
     * </ul>
     */
    @LayoutParam
    public static final String layout_width = "layout_width";

    /**
     * Determines the height of a {@code Widget} within its parent. Height should be given as one
     * of:
     *
     * <ul>
     *   <li>{@code wrap_content} - (default) {@code Widget} is only large enough to fit its
     *                              children</li>
     *   <li>{@code match_parent} - {@code Widget} will be the same size as its parent (excluding
     *                              padding)</li>
     *   <li>{@code [0-9]+\w*(px|dp|mm|cm|m)} - {@code Widget} will be sized according to the
     *                                          {@linkplain LengthUnit#toPixels(String) specified
     *                                          value}</li>
     * </ul>
     */
    @LayoutParam
    public static final String layout_height = "layout_height";

    /**
     * Determines the weight a {@code Widget} should be given within its parent. Specified as a
     * floating point value between {@code 0.0} and {@code 1.0} (inclusive). Value used will
     * determine the ratio of unused space in the parent to allocate to the component.
     */
    @LayoutParam
    public static final String layout_weight = "layout_weight";

  }

  public enum Gravity {
    /**
     * Gravity flag for top/up on the {@code y}-axis
     */
    TOP,

    /**
     * Gravity flag for bottom/down on the {@code y}-axis
     */
    BOTTOM,

    /**
     * Gravity flag for left on the {@code x}-axis
     */
    LEFT,

    /**
     * Gravity flag for right on the {@code x}-axis
     */
    RIGHT,

    /**
     * Gravity flag for vertical center (middle of {@linkplain #TOP top} and
     * {@linkplain #BOTTOM bottom) on the {@code y}-axis
     */
    CENTER_VERTICAL,

    /**
     * Gravity flag for horizontal center (middle of {@linkplain #LEFT left} and
     * {@linkplain #RIGHT right)) on the {@code x}-axis
     */
    CENTER_HORIZONTAL
  }

  /**
   * Gravity constant representing both {@linkplain Gravity#CENTER_VERTICAL vertical} and
   * {@linkplain Gravity#CENTER_HORIZONTAL horizontal} centers on the {@code x}- and {@code y}-axes
   */
  public static final ImmutableSet<Gravity> CENTER
          = Sets.immutableEnumSet(Gravity.CENTER_VERTICAL, Gravity.CENTER_HORIZONTAL);

  private static final ImmutableSet<Gravity> DEFAULT_GRAVITY
          = ImmutableSet.of(Gravity.TOP, Gravity.LEFT);

  @NonNull
  private final Collection<Widget> children;

  @IntRange(from = 0, to = Integer.MAX_VALUE)
  private int marginBottom;
  @IntRange(from = 0, to = Integer.MAX_VALUE)
  private int marginLeft;
  @IntRange(from = 0, to = Integer.MAX_VALUE)
  private int marginRight;
  @IntRange(from = 0, to = Integer.MAX_VALUE)
  private int marginTop;

  @Nullable
  private Set<Gravity> gravity;

  public WidgetGroup() {
    this.children = new ArrayList<Widget>();
  }

  @Override
  protected void draw(@NonNull final Batch batch) {
    super.draw(batch);
    drawChildren(batch);
  }

  @Override
  protected void drawDebug(@NonNull final Batch batch) {
    assert batch != null : "batch should not be null";
    final ShapeRenderer shapeRenderer = new ShapeRenderer();
    shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
    {
      shapeRenderer.setColor(Color.DARK_GRAY);
      shapeRenderer.rect(getX() + 1, getY() + 1, getWidth() - 1, getHeight() - 1);
      if (hasPadding()) {
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(getX() + getPaddingLeft() + 1, getY() + getPaddingBottom() + 1,
                getWidth() - getPaddingLeft() - getPaddingRight() - 1,
                getHeight() - getPaddingTop() - getPaddingBottom() - 1);
      }
    }
    shapeRenderer.end();

    System.out.printf("%s: %d %d %d %d%n",
            getClass().getSimpleName(),
            getX(), getY(), getWidth(), getHeight());
  }

  /**
   * Called when this {@code Widget} should draw its children content onto the passed {@link Batch}.
   *
   * @param batch {@code Batch} instance to render onto
   */
  protected void drawChildren(@NonNull final Batch batch) {
    assert batch != null : "batch should not be null";
    for (Widget child : this) {
      child.draw(batch);
    }
  }

  /**
   * Gravity of this {@code WidgetGroup}, controlling how children are aligned.
   *
   * @return Gravity of this {@code WidgetGroup}
   */
  @NonNull
  @CallSuper
  public ImmutableSet<Gravity> getGravity() {
    if (gravity == null) {
      return DEFAULT_GRAVITY;
    }

    return ImmutableSet.copyOf(gravity);
  }

  /**
   * Sets the gravity of this {@code WidgetGroup} which controls how child {@link Widget} instances
   * are aligned.
   *
   * @param gravity Gravity of this {@code WidgetGroup}
   */
  @CallSuper
  public void setGravity(@Nullable final Set<Gravity> gravity) {
    this.gravity = gravity;
  }

  /**
   * Sets the gravity of this {@code WidgetGroup} which controls how child {@link Widget} instances
   * are aligned.
   *
   * @param gravity   First gravity argument of this {@code WidgetGroup}
   * @param gravities Remaining gravity arguments of this {@code WidgetGroup}
   */
  @CallSuper
  public void setGravity(@NonNull final Gravity gravity, @NonNull final Gravity... gravities) {
    if (gravity == null) {
      throw new IllegalArgumentException("gravity cannot be null");
    }

    // EnumSet does not permit {@code null} elements, so no need to validate here
    this.gravity = EnumSet.of(gravity, gravities);
  }

  /**
   * Bottom margin of this {@code WidgetGroup}. Margin is defined as the space outside of this
   * {@code WidgetGroup}, which no other {@code Widget} may invade (i.e., marks the outside edge).
   *
   * @return Bottom margin, in pixels
   */
  @Override
  @CallSuper
  @IntRange(from = 0, to = Integer.MAX_VALUE)
  public int getMarginBottom() {
    return marginBottom;
  }

  /**
   * Sets the bottom margin of this {@code WidgetGroup}. Margin is defined as the space outside of
   * this {@code WidgetGroup}, which no other {@code Widget} may invade (i.e., marks the outside
   * edge).
   *
   * @param marginBottom Bottom margin, in pixels
   */
  @Override
  @CallSuper
  public void setMarginBottom(@IntRange(from = 0, to = Integer.MAX_VALUE) final int marginBottom) {
    if (marginBottom < 0) {
      throw new IllegalArgumentException("bottom margin should be positive");
    }

    this.marginBottom = marginBottom;
  }

  /**
   * Left margin of this {@code WidgetGroup}. Margin is defined as the space outside of this
   * {@code WidgetGroup}, which no other {@code Widget} may invade (i.e., marks the outside edge).
   *
   * @return Left margin, in pixels
   */
  @Override
  @CallSuper
  @IntRange(from = 0, to = Integer.MAX_VALUE)
  public int getMarginLeft() {
    return marginLeft;
  }

  /**
   * Sets the left margin of this {@code WidgetGroup}. Margin is defined as the space outside of
   * this {@code WidgetGroup}, which no other {@code Widget} may invade (i.e., marks the outside
   * edge).
   *
   * @param marginLeft Left margin, in pixels
   */
  @Override
  @CallSuper
  public void setMarginLeft(@IntRange(from = 0, to = Integer.MAX_VALUE) final int marginLeft) {
    if (marginLeft < 0) {
      throw new IllegalArgumentException("left margin should be positive");
    }

    this.marginLeft = marginLeft;
  }

  /**
   * Right margin of this {@code WidgetGroup}. Margin is defined as the space outside of this
   * {@code WidgetGroup}, which no other {@code Widget} may invade (i.e., marks the outside edge).
   *
   * @return Right margin, in pixels
   */
  @Override
  @CallSuper
  @IntRange(from = 0, to = Integer.MAX_VALUE)
  public int getMarginRight() {
    return marginRight;
  }

  /**
   * Sets the right margin of this {@code WidgetGroup}. Margin is defined as the space outside of
   * this {@code WidgetGroup}, which no other {@code Widget} may invade (i.e., marks the outside
   * edge).
   *
   * @param marginRight Right margin, in pixels
   */
  @Override
  @CallSuper
  public void setMarginRight(@IntRange(from = 0, to = Integer.MAX_VALUE) final int marginRight) {
    if (marginRight < 0) {
      throw new IllegalArgumentException("right margin should be positive");
    }

    this.marginRight = marginRight;
  }

  /**
   * Top margin of this {@code WidgetGroup}. Margin is defined as the space outside of this
   * {@code WidgetGroup}, which no other {@code Widget} may invade (i.e., marks the outside edge).
   *
   * @return Top margin, in pixels
   */
  @Override
  @CallSuper
  @IntRange(from = 0, to = Integer.MAX_VALUE)
  public int getMarginTop() {
    return marginTop;
  }

  /**
   * Sets the top margin of this {@code WidgetGroup}. Margin is defined as the space outside of
   * this {@code WidgetGroup}, which no other {@code Widget} may invade (i.e., marks the outside
   * edge).
   *
   * @param marginTop Top margin, in pixels
   */
  @Override
  @CallSuper
  public void setMarginTop(@IntRange(from = 0, to = Integer.MAX_VALUE) final int marginTop) {
    if (marginTop < 0) {
      throw new IllegalArgumentException("marginTop margin should be positive");
    }

    this.marginTop = marginTop;
  }

  /**
   * {@link Boundary} containing of the sizes of the margins of this {@code WidgetGroup}. Margin is
   * defined as the space outside of this {@code WidgetGroup}, which no other {@code Widget} may
   * invade (i.e., marks the outside edge).
   * <p>
   * Note: Changing the sides of the returned {@code Boundary} instance will not be reflected
   * within this {@code WidgetGroup}.
   * </p>
   *
   * @return {@code Boundary} containing the sizes of the margins of this {@code WidgetGroup}
   */
  @Override
  @CallSuper
  @NonNull
  public Boundary getMargin() {
    return new Boundary(getMarginLeft(), getMarginTop(), getMarginRight(), getMarginBottom());
  }

  /**
   * Populates the passed {@link Boundary} instance with the sizes of the margins of this
   * {@code WidgetGroup}. Margin is defined as the space outside of this {@code WidgetGroup}, which
   * no other {@code Widget} may invade (i.e., marks the outside edge).
   *
   * @param dst {@code Boundary} instance to populate, otherwise if a {@code null} reference is
   *            passed, then this method would behave the same as if {@link #getMargin} were
   *            called.
   * @return {@code Boundary} containing the sizes of the margins of this {@code WidgetGroup}
   */
  @NonNull
  @CallSuper
  @Override
  public Boundary getMargin(@Nullable final Boundary dst) {
    if (dst == null) {
      return getMargin();
    }

    dst.set(getMarginLeft(), getMarginTop(), getMarginRight(), getMarginBottom());
    return dst;
  }

  /**
   * Sets the margin on all sides of this {@code WidgetGroup}. Margin is defined as the space
   * outside of this {@code WidgetGroup}, which no other {@code Widget} may invade (i.e., marks the
   * outside edge).
   * <p>
   * Precondition: {@code marginLeft >= 0 AND marginRight >= 0 AND marginBottom >= 0
   * AND marginTop >= 0}
   * </p>
   *
   * @param marginLeft   Left margin, in pixels
   * @param marginTop    Top margin, in pixels
   * @param marginRight  Right margin, in pixels
   * @param marginBottom Bottom margin, in pixels
   */
  @Override
  @CallSuper
  public final void setMargin(@IntRange(from = 0, to = Integer.MAX_VALUE) final int marginLeft,
                              @IntRange(from = 0, to = Integer.MAX_VALUE) final int marginTop,
                              @IntRange(from = 0, to = Integer.MAX_VALUE) final int marginRight,
                              @IntRange(from = 0, to = Integer.MAX_VALUE) final int marginBottom) {
    setMarginLeft(marginLeft);
    setMarginTop(marginTop);
    setMarginRight(marginRight);
    setMarginBottom(marginBottom);
  }

  /**
   * Sets the margin on all sides of this {@code WidgetGroup} to those of the source
   * {@link Boundary}. Margin is defined as the space outside of this {@code WidgetGroup}, which no
   * other {@code Widget} may invade (i.e., marks the outside edge).
   * <p>
   * Precondition: {@code src.getLeft() >= 0 AND src.getRight() >= 0 AND src.getBottom() >= 0
   * AND src.getTop() >= 0}
   * </p>
   *
   * @param src {@code Boundary} to copy the margin onto this {@code WidgetGroup}
   */
  @Override
  @CallSuper
  public final void setMargin(@NonNull final Boundary src) {
    if (src == null) {
      throw new IllegalArgumentException("src margin cannot be null");
    }

    setMargin(src.getLeft(), src.getTop(), src.getRight(), src.getBottom());
  }

  /**
   * Sets the margin on all sides of this {@code WidgetGroup} to the specified value. This method
   * would be the same as calling {@link #setMargin(int, int, int, int)} with all the same
   * parameter. Margin is defined as the space outside of this {@code WidgetGroup}, which no
   * other {@code Widget} may invade (i.e., marks the outside edge).
   *
   * @param margin Margin, in pixels
   */
  @Override
  public final void setMargin(@IntRange(from = 0, to = Integer.MAX_VALUE) final int margin) {
    setMargin(margin, margin, margin, margin);
  }

  /**
   * Checks whether or not at least one side of this {@code WidgetGroup} has a positive margin
   * value. Margin is defined as the space outside of this {@code WidgetGroup}, which no
   * other {@code Widget} may invade (i.e., marks the outside edge).
   *
   * @return {@code true} if at least one side of this {@code WidgetGroup} has a positive margin
   * value, otherwise {@code false}
   */
  @Override
  @CallSuper
  public boolean hasMargin() {
    return getMarginLeft() > 0 || getMarginRight() > 0
            || getMarginBottom() > 0 || getMarginTop() > 0;
  }

  /**
   * Immutable {@link Iterator} which iterates through each child {@link Widget} belonging to this
   * {@code WidgetGroup}.
   *
   * @return Immutable {@code Iterator} which iterates through each child {@link Widget}
   */
  @Override
  @NonNull
  public Iterator<Widget> iterator() {
    return Iterators.unmodifiableIterator(children.iterator());
  }

  /**
   * Adds the given {@link Widget} to this {@code WidgetGroup}, setting the {@linkplain #getParent
   * parent} of that {@code Widget} to this and removing it from its current parent (if any).
   *
   * @param widget {@code Widget} to add to this {@code WidgetGroup} container
   */
  @Override
  @CallSuper
  public void addWidget(@NonNull final Widget widget) {
    if (widget == null) {
      throw new IllegalArgumentException("child widget cannot be null");
    }

    children.add(widget);
    widget.setParent(this);
    requestLayout();
  }

  /**
   * Checks whether or not the given {@link Widget} belongs to this {@code WidgetGroup}.
   *
   * @param widget {@code Widget} to check
   * @return {@code true} if the given {@link Widget} belongs to this {@code WidgetGroup},
   * otherwise {@code false}
   */
  @Override
  @CallSuper
  public boolean containsWidget(@Nullable Widget widget) {
    assert !children.contains(widget) || widget.getParent() == this;
    return widget != null && children.contains(widget);
  }

  /**
   * Removes the given {@link Widget} from this {@code WidgetGroup} if it belongs to it.
   *
   * @param widget {@code Widget} to remove from this {@code WidgetGroup} container
   * @return {@code true} if the {@code Widget} was successfully removed, otherwise {@code false}
   */
  @Override
  @CallSuper
  public boolean removeWidget(@Nullable Widget widget) {
    if (widget == null) {
      return false;
    } else if (widget.getParent() != this) {
      assert !containsWidget(widget)
              : "widget parent is not this WidgetGroup, so this WidgetGroup should not contain it";
      return false;
    }

    widget.setParent(null);
    boolean removed = children.remove(widget);
    requestLayout();
    assert removed : "widget parent was this WidgetGroup but was not a child";
    return removed;
  }

  /**
   * Total number of {@link Widget} instances contained by this {@code WidgetGroup}.
   *
   * @return Number of child {@code Widget} instances contained by this {@code WidgetGroup}
   */
  @Override
  @CallSuper
  public int getNumWidgets() {
    return children.size();
  }

  /**
   * Immutable view of all children {@link Widget} instances belonging to this {@code WidgetGroup}
   * container.
   *
   * @return Immutable view of all children {@link Widget} instances
   */
  @NonNull
  @CallSuper
  @Override
  public Collection<Widget> getChildren() {
    return Collections.unmodifiableCollection(children);
  }

  @Override
  public void dispose() {
    for (Widget child : this) {
      child.dispose();
    }
  }

  @Override
  public final void requestLayout() {
    System.out.println("WidgetGroup#requestLayout();");
    layout();
  }

  public void layout() {
    double layout_width, layout_height;
    for (Widget child : this) {
      layout_width = LengthUnit.parse(child.get(LayoutParams.layout_width).toString());
      layout_height = LengthUnit.parse(child.get(LayoutParams.layout_height).toString());
      if (layout_width == LengthUnit.MATCH_PARENT) {
        child.setLeft(getPaddingLeft());
        child.setRight(getWidth() - getPaddingRight());
      } else if (layout_width > 0) {
        //System.out.println("layout_width = " + LengthUnit.toPixels(layout_width));
        child.setRight(child.getLeft() + (int) layout_width);
      }

      if (layout_height == LengthUnit.MATCH_PARENT) {
        child.setBottom(getPaddingBottom());
        child.setTop(getHeight() - getPaddingTop());
      } else if (layout_height > 0) {
        //System.out.println("layout_height = " + LengthUnit.toPixels(layout_height));
        child.setBottom(child.getTop() - (int) layout_height);
      }
    }

    layoutChildren();

    for (Widget child : this) {
      layout_width = LengthUnit.parse(child.get(LayoutParams.layout_width).toString());
      layout_height = LengthUnit.parse(child.get(LayoutParams.layout_height).toString());
      if (layout_width == LengthUnit.WRAP_CONTENT) {
        child.setRight(child.getLeft() + Math.max(child.getPreferredWidth(), child.getMinWidth())
                + child.getPaddingLeft() + child.getPaddingRight());
      }

      if (layout_height == LengthUnit.WRAP_CONTENT) {
        child.setBottom(child.getTop() - Math.max(child.getPreferredHeight(), child.getMinHeight())
                - child.getPaddingTop() - child.getPaddingBottom());
      }
    }
  }

  public void layoutChildren() {
    for (Widget child : this) {
      if (child instanceof WidgetGroup) {
        ((WidgetGroup) child).layout();
      }
    }
  }

}
