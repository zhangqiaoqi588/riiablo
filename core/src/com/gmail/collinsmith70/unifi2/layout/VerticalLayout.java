package com.gmail.collinsmith70.unifi2.layout;

import com.gmail.collinsmith70.unifi2.WidgetGroup;
import com.gmail.collinsmith70.unifi2.Widget;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import java.util.Set;

/**
 * A {@link com.gmail.collinsmith70.unifi.layout.LinearLayout} which lays out child widgets in a vertical line. Supports {@code Widget}
 * instances with top and bottom {@linkplain Widget#getMargins() margins}.
 */
public class VerticalLayout extends com.gmail.collinsmith70.unifi.layout.LinearLayout {

  @Override
  public void layoutChildren() {
    super.layoutChildren();
    final int spacing = getSpacing();
    final Set<WidgetGroup.Gravity> gravity = getGravity();
    final Direction direction = getDirection();
    int childrenHeight = -spacing;
    int preferredWidth = 0;
    int gap = Integer.MIN_VALUE;
    PeekingIterator<Widget> peekingIterator = Iterators.peekingIterator(iterator());
    while (peekingIterator.hasNext()) {
      Widget child = peekingIterator.next();
      if (gravity.contains(WidgetGroup.Gravity.CENTER_HORIZONTAL)) {
        child.translateHorizontalCenter((getWidth() - getPaddingLeft() - getPaddingRight()) / 2);
      } else if (gravity.contains(WidgetGroup.Gravity.RIGHT)) {
        child.translateRight(getWidth() - getPaddingRight());
      } else {
        child.translateLeft(getPaddingLeft());
      }

      gap = Math.max(spacing, child.getMarginBottom());
      if (peekingIterator.hasNext()) {
        gap = Math.max(gap, peekingIterator.peek().getMarginTop());
      }

      childrenHeight += child.getHeight() + gap;
      preferredWidth = Math.max(preferredWidth, child.getWidth());

    }

    if (gap != Integer.MIN_VALUE) {
      childrenHeight -= gap;
    }

    setPreferredWidth(preferredWidth);
    setPreferredHeight(childrenHeight);

    int offset = 0;
    peekingIterator = Iterators.peekingIterator(iterator());
    while (peekingIterator.hasNext()) {
      Widget child = peekingIterator.next();
      switch (direction) {
        case START_TO_END:
          child.translateTop(getHeight() - getPaddingTop() - offset);
          break;
        case END_TO_START:
          child.translateBottom(getHeight() - childrenHeight + offset);
          break;
        default:
      }

      gap = Math.max(spacing, child.getMarginBottom());
      if (peekingIterator.hasNext()) {
        gap = Math.max(gap, peekingIterator.peek().getMarginTop());
      }


      offset += child.getHeight() + gap;

    }

    if (gravity.contains(WidgetGroup.Gravity.TOP)) {
      return;
    }

    if (gravity.contains(WidgetGroup.Gravity.CENTER_VERTICAL)) {
      final int shift = getHeight() / 2 - childrenHeight / 2;
      for (Widget child : this) {
        child.translateTop(child.getTop() - shift);
      }
    } else if (gravity.contains(WidgetGroup.Gravity.BOTTOM)) {
      final int shift = getHeight() - getPaddingTop() - childrenHeight;
      for (Widget child : this) {
        child.translateBottom(child.getBottom() + shift);
      }
    }
  }

}