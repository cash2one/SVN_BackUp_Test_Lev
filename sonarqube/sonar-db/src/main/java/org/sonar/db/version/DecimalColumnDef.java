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

package org.sonar.db.version;

import javax.annotation.CheckForNull;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.sonar.db.version.ColumnDefValidation.validateColumnName;

public class DecimalColumnDef extends AbstractColumnDef {

  public static int DEFAULT_PRECISION = 38;
  public static int DEFAULT_SACLE = 20;

  private final int precision;
  private final int scale;

  private DecimalColumnDef(Builder builder) {
    super(builder.columnName, builder.isNullable);
    this.precision = builder.precision;
    this.scale = builder.scale;
  }

  public static Builder newDecimalColumnDefBuilder() {
    return new Builder();
  }

  public int getPrecision() {
    return precision;
  }

  public int getScale() {
    return scale;
  }

  @Override
  public String generateSqlType(Dialect dialect) {
    switch (dialect.getId()) {
      case PostgreSql.ID:
      case Oracle.ID:
        return String.format("NUMERIC (%s,%s)", precision, scale);
      case MySql.ID:
      case MsSql.ID:
        return String.format("DECIMAL (%s,%s)", precision, scale);
      case H2.ID:
        return "DOUBLE";
      default:
        throw new UnsupportedOperationException(String.format("Unknown dialect '%s'", dialect.getId()));
    }
  }

  public static class Builder {
    @CheckForNull
    private String columnName;
    private int precision = DEFAULT_PRECISION;
    private int scale = DEFAULT_SACLE;
    private boolean isNullable = false;

    public Builder setColumnName(String columnName) {
      this.columnName = validateColumnName(columnName);
      return this;
    }

    public Builder setIsNullable(boolean isNullable) {
      this.isNullable = isNullable;
      return this;
    }

    public Builder setPrecision(int precision) {
      this.precision = precision;
      return this;
    }

    public Builder setScale(int scale) {
      this.scale = scale;
      return this;
    }

    public DecimalColumnDef build() {
      validateColumnName(columnName);
      return new DecimalColumnDef(this);
    }
  }

}
