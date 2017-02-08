package eu.fiskur.chipcloud;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * @author RAW
 */
public abstract class FlowLayout extends ViewGroup {

  private int line_height;
  private LayoutProcessor layoutProcessor = new LayoutProcessor();

  enum Gravity {
    LEFT, RIGHT, CENTER, STAGGERED
  }

  public static class LayoutParams extends ViewGroup.LayoutParams {

    public final int horizontal_spacing;
    public final int vertical_spacing;

    /**
     * @param horizontal_spacing Pixels between items, horizontally
     * @param vertical_spacing Pixels between items, vertically
     */
    public LayoutParams(int horizontal_spacing, int vertical_spacing) {
      super(0, 0);
      this.horizontal_spacing = horizontal_spacing;
      this.vertical_spacing = vertical_spacing;
    }
  }

  public FlowLayout(Context context) {
    super(context);
  }

  public FlowLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    assert (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED);

    final int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
    int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
    final int count = getChildCount();
    int line_height = 0;

    int xpos = getPaddingLeft();
    int ypos = getPaddingTop();

    int childHeightMeasureSpec;
    if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
      childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
    } else {
      childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    }

    for (int i = 0; i < count; i++) {
      final View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
            childHeightMeasureSpec);
        final int childw = child.getMeasuredWidth();
        line_height = Math.max(line_height, child.getMeasuredHeight() + lp.vertical_spacing);

        if (xpos + childw > width) {
          xpos = getPaddingLeft();
          ypos += line_height;
        }

        xpos += childw + lp.horizontal_spacing;
      }
    }
    this.line_height = line_height;

    if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED || (MeasureSpec.getMode(
        heightMeasureSpec) == MeasureSpec.AT_MOST && ypos + line_height < height)) {
      height = ypos + line_height;
    }
    setMeasuredDimension(width, height);
  }

  @Override protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(dpToPx(7), dpToPx(7));
  }

  public static int dpToPx(int dp) {
    return (int) (dp * Resources.getSystem().getDisplayMetrics().density + 0.5);
  }

  @Override protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof LayoutParams;
  }

  @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
    final int count = getChildCount();
    final int width = r - l;
    int xpos = getPaddingLeft();
    int ypos = getPaddingTop();
    layoutProcessor.setWidth(width);
    for (int i = 0; i < count; i++) {
      final View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        final int childw = child.getMeasuredWidth();
        final int childh = child.getMeasuredHeight();
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (xpos + childw > width) {
          xpos = getPaddingLeft();
          ypos += line_height;
          layoutProcessor.layoutPreviousRow();
        }
        layoutProcessor.addViewForLayout(child, ypos, childw, childh);
        xpos += childw + lp.horizontal_spacing;
      }
    }
    layoutProcessor.layoutPreviousRow();
    layoutProcessor.clear();
  }

  protected abstract Gravity getGravity();

  private class LayoutProcessor {

    private int rowY;
    private final List<View> viewsInCurrentRow;
    private final List<Integer> viewWidths;
    private final List<Integer> viewHeights;
    private Integer horizontalSpacing;
    private int width;

    private LayoutProcessor() {
      viewsInCurrentRow = new ArrayList<>();
      viewWidths = new ArrayList<>();
      viewHeights = new ArrayList<>();
    }

    void addViewForLayout(View view, int yPos, int childW, int childH) {
      rowY = yPos;
      viewsInCurrentRow.add(view);
      viewWidths.add(childW);
      viewHeights.add(childH);
      if (horizontalSpacing == null) {
        horizontalSpacing = ((LayoutParams) view.getLayoutParams()).horizontal_spacing;
      }
    }

    void clear() {
      viewsInCurrentRow.clear();
      viewWidths.clear();
      viewHeights.clear();
    }

    void layoutPreviousRow() {
      Gravity gravity = getGravity();
      switch (gravity) {
        case LEFT:
          int xPos = getPaddingLeft();
          for (int i = 0; i < viewsInCurrentRow.size(); i++) {
            viewsInCurrentRow.get(i).layout(xPos, rowY, xPos + viewWidths.get(i), rowY + viewHeights.get(i));
            xPos += viewWidths.get(i) + horizontalSpacing;
          }
          break;
        case RIGHT:
          int xEnd = width - getPaddingRight();
          for (int i = viewsInCurrentRow.size() - 1; i >= 0; i--) {
            int xStart = xEnd - viewWidths.get(i);
            viewsInCurrentRow.get(i).layout(xStart, rowY, xEnd, rowY + viewHeights.get(i));
            xEnd = xStart - horizontalSpacing;
          }
          break;
        case STAGGERED:
          int totalWidthOfChildren = 0;
          for (int i = 0; i < viewWidths.size(); i++) {
            totalWidthOfChildren += viewWidths.get(i);
          }
          int horizontalSpacingForStaggered = (width - totalWidthOfChildren - getPaddingLeft()
                  - getPaddingRight()) / (viewsInCurrentRow.size() + 1);
          xPos = getPaddingLeft() + horizontalSpacingForStaggered;
          for (int i = 0; i < viewsInCurrentRow.size(); i++) {
            viewsInCurrentRow.get(i).layout(xPos, rowY, xPos + viewWidths.get(i), rowY + viewHeights.get(i));
            xPos += viewWidths.get(i) + horizontalSpacingForStaggered;
          }
          break;
        case CENTER:
          totalWidthOfChildren = 0;
          for (int i = 0; i < viewWidths.size(); i++) {
            totalWidthOfChildren += viewWidths.get(i);
          }
          xPos = getPaddingLeft() + (width - getPaddingLeft() - getPaddingRight() -
                  totalWidthOfChildren - (horizontalSpacing * (viewsInCurrentRow.size() - 1))) / 2;
          for (int i = 0; i < viewsInCurrentRow.size(); i++) {
            viewsInCurrentRow.get(i).layout(xPos, rowY, xPos + viewWidths.get(i), rowY + viewHeights.get(i));
            xPos += viewWidths.get(i) + horizontalSpacing;
          }
          break;
      }
      clear();
    }

    void setWidth(int width) {
      this.width = width;
    }
  }
}
