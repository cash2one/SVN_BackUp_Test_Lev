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
package org.sonar.server.computation.measure;

import com.google.common.base.Optional;
import javax.annotation.Nullable;
import org.sonar.db.measure.MeasureDto;
import org.sonar.server.computation.metric.Metric;

import static com.google.common.base.Optional.of;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.computation.measure.Measure.Level.toLevel;

public class MeasureDtoToMeasure {

  public Optional<Measure> toMeasure(@Nullable MeasureDto measureDto, Metric metric) {
    requireNonNull(metric);
    if (measureDto == null) {
      return Optional.absent();
    }
    Double value = measureDto.getValue();
    String data = measureDto.getData();
    switch (metric.getType().getValueType()) {
      case INT:
        return toIntegerMeasure(measureDto, value, data);
      case LONG:
        return toLongMeasure(measureDto, value, data);
      case DOUBLE:
        return toDoubleMeasure(measureDto, value, data);
      case BOOLEAN:
        return toBooleanMeasure(measureDto, value, data);
      case STRING:
        return toStringMeasure(measureDto, data);
      case LEVEL:
        return toLevelMeasure(measureDto, data);
      case NO_VALUE:
        return toNoValueMeasure(measureDto);
      default:
        throw new IllegalArgumentException("Unsupported Measure.ValueType " + metric.getType().getValueType());
    }
  }

  private static Optional<Measure> toIntegerMeasure(MeasureDto measureDto, @Nullable Double value, String data) {
    if (value == null) {
      return toNoValueMeasure(measureDto);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto).create(value.intValue(), data));
  }

  private static Optional<Measure> toLongMeasure(MeasureDto measureDto, @Nullable Double value, String data) {
    if (value == null) {
      return toNoValueMeasure(measureDto);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto).create(value.longValue(), data));
  }

  private static Optional<Measure> toDoubleMeasure(MeasureDto measureDto, @Nullable Double value, String data) {
    if (value == null) {
      return toNoValueMeasure(measureDto);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto).create(value.doubleValue(), data));
  }

  private static Optional<Measure> toBooleanMeasure(MeasureDto measureDto, @Nullable Double value, String data) {
    if (value == null) {
      return toNoValueMeasure(measureDto);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto).create(value == 1.0d, data));
  }

  private static Optional<Measure> toStringMeasure(MeasureDto measureDto, @Nullable String data) {
    if (data == null) {
      return toNoValueMeasure(measureDto);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto).create(data));
  }

  private static Optional<Measure> toLevelMeasure(MeasureDto measureDto, @Nullable String data) {
    if (data == null) {
      return toNoValueMeasure(measureDto);
    }
    Optional<Measure.Level> level = toLevel(data);
    if (!level.isPresent()) {
      return toNoValueMeasure(measureDto);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto).create(level.get()));
  }

  private static Optional<Measure> toNoValueMeasure(MeasureDto measureDto) {
    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto).createNoValue());
  }

  private static Measure.NewMeasureBuilder setCommonProperties(Measure.NewMeasureBuilder builder, MeasureDto measureDto) {
    if (measureDto.getAlertStatus() != null) {
      Optional<Measure.Level> qualityGateStatus = toLevel(measureDto.getAlertStatus());
      if (qualityGateStatus.isPresent()) {
        builder.setQualityGateStatus(new QualityGateStatus(qualityGateStatus.get(), measureDto.getAlertText()));
      }
    }
    if (hasAnyVariation(measureDto)) {
      builder.setVariations(createVariations(measureDto));
    }
    Integer ruleId = measureDto.getRuleId();
    if (ruleId != null) {
      builder.forRule(ruleId);
    }
    Integer characteristicId = measureDto.getCharacteristicId();
    if (characteristicId != null) {
      builder.forCharacteristic(characteristicId);
    }

    return builder;
  }

  private static boolean hasAnyVariation(MeasureDto measureDto) {
    for (int i = 1; i < 6; i++) {
      if (measureDto.getVariation(i) != null) {
        return true;
      }
    }
    return false;
  }

  private static MeasureVariations createVariations(MeasureDto measureDto) {
    return new MeasureVariations(
      measureDto.getVariation(1),
      measureDto.getVariation(2),
      measureDto.getVariation(3),
      measureDto.getVariation(4),
      measureDto.getVariation(5));
  }

}
