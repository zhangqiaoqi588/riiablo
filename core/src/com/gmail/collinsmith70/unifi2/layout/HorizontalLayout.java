package com.gmail.collinsmith70.unifi2.layout;

import com.gmail.collinsmith70.unifi2.WidgetGroup;
import com.gmail.collinsmith70.unifi2.Widget;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import java.util.Set;

/**
 * A {@link com.gmail.collinsmith70.unifi.layout.LinearLayout} which lays out child widgets in a horizontal line. Supports {@code Widget}
 * instances with left and right {@linkplain Widget#getMargins() margins}.
 */
public class HorizontalLayout extends com.gmail.collinsmith70.unifi.layout.LinearLayout {

  @Override
  public void layoutChildren() {
    super.layoutChildren();
    final int spacing = getSpacing();
    final Set<WidgetGroup.Gravity> gravity = getGravity();
    final Direction direction = getDirection();
    int childrenWidth = 0;
    int preferredHeight = 0;
    int gap = Integer.MIN_VALUE;
    PeekingIterator<Widget> peekingIterator = Iterators.peekingIterator(iterator());
    while (peekingIterator.hasNext()) {
      Widget child = peekingIterator.next();
      if (gravity.contains(WidgetGroup.Gravity.CENTER_VERTICAL)) {
        child.translateVerticalCenter((getHeight() - getPaddingTop() - getPaddingBottom()) / 2);
      } else if (gravity.contains(WidgetGroup.Gravity.BOTTOM)) {
        child.translateBottom(getPaddingBottom());
      } else {
        child.translateTop(getHeight() - getPaddingTop());
      }

      gap = Math.max(spacing, child.getMarginRight());
      if (peekingIterator.hasNext()) {
        gap = Math.max(gap, peekingIterator.peek().getMarginLeft());
      }

      childrenWidth += child.getWidth() + gap;
      preferredHeight = Math.max(preferredHeight, child.getHeight());

    }

    if (gap != Integer.MIN_VALUE) {
      childrenWidth -= gap;
    }

    setPreferredWidth(childrenWidth);
    setPreferredHeight(preferredHeight);

    int offset = 0;
    peekingIterator = Iterators.peekingIterator(iterator());
    while (peekingIterator.hasNext()) {
      Widget child = peekingIterator.next();
      switch (direction) {
        case START_TO_END:
          child.translateLeft(getPaddingLeft() + offset);
          break;
        case END_TO_START:
          child.translateRight(getPaddingLeft() + childrenWidth - offset);
          break;
        default:
      }

      gap = Math.max(spacing, child.getMarginRight());
      if (peekingIterator.hasNext()) {
        gap = Math.max(gap, peekingIterator.peek().getMarginLeft());
      }

      offset += child.getWidth() + gap;

    }

    if (gravity.contains(WidgetGroup.Gravity.LEFT)) {
      return;
    }

    if (gravity.contains(WidgetGroup.Gravity.CENTER_HORIZONTAL)) {
      final int shift = getWidth() / 2 - childrenWidth / 2;
      for (Widget child : this) {
        child.translateLeft(child.getLeft() + shift);
      }
    } else if (gravity.contains(WidgetGroup.Gravity.RIGHT)) {
      final int shift = getWidth() - getPaddingLeft() - childrenWidth;
      for (Widget child : this) {
        child.translateRight(child.getRight() + shift);
      }
    }
  }

}