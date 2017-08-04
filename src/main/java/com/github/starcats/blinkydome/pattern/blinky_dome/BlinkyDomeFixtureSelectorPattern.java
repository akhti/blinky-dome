package com.github.starcats.blinkydome.pattern.blinky_dome;

import com.github.starcats.blinkydome.model.blinky_dome.BlinkyModel;
import com.github.starcats.blinkydome.model.blinky_dome.BlinkyTriangle;
import com.github.starcats.blinkydome.pattern.AbstractFixtureSelectorPattern;
import heronarts.lx.LX;
import heronarts.lx.model.LXFixture;
import heronarts.lx.parameter.EnumParameter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Debugging pattern to select specific triangles (or parts of triangles) in blinky-dome
 */
public class BlinkyDomeFixtureSelectorPattern
    extends AbstractFixtureSelectorPattern<BlinkyModel, BlinkyDomeFixtureSelectorPattern.BlinkyDomeFixtureType>
{
  public final EnumParameter<BlinkyDomeSubfixtureType> subfixtureParam;

  public enum BlinkyDomeFixtureType {
    GROUP,
    TRIANGLE,
    ALL
  }

  /** Which leg of the triangle to use*/
  public enum BlinkyDomeSubfixtureType {
    ALL,
    X,
    Y,
    Z
  }

  public BlinkyDomeFixtureSelectorPattern(LX lx, BlinkyModel model) {
    super(lx, model);

    subfixtureParam = new EnumParameter<>("Triangle leg", BlinkyDomeSubfixtureType.ALL)
        .setDescription("Select which leg of the triangle to show for selected fixtures");
    subfixtureParam.addListener(param -> updateFixtures());
    addParameter(subfixtureParam);
  }

  @Override
  protected EnumParameter<BlinkyDomeFixtureType> makeFixtureFamilyParameter() {
    return new EnumParameter<>("class", BlinkyDomeFixtureType.GROUP);
  }

  @Override
  protected Object[] getFixtureKeysForFamily(BlinkyDomeFixtureType fixtureFamily) {
    Set<Integer> keys;
    if (fixtureFamily == BlinkyDomeFixtureType.GROUP) {
      keys = model.getTriangleGroupsKeys();
    } else if (fixtureFamily == BlinkyDomeFixtureType.TRIANGLE) {
      keys = new HashSet<>();
      for (int i=0; i<model.allTriangles.size(); i++) {
        keys.add(i);
      }
    } else if (fixtureFamily == BlinkyDomeFixtureType.ALL) {
      return new Integer[] {0};

    } else {
      throw new RuntimeException("Unsupported fixture type: " + fixtureFamily);
    }

    return keys.toArray();
  }

  @Override
  protected List<? extends LXFixture> getFixturesByKey(BlinkyDomeFixtureType fixtureFamily, Object keyObj) {
    Integer key = (Integer) keyObj;

    List<BlinkyTriangle> fixtures;

    if (fixtureFamily == BlinkyDomeFixtureType.GROUP) {
      fixtures = model.getTrianglesByGroup(key);

    } else if (fixtureFamily == BlinkyDomeFixtureType.TRIANGLE) {
      BlinkyTriangle triangle = model.allTriangles.get(key);
      System.out.println("Selected triangle: " + triangle.toString());
      fixtures = Collections.singletonList(triangle);
    } else if (fixtureFamily == BlinkyDomeFixtureType.ALL) {
      fixtures = model.allTriangles;

    } else {
      throw new RuntimeException("Unsupported fixture type: " + fixtureFamily);
    }


    // Subfixture filter:
    if (subfixtureParam == null) {
      return fixtures;
    } else if (subfixtureParam.getEnum() == BlinkyDomeSubfixtureType.X) {
      return fixtures.stream().map(f -> f.sX).collect(Collectors.toList());
    } else if (subfixtureParam.getEnum() == BlinkyDomeSubfixtureType.Y) {
      return fixtures.stream().map(f -> f.sY).collect(Collectors.toList());
    } else if (subfixtureParam.getEnum() == BlinkyDomeSubfixtureType.Z) {
      return fixtures.stream().map(f -> f.sZ).collect(Collectors.toList());
    }

    return fixtures;
  }


}
