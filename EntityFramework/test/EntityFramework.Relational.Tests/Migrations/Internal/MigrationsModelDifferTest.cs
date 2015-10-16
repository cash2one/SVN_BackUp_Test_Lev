// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Linq;
using Microsoft.Data.Entity.Metadata;
using Microsoft.Data.Entity.Migrations.Operations;
using Xunit;

namespace Microsoft.Data.Entity.Migrations.Internal
{
    // TODO: Test matching
    public class MigrationsModelDifferTest : MigrationsModelDifferTestBase
    {
        [Fact]
        public void Model_differ_breaks_foreign_key_cycles_in_create_table_operations()
        {
            Execute(
                _ => { },
                modelBuilder =>
                {
                    modelBuilder.Entity(
                        "First",
                        x =>
                        {
                            x.Property<int>("ID");
                            x.HasKey("ID");
                            x.Property<int>("FK");
                        });

                    modelBuilder.Entity(
                        "Second",
                        x =>
                        {
                            x.Property<int>("ID");
                            x.HasKey("ID");
                            x.Property<int>("FK");
                        });

                    modelBuilder.Entity("First").HasOne("Second").WithMany().HasForeignKey("FK").HasPrincipalKey("ID");
                    modelBuilder.Entity("Second").HasOne("First").WithMany().HasForeignKey("FK").HasPrincipalKey("ID");
                },
                result =>
                {
                    Assert.Equal(3, result.Count);

                    var firstOperation = result[0] as CreateTableOperation;
                    var secondOperation = result[1] as CreateTableOperation;
                    var thirdOperation = result[2] as AddForeignKeyOperation;

                    Assert.NotNull(firstOperation);
                    Assert.NotNull(secondOperation);
                    Assert.NotNull(thirdOperation);

                    Assert.Equal(0, firstOperation.ForeignKeys.Count);
                    Assert.Equal(1, secondOperation.ForeignKeys.Count);
                    Assert.Equal(firstOperation.Name, thirdOperation.Table);
                });
        }

        [Fact]
        public void Model_differ_breaks_foreign_key_cycles_in_drop_table_operations()
        {
            Execute(
                modelBuilder =>
                {
                    modelBuilder.Entity(
                        "Third",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("FourthId");
                        });
                    modelBuilder.Entity(
                        "Fourth",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("ThirdId");
                        });

                    modelBuilder.Entity("Third").HasOne("Fourth").WithMany().HasForeignKey("FourthId");
                    modelBuilder.Entity("Fourth").HasOne("Third").WithMany().HasForeignKey("ThirdId");
                },
                _ => { },
                operations =>
                {
                    Assert.Collection(
                        operations,
                        o => Assert.IsType<DropForeignKeyOperation>(o),
                        o => Assert.IsType<DropTableOperation>(o),
                        o => Assert.IsType<DropTableOperation>(o));
                });
        }

        [Fact]
        public void Create_table()
        {
            Execute(
                _ => { },
                modelBuilder => modelBuilder.Entity(
                    "Node",
                    x =>
                    {
                        x.ToTable("Node", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("AltId");
                        x.HasAlternateKey("AltId");
                        x.Property<int?>("ParentAltId");
                        x.HasOne("Node").WithMany().HasForeignKey("ParentAltId");
                        x.HasIndex("ParentAltId");
                    }),
                operations =>
                {
                    Assert.Equal(3, operations.Count);

                    var ensureSchemaOperation = Assert.IsType<EnsureSchemaOperation>(operations[0]);
                    Assert.Equal("dbo", ensureSchemaOperation.Name);

                    var createTableOperation = Assert.IsType<CreateTableOperation>(operations[1]);
                    Assert.Equal("Node", createTableOperation.Name);
                    Assert.Equal("dbo", createTableOperation.Schema);
                    Assert.Equal(3, createTableOperation.Columns.Count);
                    Assert.Null(createTableOperation.Columns.First(o => o.Name == "AltId").DefaultValue);
                    Assert.NotNull(createTableOperation.PrimaryKey);
                    Assert.Equal(1, createTableOperation.UniqueConstraints.Count);
                    Assert.Equal(1, createTableOperation.ForeignKeys.Count);

                    Assert.IsType<CreateIndexOperation>(operations[2]);
                });
        }

        [Fact]
        public void Drop_table()
        {
            Execute(
                modelBuilder => modelBuilder.Entity("Fox").ToTable("Fox", "dbo"),
                _ => { },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<DropTableOperation>(operations[0]);
                    Assert.Equal("Fox", operation.Name);
                    Assert.Equal("dbo", operation.Schema);
                });
        }

        [Fact]
        public void Rename_table()
        {
            Execute(
                source => source.Entity(
                    "Cat",
                    x =>
                    {
                        x.ToTable("Cat", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                target => target.Entity(
                    "Cat",
                    x =>
                    {
                        x.ToTable("Cats", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<RenameTableOperation>(operations[0]);
                    Assert.Equal("Cat", operation.Name);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Cats", operation.NewName);
                    Assert.Null(operation.NewSchema);
                });
        }

        [Fact]
        public void Move_table()
        {
            Execute(
                source => source.Entity(
                    "Person",
                    x =>
                    {
                        x.ToTable("People", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                target => target.Entity("Person",
                    x =>
                    {
                        x.ToTable("People", "public");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var ensureSchemaOperation = Assert.IsType<EnsureSchemaOperation>(operations[0]);
                    Assert.Equal("public", ensureSchemaOperation.Name);

                    var renameTableOperation = Assert.IsType<RenameTableOperation>(operations[1]);
                    Assert.Equal("People", renameTableOperation.Name);
                    Assert.Equal("dbo", renameTableOperation.Schema);
                    Assert.Null(renameTableOperation.NewName);
                    Assert.Equal("public", renameTableOperation.NewSchema);
                });
        }

        [Fact]
        public void Rename_entity_type()
        {
            Execute(
                source => source.Entity(
                    "Dog",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id").HasName("PK_Dog");
                    }),
                target => target.Entity(
                    "Doge",
                    x =>
                    {
                        x.ToTable("Dog");
                        x.Property<int>("Id");
                        x.HasKey("Id").HasName("PK_Dog");
                    }),
                operations => Assert.Empty(operations));
        }

        [Fact]
        public void Add_column()
        {
            Execute(
                source => source.Entity(
                    "Dragon",
                    x =>
                    {
                        x.ToTable("Dragon", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                target => target.Entity(
                    "Dragon",
                    x =>
                    {
                        x.ToTable("Dragon", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name")
                            .HasColumnType("nvarchar(30)")
                            .IsRequired()
                            .HasDefaultValue("Draco")
                            .HasDefaultValueSql("CreateDragonName()");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddColumnOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Dragon", operation.Table);
                    Assert.Equal("Name", operation.Name);
                    Assert.Equal(typeof(string), operation.ClrType);
                    Assert.Equal("nvarchar(30)", operation.ColumnType);
                    Assert.False(operation.IsNullable);
                    Assert.Equal("Draco", operation.DefaultValue);
                    Assert.Equal("CreateDragonName()", operation.DefaultValueSql);
                });
        }

        [Fact]
        public void Add_column_with_computed_value()
        {
            Execute(
                source => source.Entity(
                    "Dragon",
                    x =>
                    {
                        x.ToTable("Dragon", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                target => target.Entity(
                    "Dragon",
                    x =>
                    {
                        x.ToTable("Dragon", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name")
                            .HasColumnType("nvarchar(30)")
                            .IsRequired()
                            .HasDefaultValue("Draco")
                            .HasComputedColumnSql("CreateDragonName()");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddColumnOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Dragon", operation.Table);
                    Assert.Equal("Name", operation.Name);
                    Assert.Equal(typeof(string), operation.ClrType);
                    Assert.Equal("nvarchar(30)", operation.ColumnType);
                    Assert.False(operation.IsNullable);
                    Assert.Equal("Draco", operation.DefaultValue);
                    Assert.Equal("CreateDragonName()", operation.ComputedColumnSql);
                });
        }

        [Theory]
        [InlineData(typeof(int), 0)]
        [InlineData(typeof(int?), 0)]
        [InlineData(typeof(string), "")]
        [InlineData(typeof(byte[]), new byte[0])]
        public void Add_column_not_null(Type type, object expectedDefault)
        {
            Execute(
                source => source.Entity(
                    "Robin",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                target => target.Entity(
                    "Robin",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property(type, "Value").IsRequired();
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddColumnOperation>(operations[0]);
                    Assert.Equal("Robin", operation.Table);
                    Assert.Equal("Value", operation.Name);
                    Assert.Equal(expectedDefault, operation.DefaultValue);
                });
        }

        [Fact]
        public void Drop_column()
        {
            Execute(
                source => source.Entity(
                    "Firefly",
                    x =>
                    {
                        x.ToTable("Firefly", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name").HasColumnType("nvarchar(30)");
                    }),
                target => target.Entity(
                    "Firefly",
                    x =>
                    {
                        x.ToTable("Firefly", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<DropColumnOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Firefly", operation.Table);
                    Assert.Equal("Name", operation.Name);
                });
        }

        [Fact]
        public void Rename_column()
        {
            Execute(
                source => source.Entity(
                    "Zebra",
                    x =>
                    {
                        x.ToTable("Zebra", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name").HasColumnType("nvarchar(30)");
                    }),
                target => target.Entity(
                    "Zebra",
                    x =>
                    {
                        x.ToTable("Zebra", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name").HasColumnName("ZebraName").HasColumnType("nvarchar(30)");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<RenameColumnOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Zebra", operation.Table);
                    Assert.Equal("Name", operation.Name);
                    Assert.Equal("ZebraName", operation.NewName);
                });
        }

        [Fact]
        public void Rename_property()
        {
            Execute(
                source => source.Entity(
                    "Buffalo",
                    x =>
                    {
                        x.ToTable("Buffalo", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("BuffaloName").HasColumnType("nvarchar(30)");
                    }),
                target => target.Entity(
                    "Buffalo",
                    x =>
                    {
                        x.ToTable("Buffalo", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name").HasColumnName("BuffaloName").HasColumnType("nvarchar(30)");
                    }),
                operations => Assert.Empty(operations));
        }

        [Fact]
        public void Alter_column_nullability()
        {
            Execute(
                source => source.Entity(
                    "Bison",
                    x =>
                    {
                        x.ToTable("Bison", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name")
                            .HasColumnType("nvarchar(30)")
                            .IsRequired(true)
                            .HasDefaultValue("Buffy")
                            .HasDefaultValueSql("CreateBisonName()");
                    }),
                target => target.Entity(
                    "Bison",
                    x =>
                    {
                        x.ToTable("Bison", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name")
                            .HasColumnType("nvarchar(30)")
                            .IsRequired(false)
                            .HasDefaultValue("Buffy")
                            .HasDefaultValueSql("CreateBisonName()");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AlterColumnOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Bison", operation.Table);
                    Assert.Equal("Name", operation.Name);
                    Assert.Equal(typeof(string), operation.ClrType);
                    Assert.Equal("nvarchar(30)", operation.ColumnType);
                    Assert.True(operation.IsNullable);
                    Assert.Equal("Buffy", operation.DefaultValue);
                    Assert.Equal("CreateBisonName()", operation.DefaultValueSql);
                });
        }

        [Fact]
        public void Alter_column_type()
        {
            Execute(
                source => source.Entity(
                    "Puma",
                    x =>
                    {
                        x.ToTable("Puma", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name")
                            .HasColumnType("nvarchar(30)")
                            .IsRequired()
                            .HasDefaultValue("Puff")
                            .HasDefaultValueSql("CreatePumaName()");
                    }),
                target => target.Entity(
                    "Puma",
                    x =>
                    {
                        x.ToTable("Puma", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name")
                            .HasColumnType("nvarchar(450)")
                            .IsRequired()
                            .HasDefaultValue("Puff")
                            .HasDefaultValueSql("CreatePumaName()");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AlterColumnOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Puma", operation.Table);
                    Assert.Equal("Name", operation.Name);
                    Assert.Equal(typeof(string), operation.ClrType);
                    Assert.Equal("nvarchar(450)", operation.ColumnType);
                    Assert.False(operation.IsNullable);
                    Assert.Equal("Puff", operation.DefaultValue);
                    Assert.Equal("CreatePumaName()", operation.DefaultValueSql);
                });
        }

        [Fact]
        public void Alter_column_default()
        {
            Execute(
                source => source.Entity(
                    "Cougar",
                    x =>
                    {
                        x.ToTable("Cougar", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name")
                            .HasColumnType("nvarchar(30)")
                            .IsRequired()
                            .HasDefaultValue("Butch")
                            .HasDefaultValueSql("CreateCougarName()");
                    }),
                target => target.Entity(
                    "Cougar",
                    x =>
                    {
                        x.ToTable("Cougar", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name")
                            .HasColumnType("nvarchar(30)")
                            .IsRequired()
                            .HasDefaultValue("Cosmo")
                            .HasDefaultValueSql("CreateCougarName()");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AlterColumnOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Cougar", operation.Table);
                    Assert.Equal("Name", operation.Name);
                    Assert.Equal(typeof(string), operation.ClrType);
                    Assert.Equal("nvarchar(30)", operation.ColumnType);
                    Assert.False(operation.IsNullable);
                    Assert.Equal("Cosmo", operation.DefaultValue);
                    Assert.Equal("CreateCougarName()", operation.DefaultValueSql);
                });
        }

        [Fact]
        public void Alter_column_default_expression()
        {
            Execute(
                source => source.Entity(
                    "MountainLion",
                    x =>
                    {
                        x.ToTable("MountainLion", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name")
                            .HasColumnType("nvarchar(30)")
                            .IsRequired()
                            .HasDefaultValue("Liam")
                            .HasDefaultValueSql("CreateMountainLionName()");
                    }),
                target => target.Entity(
                    "MountainLion",
                    x =>
                    {
                        x.ToTable("MountainLion", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name")
                            .HasColumnType("nvarchar(30)")
                            .IsRequired()
                            .HasDefaultValue("Liam")
                            .HasDefaultValueSql("CreateCatamountName()");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AlterColumnOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("MountainLion", operation.Table);
                    Assert.Equal("Name", operation.Name);
                    Assert.Equal(typeof(string), operation.ClrType);
                    Assert.Equal("nvarchar(30)", operation.ColumnType);
                    Assert.False(operation.IsNullable);
                    Assert.Equal("Liam", operation.DefaultValue);
                    Assert.Equal("CreateCatamountName()", operation.DefaultValueSql);
                });
        }

        [Fact]
        public void Alter_column_computed_expression()
        {
            Execute(
                source => source.Entity(
                    "MountainLion",
                    x =>
                    {
                        x.ToTable("MountainLion", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name")
                            .HasColumnType("nvarchar(30)")
                            .IsRequired()
                            .HasDefaultValue("Liam")
                            .HasComputedColumnSql("CreateMountainLionName()");
                    }),
                target => target.Entity(
                    "MountainLion",
                    x =>
                    {
                        x.ToTable("MountainLion", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name")
                            .HasColumnType("nvarchar(30)")
                            .IsRequired()
                            .HasDefaultValue("Liam")
                            .HasComputedColumnSql("CreateCatamountName()");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AlterColumnOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("MountainLion", operation.Table);
                    Assert.Equal("Name", operation.Name);
                    Assert.Equal(typeof(string), operation.ClrType);
                    Assert.Equal("nvarchar(30)", operation.ColumnType);
                    Assert.False(operation.IsNullable);
                    Assert.Equal("Liam", operation.DefaultValue);
                    Assert.Equal("CreateCatamountName()", operation.ComputedColumnSql);
                });
        }

        [Fact]
        public void Add_unique_constraint()
        {
            Execute(
                source => source.Entity(
                    "Flamingo",
                    x =>
                    {
                        x.ToTable("Flamingo", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("AlternateId");
                    }),
                target => target.Entity(
                    "Flamingo",
                    x =>
                    {
                        x.ToTable("Flamingo", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("AlternateId");
                        x.HasAlternateKey("AlternateId");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddUniqueConstraintOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Flamingo", operation.Table);
                    Assert.Equal("AK_Flamingo_AlternateId", operation.Name);
                    Assert.Equal(new[] { "AlternateId" }, operation.Columns);
                });
        }

        [Fact]
        public void Drop_unique_constraint()
        {
            Execute(
                source => source.Entity(
                    "Penguin",
                    x =>
                    {
                        x.ToTable("Penguin", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("AlternateId");
                        x.HasAlternateKey("AlternateId");
                    }),
                target => target.Entity(
                    "Penguin",
                    x =>
                    {
                        x.ToTable("Penguin", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("AlternateId");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<DropUniqueConstraintOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Penguin", operation.Table);
                    Assert.Equal("AK_Penguin_AlternateId", operation.Name);
                });
        }

        [Fact]
        public void Rename_unique_constraint()
        {
            Execute(
                source => source.Entity(
                    "Pelican",
                    x =>
                    {
                        x.ToTable("Pelican", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("AlternateId");
                        x.HasAlternateKey("AlternateId");
                    }),
                target => target.Entity(
                    "Pelican",
                    x =>
                    {
                        x.ToTable("Pelican", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("AlternateId");
                        x.HasAlternateKey("AlternateId").HasName("AK_dbo.Pelican_AlternateId");
                    }),
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var dropOperation = Assert.IsType<DropUniqueConstraintOperation>(operations[0]);
                    Assert.Equal("dbo", dropOperation.Schema);
                    Assert.Equal("Pelican", dropOperation.Table);
                    Assert.Equal("AK_Pelican_AlternateId", dropOperation.Name);

                    var addOperation = Assert.IsType<AddUniqueConstraintOperation>(operations[1]);
                    Assert.Equal("dbo", addOperation.Schema);
                    Assert.Equal("Pelican", addOperation.Table);
                    Assert.Equal("AK_dbo.Pelican_AlternateId", addOperation.Name);
                    Assert.Equal(new[] { "AlternateId" }, addOperation.Columns);
                });
        }

        [Fact]
        public void Alter_unique_constraint_columns()
        {
            Execute(
                source => source.Entity(
                    "Rook",
                    x =>
                    {
                        x.ToTable("Rook", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("AlternateId");
                        x.HasAlternateKey("AlternateId");
                        x.Property<int>("AlternateRookId");
                    }),
                target => target.Entity(
                    "Rook",
                    x =>
                    {
                        x.ToTable("Rook", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("AlternateId");
                        x.Property<int>("AlternateRookId");
                        x.HasAlternateKey("AlternateRookId").HasName("AK_Rook_AlternateId");
                    }),
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var dropOperation = Assert.IsType<DropUniqueConstraintOperation>(operations[0]);
                    Assert.Equal("dbo", dropOperation.Schema);
                    Assert.Equal("Rook", dropOperation.Table);
                    Assert.Equal("AK_Rook_AlternateId", dropOperation.Name);

                    var addOperation = Assert.IsType<AddUniqueConstraintOperation>(operations[1]);
                    Assert.Equal("dbo", addOperation.Schema);
                    Assert.Equal("Rook", addOperation.Table);
                    Assert.Equal("AK_Rook_AlternateId", addOperation.Name);
                    Assert.Equal(new[] { "AlternateRookId" }, addOperation.Columns);
                });
        }

        [Fact]
        public void Rename_primary_key()
        {
            Execute(
                source => source.Entity(
                    "Puffin",
                    x =>
                    {
                        x.ToTable("Puffin", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                target => target.Entity(
                    "Puffin",
                    x =>
                    {
                        x.ToTable("Puffin", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id").HasName("PK_dbo.Puffin");
                    }),
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var dropOperation = Assert.IsType<DropPrimaryKeyOperation>(operations[0]);
                    Assert.Equal("dbo", dropOperation.Schema);
                    Assert.Equal("Puffin", dropOperation.Table);
                    Assert.Equal("PK_Puffin", dropOperation.Name);

                    var addOperation = Assert.IsType<AddPrimaryKeyOperation>(operations[1]);
                    Assert.Equal("dbo", addOperation.Schema);
                    Assert.Equal("Puffin", addOperation.Table);
                    Assert.Equal("PK_dbo.Puffin", addOperation.Name);
                    Assert.Equal(new[] { "Id" }, addOperation.Columns);
                });
        }

        [Fact]
        public void Alter_primary_key_columns()
        {
            Execute(
                source => source.Entity(
                    "Raven",
                    x =>
                    {
                        x.ToTable("Raven", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("RavenId");
                    }),
                target => target.Entity(
                    "Raven",
                    x =>
                    {
                        x.ToTable("Raven", "dbo");
                        x.Property<int>("Id");
                        x.Property<int>("RavenId");
                        x.HasKey("RavenId");
                    }),
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var dropOperation = Assert.IsType<DropPrimaryKeyOperation>(operations[0]);
                    Assert.Equal("dbo", dropOperation.Schema);
                    Assert.Equal("Raven", dropOperation.Table);
                    Assert.Equal("PK_Raven", dropOperation.Name);

                    var addOperation = Assert.IsType<AddPrimaryKeyOperation>(operations[1]);
                    Assert.Equal("dbo", addOperation.Schema);
                    Assert.Equal("Raven", addOperation.Table);
                    Assert.Equal("PK_Raven", addOperation.Name);
                    Assert.Equal(new[] { "RavenId" }, addOperation.Columns);
                });
        }

        [Fact]
        public void Add_foreign_key()
        {
            Execute(
                source => source.Entity(
                    "Amoeba",
                    x =>
                    {
                        x.ToTable("Amoeba", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId");
                    }),
                target => target.Entity(
                    "Amoeba",
                    x =>
                    {
                        x.ToTable("Amoeba", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId");
                        x.HasOne("Amoeba").WithMany().HasForeignKey("ParentId");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddForeignKeyOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Amoeba", operation.Table);
                    Assert.Equal("FK_Amoeba_Amoeba_ParentId", operation.Name);
                    Assert.Equal(new[] { "ParentId" }, operation.Columns);
                    Assert.Equal("dbo", operation.PrincipalSchema);
                    Assert.Equal("Amoeba", operation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, operation.PrincipalColumns);
                    Assert.Equal(ReferentialAction.Cascade, operation.OnDelete);
                    Assert.Equal(ReferentialAction.NoAction, operation.OnUpdate);
                });
        }

        [Fact]
        public void Add_optional_foreign_key()
        {
            Execute(
                source => source.Entity(
                    "Amoeba",
                    x =>
                    {
                        x.ToTable("Amoeba", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int?>("ParentId");
                    }),
                target => target.Entity(
                    "Amoeba",
                    x =>
                    {
                        x.ToTable("Amoeba", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int?>("ParentId");
                        x.HasOne("Amoeba").WithMany().HasForeignKey("ParentId");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddForeignKeyOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Amoeba", operation.Table);
                    Assert.Equal("FK_Amoeba_Amoeba_ParentId", operation.Name);
                    Assert.Equal(new[] { "ParentId" }, operation.Columns);
                    Assert.Equal("dbo", operation.PrincipalSchema);
                    Assert.Equal("Amoeba", operation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, operation.PrincipalColumns);
                    Assert.Equal(ReferentialAction.Restrict, operation.OnDelete);
                    Assert.Equal(ReferentialAction.NoAction, operation.OnUpdate);
                });
        }

        [Fact]
        public void Add_optional_foreign_key_with_cascade_delete()
        {
            Execute(
                source => source.Entity(
                    "Amoeba",
                    x =>
                    {
                        x.ToTable("Amoeba", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int?>("ParentId");
                    }),
                target => target.Entity(
                    "Amoeba",
                    x =>
                    {
                        x.ToTable("Amoeba", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int?>("ParentId");
                        x.HasOne("Amoeba").WithMany().HasForeignKey("ParentId").OnDelete(DeleteBehavior.Cascade);
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddForeignKeyOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Amoeba", operation.Table);
                    Assert.Equal("FK_Amoeba_Amoeba_ParentId", operation.Name);
                    Assert.Equal(new[] { "ParentId" }, operation.Columns);
                    Assert.Equal("dbo", operation.PrincipalSchema);
                    Assert.Equal("Amoeba", operation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, operation.PrincipalColumns);
                    Assert.Equal(ReferentialAction.Cascade, operation.OnDelete);
                    Assert.Equal(ReferentialAction.NoAction, operation.OnUpdate);
                });
        }

        [Fact]
        public void Add_required_foreign_key_without_cascade_delete()
        {
            Execute(
                source => source.Entity(
                    "Amoeba",
                    x =>
                    {
                        x.ToTable("Amoeba", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId");
                    }),
                target => target.Entity(
                    "Amoeba",
                    x =>
                    {
                        x.ToTable("Amoeba", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId");
                        x.HasOne("Amoeba").WithMany().HasForeignKey("ParentId").OnDelete(DeleteBehavior.Restrict);
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddForeignKeyOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Amoeba", operation.Table);
                    Assert.Equal("FK_Amoeba_Amoeba_ParentId", operation.Name);
                    Assert.Equal(new[] { "ParentId" }, operation.Columns);
                    Assert.Equal("dbo", operation.PrincipalSchema);
                    Assert.Equal("Amoeba", operation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, operation.PrincipalColumns);
                    Assert.Equal(ReferentialAction.Restrict, operation.OnDelete);
                    Assert.Equal(ReferentialAction.NoAction, operation.OnUpdate);
                });
        }

        [Fact]
        public void Add_optional_foreign_key_wit_set_null()
        {
            Execute(
                source => source.Entity(
                    "Amoeba",
                    x =>
                    {
                        x.ToTable("Amoeba", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int?>("ParentId");
                    }),
                target => target.Entity(
                    "Amoeba",
                    x =>
                    {
                        x.ToTable("Amoeba", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int?>("ParentId");
                        x.HasOne("Amoeba").WithMany().HasForeignKey("ParentId").OnDelete(DeleteBehavior.SetNull);
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddForeignKeyOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Amoeba", operation.Table);
                    Assert.Equal("FK_Amoeba_Amoeba_ParentId", operation.Name);
                    Assert.Equal(new[] { "ParentId" }, operation.Columns);
                    Assert.Equal("dbo", operation.PrincipalSchema);
                    Assert.Equal("Amoeba", operation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, operation.PrincipalColumns);
                    Assert.Equal(ReferentialAction.SetNull, operation.OnDelete);
                    Assert.Equal(ReferentialAction.NoAction, operation.OnUpdate);
                });
        }

        [Fact]
        public void Remove_foreign_key()
        {
            Execute(
                source => source.Entity(
                    "Anemone",
                    x =>
                    {
                        x.ToTable("Anemone", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId");
                        x.HasOne("Anemone").WithMany().HasForeignKey("ParentId");
                    }),
                target => target.Entity(
                    "Anemone",
                    x =>
                    {
                        x.ToTable("Anemone", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<DropForeignKeyOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Anemone", operation.Table);
                    Assert.Equal("FK_Anemone_Anemone_ParentId", operation.Name);
                });
        }

        [Fact]
        public void Rename_foreign_key()
        {
            Execute(
                source => source.Entity(
                    "Nematode",
                    x =>
                    {
                        x.ToTable("Nematode", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId");
                        x.HasOne("Nematode").WithMany().HasForeignKey("ParentId");
                    }),
                target => target.Entity(
                    "Nematode",
                    x =>
                    {
                        x.ToTable("Nematode", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId");
                        x.HasOne("Nematode").WithMany().HasForeignKey("ParentId").HasConstraintName("FK_Nematode_NematodeParent");
                    }),
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var dropOperation = Assert.IsType<DropForeignKeyOperation>(operations[0]);
                    Assert.Equal("dbo", dropOperation.Schema);
                    Assert.Equal("Nematode", dropOperation.Table);
                    Assert.Equal("FK_Nematode_Nematode_ParentId", dropOperation.Name);

                    var addOperation = Assert.IsType<AddForeignKeyOperation>(operations[1]);
                    Assert.Equal("dbo", addOperation.Schema);
                    Assert.Equal("Nematode", addOperation.Table);
                    Assert.Equal("FK_Nematode_NematodeParent", addOperation.Name);
                    Assert.Equal(new[] { "ParentId" }, addOperation.Columns);
                    Assert.Equal("dbo", addOperation.PrincipalSchema);
                    Assert.Equal("Nematode", addOperation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, addOperation.PrincipalColumns);
                });
        }

        [Fact]
        public void Alter_foreign_key_columns()
        {
            Execute(
                source => source.Entity(
                    "Mushroom",
                    x =>
                    {
                        x.ToTable("Mushroom", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId1");
                        x.HasOne("Mushroom").WithMany().HasForeignKey("ParentId1");
                        x.Property<int>("ParentId2");
                    }),
                target => target.Entity(
                    "Mushroom",
                    x =>
                    {
                        x.ToTable("Mushroom", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId1");
                        x.Property<int>("ParentId2");
                        x.HasOne("Mushroom").WithMany().HasForeignKey("ParentId2").HasConstraintName("FK_Mushroom_Mushroom_ParentId1");
                    }),
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var dropOperation = Assert.IsType<DropForeignKeyOperation>(operations[0]);
                    Assert.Equal("dbo", dropOperation.Schema);
                    Assert.Equal("Mushroom", dropOperation.Table);
                    Assert.Equal("FK_Mushroom_Mushroom_ParentId1", dropOperation.Name);

                    var addOperation = Assert.IsType<AddForeignKeyOperation>(operations[1]);
                    Assert.Equal("dbo", addOperation.Schema);
                    Assert.Equal("Mushroom", addOperation.Table);
                    Assert.Equal("FK_Mushroom_Mushroom_ParentId1", addOperation.Name);
                    Assert.Equal(new[] { "ParentId2" }, addOperation.Columns);
                    Assert.Equal("dbo", addOperation.PrincipalSchema);
                    Assert.Equal("Mushroom", addOperation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, addOperation.PrincipalColumns);
                });
        }

        [Fact]
        public void Alter_foreign_key_cascade_delete()
        {
            Execute(
                source => source.Entity(
                    "Mushroom",
                    x =>
                    {
                        x.ToTable("Mushroom", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId1");
                        x.HasOne("Mushroom").WithMany().HasForeignKey("ParentId1").OnDelete(DeleteBehavior.Restrict);
                        x.Property<int>("ParentId2");
                    }),
                target => target.Entity(
                    "Mushroom",
                    x =>
                    {
                        x.ToTable("Mushroom", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId1");
                        x.HasOne("Mushroom").WithMany().HasForeignKey("ParentId1").OnDelete(DeleteBehavior.Cascade);
                        x.Property<int>("ParentId2");
                    }),
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var dropOperation = Assert.IsType<DropForeignKeyOperation>(operations[0]);
                    Assert.Equal("dbo", dropOperation.Schema);
                    Assert.Equal("Mushroom", dropOperation.Table);
                    Assert.Equal("FK_Mushroom_Mushroom_ParentId1", dropOperation.Name);

                    var addOperation = Assert.IsType<AddForeignKeyOperation>(operations[1]);
                    Assert.Equal("dbo", addOperation.Schema);
                    Assert.Equal("Mushroom", addOperation.Table);
                    Assert.Equal("FK_Mushroom_Mushroom_ParentId1", addOperation.Name);
                    Assert.Equal(new[] { "ParentId1" }, addOperation.Columns);
                    Assert.Equal("dbo", addOperation.PrincipalSchema);
                    Assert.Equal("Mushroom", addOperation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, addOperation.PrincipalColumns);
                    Assert.Equal(ReferentialAction.Cascade, addOperation.OnDelete);
                    Assert.Equal(ReferentialAction.NoAction, addOperation.OnUpdate);
                });
        }

        [Fact]
        public void Alter_foreign_key_target()
        {
            Execute(
                source =>
                {
                    source.Entity(
                        "Lion",
                        x =>
                        {
                            x.ToTable("Lion", "odb");
                            x.Property<int>("LionId");
                            x.HasKey("LionId");
                        });
                    source.Entity(
                        "Tiger",
                        x =>
                        {
                            x.ToTable("Tiger", "bod");
                            x.Property<int>("TigerId");
                            x.HasKey("TigerId");
                        });
                    source.Entity(
                        "Liger",
                        x =>
                        {
                            x.ToTable("Liger", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("ParentId");
                            x.HasOne("Lion").WithMany().HasForeignKey("ParentId");
                        });
                },
                target =>
                {
                    target.Entity(
                        "Lion",
                        x =>
                        {
                            x.ToTable("Lion", "odb");
                            x.Property<int>("LionId");
                            x.HasKey("LionId");
                        });
                    target.Entity(
                        "Tiger",
                        x =>
                        {
                            x.ToTable("Tiger", "bod");
                            x.Property<int>("TigerId");
                            x.HasKey("TigerId");
                        });
                    target.Entity(
                        "Liger",
                        x =>
                        {
                            x.ToTable("Liger", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("ParentId");
                            x.HasOne("Tiger").WithMany().HasForeignKey("ParentId").HasConstraintName("FK_Liger_Lion_ParentId");
                        });
                },
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var dropOperation = Assert.IsType<DropForeignKeyOperation>(operations[0]);
                    Assert.Equal("dbo", dropOperation.Schema);
                    Assert.Equal("Liger", dropOperation.Table);
                    Assert.Equal("FK_Liger_Lion_ParentId", dropOperation.Name);

                    var addOperation = Assert.IsType<AddForeignKeyOperation>(operations[1]);
                    Assert.Equal("dbo", addOperation.Schema);
                    Assert.Equal("Liger", addOperation.Table);
                    Assert.Equal("FK_Liger_Lion_ParentId", addOperation.Name);
                    Assert.Equal(new[] { "ParentId" }, addOperation.Columns);
                    Assert.Equal("bod", addOperation.PrincipalSchema);
                    Assert.Equal("Tiger", addOperation.PrincipalTable);
                    Assert.Equal(new[] { "TigerId" }, addOperation.PrincipalColumns);
                });
        }

        [Fact]
        public void Create_index()
        {
            Execute(
                source => source.Entity(
                    "Hippo",
                    x =>
                    {
                        x.ToTable("Hippo", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                    }),
                target => target.Entity(
                    "Hippo",
                    x =>
                    {
                        x.ToTable("Hippo", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                        x.HasIndex("Value").IsUnique();
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<CreateIndexOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Hippo", operation.Table);
                    Assert.Equal("IX_Hippo_Value", operation.Name);
                    Assert.Equal(new[] { "Value" }, operation.Columns);
                    Assert.True(operation.IsUnique);
                });
        }

        [Fact]
        public void Drop_index()
        {
            Execute(
                source => source.Entity(
                    "Horse",
                    x =>
                    {
                        x.ToTable("Horse", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                        x.HasIndex("Value");
                    }),
                target => target.Entity(
                    "Horse",
                    x =>
                    {
                        x.ToTable("Horse", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<DropIndexOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Horse", operation.Table);
                    Assert.Equal("IX_Horse_Value", operation.Name);
                });
        }

        [Fact]
        public void Rename_index()
        {
            Execute(
                source => source.Entity(
                    "Donkey",
                    x =>
                    {
                        x.ToTable("Donkey", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                        x.HasIndex("Value");
                    }),
                target => target.Entity(
                    "Donkey",
                    x =>
                    {
                        x.ToTable("Donkey", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                        x.HasIndex("Value").HasName("IX_dbo.Donkey_Value");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<RenameIndexOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Donkey", operation.Table);
                    Assert.Equal("IX_Donkey_Value", operation.Name);
                    Assert.Equal("IX_dbo.Donkey_Value", operation.NewName);
                });
        }

        [Fact]
        public void Alter_index_columns()
        {
            Execute(
                source => source.Entity(
                    "Muel",
                    x =>
                    {
                        x.ToTable("Muel", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                        x.HasIndex("Value");
                        x.Property<int>("MuleValue");
                    }),
                target => target.Entity(
                    "Muel",
                    x =>
                    {
                        x.ToTable("Muel", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                        x.Property<int>("MuleValue");
                        x.HasIndex("MuleValue").HasName("IX_Muel_Value");
                    }),
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var dropOperation = Assert.IsType<DropIndexOperation>(operations[0]);
                    Assert.Equal("dbo", dropOperation.Schema);
                    Assert.Equal("Muel", dropOperation.Table);
                    Assert.Equal("IX_Muel_Value", dropOperation.Name);

                    var createOperation = Assert.IsType<CreateIndexOperation>(operations[1]);
                    Assert.Equal("dbo", createOperation.Schema);
                    Assert.Equal("Muel", createOperation.Table);
                    Assert.Equal("IX_Muel_Value", createOperation.Name);
                    Assert.Equal(new[] { "MuleValue" }, createOperation.Columns);
                });
        }

        [Fact]
        public void Alter_index_uniqueness()
        {
            Execute(
                source => source.Entity(
                    "Pony",
                    x =>
                    {
                        x.ToTable("Pony", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                        x.HasIndex("Value").IsUnique(false);
                    }),
                target => target.Entity(
                    "Pony",
                    x =>
                    {
                        x.ToTable("Pony", "dbo");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                        x.HasIndex("Value").IsUnique(true);
                    }),
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var dropOperation = Assert.IsType<DropIndexOperation>(operations[0]);
                    Assert.Equal("dbo", dropOperation.Schema);
                    Assert.Equal("Pony", dropOperation.Table);
                    Assert.Equal("IX_Pony_Value", dropOperation.Name);

                    var createOperation = Assert.IsType<CreateIndexOperation>(operations[1]);
                    Assert.Equal("dbo", createOperation.Schema);
                    Assert.Equal("Pony", createOperation.Table);
                    Assert.Equal("IX_Pony_Value", createOperation.Name);
                    Assert.True(createOperation.IsUnique);
                });
        }

        [Fact]
        public void Create_sequence()
        {
            Execute(
                _ => { },
                modelBuilder => modelBuilder.HasSequence<int>("Tango", "dbo")
                    .StartsAt(2)
                    .IncrementsBy(3)
                    .HasMin(1)
                    .HasMax(4)
                    .IsCyclic(),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<CreateSequenceOperation>(operations[0]);
                    Assert.Equal("Tango", operation.Name);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal(typeof(int), operation.ClrType);
                    Assert.Equal(2, operation.StartValue);
                    Assert.Equal(3, operation.IncrementBy);
                    Assert.Equal(1, operation.MinValue);
                    Assert.Equal(4, operation.MaxValue);
                    Assert.True(operation.IsCyclic);
                });
        }

        [Fact]
        public void Drop_sequence()
        {
            Execute(
                modelBuilder => modelBuilder.HasSequence("Bravo", "dbo"),
                _ => { },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<DropSequenceOperation>(operations[0]);
                    Assert.Equal("Bravo", operation.Name);
                    Assert.Equal("dbo", operation.Schema);
                });
        }

        [Fact]
        public void Rename_sequence()
        {
            Execute(
                source => source.HasSequence("Bravo", "dbo"),
                target => target.HasSequence("bravo", "dbo"),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<RenameSequenceOperation>(operations[0]);
                    Assert.Equal("Bravo", operation.Name);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("bravo", operation.NewName);
                    Assert.Null(operation.NewSchema);
                });
        }

        [Fact]
        public void Move_sequence()
        {
            Execute(
                source => source.HasSequence("Charlie", "dbo"),
                target => target.HasSequence("Charlie", "odb"),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<RenameSequenceOperation>(operations[0]);
                    Assert.Equal("Charlie", operation.Name);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Null(operation.NewName);
                    Assert.Equal("odb", operation.NewSchema);
                });
        }

        [Fact]
        public void Alter_sequence_increment_by()
        {
            Execute(
                source => source.HasSequence<int>("Alpha", "dbo")
                    .StartsAt(2)
                    .IncrementsBy(3)
                    .HasMin(1)
                    .HasMax(4)
                    .IsCyclic(),
                source => source.HasSequence<int>("Alpha", "dbo")
                    .StartsAt(2)
                    .IncrementsBy(5)
                    .HasMin(1)
                    .HasMax(4)
                    .IsCyclic(),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AlterSequenceOperation>(operations[0]);
                    Assert.Equal("Alpha", operation.Name);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal(5, operation.IncrementBy);
                    Assert.Equal(1, operation.MinValue);
                    Assert.Equal(4, operation.MaxValue);
                    Assert.True(operation.IsCyclic);
                });
        }

        [Fact]
        public void Alter_sequence_max_value()
        {
            Execute(
                source => source.HasSequence<int>("Echo", "dbo")
                    .StartsAt(2)
                    .IncrementsBy(3)
                    .HasMin(1)
                    .HasMax(4)
                    .IsCyclic(),
                source => source.HasSequence<int>("Echo", "dbo")
                    .StartsAt(2)
                    .IncrementsBy(3)
                    .HasMin(1)
                    .HasMax(5)
                    .IsCyclic(),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AlterSequenceOperation>(operations[0]);
                    Assert.Equal("Echo", operation.Name);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal(3, operation.IncrementBy);
                    Assert.Equal(1, operation.MinValue);
                    Assert.Equal(5, operation.MaxValue);
                    Assert.True(operation.IsCyclic);
                });
        }

        [Fact]
        public void Alter_sequence_min_value()
        {
            Execute(
                source => source.HasSequence<int>("Delta", "dbo")
                    .StartsAt(2)
                    .IncrementsBy(3)
                    .HasMin(1)
                    .HasMax(4)
                    .IsCyclic(),
                source => source.HasSequence<int>("Delta", "dbo")
                    .StartsAt(2)
                    .IncrementsBy(3)
                    .HasMin(5)
                    .HasMax(4)
                    .IsCyclic(),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AlterSequenceOperation>(operations[0]);
                    Assert.Equal("Delta", operation.Name);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal(3, operation.IncrementBy);
                    Assert.Equal(5, operation.MinValue);
                    Assert.Equal(4, operation.MaxValue);
                    Assert.True(operation.IsCyclic);
                });
        }

        [Fact]
        public void Alter_sequence_cycle()
        {
            Execute(
                source => source.HasSequence<int>("Foxtrot", "dbo")
                    .StartsAt(2)
                    .IncrementsBy(3)
                    .HasMin(1)
                    .HasMax(4)
                    .IsCyclic(true),
                source => source.HasSequence<int>("Foxtrot", "dbo")
                    .StartsAt(2)
                    .IncrementsBy(3)
                    .HasMin(1)
                    .HasMax(4)
                    .IsCyclic(false),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AlterSequenceOperation>(operations[0]);
                    Assert.Equal("Foxtrot", operation.Name);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal(3, operation.IncrementBy);
                    Assert.Equal(1, operation.MinValue);
                    Assert.Equal(4, operation.MaxValue);
                    Assert.False(operation.IsCyclic);
                });
        }

        [Fact]
        public void Alter_sequence_type()
        {
            Execute(
                source => source.HasSequence<int>("Hotel", "dbo")
                    .StartsAt(2)
                    .IncrementsBy(3)
                    .HasMin(1)
                    .HasMax(4)
                    .IsCyclic(),
                source => source.HasSequence<long>("Hotel", "dbo")
                    .StartsAt(2)
                    .IncrementsBy(3)
                    .HasMin(1)
                    .HasMax(4)
                    .IsCyclic(),
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var dropOperation = Assert.IsType<DropSequenceOperation>(operations[0]);
                    Assert.Equal("Hotel", dropOperation.Name);
                    Assert.Equal("dbo", dropOperation.Schema);

                    var createOperation = Assert.IsType<CreateSequenceOperation>(operations[1]);
                    Assert.Equal("Hotel", createOperation.Name);
                    Assert.Equal("dbo", createOperation.Schema);
                    Assert.Equal(typeof(long), createOperation.ClrType);
                    Assert.Equal(2, createOperation.StartValue);
                    Assert.Equal(3, createOperation.IncrementBy);
                    Assert.Equal(1, createOperation.MinValue);
                    Assert.Equal(4, createOperation.MaxValue);
                    Assert.True(createOperation.IsCyclic);
                });
        }

        [Fact]
        public void Alter_sequence_start()
        {
            Execute(
                source => source.HasSequence<int>("Golf", "dbo")
                    .StartsAt(2)
                    .IncrementsBy(3)
                    .HasMin(1)
                    .HasMax(4)
                    .IsCyclic(),
                source => source.HasSequence<int>("Golf", "dbo")
                    .StartsAt(5)
                    .IncrementsBy(3)
                    .HasMin(1)
                    .HasMax(4)
                    .IsCyclic(),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<RestartSequenceOperation>(operations[0]);

                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Golf", operation.Name);
                    Assert.Equal(5, operation.StartValue);
                });
        }

        [Fact]
        public void Diff_IProperty_destructive_when_null_to_not_null()
        {
            Execute(
                source => source.Entity(
                    "Lizard",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int?>("Value");
                    }),
                target => target.Entity(
                    "Lizard",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AlterColumnOperation>(operations[0]);
                    Assert.True(operation.IsDestructiveChange);
                });
        }

        [Fact]
        public void Diff_IProperty_not_destructive_when_not_null_to_null()
        {
            Execute(
                source => source.Entity(
                    "Frog",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                    }),
                target => target.Entity(
                    "Frog",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int?>("Value");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AlterColumnOperation>(operations[0]);
                    Assert.False(operation.IsDestructiveChange);
                });
        }

        [Fact]
        public void Diff_IProperty_destructive_when_type_changed()
        {
            Execute(
                source => source.Entity(
                    "Frog",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                    }),
                target => target.Entity(
                    "Frog",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value").HasColumnType("integer");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AlterColumnOperation>(operations[0]);
                    Assert.True(operation.IsDestructiveChange);
                });
        }

        [Fact]
        public void Sort_works_with_primary_keys_and_columns()
        {
            Execute(
                source => source.Entity(
                    "Jaguar",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                target => target.Entity(
                    "Jaguar",
                    x =>
                    {
                        x.Property<string>("Name");
                        x.HasKey("Name");
                    }),
                operations => Assert.Collection(
                    operations,
                    o => Assert.IsType<DropPrimaryKeyOperation>(o),
                    o => Assert.IsType<DropColumnOperation>(o),
                    o => Assert.IsType<AddColumnOperation>(o),
                    o => Assert.IsType<AddPrimaryKeyOperation>(o)));
        }

        [Fact]
        public void Sort_adds_unique_constraint_after_column()
        {
            Execute(
                source => source.Entity(
                    "Panther",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                target => target.Entity(
                    "Panther",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("AlternateId");
                        x.HasAlternateKey("AlternateId");
                    }),
                operations => Assert.Collection(
                    operations,
                    o => Assert.IsType<AddColumnOperation>(o),
                    o => Assert.IsType<AddUniqueConstraintOperation>(o)));
        }

        [Fact]
        public void Sort_drops_unique_constraint_before_column()
        {
            Execute(
                source => source.Entity(
                    "Bobcat",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("AlternateId");
                        x.HasAlternateKey("AlternateId");
                    }),
                target => target.Entity(
                    "Bobcat",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                operations => Assert.Collection(
                    operations,
                    o => Assert.IsType<DropUniqueConstraintOperation>(o),
                    o => Assert.IsType<DropColumnOperation>(o)));
        }

        [Fact]
        public void Sort_creates_index_after_column()
        {
            Execute(
                source => source.Entity(
                    "Coyote",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                target => target.Entity(
                    "Coyote",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                        x.HasIndex("Value");
                    }),
                operations => Assert.Collection(
                    operations,
                    o => Assert.IsType<AddColumnOperation>(o),
                    o => Assert.IsType<CreateIndexOperation>(o)));
        }

        [Fact]
        public void Sort_drops_index_before_column()
        {
            Execute(
                source => source.Entity(
                    "Wolf",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("Value");
                        x.HasIndex("Value");
                    }),
                target => target.Entity(
                    "Wolf",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                operations => Assert.Collection(
                    operations,
                    o => Assert.IsType<DropIndexOperation>(o),
                    o => Assert.IsType<DropColumnOperation>(o)));
        }

        [Fact]
        public void Sort_adds_foreign_key_after_column()
        {
            Execute(
                source => source.Entity(
                    "Algae",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                target => target.Entity(
                    "Algae",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId");
                        x.HasOne("Algae").WithMany().HasForeignKey("ParentId");
                    }),
                operations => Assert.Collection(
                    operations,
                    o => Assert.IsType<AddColumnOperation>(o),
                    o => Assert.IsType<AddForeignKeyOperation>(o)));
        }

        [Fact]
        public void Sort_drops_foreign_key_before_column()
        {
            Execute(
                source => source.Entity(
                    "Bacteria",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId");
                        x.HasOne("Bacteria").WithMany().HasForeignKey("ParentId");
                    }),
                target => target.Entity(
                    "Bacteria",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                operations => Assert.Collection(
                    operations,
                    o => Assert.IsType<DropForeignKeyOperation>(o),
                    o => Assert.IsType<DropColumnOperation>(o)));
        }

        [Fact]
        public void Sort_adds_foreign_key_after_target_table()
        {
            Execute(
                source => source.Entity(
                    "Car",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("MakerId");
                    }),
                target =>
                {
                    target.Entity(
                        "Maker",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    target.Entity(
                        "Car",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("MakerId");
                            x.HasOne("Maker").WithMany().HasForeignKey("MakerId");
                        });
                },
                operations => Assert.Collection(
                    operations,
                    o => Assert.IsType<CreateTableOperation>(o),
                    o => Assert.IsType<AddForeignKeyOperation>(o)));
        }

        [Fact]
        public void Sort_drops_foreign_key_before_target_table()
        {
            Execute(
                source =>
                {
                    source.Entity(
                        "Maker",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    source.Entity(
                        "Boat",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("MakerId");
                            x.HasOne("Maker").WithMany().HasForeignKey("MakerId");
                        });
                },
                target => target.Entity(
                    "Boat",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("MakerId");
                    }),
                operations => Assert.Collection(
                    operations,
                    o => Assert.IsType<DropForeignKeyOperation>(o),
                    o => Assert.IsType<DropTableOperation>(o)));
        }

        [Fact]
        public void Sort_adds_foreign_key_after_target_column_and_unique_constraint()
        {
            Execute(
                source =>
                {
                    source.Entity(
                        "Maker",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    source.Entity(
                        "Airplane",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("MakerId");
                        });
                },
                target =>
                {
                    target.Entity(
                        "Maker",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("AlternateId");
                        });
                    target.Entity(
                        "Airplane",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("MakerId");
                            x.HasOne("Maker").WithMany().HasForeignKey("MakerId").HasPrincipalKey("AlternateId");
                        });
                },
                operations => Assert.Collection(
                    operations,
                    o => Assert.IsType<AddColumnOperation>(o),
                    o => Assert.IsType<AddUniqueConstraintOperation>(o),
                    o => Assert.IsType<AddForeignKeyOperation>(o)));
        }

        [Fact]
        public void Sort_drops_foreign_key_before_target_column_and_unique_constraint()
        {
            Execute(
                source =>
                {
                    source.Entity(
                        "Maker",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("AlternateId");
                        });
                    source.Entity(
                        "Submarine",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("MakerId");
                            x.HasOne("Maker").WithMany().HasForeignKey("MakerId").HasPrincipalKey("AlternateId");
                        });
                },
                target =>
                {
                    target.Entity(
                        "Maker",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    target.Entity(
                        "Submarine",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("MakerId");
                        });
                },
                operations => Assert.Collection(
                    operations,
                    o => Assert.IsType<DropForeignKeyOperation>(o),
                    o => Assert.IsType<DropUniqueConstraintOperation>(o),
                    o => Assert.IsType<DropColumnOperation>(o)));
        }

        [Fact]
        public void Sort_creates_tables_in_topologic_order()
        {
            Execute(
                _ => { },
                modelBuilder =>
                {
                    modelBuilder.Entity(
                        "Maker",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    modelBuilder.Entity(
                        "Helicopter",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("MakerId");
                            x.HasOne("Maker").WithMany().HasForeignKey("MakerId");
                        });
                },
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var operation1 = Assert.IsType<CreateTableOperation>(operations[0]);
                    Assert.Equal("Maker", operation1.Name);

                    var operation2 = Assert.IsType<CreateTableOperation>(operations[1]);
                    Assert.Equal("Helicopter", operation2.Name);
                });
        }

        [Fact]
        public void Sort_drops_tables_in_topologic_order()
        {
            Execute(
                modelBuilder =>
                {
                    modelBuilder.Entity(
                        "Maker",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    modelBuilder.Entity(
                        "Glider",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("MakerId");
                            x.HasOne("Maker").WithMany().HasForeignKey("MakerId");
                        });
                },
                _ => { },
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    var operation1 = Assert.IsType<DropTableOperation>(operations[0]);
                    Assert.Equal("Glider", operation1.Name);

                    var operation2 = Assert.IsType<DropTableOperation>(operations[1]);
                    Assert.Equal("Maker", operation2.Name);
                });
        }

        [Fact]
        public void Rename_column_with_primary_key()
        {
            Execute(
                source => source.Entity(
                    "Hornet",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                    }),
                target => target.Entity(
                    "Hornet",
                    x =>
                    {
                        x.Property<int>("Id").HasColumnName("HornetId");
                        x.HasKey("Id");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    Assert.IsType<RenameColumnOperation>(operations[0]);
                });
        }

        [Fact]
        public void Rename_column_with_unique_constraint()
        {
            Execute(
                source => source.Entity(
                    "Wasp",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name");
                        x.HasAlternateKey("Name");
                    }),
                target => target.Entity(
                    "Wasp",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name").HasColumnName("WaspName");
                        x.HasAlternateKey("Name");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    Assert.IsType<RenameColumnOperation>(operations[0]);
                });
        }

        [Fact]
        public void Rename_column_with_index()
        {
            Execute(
                source => source.Entity(
                    "Bee",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name");
                        x.HasIndex("Name");
                    }),
                target => target.Entity(
                    "Bee",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name").HasColumnName("BeeName");
                        x.HasIndex("Name");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    Assert.IsType<RenameColumnOperation>(operations[0]);
                });
        }

        [Fact]
        public void Rename_table_with_unique_constraint()
        {
            Execute(
                source => source.Entity(
                    "Fly",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name");
                        x.HasAlternateKey("Name");
                    }),
                target => target.Entity(
                    "Fly",
                    x =>
                    {
                        x.ToTable("Flies");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name");
                        x.HasAlternateKey("Name");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    Assert.IsType<RenameTableOperation>(operations[0]);
                });
        }

        [Fact]
        public void Rename_table_with_index()
        {
            Execute(
                source => source.Entity(
                    "Gnat",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name");
                        x.HasIndex("Name");
                    }),
                target => target.Entity(
                    "Gnat",
                    x =>
                    {
                        x.ToTable("Gnats");
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name");
                        x.HasIndex("Name");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    Assert.IsType<RenameTableOperation>(operations[0]);
                });
        }

        [Fact]
        public void Rename_entity_type_with_primary_key_and_unique_constraint()
        {
            Execute(
                source => source.Entity(
                    "Grasshopper",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name");
                        x.HasAlternateKey("Name");
                    }),
                target => target.Entity(
                    "grasshopper",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id").HasName("PK_Grasshopper");
                        x.Property<string>("Name");
                        x.HasAlternateKey("Name").HasName("AK_Grasshopper_Name");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    Assert.IsType<RenameTableOperation>(operations[0]);
                });
        }

        [Fact]
        public void Rename_entity_type_with_index()
        {
            Execute(
                source => source.Entity(
                    "Cricket",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<string>("Name");
                        x.HasIndex("Name");
                    }),
                target => target.Entity(
                    "cricket",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id").HasName("PK_Cricket");
                        x.Property<string>("Name");
                        x.HasIndex("Name").HasName("IX_Cricket_Name");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    Assert.IsType<RenameTableOperation>(operations[0]);
                });
        }

        [Fact]
        public void Rename_column_with_foreign_key()
        {
            Execute(
                source => source.Entity(
                    "Yeast",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId");
                        x.HasOne("Yeast").WithMany().HasForeignKey("ParentId");
                    }),
                target => target.Entity(
                    "Yeast",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId").HasColumnName("ParentYeastId");
                        x.HasOne("Yeast").WithMany().HasForeignKey("ParentId");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);
                    Assert.IsType<RenameColumnOperation>(operations[0]);
                });
        }

        [Fact]
        public void Rename_column_with_referencing_foreign_key()
        {
            Execute(
                source => source.Entity(
                    "Mucor",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<int>("ParentId");
                        x.HasOne("Mucor").WithMany().HasForeignKey("ParentId");
                    }),
                target => target.Entity(
                    "Mucor",
                    x =>
                    {
                        x.Property<int>("Id").HasColumnName("MucorId");
                        x.HasKey("Id");
                        x.Property<int>("ParentId");
                        x.HasOne("Mucor").WithMany().HasForeignKey("ParentId");
                    }),
                operations =>
                {
                    Assert.Equal(1, operations.Count);
                    Assert.IsType<RenameColumnOperation>(operations[0]);
                });
        }

        [Fact]
        public void Rename_table_with_foreign_key()
        {
            Execute(
                source =>
                {
                    source.Entity(
                        "Zebra",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    source.Entity(
                        "Zonkey",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("ParentId");
                            x.HasOne("Zebra").WithMany().HasForeignKey("ParentId");
                        });
                },
                target =>
                {
                    target.Entity(
                        "Zebra",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    target.Entity(
                        "Zonkey",
                        x =>
                        {
                            x.ToTable("Zonkeys");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("ParentId");
                            x.HasOne("Zebra").WithMany().HasForeignKey("ParentId");
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);
                    Assert.IsType<RenameTableOperation>(operations[0]);
                });
        }

        [Fact]
        public void Rename_table_with_referencing_foreign_key()
        {
            Execute(
                source =>
                {
                    source.Entity(
                        "Jaguar",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    source.Entity(
                        "Jaglion",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("ParentId");
                            x.HasOne("Jaguar").WithMany().HasForeignKey("ParentId");
                        });
                },
                target =>
                {
                    target.Entity(
                        "Jaguar",
                        x =>
                        {
                            x.ToTable("Jaguars");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    target.Entity(
                        "Jaglion",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("ParentId");
                            x.HasOne("Jaguar").WithMany().HasForeignKey("ParentId");
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);
                    Assert.IsType<RenameTableOperation>(operations[0]);
                });
        }

        [Fact]
        public void Create_table_with_property_on_subtype()
        {
            Execute(
                _ => { },
                modelBuilder =>
                {
                    EntityType animal = null;
                    modelBuilder.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    modelBuilder.Entity(
                        "Fish",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<string>("Name");
                            x.Metadata.Relational().DiscriminatorValue = "Fish";
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<CreateTableOperation>(operations[0]);
                    Assert.Equal("Animal", operation.Name);
                    Assert.Equal(3, operation.Columns.Count);

                    Assert.Contains(operation.Columns, c => c.Name == "Name");
                });
        }

        [Fact]
        public void Create_table_with_required_property_on_subtype()
        {
            Execute(
                _ => { },
                modelBuilder =>
                {
                    EntityType animal = null;
                    modelBuilder.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    modelBuilder.Entity(
                        "Whale",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<int>("Value");
                            x.Metadata.Relational().DiscriminatorValue = "Whale";
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<CreateTableOperation>(operations[0]);
                    Assert.Equal("Animal", operation.Name);
                    Assert.Equal(3, operation.Columns.Count);

                    Assert.True(operation.Columns.First(c => c.Name == "Value").IsNullable);
                });
        }

        [Fact]
        public void Add_property_on_subtype()
        {
            Execute(
                source =>
                {
                    EntityType animal = null;
                    source.Entity(
                        "Animal",
                        x =>
                        {
                            x.ToTable("Animal", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    source.Entity(
                        "Shark",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Metadata.Relational().DiscriminatorValue = "Shark";
                        });
                },
                target =>
                {
                    EntityType animal = null;
                    target.Entity(
                        "Animal",
                        x =>
                        {
                            x.ToTable("Animal", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    target.Entity(
                        "Shark",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<string>("Name");
                            x.Metadata.Relational().DiscriminatorValue = "Shark";
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddColumnOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Animal", operation.Table);
                    Assert.Equal("Name", operation.Name);
                });
        }

        [Fact]
        public void Add_required_property_on_subtype()
        {
            Execute(
                source =>
                {
                    EntityType animal = null;
                    source.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    source.Entity(
                        "Marlin",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Metadata.Relational().DiscriminatorValue = "Marlin";
                        });
                },
                target =>
                {
                    EntityType animal = null;
                    target.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    target.Entity(
                        "Marlin",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<int>("Value");
                            x.Metadata.Relational().DiscriminatorValue = "Marlin";
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddColumnOperation>(operations[0]);
                    Assert.Equal("Value", operation.Name);
                    Assert.Equal("Value", operation.Name);
                    Assert.True(operation.IsNullable);
                });
        }

        [Fact]
        public void Remove_property_on_subtype()
        {
            Execute(
                source =>
                {
                    EntityType animal = null;
                    source.Entity(
                        "Animal",
                        x =>
                        {
                            x.ToTable("Animal", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    source.Entity(
                        "Blowfish",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<string>("Name");
                            x.Metadata.Relational().DiscriminatorValue = "Blowfish";
                        });
                },
                target =>
                {
                    EntityType animal = null;
                    target.Entity(
                        "Animal",
                        x =>
                        {
                            x.ToTable("Animal", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    target.Entity(
                        "Blowfish",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Metadata.Relational().DiscriminatorValue = "Blowfish";
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<DropColumnOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Animal", operation.Table);
                    Assert.Equal("Name", operation.Name);
                });
        }

        [Fact]
        public void Alter_property_on_subtype()
        {
            Execute(
                source =>
                {
                    EntityType animal = null;
                    source.Entity(
                        "Animal",
                        x =>
                        {
                            x.ToTable("Animal", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    source.Entity(
                        "Barracuda",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<string>("Name");
                            x.Metadata.Relational().DiscriminatorValue = "Barracuda";
                        });
                },
                target =>
                {
                    EntityType animal = null;
                    target.Entity(
                        "Animal",
                        x =>
                        {
                            x.ToTable("Animal", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    target.Entity(
                        "Barracuda",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<string>("Name").HasColumnType("varchar(30)");
                            x.Metadata.Relational().DiscriminatorValue = "Barracuda";
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AlterColumnOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Animal", operation.Table);
                    Assert.Equal("Name", operation.Name);
                    Assert.Equal("varchar(30)", operation.ColumnType);
                });
        }

        [Fact]
        public void Create_index_on_subtype()
        {
            Execute(
                source =>
                {
                    EntityType animal = null;
                    source.Entity(
                        "Animal",
                        x =>
                        {
                            x.ToTable("Animal", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    source.Entity(
                        "Minnow",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<string>("Name");
                            x.Metadata.Relational().DiscriminatorValue = "Minnow";
                        });
                },
                target =>
                {
                    EntityType animal = null;
                    target.Entity(
                        "Animal",
                        x =>
                        {
                            x.ToTable("Animal", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    target.Entity(
                        "Minnow",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<string>("Name");
                            x.HasIndex("Name");
                            x.Metadata.Relational().DiscriminatorValue = "Minnow";
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<CreateIndexOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Animal", operation.Table);
                    Assert.Equal("IX_Minnow_Name", operation.Name);
                    Assert.Equal(new[] { "Name" }, operation.Columns);
                });
        }

        [Fact]
        public void Alter_index_on_subtype()
        {
            Execute(
                source =>
                {
                    EntityType animal = null;
                    source.Entity(
                        "Animal",
                        x =>
                        {
                            x.ToTable("Animal", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    source.Entity(
                        "Pike",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<string>("Name");
                            x.HasIndex("Name");
                            x.Metadata.Relational().DiscriminatorValue = "Pike";
                        });
                },
                target =>
                {
                    EntityType animal = null;
                    target.Entity(
                        "Animal",
                        x =>
                        {
                            x.ToTable("Animal", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    target.Entity(
                        "Pike",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<string>("Name");
                            x.HasIndex("Name").HasName("IX_Animal_Pike_Name");
                            x.Metadata.Relational().DiscriminatorValue = "Pike";
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<RenameIndexOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Animal", operation.Table);
                    Assert.Equal("IX_Pike_Name", operation.Name);
                    Assert.Equal("IX_Animal_Pike_Name", operation.NewName);
                });
        }

        [Fact]
        public void Drop_index_on_subtype()
        {
            Execute(
                source =>
                {
                    EntityType animal = null;
                    source.Entity(
                        "Animal",
                        x =>
                        {
                            x.ToTable("Animal", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    source.Entity(
                        "Catfish",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<string>("Name");
                            x.HasIndex("Name");
                            x.Metadata.Relational().DiscriminatorValue = "Catfish";
                        });
                },
                target =>
                {
                    EntityType animal = null;
                    target.Entity(
                        "Animal",
                        x =>
                        {
                            x.ToTable("Animal", "dbo");
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    target.Entity(
                        "Catfish",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<string>("Name");
                            x.Metadata.Relational().DiscriminatorValue = "Catfish";
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<DropIndexOperation>(operations[0]);
                    Assert.Equal("dbo", operation.Schema);
                    Assert.Equal("Animal", operation.Table);
                    Assert.Equal("IX_Catfish_Name", operation.Name);
                });
        }

        [Fact]
        public void Create_table_with_foreign_key_on_base_type()
        {
            Execute(
                _ => { },
                modelBuilder =>
                {
                    modelBuilder.Entity(
                        "Person",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    modelBuilder.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("HandlerId");
                            x.HasOne("Person").WithMany().HasForeignKey("HandlerId");
                        });
                    modelBuilder.Entity("Wyvern").HasBaseType("Animal");
                },
                operations =>
                {
                    Assert.Equal(2, operations.Count);
                    Assert.IsType<CreateTableOperation>(operations[0]);

                    var createTableOperation = Assert.IsType<CreateTableOperation>(operations[1]);
                    Assert.Equal("Animal", createTableOperation.Name);
                    Assert.Equal(1, createTableOperation.ForeignKeys.Count);

                    var addForeignKeyOperation = createTableOperation.ForeignKeys[0];
                    Assert.Equal("FK_Animal_Person_HandlerId", addForeignKeyOperation.Name);
                    Assert.Equal(new[] { "HandlerId" }, addForeignKeyOperation.Columns);
                    Assert.Equal("Person", addForeignKeyOperation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, addForeignKeyOperation.PrincipalColumns);
                });
        }

        [Fact]
        public void Create_table_with_foreign_key_on_subtype()
        {
            Execute(
                _ => { },
                modelBuilder =>
                {
                    modelBuilder.Entity(
                        "Person",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    EntityType animal = null;
                    modelBuilder.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    modelBuilder.Entity(
                        "Stag",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<int>("HandlerId");
                            x.HasOne("Person").WithMany().HasForeignKey("HandlerId");
                            x.Metadata.Relational().DiscriminatorValue = "Stag";
                        });
                },
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    Assert.IsType<CreateTableOperation>(operations[0]);

                    var createTableOperation = Assert.IsType<CreateTableOperation>(operations[1]);
                    Assert.Equal("Animal", createTableOperation.Name);
                    Assert.Equal(1, createTableOperation.ForeignKeys.Count);

                    var addForeignKeyOperation = createTableOperation.ForeignKeys[0];
                    Assert.Equal("FK_Stag_Person_HandlerId", addForeignKeyOperation.Name);
                    Assert.Equal(new[] { "HandlerId" }, addForeignKeyOperation.Columns);
                    Assert.Equal("Person", addForeignKeyOperation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, addForeignKeyOperation.PrincipalColumns);
                });
        }

        [Fact]
        public void Create_table_with_foreign_key_to_subtype()
        {
            Execute(
                _ => { },
                modelBuilder =>
                {
                    EntityType animal = null;
                    modelBuilder.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    modelBuilder.Entity(
                        "DomesticAnimal",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Metadata.Relational().DiscriminatorValue = "DomesticAnimal";
                        });
                    modelBuilder.Entity(
                        "Person",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("PetId");
                            x.HasOne("DomesticAnimal").WithMany().HasForeignKey("PetId");
                        });
                },
                operations =>
                {
                    Assert.Equal(2, operations.Count);

                    Assert.IsType<CreateTableOperation>(operations[0]);

                    var createTableOperation = Assert.IsType<CreateTableOperation>(operations[1]);
                    Assert.Equal("Person", createTableOperation.Name);
                    Assert.Equal(1, createTableOperation.ForeignKeys.Count);

                    var addForeignKeyOperation = createTableOperation.ForeignKeys[0];
                    Assert.Equal("FK_Person_DomesticAnimal_PetId", addForeignKeyOperation.Name);
                    Assert.Equal(new[] { "PetId" }, addForeignKeyOperation.Columns);
                    Assert.Equal("Animal", addForeignKeyOperation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, addForeignKeyOperation.PrincipalColumns);
                });
        }

        [Fact]
        public void Create_table_with_selfReferencing_foreign_key_in_hierarchy()
        {
            Execute(
                _ => { },
                modelBuilder =>
                {
                    EntityType animal = null;
                    modelBuilder.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    modelBuilder.Entity(
                        "Predator",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<int>("PreyId");
                            x.HasOne("Animal").WithMany().HasForeignKey("PreyId");
                            x.Metadata.Relational().DiscriminatorValue = "Predator";
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var createTableOperation = Assert.IsType<CreateTableOperation>(operations[0]);
                    Assert.Equal(1, createTableOperation.ForeignKeys.Count);

                    var addForeignKeyOperation = createTableOperation.ForeignKeys[0];
                    Assert.Equal("FK_Predator_Animal_PreyId", addForeignKeyOperation.Name);
                    Assert.Equal(new[] { "PreyId" }, addForeignKeyOperation.Columns);
                    Assert.Equal("Animal", addForeignKeyOperation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, addForeignKeyOperation.PrincipalColumns);
                });
        }

        [Fact]
        public void Add_foreign_key_on_base_type()
        {
            Execute(
                modelBuilder =>
                {
                    modelBuilder.Entity(
                        "Person",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    modelBuilder.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("HandlerId");
                        });
                    modelBuilder.Entity("Drakee").HasBaseType("Animal");
                },
                modelBuilder =>
                {
                    modelBuilder.Entity(
                        "Person",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    modelBuilder.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("HandlerId");
                            x.HasOne("Person").WithMany().HasForeignKey("HandlerId");
                        });
                    modelBuilder.Entity("Drakee").HasBaseType("Animal");
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddForeignKeyOperation>(operations[0]);
                    Assert.Equal("Animal", operation.Table);
                    Assert.Equal("FK_Animal_Person_HandlerId", operation.Name);
                    Assert.Equal(new[] { "HandlerId" }, operation.Columns);
                    Assert.Equal("Person", operation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, operation.PrincipalColumns);
                });
        }

        [Fact]
        public void Add_foreign_key_on_subtype()
        {
            Execute(
                source =>
                {
                    source.Entity(
                        "Person",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    EntityType animal = null;
                    source.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    source.Entity(
                        "GameAnimal",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<int>("HunterId");
                            x.Metadata.Relational().DiscriminatorValue = "GameAnimal";
                        });
                },
                target =>
                {
                    target.Entity(
                        "Person",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    EntityType animal = null;
                    target.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    target.Entity(
                        "GameAnimal",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<int>("HunterId");
                            x.HasOne("Person").WithMany().HasForeignKey("HunterId");
                            x.Metadata.Relational().DiscriminatorValue = "GameAnimal";
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddForeignKeyOperation>(operations[0]);
                    Assert.Equal("Animal", operation.Table);
                    Assert.Equal("FK_GameAnimal_Person_HunterId", operation.Name);
                    Assert.Equal(new[] { "HunterId" }, operation.Columns);
                    Assert.Equal("Person", operation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, operation.PrincipalColumns);
                });
        }

        [Fact]
        public void Add_foreign_key_to_subtype()
        {
            Execute(
                source =>
                {
                    EntityType animal = null;
                    source.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    source.Entity(
                        "TrophyAnimal",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Metadata.Relational().DiscriminatorValue = "TrophyAnimal";
                        });
                    source.Entity(
                        "Person",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("TrophyId");
                        });
                },
                target =>
                {
                    EntityType animal = null;
                    target.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    target.Entity(
                        "TrophyAnimal",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Metadata.Relational().DiscriminatorValue = "TrophyAnimal";
                        });
                    target.Entity(
                        "Person",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            x.Property<int>("TrophyId");
                            x.HasOne("TrophyAnimal").WithMany().HasForeignKey("TrophyId");
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<AddForeignKeyOperation>(operations[0]);
                    Assert.Equal("Person", operation.Table);
                    Assert.Equal("FK_Person_TrophyAnimal_TrophyId", operation.Name);
                    Assert.Equal(new[] { "TrophyId" }, operation.Columns);
                    Assert.Equal("Animal", operation.PrincipalTable);
                    Assert.Equal(new[] { "Id" }, operation.PrincipalColumns);
                });
        }

        [Fact]
        public void Drop_foreign_key_on_subtype()
        {
            Execute(
                source =>
                {
                    source.Entity(
                        "Person",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    EntityType animal = null;
                    source.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    source.Entity(
                        "MountAnimal",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<int>("RiderId");
                            x.HasOne("Person").WithMany().HasForeignKey("RiderId");
                            x.Metadata.Relational().DiscriminatorValue = "MountAnimal";
                        });
                },
                target =>
                {
                    target.Entity(
                        "Person",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                        });
                    EntityType animal = null;
                    target.Entity(
                        "Animal",
                        x =>
                        {
                            x.Property<int>("Id");
                            x.HasKey("Id");
                            var discriminatorProperty = x.Property<string>("Discriminator").IsRequired().Metadata;

                            animal = x.Metadata;
                            animal.Relational().DiscriminatorProperty = discriminatorProperty;
                            animal.Relational().DiscriminatorValue = "Animal";
                        });
                    target.Entity(
                        "MountAnimal",
                        x =>
                        {
                            x.Metadata.BaseType = animal;
                            x.Property<int>("RiderId");
                            x.Metadata.Relational().DiscriminatorValue = "MountAnimal";
                        });
                },
                operations =>
                {
                    Assert.Equal(1, operations.Count);

                    var operation = Assert.IsType<DropForeignKeyOperation>(operations[0]);
                    Assert.Equal("Animal", operation.Table);
                    Assert.Equal("FK_MountAnimal_Person_RiderId", operation.Name);
                });
        }

        [Fact] // See #2802
        public void Diff_IProperty_compares_values_not_references()
        {
            Execute(
                source => source.Entity(
                    "Stork",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<bool>("Value").HasDefaultValue(true);
                    }),
                 target => target.Entity(
                    "Stork",
                    x =>
                    {
                        x.Property<int>("Id");
                        x.HasKey("Id");
                        x.Property<bool>("Value").HasDefaultValue(true);
                    }),
                 Assert.Empty);
        }
    }
}
