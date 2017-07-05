package com.github.starcats.blinkydome.pattern;

import com.github.starcats.blinkydome.color.ColorMappingSource;
import heronarts.lx.LX;
import heronarts.lx.LXPattern;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXFixture;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.p3lx.ui.UITriggerTarget;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Sends bars of colors up a fixture
 */
public class FixtureColorBarsPattern extends LXPattern implements UITriggerTarget {

  /** Public Compound Modulation Sink: Input into {@link ColorMappingSource} to pick color of current bar */
  public final CompoundParameter colorSourceI;

  /** Public Compound Modulation Sink: What percentage of the fixture is covered by color bars (remainder will be black */
  public final CompoundParameter visibleRange;

//  /** Public Compound Modulation Sink: When this crosses 0.95, triggers a new color bar to appear */
//  public final CompoundParameter newBarModulationSink;


  private LXFixture[] fixtures;
  private ColorMappingSource colorSource;


  /** How long it takes a color bar to travel entire length of fixture (100% of LEDs) */
  private final BoundedParameter barPeriodMs;

  /** Internal representation of color bars */
  private final LinkedList<ColorBar> colorBars;

  private final BooleanParameter newBarTrigger;

  public FixtureColorBarsPattern(LX lx, LXFixture[] fixtures, ColorMappingSource colorSource)
  {
    super(lx);
    this.fixtures = fixtures;
    this.colorSource = colorSource;

    this.colorBars = new LinkedList<>();

    this.barPeriodMs = new BoundedParameter("period", 2000, 100, 5000);
    this.barPeriodMs
        .setDescription("Period (ms) that a color bar takes to travel 100% length of fixture")
        .setUnits(LXParameter.Units.MILLISECONDS);
    this.addParameter(this.barPeriodMs);

    this.colorSourceI = new CompoundParameter("color", 0);
    this.colorSourceI.setDescription("Choose a color from the color source");
    this.addParameter(this.colorSourceI);

    this.visibleRange = new CompoundParameter("range", 1);
    this.visibleRange.setDescription("What percentage of the fixture(s)'s length will have color bars travelling on");
    this.addParameter(visibleRange);

    this.newBarTrigger = new BooleanParameter("new bar", false);
    this.newBarTrigger
    .setDescription("Press to trigger a new bar")
    .setMode(BooleanParameter.Mode.MOMENTARY)
    .addListener(parameter -> this.triggerNewBar());
    this.addParameter(newBarTrigger);
  }

  private static class ColorBar {
    /** Color position from a {@link ColorMappingSource} */
    final double colorPos;

    final ColorMappingSource colorSource;

    /** color picked from a {@link ColorMappingSource} */
    int color;

    /** Normalized ending value of the color bar.  Gets incremented every frame. */
    double endPos;

    ColorBar(ColorMappingSource colorSource, double colorPos, double endPos) {
      this.colorSource = colorSource;
      this.colorPos = colorPos;
      this.endPos = endPos;

      this.refreshColor();
    }

    public void refreshColor() {
      this.color = colorSource.getColor(colorPos);
    }
  }

  private void triggerNewBar() {
    colorBars.addFirst(
        new ColorBar(this.colorSource, this.colorSourceI.getNormalized(), 0d)
    );
  }

  @Override
  protected void run(double deltaMs) {
    // First, refresh the colors from the color bars incase underlying color source changed palettes
    this.colorBars.forEach(ColorBar::refreshColor);


    for (LXFixture fixture : fixtures) {

      Iterator<ColorBar> colorBarsIter = this.colorBars.iterator();

      // Paint it black and next fixture
      if (!colorBarsIter.hasNext()) {
        setColor(fixture, LXColor.BLACK);
        continue;
      }

      ColorBar currentColor = colorBarsIter.next();

      int i=0;
      while (i < fixture.getPoints().size()) {
        double currentPos = (double) i / fixture.getPoints().size();
        LXPoint pt = fixture.getPoints().get(i);

        if (currentColor == null || currentPos > this.visibleRange.getNormalized()) {
          setColor(pt.index, LXColor.BLACK);
          i++;

        } else if (currentPos <= currentColor.endPos) {
          setColor(pt.index, currentColor.color);
          i++;

        // Past the current color position: advance to next color, but don't increment LED i
        } else {
          currentColor = colorBarsIter.hasNext() ? colorBarsIter.next() : null;
        }
      }
    }

    double posDelta = deltaMs / this.barPeriodMs.getValue();
    Iterator<ColorBar> colorBarIter = colorBars.iterator();
    while (colorBarIter.hasNext()) {
      ColorBar color = colorBarIter.next();

      color.endPos += posDelta;
      if (color.endPos > 1) {
        // empty out all bars after this one -- they'll never be shown
        while (colorBarIter.hasNext()) {
          colorBarIter.next();
          colorBarIter.remove();
        }
      }
    }
  }

  private static int getPointI(LXFixture fixture, double normalizedValue) {
    return (int) (normalizedValue * fixture.getPoints().size());
  }

  @Override
  public BooleanParameter getTriggerTarget() {
    return this.newBarTrigger;
  }
}
