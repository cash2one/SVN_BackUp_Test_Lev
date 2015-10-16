// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Infrastructure;
using Microsoft.Data.Entity.Internal;
using Microsoft.Data.Entity.Metadata;
using Microsoft.Data.Entity.Migrations.Operations;
using Microsoft.Data.Entity.Storage;
using Microsoft.Data.Entity.Utilities;

namespace Microsoft.Data.Entity.Migrations
{
    public class MigrationsSqlGenerator : IMigrationsSqlGenerator
    {
        private static readonly IReadOnlyDictionary<Type, Action<MigrationsSqlGenerator, MigrationOperation, IModel, RelationalCommandListBuilder>> _generateActions =
            new Dictionary<Type, Action<MigrationsSqlGenerator, MigrationOperation, IModel, RelationalCommandListBuilder>>
            {
                { typeof(AddColumnOperation), (g, o, m, b) => g.Generate((AddColumnOperation)o, m, b) },
                { typeof(AddForeignKeyOperation), (g, o, m, b) => g.Generate((AddForeignKeyOperation)o, m, b) },
                { typeof(AddPrimaryKeyOperation), (g, o, m, b) => g.Generate((AddPrimaryKeyOperation)o, m, b) },
                { typeof(AddUniqueConstraintOperation), (g, o, m, b) => g.Generate((AddUniqueConstraintOperation)o, m, b) },
                { typeof(AlterColumnOperation), (g, o, m, b) => g.Generate((AlterColumnOperation)o, m, b) },
                { typeof(AlterSequenceOperation), (g, o, m, b) => g.Generate((AlterSequenceOperation)o, m, b) },
                { typeof(CreateIndexOperation), (g, o, m, b) => g.Generate((CreateIndexOperation)o, m, b) },
                { typeof(CreateSequenceOperation), (g, o, m, b) => g.Generate((CreateSequenceOperation)o, m, b) },
                { typeof(CreateTableOperation), (g, o, m, b) => g.Generate((CreateTableOperation)o, m, b) },
                { typeof(DropColumnOperation), (g, o, m, b) => g.Generate((DropColumnOperation)o, m, b) },
                { typeof(DropForeignKeyOperation), (g, o, m, b) => g.Generate((DropForeignKeyOperation)o, m, b) },
                { typeof(DropIndexOperation), (g, o, m, b) => g.Generate((DropIndexOperation)o, m, b) },
                { typeof(DropPrimaryKeyOperation), (g, o, m, b) => g.Generate((DropPrimaryKeyOperation)o, m, b) },
                { typeof(DropSchemaOperation), (g, o, m, b) => g.Generate((DropSchemaOperation)o, m, b) },
                { typeof(DropSequenceOperation), (g, o, m, b) => g.Generate((DropSequenceOperation)o, m, b) },
                { typeof(DropTableOperation), (g, o, m, b) => g.Generate((DropTableOperation)o, m, b) },
                { typeof(DropUniqueConstraintOperation), (g, o, m, b) => g.Generate((DropUniqueConstraintOperation)o, m, b) },
                { typeof(EnsureSchemaOperation), (g, o, m, b) => g.Generate((EnsureSchemaOperation)o, m, b) },
                { typeof(RenameColumnOperation), (g, o, m, b) => g.Generate((RenameColumnOperation)o, m, b) },
                { typeof(RenameIndexOperation), (g, o, m, b) => g.Generate((RenameIndexOperation)o, m, b) },
                { typeof(RenameSequenceOperation), (g, o, m, b) => g.Generate((RenameSequenceOperation)o, m, b) },
                { typeof(RenameTableOperation), (g, o, m, b) => g.Generate((RenameTableOperation)o, m, b) },
                { typeof(RestartSequenceOperation), (g, o, m, b) => g.Generate((RestartSequenceOperation)o, m, b) },
                { typeof(SqlOperation), (g, o, m, b) => g.Generate((SqlOperation)o, m, b) }
            };

        private readonly IRelationalCommandBuilderFactory _commandBuilderFactory;
        private readonly IRelationalAnnotationProvider _annotations;

        public MigrationsSqlGenerator(
            [NotNull] IRelationalCommandBuilderFactory commandBuilderFactory,
            [NotNull] ISqlGenerator sqlGenerator,
            [NotNull] IRelationalTypeMapper typeMapper,
            [NotNull] IRelationalAnnotationProvider annotations)
        {
            Check.NotNull(commandBuilderFactory, nameof(commandBuilderFactory));
            Check.NotNull(sqlGenerator, nameof(sqlGenerator));
            Check.NotNull(typeMapper, nameof(typeMapper));
            Check.NotNull(annotations, nameof(annotations));

            _commandBuilderFactory = commandBuilderFactory;
            SqlGenerator = sqlGenerator;
            TypeMapper = typeMapper;
            _annotations = annotations;
        }

        protected virtual ISqlGenerator SqlGenerator { get; }
        protected virtual IRelationalTypeMapper TypeMapper { get; }

        public virtual IReadOnlyList<IRelationalCommand> Generate(
            IReadOnlyList<MigrationOperation> operations,
            IModel model = null)
        {
            Check.NotNull(operations, nameof(operations));

            var builder = new RelationalCommandListBuilder(_commandBuilderFactory);
            foreach (var operation in operations)
            {
                Generate(operation, model, builder);
                builder
                    .AppendLine(SqlGenerator.BatchCommandSeparator)
                    .EndCommand();
            }

            return builder.GetCommands();
        }

        protected virtual void Generate(
            [NotNull] MigrationOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            var operationType = operation.GetType();
            Action<MigrationsSqlGenerator, MigrationOperation, IModel, RelationalCommandListBuilder> generateAction;
            if (!_generateActions.TryGetValue(operationType, out generateAction))
            {
                throw new InvalidOperationException(RelationalStrings.UnknownOperation(GetType().Name, operationType));
            }

            generateAction(this, operation, model, builder);
        }

        protected virtual void Generate(
            [NotNull] AddColumnOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("ALTER TABLE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Table, operation.Schema))
                .Append(" ADD ");
            ColumnDefinition(operation, model, builder);
        }

        protected virtual void Generate(
            [NotNull] AddForeignKeyOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("ALTER TABLE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Table, operation.Schema))
                .Append(" ADD ");
            ForeignKeyConstraint(operation, model, builder);
        }

        protected virtual void Generate(
            [NotNull] AddPrimaryKeyOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("ALTER TABLE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Table, operation.Schema))
                .Append(" ADD ");
            PrimaryKeyConstraint(operation, model, builder);
        }

        protected virtual void Generate(
            [NotNull] AddUniqueConstraintOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("ALTER TABLE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Table, operation.Schema))
                .Append(" ADD ");
            UniqueConstraint(operation, model, builder);
        }

        protected virtual void Generate(
            [NotNull] AlterColumnOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            throw new NotImplementedException();
        }

        protected virtual void Generate(
            [NotNull] RenameIndexOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            throw new NotImplementedException();
        }

        protected virtual void Generate(
            [NotNull] AlterSequenceOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("ALTER SEQUENCE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Name, operation.Schema));

            SequenceOptions(operation, model, builder);
        }

        protected virtual void Generate(
            [NotNull] RenameTableOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            throw new NotImplementedException();
        }

        protected virtual void Generate(
            [NotNull] CreateIndexOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder.Append("CREATE ");

            if (operation.IsUnique)
            {
                builder.Append("UNIQUE ");
            }

            IndexTraits(operation, model, builder);

            builder
                .Append("INDEX ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Name))
                .Append(" ON ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Table, operation.Schema))
                .Append(" (")
                .Append(ColumnList(operation.Columns))
                .Append(")");
        }

        protected virtual void Generate(
            [NotNull] EnsureSchemaOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            throw new NotImplementedException();
        }

        protected virtual void Generate(
            [NotNull] CreateSequenceOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("CREATE SEQUENCE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Name, operation.Schema));

            if (operation.ClrType != typeof(long))
            {
                builder
                    .Append(" AS ")
                    .Append(TypeMapper.GetMapping(operation.ClrType).DefaultTypeName);
            }

            builder
                .Append(" START WITH ")
                .Append(SqlGenerator.GenerateLiteral(operation.StartValue));
            SequenceOptions(operation, model, builder);
        }

        protected virtual void Generate(
            [NotNull] CreateTableOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("CREATE TABLE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Name, operation.Schema))
                .AppendLine(" (");

            using (builder.Indent())
            {
                for (var i = 0; i < operation.Columns.Count; i++)
                {
                    var column = operation.Columns[i];
                    ColumnDefinition(column, model, builder);

                    if (i != operation.Columns.Count - 1)
                    {
                        builder.AppendLine(",");
                    }
                }

                if (operation.PrimaryKey != null)
                {
                    builder.AppendLine(",");
                    PrimaryKeyConstraint(operation.PrimaryKey, model, builder);
                }

                foreach (var uniqueConstraint in operation.UniqueConstraints)
                {
                    builder.AppendLine(",");
                    UniqueConstraint(uniqueConstraint, model, builder);
                }

                foreach (var foreignKey in operation.ForeignKeys)
                {
                    builder.AppendLine(",");
                    ForeignKeyConstraint(foreignKey, model, builder);
                }

                builder.AppendLine();
            }

            builder.Append(")");
        }

        protected virtual void Generate(
            [NotNull] DropColumnOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("ALTER TABLE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Table, operation.Schema))
                .Append(" DROP COLUMN ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Name));
        }

        protected virtual void Generate(
            [NotNull] DropForeignKeyOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("ALTER TABLE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Table, operation.Schema))
                .Append(" DROP CONSTRAINT ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Name));
        }

        protected virtual void Generate(
            [NotNull] DropIndexOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            throw new NotImplementedException();
        }

        protected virtual void Generate(
            [NotNull] DropPrimaryKeyOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("ALTER TABLE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Table, operation.Schema))
                .Append(" DROP CONSTRAINT ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Name));
        }

        protected virtual void Generate(
            [NotNull] DropSchemaOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("DROP SCHEMA ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Name));
        }

        protected virtual void Generate(
            [NotNull] DropSequenceOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("DROP SEQUENCE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Name, operation.Schema));
        }

        protected virtual void Generate(
            [NotNull] DropTableOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("DROP TABLE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Name, operation.Schema));
        }

        protected virtual void Generate(
            [NotNull] DropUniqueConstraintOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("ALTER TABLE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Table, operation.Schema))
                .Append(" DROP CONSTRAINT ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Name));
        }

        protected virtual void Generate(
            [NotNull] RenameColumnOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            throw new NotImplementedException();
        }

        protected virtual void Generate(
            [NotNull] RenameSequenceOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            throw new NotImplementedException();
        }

        protected virtual void Generate(
            [NotNull] RestartSequenceOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append("ALTER SEQUENCE ")
                .Append(SqlGenerator.DelimitIdentifier(operation.Name, operation.Schema))
                .Append(" RESTART WITH ")
                .Append(SqlGenerator.GenerateLiteral(operation.StartValue));
        }

        protected virtual void Generate(
            [NotNull] SqlOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            builder.Append(operation.Sql);
        }

        protected virtual void SequenceOptions(
            [NotNull] AlterSequenceOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder) =>
                SequenceOptions(
                    operation.Schema,
                    operation.Name,
                    operation.IncrementBy,
                    operation.MinValue,
                    operation.MaxValue,
                    operation.IsCyclic,
                    model,
                    builder);

        protected virtual void SequenceOptions(
            [NotNull] CreateSequenceOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder) =>
                SequenceOptions(
                    operation.Schema,
                    operation.Name,
                    operation.IncrementBy,
                    operation.MinValue,
                    operation.MaxValue,
                    operation.IsCyclic,
                    model,
                    builder);

        protected virtual void SequenceOptions(
            [CanBeNull] string schema,
            [NotNull] string name,
            [NotNull] int increment,
            long? minimumValue,
            long? maximumValue,
            [NotNull] bool cycle,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotEmpty(name, nameof(name));
            Check.NotNull(increment, nameof(increment));
            Check.NotNull(cycle, nameof(cycle));
            Check.NotNull(builder, nameof(builder));

            builder
                .Append(" INCREMENT BY ")
                .Append(SqlGenerator.GenerateLiteral(increment));

            if (minimumValue != null)
            {
                builder
                    .Append(" MINVALUE ")
                    .Append(SqlGenerator.GenerateLiteral(minimumValue));
            }
            else
            {
                builder.Append(" NO MINVALUE");
            }

            if (maximumValue != null)
            {
                builder
                    .Append(" MAXVALUE ")
                    .Append(SqlGenerator.GenerateLiteral(maximumValue));
            }
            else
            {
                builder.Append(" NO MAXVALUE");
            }

            builder.Append(cycle ? " CYCLE" : " NO CYCLE");
        }

        protected virtual void ColumnDefinition(
            [NotNull] AddColumnOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder) =>
                ColumnDefinition(
                    operation.Schema,
                    operation.Table,
                    operation.Name,
                    operation.ClrType,
                    operation.ColumnType,
                    operation.IsNullable,
                    operation.DefaultValue,
                    operation.DefaultValueSql,
                    operation.ComputedColumnSql,
                    operation,
                    model,
                    builder);

        protected virtual void ColumnDefinition(
            [CanBeNull] string schema,
            [CanBeNull] string table,
            [NotNull] string name,
            [NotNull] Type clrType,
            [CanBeNull] string type,
            bool nullable,
            [CanBeNull] object defaultValue,
            [CanBeNull] string defaultValueSql,
            [CanBeNull] string computedColumnSql,
            [NotNull] IAnnotatable annotatable,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotEmpty(name, nameof(name));
            Check.NotNull(clrType, nameof(clrType));
            Check.NotNull(annotatable, nameof(annotatable));
            Check.NotNull(builder, nameof(builder));

            if (type == null)
            {
                var property = FindProperty(model, schema, table, name);
                type = property != null
                    ? TypeMapper.GetMapping(property).DefaultTypeName
                    : TypeMapper.GetMapping(clrType).DefaultTypeName;
            }

            builder
                .Append(SqlGenerator.DelimitIdentifier(name))
                .Append(" ")
                .Append(type);

            if (!nullable)
            {
                builder.Append(" NOT NULL");
            }

            DefaultValue(defaultValue, defaultValueSql, builder);
        }

        protected virtual void DefaultValue(
            [CanBeNull] object defaultValue,
            [CanBeNull] string defaultValueSql,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(builder, nameof(builder));

            if (defaultValueSql != null)
            {
                builder
                    .Append(" DEFAULT (")
                    .Append(defaultValueSql)
                    .Append(")");
            }
            else if (defaultValue != null)
            {
                builder
                    .Append(" DEFAULT ")
                    .Append(SqlGenerator.GenerateLiteral(defaultValue));
            }
        }

        protected virtual void ForeignKeyConstraint(
            [NotNull] AddForeignKeyOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            if (operation.Name != null)
            {
                builder
                    .Append("CONSTRAINT ")
                    .Append(SqlGenerator.DelimitIdentifier(operation.Name))
                    .Append(" ");
            }

            builder
                .Append("FOREIGN KEY (")
                .Append(ColumnList(operation.Columns))
                .Append(") REFERENCES ")
                .Append(SqlGenerator.DelimitIdentifier(operation.PrincipalTable, operation.PrincipalSchema))
                .Append(" (")
                .Append(ColumnList(operation.PrincipalColumns))
                .Append(")");

            if (operation.OnUpdate != ReferentialAction.NoAction)
            {
                builder.Append(" ON UPDATE ");
                ForeignKeyAction(operation.OnUpdate, builder);
            }

            if (operation.OnDelete != ReferentialAction.NoAction)
            {
                builder.Append(" ON DELETE ");
                ForeignKeyAction(operation.OnDelete, builder);
            }
        }

        protected virtual void PrimaryKeyConstraint(
            [NotNull] AddPrimaryKeyOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            if (operation.Name != null)
            {
                builder
                    .Append("CONSTRAINT ")
                    .Append(SqlGenerator.DelimitIdentifier(operation.Name))
                    .Append(" ");
            }

            builder
                .Append("PRIMARY KEY ");

            IndexTraits(operation, model, builder);

            builder.Append("(")
                .Append(ColumnList(operation.Columns))
                .Append(")");
        }

        protected virtual void UniqueConstraint(
            [NotNull] AddUniqueConstraintOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(operation, nameof(operation));
            Check.NotNull(builder, nameof(builder));

            if (operation.Name != null)
            {
                builder
                    .Append("CONSTRAINT ")
                    .Append(SqlGenerator.DelimitIdentifier(operation.Name))
                    .Append(" ");
            }

            builder
                .Append("UNIQUE ");

            IndexTraits(operation, model, builder);

            builder.Append("(")
                .Append(ColumnList(operation.Columns))
                .Append(")");
        }

        protected virtual void IndexTraits(
            [NotNull] MigrationOperation operation,
            [CanBeNull] IModel model,
            [NotNull] RelationalCommandListBuilder builder)
        {
        }

        protected virtual void ForeignKeyAction(
            ReferentialAction referentialAction,
            [NotNull] RelationalCommandListBuilder builder)
        {
            Check.NotNull(builder, nameof(builder));

            switch (referentialAction)
            {
                case ReferentialAction.Restrict:
                    builder.Append("RESTRICT");
                    break;
                case ReferentialAction.Cascade:
                    builder.Append("CASCADE");
                    break;
                case ReferentialAction.SetNull:
                    builder.Append("SET NULL");
                    break;
                case ReferentialAction.SetDefault:
                    builder.Append("SET DEFAULT");
                    break;
                default:
                    Debug.Assert(
                        referentialAction == ReferentialAction.NoAction,
                        "Unexpected value: " + referentialAction);
                    break;
            }
        }

        protected virtual IEntityType FindEntityType(
            [CanBeNull] IModel model,
            [CanBeNull] string schema,
            [NotNull] string tableName)
            => model?.EntityTypes.FirstOrDefault(
                t => _annotations.For(t).TableName == tableName && _annotations.For(t).Schema == schema);

        protected virtual IProperty FindProperty(
            [CanBeNull] IModel model,
            [CanBeNull] string schema,
            [NotNull] string tableName,
            [NotNull] string columnName)
            => FindEntityType(model, schema, tableName)
                ?.GetProperties().FirstOrDefault(p => _annotations.For(p).ColumnName == columnName);

        private string ColumnList(string[] columns) => string.Join(", ", columns.Select(SqlGenerator.DelimitIdentifier));
    }
}
