/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.measures;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.server.ServerSide;

@BatchSide
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ServerSide
public class Metric<G extends Serializable> implements Serializable, org.sonar.api.batch.measure.Metric<G> {

  /**
   * A metric bigger value means a degradation
   */
  public static final int DIRECTION_WORST = -1;
  /**
   * A metric bigger value means an improvement
   */
  public static final int DIRECTION_BETTER = 1;
  /**
   * The metric direction has no meaning
   */
  public static final int DIRECTION_NONE = 0;

  public enum ValueType {
    INT(Integer.class),
    FLOAT(Double.class),
    PERCENT(Double.class),
    BOOL(Boolean.class),
    STRING(String.class),
    MILLISEC(Long.class),
    DATA(String.class),
    LEVEL(Metric.Level.class),
    DISTRIB(String.class),
    RATING(Integer.class),
    WORK_DUR(Long.class);

    private final Class valueClass;

    ValueType(Class valueClass) {
      this.valueClass = valueClass;
    }

    private Class valueType() {
      return valueClass;
    }

    public static String[] names() {
      ValueType[] values = values();
      String[] names = new String[values.length];
      for (int i = 0; i < values.length; i += 1) {
        names[i] = values[i].name();
      }

      return names;
    }
  }

  public enum Level {
    OK("Green"), WARN("Orange"), ERROR("Red");

    private String colorName;

    Level(String colorName) {
      this.colorName = colorName;
    }

    public String getColorName() {
      return colorName;
    }

    public static List<String> names() {
      return Lists.transform(Arrays.asList(values()), new Function<Level, String>() {
        @Nonnull
        @Override
        public String apply(@Nonnull Level level) {
          return level.name();
        }
      });
    }
  }

  private Integer id;
  private transient Formula formula;
  private String key;
  private String description;
  private ValueType type;
  private Integer direction;
  private String domain;
  private String name;
  private Boolean qualitative = Boolean.FALSE;
  private Boolean userManaged = Boolean.FALSE;
  private Boolean enabled = Boolean.TRUE;
  private Double worstValue;
  private Double bestValue;
  private Boolean optimizedBestValue;
  private Boolean hidden = Boolean.FALSE;
  private Boolean deleteHistoricalData;

  private Metric(Builder builder) {
    this.key = builder.key;
    this.name = builder.name;
    this.description = builder.description;
    this.type = builder.type;
    this.direction = builder.direction;
    this.domain = builder.domain;
    this.qualitative = builder.qualitative;
    this.enabled = Boolean.TRUE;
    this.worstValue = builder.worstValue;
    this.optimizedBestValue = builder.optimizedBestValue;
    this.bestValue = builder.bestValue;
    this.hidden = builder.hidden;
    this.formula = builder.formula;
    this.userManaged = builder.userManaged;
    this.deleteHistoricalData = builder.deleteHistoricalData;
  }

  /**
   * Creates an empty metric
   *
   * @deprecated in 1.12. Use the {@link Builder} factory.
   */
  @Deprecated
  public Metric() {
  }

  /**
   * Creates a metric based on its key. Shortcut to Metric(key, ValueType.INT)
   *
   * @param key the metric key
   * @deprecated since 2.7 use the {@link Builder} factory.
   */
  @Deprecated
  public Metric(String key) {
    this(key, ValueType.INT);
  }

  /**
   * Creates a metric based on a key and a type. Shortcut to
   * Metric(key, key, key, type, -1, Boolean.FALSE, null, false)
   *
   * @param key  the key
   * @param type the type
   * @deprecated since 2.7 use the {@link Builder} factory.
   */
  @Deprecated
  public Metric(String key, ValueType type) {
    this(key, key, key, type, -1, Boolean.FALSE, null, false);
  }

  /**
   * @deprecated since 2.7 use the {@link Builder} factory.
   */
  @Deprecated
  public Metric(String key, String name, String description, ValueType type, Integer direction, Boolean qualitative, String domain) {
    this(key, name, description, type, direction, qualitative, domain, false);
  }

  /**
   * Creates a fully qualified metric.
   *
   * @param key         the metric key
   * @param name        the metric name
   * @param description the metric description
   * @param type        the metric type
   * @param direction   the metric direction
   * @param qualitative whether the metric is qualitative
   * @param domain      the metric domain
   * @param userManaged whether the metric is user managed
   */
  private Metric(String key, String name, String description, ValueType type, Integer direction, Boolean qualitative, @Nullable String domain,
    boolean userManaged) {
    this.key = key;
    this.description = description;
    this.type = type;
    this.direction = direction;
    this.domain = domain;
    this.name = name;
    this.qualitative = qualitative;
    this.userManaged = userManaged;
    if (ValueType.PERCENT.equals(this.type)) {
      this.bestValue = (direction == DIRECTION_BETTER) ? 100.0 : 0.0;
      this.worstValue = (direction == DIRECTION_BETTER) ? 0.0 : 100.0;
    }
  }

  /**
   * For internal use only
   */
  public Integer getId() {
    return id;
  }

  /**
   * For internal use only
   */
  public Metric<G> setId(@Nullable Integer id) {
    this.id = id;
    return this;
  }

  /**
   * @return the metric formula
   * @deprecated since 5.2 there's no more decorator on batch side
   * TODO add link to new API
   */
  @Deprecated
  public Formula getFormula() {
    return formula;
  }

  /**
   * Sets the metric formula
   *
   * @param formula the formula
   * @return this
   * @deprecated since 5.2 there's no more decorator on batch side
   * TODO add link to new API
   */
  @Deprecated
  public Metric<G> setFormula(Formula formula) {
    this.formula = formula;
    return this;
  }

  /**
   * @return wether the metric is qualitative
   */
  public Boolean getQualitative() {
    return qualitative;
  }

  /**
   * Sets whether the metric is qualitative
   *
   * @param qualitative whether the metric is qualitative
   * @return this
   */
  public Metric<G> setQualitative(Boolean qualitative) {
    this.qualitative = qualitative;
    return this;
  }

  /**
   * @return the metric key
   */
  public String getKey() {
    return key;
  }

  /**
   * Sets the metric key
   *
   * @param key the key
   * @return this
   */
  public Metric<G> setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * @return the metric type
   */
  public ValueType getType() {
    return type;
  }

  /**
   * Sets the metric type
   *
   * @param type the type
   * @return this
   */
  public Metric<G> setType(ValueType type) {
    this.type = type;
    return this;
  }

  /**
   * @return the metric description
   */
  @CheckForNull
  public String getDescription() {
    return description;
  }

  /**
   * Sets the metric description
   *
   * @param description the description
   * @return this
   */
  public Metric<G> setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * @return whether the metric is a managed by the users ("manual metric")
   */
  public Boolean getUserManaged() {
    return userManaged;
  }

  /**
   * Sets whether the metric is managed by users ("manual metric")
   *
   * @param userManaged whether the metric is user managed
   * @return this
   */
  public Metric<G> setUserManaged(Boolean userManaged) {
    this.userManaged = userManaged;
    return this;
  }

  /**
   * @return whether the metric is enabled
   */
  public Boolean getEnabled() {
    return enabled;
  }

  /**
   * Sets whether the metric is enabled
   *
   * @param enabled whether the metric is enabled
   * @return this
   */
  public Metric<G> setEnabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * @return the metric direction
   */
  public Integer getDirection() {
    return direction;
  }

  /**
   * Sets the metric direction.
   *
   * @param direction the direction
   */
  public Metric<G> setDirection(Integer direction) {
    this.direction = direction;
    return this;
  }

  /**
   * @return the domain of the metric
   */
  public String getDomain() {
    return domain;
  }

  /**
   * Sets the domain for the metric (General, Complexity...)
   *
   * @param domain the domain
   * @return this
   */
  public Metric<G> setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  /**
   * @return the metric name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the metric name
   *
   * @param name the name
   * @return this
   */
  public Metric<G> setName(String name) {
    this.name = name;
    return this;
  }

  public Double getWorstValue() {
    return worstValue;
  }

  @CheckForNull
  public Double getBestValue() {
    return bestValue;
  }

  /**
   * @return this
   */
  public Metric<G> setWorstValue(@Nullable Double d) {
    this.worstValue = d;
    return this;
  }

  /**
   * @param bestValue the best value. It can be null.
   * @return this
   */
  public Metric<G> setBestValue(@Nullable Double bestValue) {
    this.bestValue = bestValue;
    return this;
  }

  /**
   * @return whether the metric is of a numeric type (int, percentage...)
   */
  public boolean isNumericType() {
    return ValueType.INT.equals(type)
      || ValueType.FLOAT.equals(type)
      || ValueType.PERCENT.equals(type)
      || ValueType.BOOL.equals(type)
      || ValueType.MILLISEC.equals(type)
      || ValueType.RATING.equals(type)
      || ValueType.WORK_DUR.equals(type);
  }

  /**
   * @return whether the metric is of type data
   */
  public boolean isDataType() {
    return ValueType.DATA.equals(type) || ValueType.DISTRIB.equals(type);
  }

  /**
   * @return whether the metric is of type percentage
   */
  public boolean isPercentageType() {
    return ValueType.PERCENT.equals(type);
  }

  public Metric<G> setOptimizedBestValue(@Nullable Boolean b) {
    this.optimizedBestValue = b;
    return this;
  }

  /**
   * @return null for manual metrics
   */
  @CheckForNull
  public Boolean isOptimizedBestValue() {
    return optimizedBestValue;
  }

  public Boolean isHidden() {
    return hidden;
  }

  public Metric<G> setHidden(Boolean hidden) {
    this.hidden = hidden;
    return this;
  }

  public Boolean getDeleteHistoricalData() {
    return deleteHistoricalData;
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Metric)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    Metric other = (Metric) obj;
    return key.equals(other.getKey());
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }

  /**
   * Merge with fields from other metric. All fields are copied, except the id.
   *
   * @return this
   */
  public Metric<G> merge(final Metric with) {
    this.description = with.description;
    this.domain = with.domain;
    this.enabled = with.enabled;
    this.qualitative = with.qualitative;
    this.worstValue = with.worstValue;
    this.bestValue = with.bestValue;
    this.optimizedBestValue = with.optimizedBestValue;
    this.direction = with.direction;
    this.key = with.key;
    this.type = with.type;
    this.name = with.name;
    this.userManaged = with.userManaged;
    this.hidden = with.hidden;
    this.deleteHistoricalData = with.deleteHistoricalData;
    return this;
  }

  /**
   * Metric.Builder is used to create metric definitions. It must be preferred to creating new instances of the Metric class directly.
   *
   * @since 2.7
   */
  public static final class Builder {
    private String key;
    private Metric.ValueType type;
    private String name;
    private String description;
    private Integer direction = DIRECTION_NONE;
    private Boolean qualitative = Boolean.FALSE;
    private String domain = null;
    private Formula formula;
    private Double worstValue;
    private Double bestValue;
    private boolean optimizedBestValue = false;
    private boolean hidden = false;
    private boolean userManaged = false;
    private boolean deleteHistoricalData = false;

    /**
     * Creates a new {@link Builder} object.
     *
     * @param key  the metric key, should be unique among all metrics
     * @param name the metric name
     * @param type the metric type
     */
    public Builder(String key, String name, ValueType type) {
      if (StringUtils.isBlank(key)) {
        throw new IllegalArgumentException("Metric key can not be blank");
      }
      if (StringUtils.isBlank(name)) {
        throw new IllegalArgumentException("Metric name can not be blank");
      }
      if (type == null) {
        throw new IllegalArgumentException("Metric type can not be null");
      }
      this.key = key;
      this.name = name;
      this.type = type;
    }

    /**
     * Sets the metric description.
     *
     * @param d the description
     * @return the builder
     */
    public Builder setDescription(String d) {
      this.description = d;
      return this;
    }

    /**
     * Sets the metric direction (used for numeric values only), which is used in the Web UI to show if the trend of a metric is good or not.
     * <ul>
     * <li>Metric.DIRECTION_WORST: indicates that an increase of the metric value is not a good thing (example: the complexity of a function)</li>
     * <li>Metric.DIRECTION_BETTER: indicates that an increase of the metric value is a good thing (example: the code coverage of a function)</li>
     * <li>Metric.DIRECTION_NONE: indicates that the variation of the metric value is neither good nor bad (example: number of files).</li>
     * </ul>
     * Metric.DIRECTION_NONE is the default value.
     *
     * @see Metric#DIRECTION_WORST
     * @see Metric#DIRECTION_BETTER
     * @see Metric#DIRECTION_NONE
     *
     * @param d the direction
     * @return the builder
     */
    public Builder setDirection(Integer d) {
      this.direction = d;
      return this;
    }

    /**
     * Sets whether the metric is qualitative or not. Default value is false.
     * <br/>
     * If set to true, then variations of this metric will be highlighted in the Web UI (for instance, trend icons will be red or green instead of default grey).
     *
     * @param b Boolean.TRUE if the metric is qualitative
     * @return the builder
     */
    public Builder setQualitative(Boolean b) {
      this.qualitative = b;
      return this;
    }

    /**
     * Sets the domain for the metric (General, Complexity...). This is used to group metrics in the Web UI.
     * <br/>
     * By default, the metric belongs to no specific domain.
     *
     * @param d the domain
     * @return the builder
     */
    public Builder setDomain(String d) {
      this.domain = d;
      return this;
    }

    /**
     * Specifies the formula used by Sonar to automatically aggregate measures stored on files up to the project level.
     * <br/>
     * <br/>
     * By default, no formula is defined, which means that it's up to a sensor/decorator to compute measures on appropriate levels.
     * <br/>
     * When a formula is set, sensors/decorators just need to store measures at a specific level and let Sonar run the formula to store
     * measures on the remaining levels.
     *
     * @see SumChildDistributionFormula
     * @see SumChildValuesFormula
     * @see MeanAggregationFormula
     * @see WeightedMeanAggregationFormula
     *
     * @param f the formula
     * @return the builder
     *
     * @deprecated since 5.2, it's no more possible to define a formula on a metric
     * TODO add a link to the new API to declare formulas
     */
    @Deprecated
    public Builder setFormula(Formula f) {
      this.formula = f;
      return this;
    }

    /**
     * Sets the worst value that the metric can get (example: 0.0 for code coverage). No worst value is set by default.
     *
     * @param d the worst value
     * @return the builder
     */
    public Builder setWorstValue(Double d) {
      this.worstValue = d;
      return this;
    }

    /**
     * Sets the best value that the metric can get (example: 100.0 for code coverage). No best value is set by default.
     * <br/>
     * Resources would be hidden on drilldown page, if the value of measure equals to best value.
     *
     * @param d the best value
     * @return the builder
     */
    public Builder setBestValue(Double d) {
      this.bestValue = d;
      return this;
    }

    /**
     * Specifies whether file-level measures that equal to the defined best value are stored or not. Default is false.
     * <br/>
     * Example with the metric that stores the number of violation ({@link CoreMetrics#VIOLATIONS}):
     * if a file has no violation, then the value '0' won't be stored in the database.
     *
     * @param b true if the measures must not be stored when they equal to the best value
     * @return the builder
     */
    public Builder setOptimizedBestValue(boolean b) {
      this.optimizedBestValue = b;
      return this;
    }

    /**
     * Sets whether the metric should be hidden in Web UI (e.g. in Time Machine). Default is false.
     *
     * @param b true if the metric should be hidden.
     * @return the builder
     */
    public Builder setHidden(boolean b) {
      this.hidden = b;
      return this;
    }

    /**
     * Specifies whether this metric can be edited online in the "Manual measures" page. Default is false.
     *
     * @since 2.10
     *
     * @param b true if the metric can be edited online.
     * @return the builder
     */
    public Builder setUserManaged(boolean b) {
      this.userManaged = b;
      return this;
    }

    /**
     * Specifies whether measures from the past can be automatically deleted to minimize database volume.
     * <br/>
     * By default, historical data are kept.
     *
     * @since 2.14
     *
     * @param b true if measures from the past can be deleted automatically.
     * @return the builder
     */
    public Builder setDeleteHistoricalData(boolean b) {
      this.deleteHistoricalData = b;
      return this;
    }

    /**
     * Creates a new metric definition based on the properties set on this metric builder.
     *
     * @return a new {@link Metric} object
     */
    public <G extends Serializable> Metric<G> create() {
      if (ValueType.PERCENT.equals(this.type)) {
        this.bestValue = (direction == DIRECTION_BETTER) ? 100.0 : 0.0;
        this.worstValue = (direction == DIRECTION_BETTER) ? 0.0 : 100.0;
      }
      return new Metric<>(this);
    }
  }

  @Override
  public String key() {
    return getKey();
  }

  @Override
  public Class<G> valueType() {
    return getType().valueType();
  }
}
