// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using Microsoft.Data.Entity.Internal;
using Microsoft.Data.Entity.Metadata.Conventions;
using Xunit;

namespace Microsoft.Data.Entity.Metadata.Internal
{
    public class InternalEntityTypeBuilderTest
    {
        [Fact]
        public void ForeignKey_returns_same_instance_for_same_properties()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var relationshipBuilder = dependentEntityBuilder.HasForeignKey(
                principalEntityBuilder,
                new[]
                {
                    dependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Convention).Metadata,
                    dependentEntityBuilder.Property(Order.CustomerUniqueProperty, ConfigurationSource.Convention).Metadata
                },
                ConfigurationSource.Explicit);

            Assert.NotNull(relationshipBuilder);
            Assert.Same(relationshipBuilder,
                dependentEntityBuilder.HasForeignKey(
                    principalEntityBuilder,
                    new[]
                    {
                        dependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Convention).Metadata,
                        dependentEntityBuilder.Property(Order.CustomerUniqueProperty, ConfigurationSource.Convention).Metadata
                    },
                    ConfigurationSource.Convention));
        }

        [Fact]
        public void Relationship_returns_same_instance_for_same_navigations()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var relationshipBuilder = dependentEntityBuilder.Relationship(principalEntityBuilder, ConfigurationSource.Explicit)
                .DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.Explicit)
                .PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.Explicit);

            Assert.NotNull(relationshipBuilder);
            Assert.Same(relationshipBuilder,
                dependentEntityBuilder.Relationship(principalEntityBuilder, ConfigurationSource.Convention)
                    .DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.Convention)
                    .PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.Convention));
        }

        [Fact]
        public void Can_add_relationship_if_navigation_to_dependent_ignored_at_lower_source()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            Assert.True(principalEntityBuilder.Ignore(Customer.OrdersProperty.Name, ConfigurationSource.Convention));

            Assert.NotNull(dependentEntityBuilder.Relationship(
                principalEntityBuilder,
                null,
                Customer.OrdersProperty.Name,
                ConfigurationSource.DataAnnotation));
        }

        [Fact]
        public void Can_add_relationship_if_navigation_to_principal_ignored_at_lower_source()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            Assert.True(dependentEntityBuilder.Ignore(Order.CustomerProperty.Name, ConfigurationSource.Convention));

            Assert.NotNull(dependentEntityBuilder.Relationship(
                principalEntityBuilder,
                Order.CustomerProperty.Name,
                null,
                ConfigurationSource.DataAnnotation));
        }

        [Fact]
        public void Can_promote_relationship_to_base()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(SpecialCustomer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Explicit);

            dependentEntityBuilder.Relationship(
                principalEntityBuilder,
                Order.CustomerProperty.Name,
                Customer.OrdersProperty.Name,
                ConfigurationSource.DataAnnotation);

            var basePrincipalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var baseDependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            principalEntityBuilder.HasBaseType(basePrincipalEntityBuilder.Metadata, ConfigurationSource.Explicit);
            dependentEntityBuilder.HasBaseType(baseDependentEntityBuilder.Metadata, ConfigurationSource.Explicit);

            Assert.Empty(baseDependentEntityBuilder.Metadata.GetForeignKeys());

            var relationship = baseDependentEntityBuilder.Relationship(
                basePrincipalEntityBuilder,
                Order.CustomerProperty.Name,
                Customer.OrdersProperty.Name,
                ConfigurationSource.Convention);

            Assert.Same(relationship.Metadata, dependentEntityBuilder.Metadata.GetForeignKeys().Single());
            Assert.Same(relationship.Metadata, principalEntityBuilder.Metadata.Navigations.Single().ForeignKey);
            Assert.Same(relationship.Metadata, dependentEntityBuilder.Metadata.Navigations.Single().ForeignKey);
            Assert.Empty(dependentEntityBuilder.Metadata.GetDeclaredForeignKeys());
        }

        [Fact]
        public void Can_promote_foreignKey_to_base()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { nameof(Customer.Id) }, ConfigurationSource.Convention);
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var derivedEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            derivedEntityBuilder.HasBaseType(entityBuilder.Metadata, ConfigurationSource.Convention);
            derivedEntityBuilder.Property(Order.IdProperty, ConfigurationSource.Convention);
            derivedEntityBuilder.HasForeignKey(principalEntityBuilder.Metadata.Name, new[] { Order.IdProperty.Name }, ConfigurationSource.DataAnnotation);

            var foreignKeyBuilder = entityBuilder.HasForeignKey(principalEntityBuilder.Metadata.Name, new[] { Order.IdProperty.Name }, ConfigurationSource.Convention);
            Assert.Same(foreignKeyBuilder.Metadata.Properties.Single(), entityBuilder.Metadata.FindProperty(Order.IdProperty.Name));
            Assert.Same(foreignKeyBuilder.Metadata, entityBuilder.Metadata.GetForeignKeys().Single());
            Assert.Empty(derivedEntityBuilder.Metadata.GetDeclaredForeignKeys());
        }

        [Fact]
        public void Can_configure_inherited_foreignKey()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            dependentEntityBuilder.Property(Order.IdProperty.Name, typeof(int), ConfigurationSource.Convention);
            dependentEntityBuilder.HasForeignKey(
                principalEntityBuilder.Metadata.Name, new[] { Order.IdProperty.Name }, ConfigurationSource.Explicit);

            var derivedDependentEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            derivedDependentEntityBuilder.HasBaseType(dependentEntityBuilder.Metadata, ConfigurationSource.Convention);

            var relationshipBuilder = derivedDependentEntityBuilder.HasForeignKey(
                principalEntityBuilder.Metadata.Name, new[] { Order.IdProperty.Name }, ConfigurationSource.Convention);
            Assert.Same(dependentEntityBuilder.Metadata.GetForeignKeys().Single(), relationshipBuilder.Metadata);

            relationshipBuilder = relationshipBuilder.IsUnique(true, ConfigurationSource.Convention);
            Assert.True(relationshipBuilder.Metadata.IsUnique);
            Assert.Null(relationshipBuilder.HasForeignKey(
                new[] { dependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Convention).Metadata },
                ConfigurationSource.Convention));
        }

        [Fact]
        public void Can_configure_relationship_on_inherited_navigation()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(SpecialCustomer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Explicit);
            var basePrincipalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var baseDependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            principalEntityBuilder.HasBaseType(basePrincipalEntityBuilder.Metadata, ConfigurationSource.Explicit);
            dependentEntityBuilder.HasBaseType(baseDependentEntityBuilder.Metadata, ConfigurationSource.Explicit);

            baseDependentEntityBuilder.Relationship(
                basePrincipalEntityBuilder,
                Order.CustomerProperty.Name,
                Customer.OrdersProperty.Name,
                ConfigurationSource.Convention);

            var relationshipBuilder = dependentEntityBuilder.Relationship(
                principalEntityBuilder,
                Order.CustomerProperty.Name,
                null,
                ConfigurationSource.Convention);

            Assert.Same(baseDependentEntityBuilder.Metadata, relationshipBuilder.Metadata.DeclaringEntityType);
            Assert.Null(relationshipBuilder.Metadata.PrincipalToDependent);
            Assert.Equal(Order.CustomerProperty.Name, relationshipBuilder.Metadata.DependentToPrincipal.Name);
            Assert.Empty(principalEntityBuilder.Metadata.Navigations);
            Assert.Same(relationshipBuilder.Metadata, dependentEntityBuilder.Metadata.GetForeignKeys().Single());
        }

        [Fact]
        public void Can_only_remove_lower_or_equal_source_relationship()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var relationshipBuilder = dependentEntityBuilder.HasForeignKey(
                principalEntityBuilder,
                new[]
                {
                    dependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Convention).Metadata,
                    dependentEntityBuilder.Property(Order.CustomerUniqueProperty, ConfigurationSource.Convention).Metadata
                },
                ConfigurationSource.DataAnnotation);
            Assert.NotNull(relationshipBuilder);

            Assert.Null(dependentEntityBuilder.RemoveForeignKey(relationshipBuilder.Metadata, ConfigurationSource.Convention));
            Assert.Equal(ConfigurationSource.DataAnnotation, dependentEntityBuilder.RemoveForeignKey(relationshipBuilder.Metadata, ConfigurationSource.DataAnnotation));

            Assert.Equal(
                new[] { Order.CustomerIdProperty.Name, Order.CustomerUniqueProperty.Name },
                dependentEntityBuilder.Metadata.Properties.Select(p => p.Name));
            Assert.Empty(dependentEntityBuilder.Metadata.GetForeignKeys());
        }

        [Fact]
        public void Removing_relationship_removes_unused_contained_shadow_properties()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var shadowProperty = dependentEntityBuilder.Property("Shadow", typeof(Guid), ConfigurationSource.Convention);

            var relationshipBuilder = dependentEntityBuilder.HasForeignKey(
                principalEntityBuilder,
                new[]
                {
                    dependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Convention).Metadata,
                    shadowProperty.Metadata
                },
                ConfigurationSource.Convention);
            Assert.NotNull(relationshipBuilder);

            Assert.Equal(ConfigurationSource.Convention, dependentEntityBuilder.RemoveForeignKey(relationshipBuilder.Metadata, ConfigurationSource.DataAnnotation));

            Assert.Same(Order.CustomerIdProperty.Name, dependentEntityBuilder.Metadata.Properties.Single().Name);
            Assert.Empty(dependentEntityBuilder.Metadata.GetForeignKeys());
        }

        [Fact]
        public void Removing_relationship_does_not_remove_contained_shadow_properties_if_referenced_elsewhere()
        {
            Test_removing_relationship_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(
                (entityBuilder, property) => entityBuilder.PrimaryKey(new[] { property.Name }, ConfigurationSource.Convention));

            Test_removing_relationship_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(
                (entityBuilder, property) => entityBuilder.HasIndex(new[] { property.Name }, ConfigurationSource.Convention));

            Test_removing_relationship_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(
                (entityBuilder, property) => entityBuilder.HasForeignKey(
                    typeof(Customer).FullName,
                    new[] { entityBuilder.Property("Shadow2", typeof(int), ConfigurationSource.Convention).Metadata.Name, property.Name },
                    ConfigurationSource.Convention));

            Test_removing_relationship_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(
                (entityBuilder, property) => entityBuilder.Property("Shadow", typeof(Guid), ConfigurationSource.Explicit));
        }

        private void Test_removing_relationship_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(Func<InternalEntityTypeBuilder, Property, object> shadowConfig)
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var shadowProperty = dependentEntityBuilder.Property("Shadow", typeof(Guid), ConfigurationSource.Convention);
            Assert.NotNull(shadowConfig(dependentEntityBuilder, shadowProperty.Metadata));

            var relationshipBuilder = dependentEntityBuilder.HasForeignKey(
                principalEntityBuilder,
                new[]
                {
                    dependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Convention).Metadata,
                    shadowProperty.Metadata
                },
                ConfigurationSource.Convention);
            Assert.NotNull(relationshipBuilder);

            Assert.Equal(ConfigurationSource.Convention, dependentEntityBuilder.RemoveForeignKey(relationshipBuilder.Metadata, ConfigurationSource.DataAnnotation));

            Assert.Equal(1, dependentEntityBuilder.Metadata.Properties.Count(p => p.Name == shadowProperty.Metadata.Name));
            Assert.Empty(dependentEntityBuilder.Metadata.GetForeignKeys().Where(foreignKey => foreignKey.Properties.SequenceEqual(relationshipBuilder.Metadata.Properties)));
        }

        [Fact]
        public void Index_returns_same_instance_for_clr_properties()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var indexBuilder = entityBuilder.HasIndex(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.Explicit);

            Assert.NotNull(indexBuilder);
            Assert.Same(indexBuilder, entityBuilder.HasIndex(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, ConfigurationSource.DataAnnotation));
        }

        [Fact]
        public void Index_returns_same_instance_for_property_names()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var indexBuilder = entityBuilder.HasIndex(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, ConfigurationSource.Convention);

            Assert.NotNull(indexBuilder);
            Assert.Same(indexBuilder, entityBuilder.HasIndex(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.Explicit));
        }

        [Fact]
        public void Can_promote_index_to_base()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var derivedEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            derivedEntityBuilder.HasBaseType(entityBuilder.Metadata, ConfigurationSource.Convention);
            derivedEntityBuilder.Property(Order.IdProperty, ConfigurationSource.Convention);
            derivedEntityBuilder.HasIndex(new[] { Order.IdProperty.Name }, ConfigurationSource.DataAnnotation);

            var indexBuilder = entityBuilder.HasIndex(new[] { Order.IdProperty.Name }, ConfigurationSource.Convention);
            Assert.Same(indexBuilder.Metadata.Properties.Single(), entityBuilder.Metadata.FindProperty(Order.IdProperty.Name));
            Assert.Same(indexBuilder.Metadata, entityBuilder.Metadata.FindIndex(indexBuilder.Metadata.Properties.Single()));
            Assert.Empty(derivedEntityBuilder.Metadata.GetDeclaredIndexes());
        }

        [Fact]
        public void Can_configure_inherited_index()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            entityBuilder.Property(Order.IdProperty.Name, typeof(int), ConfigurationSource.Convention);
            entityBuilder.HasIndex(new[] { Order.IdProperty.Name }, ConfigurationSource.Explicit);

            var derivedEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            derivedEntityBuilder.HasBaseType(entityBuilder.Metadata, ConfigurationSource.Convention);

            var indexBuilder = derivedEntityBuilder.HasIndex(new[] { Order.IdProperty.Name }, ConfigurationSource.DataAnnotation);

            Assert.Same(entityBuilder.Metadata.Indexes.Single(), indexBuilder.Metadata);
            Assert.True(indexBuilder.IsUnique(true, ConfigurationSource.Convention));
            Assert.True(indexBuilder.Metadata.IsUnique);
        }

        [Fact]
        public void Index_returns_null_for_ignored_clr_properties()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            entityBuilder.Ignore(Order.CustomerIdProperty.Name, ConfigurationSource.Explicit);

            Assert.Null(entityBuilder.HasIndex(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.DataAnnotation));
        }

        [Fact]
        public void Index_returns_null_for_ignored_property_names()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            entityBuilder.Ignore(Order.CustomerIdProperty.Name, ConfigurationSource.DataAnnotation);

            Assert.Null(entityBuilder.HasIndex(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, ConfigurationSource.Convention));
        }

        [Fact]
        public void Can_only_remove_lower_or_equal_source_index()
        {
            var modelBuilder = CreateModelBuilder();
            modelBuilder
                .Entity(typeof(Customer), ConfigurationSource.Explicit)
                .PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var index = entityBuilder.HasIndex(new[] { Order.CustomerIdProperty.Name }, ConfigurationSource.DataAnnotation);
            Assert.NotNull(index);

            Assert.Null(entityBuilder.RemoveIndex(index.Metadata, ConfigurationSource.Convention));
            Assert.Equal(ConfigurationSource.DataAnnotation, entityBuilder.RemoveIndex(index.Metadata, ConfigurationSource.DataAnnotation));

            Assert.Equal(Order.CustomerIdProperty.Name, entityBuilder.Metadata.Properties.Single().Name);
            Assert.Empty(entityBuilder.Metadata.Indexes);
        }

        [Fact]
        public void Removing_index_removes_unused_contained_shadow_properties()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var shadowProperty = entityBuilder.Property("Shadow", typeof(Guid), ConfigurationSource.Convention);

            var index = entityBuilder.HasIndex(new[] { Order.CustomerIdProperty.Name, shadowProperty.Metadata.Name }, ConfigurationSource.Convention);
            Assert.NotNull(index);

            Assert.Equal(ConfigurationSource.Convention, entityBuilder.RemoveIndex(index.Metadata, ConfigurationSource.DataAnnotation));

            Assert.Equal(Order.CustomerIdProperty.Name, entityBuilder.Metadata.Properties.Single().Name);
            Assert.Empty(entityBuilder.Metadata.Indexes);
        }

        [Fact]
        public void Removing_index_does_not_remove_contained_shadow_properties_if_referenced_elsewhere()
        {
            Test_removing_index_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(
                (entityBuilder, property) => entityBuilder.PrimaryKey(new[] { property.Name }, ConfigurationSource.Convention));

            Test_removing_index_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(
                (entityBuilder, property) => entityBuilder.HasIndex(new[] { property.Name }, ConfigurationSource.Convention));

            Test_removing_index_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(
                (entityBuilder, property) => entityBuilder.HasForeignKey(
                    typeof(Customer).FullName,
                    new[] { Order.CustomerIdProperty.Name, property.Name },
                    ConfigurationSource.Convention));

            Test_removing_index_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(
                (entityBuilder, property) => entityBuilder.Property(((IProperty)property).Name, property.ClrType, ConfigurationSource.Explicit));
        }

        private void Test_removing_index_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(Func<InternalEntityTypeBuilder, Property, object> shadowConfig)
        {
            var modelBuilder = CreateModelBuilder();
            modelBuilder
                .Entity(typeof(Customer), ConfigurationSource.Explicit)
                .PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var shadowProperty = entityBuilder.Property("Shadow", typeof(Guid), ConfigurationSource.Convention);
            Assert.NotNull(shadowConfig(entityBuilder, shadowProperty.Metadata));

            var index = entityBuilder.HasIndex(new[] { Order.CustomerIdProperty.Name, shadowProperty.Metadata.Name }, ConfigurationSource.Convention);
            Assert.NotNull(index);

            Assert.Equal(ConfigurationSource.Convention, entityBuilder.RemoveIndex(index.Metadata, ConfigurationSource.DataAnnotation));

            Assert.Equal(1, entityBuilder.Metadata.Properties.Count(p => p.Name == shadowProperty.Metadata.Name));
            Assert.Empty(entityBuilder.Metadata.Indexes.Where(i => i.Properties.SequenceEqual(index.Metadata.Properties)));
        }

        [Fact]
        public void Key_returns_same_instance_for_clr_properties()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var keyBuilder = entityBuilder.HasKey(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.DataAnnotation);

            Assert.NotNull(keyBuilder);
            Assert.Same(keyBuilder, entityBuilder.PrimaryKey(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, ConfigurationSource.Convention));
        }

        [Fact]
        public void Key_returns_same_instance_for_property_names()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var keyBuilder = entityBuilder.PrimaryKey(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, ConfigurationSource.Convention);

            Assert.NotNull(keyBuilder);
            Assert.Same(keyBuilder, entityBuilder.HasKey(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.DataAnnotation));
        }

        [Fact]
        public void Key_throws_for_property_names_if_they_do_not_exist()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            Assert.Equal(CoreStrings.NoClrProperty(Customer.UniqueProperty.Name, typeof(Order).FullName),
                Assert.Throws<InvalidOperationException>(() =>
                    entityBuilder.HasKey(new[] { Customer.UniqueProperty.Name }, ConfigurationSource.Convention)).Message);
        }

        [Fact]
        public void Key_throws_for_derived_type()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var keyBuilder = entityBuilder.HasKey(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, ConfigurationSource.Convention);

            var derivedEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            derivedEntityBuilder.HasBaseType(entityBuilder.Metadata, ConfigurationSource.Convention);

            Assert.Equal(CoreStrings.DerivedEntityTypeKey(typeof(SpecialOrder).FullName),
                Assert.Throws<InvalidOperationException>(() =>
                    derivedEntityBuilder.HasKey(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, ConfigurationSource.DataAnnotation)).Message);
        }

        [Fact]
        public void Key_throws_for_property_names_for_shadow_entity_type_if_they_do_not_exist()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order).Name, ConfigurationSource.Explicit);

            Assert.Equal(CoreStrings.PropertyNotFound(Order.IdProperty.Name, typeof(Order).Name),
                Assert.Throws<ModelItemNotFoundException>(() =>
                    entityBuilder.HasKey(new[] { Order.IdProperty.Name }, ConfigurationSource.Convention)).Message);
        }

        [Fact]
        public void Key_works_for_property_names_for_shadow_entity_type()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            entityBuilder.Property(Order.CustomerIdProperty.Name, Order.CustomerIdProperty.PropertyType, ConfigurationSource.Convention);

            Assert.NotNull(entityBuilder.HasKey(new[] { Order.CustomerIdProperty.Name }, ConfigurationSource.Convention));

            Assert.Equal(Order.CustomerIdProperty.Name, entityBuilder.Metadata.GetKeys().Single().Properties.Single().Name);
        }

        [Fact]
        public void Key_returns_null_for_ignored_clr_properties()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            entityBuilder.Ignore(Order.CustomerIdProperty.Name, ConfigurationSource.Explicit);

            Assert.Null(entityBuilder.HasKey(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.DataAnnotation));
        }

        [Fact]
        public void Key_returns_null_for_ignored_property_names()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            entityBuilder.Ignore(Order.CustomerIdProperty.Name, ConfigurationSource.DataAnnotation);

            Assert.Null(entityBuilder.HasKey(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, ConfigurationSource.Convention));
        }

        [Fact]
        public void Can_only_remove_lower_or_equal_source_key()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var key = entityBuilder.HasKey(new[] { Order.CustomerIdProperty.Name }, ConfigurationSource.DataAnnotation);
            Assert.NotNull(key);

            Assert.Null(entityBuilder.RemoveKey(key.Metadata, ConfigurationSource.Convention));
            Assert.Equal(ConfigurationSource.DataAnnotation, entityBuilder.RemoveKey(key.Metadata, ConfigurationSource.DataAnnotation));

            Assert.Equal(Order.CustomerIdProperty.Name, entityBuilder.Metadata.Properties.Single().Name);
            Assert.Empty(entityBuilder.Metadata.GetKeys());
        }

        [Fact]
        public void Removing_key_removes_unused_contained_shadow_properties()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var shadowProperty = entityBuilder.Property("Shadow", typeof(Guid), ConfigurationSource.Convention);

            var key = entityBuilder.HasKey(new[] { Order.CustomerIdProperty.Name, shadowProperty.Metadata.Name }, ConfigurationSource.Convention);
            Assert.NotNull(key);

            Assert.Equal(ConfigurationSource.Convention, entityBuilder.RemoveKey(key.Metadata, ConfigurationSource.DataAnnotation));

            Assert.Equal(Order.CustomerIdProperty.Name, entityBuilder.Metadata.Properties.Single().Name);
            Assert.Empty(entityBuilder.Metadata.GetKeys());
        }

        [Fact]
        public void Removing_key_does_not_remove_contained_shadow_properties_if_referenced_elsewhere()
        {
            Test_removing_key_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(
                (entityBuilder, property) => entityBuilder.PrimaryKey(
                    new[]
                    {
                        entityBuilder.Property("Shadow2", typeof(int), ConfigurationSource.Convention).Metadata.Name,
                        property.Name
                    }, ConfigurationSource.Convention));

            Test_removing_key_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(
                (entityBuilder, property) => entityBuilder.HasIndex(new[] { property.Name }, ConfigurationSource.Convention));

            Test_removing_key_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(
                (entityBuilder, property) => entityBuilder.HasForeignKey(
                    typeof(Customer).FullName,
                    new[] { Order.CustomerIdProperty.Name, property.Name },
                    ConfigurationSource.Convention));

            Test_removing_key_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(
                (entityBuilder, property) => entityBuilder.Property("Shadow", typeof(Guid), ConfigurationSource.Explicit));
        }

        private void Test_removing_key_does_not_remove_contained_shadow_properties_if_referenced_elsewhere(Func<InternalEntityTypeBuilder, Property, object> shadowConfig)
        {
            var modelBuilder = CreateModelBuilder();
            modelBuilder
                .Entity(typeof(Customer), ConfigurationSource.Explicit)
                .PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var shadowProperty = entityBuilder.Property("Shadow", typeof(Guid), ConfigurationSource.Convention);
            Assert.NotNull(shadowConfig(entityBuilder, shadowProperty.Metadata));

            var key = entityBuilder.HasKey(new[] { Order.CustomerIdProperty.Name, shadowProperty.Metadata.Name }, ConfigurationSource.Convention);
            Assert.NotNull(key);

            Assert.Equal(ConfigurationSource.Convention, entityBuilder.RemoveKey(key.Metadata, ConfigurationSource.DataAnnotation));

            Assert.Equal(1, entityBuilder.Metadata.Properties.Count(p => p.Name == shadowProperty.Metadata.Name));
            Assert.Empty(entityBuilder.Metadata.GetKeys().Where(foreignKey => foreignKey.Properties.SequenceEqual(key.Metadata.Properties)));
        }

        [Fact]
        public void Removing_key_removes_referencing_foreign_key_of_lower_or_equal_source()
        {
            var modelBuilder = CreateModelBuilder();
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var key = principalEntityBuilder.HasKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Convention);
            dependentEntityBuilder.HasForeignKey(
                principalEntityBuilder,
                new[]
                {
                    dependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Convention).Metadata,
                    dependentEntityBuilder.Property(Order.CustomerUniqueProperty, ConfigurationSource.Convention).Metadata
                }, ConfigurationSource.DataAnnotation)
                .HasPrincipalKey(key.Metadata.Properties, ConfigurationSource.DataAnnotation);

            Assert.Null(principalEntityBuilder.RemoveKey(key.Metadata, ConfigurationSource.Convention));
            Assert.Equal(ConfigurationSource.DataAnnotation, principalEntityBuilder.RemoveKey(key.Metadata, ConfigurationSource.DataAnnotation));

            Assert.Equal(
                new[] { Order.CustomerIdProperty.Name, Order.CustomerUniqueProperty.Name },
                dependentEntityBuilder.Metadata.Properties.Select(p => p.Name));
            Assert.Empty(dependentEntityBuilder.Metadata.GetForeignKeys());
            Assert.Empty(principalEntityBuilder.Metadata.GetKeys());
        }

        [Fact]
        public void PrimaryKey_returns_same_instance_for_clr_properties()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var keyBuilder = entityBuilder.PrimaryKey(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.DataAnnotation);

            Assert.NotNull(keyBuilder);
            Assert.Same(keyBuilder, entityBuilder.HasKey(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, ConfigurationSource.Convention));

            Assert.Same(entityBuilder.Metadata.GetPrimaryKey(), entityBuilder.Metadata.GetKeys().Single());
        }

        [Fact]
        public void PrimaryKey_returns_same_instance_for_property_names()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var keyBuilder = entityBuilder.HasKey(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, ConfigurationSource.Convention);

            Assert.NotNull(keyBuilder);
            Assert.Same(keyBuilder, entityBuilder.PrimaryKey(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.DataAnnotation));

            Assert.Same(entityBuilder.Metadata.GetPrimaryKey(), entityBuilder.Metadata.GetKeys().Single());
        }

        [Fact]
        public void PrimaryKey_throws_for_property_names_if_they_do_not_exist()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            Assert.Equal(CoreStrings.NoClrProperty(Customer.UniqueProperty.Name, typeof(Order).FullName),
                Assert.Throws<InvalidOperationException>(() =>
                    entityBuilder.PrimaryKey(new[] { Customer.UniqueProperty.Name }, ConfigurationSource.Convention)).Message);
        }

        [Fact]
        public void PrimaryKey_throws_for_property_names_for_shadow_entity_type_if_they_do_not_exist()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order).Name, ConfigurationSource.Explicit);

            Assert.Equal(CoreStrings.PropertyNotFound(Order.IdProperty.Name, typeof(Order).Name),
                Assert.Throws<ModelItemNotFoundException>(() =>
                    entityBuilder.PrimaryKey(new[] { Order.IdProperty.Name }, ConfigurationSource.Convention)).Message);
        }

        [Fact]
        public void PrimaryKey_throws_for_derived_type()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var keyBuilder = entityBuilder.PrimaryKey(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, ConfigurationSource.Convention);

            var derivedEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            derivedEntityBuilder.HasBaseType(entityBuilder.Metadata, ConfigurationSource.Convention);

            Assert.Equal(CoreStrings.DerivedEntityTypeKey(typeof(SpecialOrder).FullName),
                Assert.Throws<InvalidOperationException>(() =>
                    derivedEntityBuilder.PrimaryKey(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, ConfigurationSource.DataAnnotation)).Message);
        }

        [Fact]
        public void PrimaryKey_works_for_property_names_for_shadow_entity_type()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            entityBuilder.Property(Order.CustomerIdProperty.Name, Order.CustomerIdProperty.PropertyType, ConfigurationSource.Convention);

            Assert.NotNull(entityBuilder.PrimaryKey(new[] { Order.CustomerIdProperty.Name }, ConfigurationSource.Convention));

            Assert.Equal(Order.CustomerIdProperty.Name, entityBuilder.Metadata.GetPrimaryKey().Properties.Single().Name);
        }

        [Fact]
        public void PrimaryKey_returns_null_for_ignored_clr_properties()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            entityBuilder.Ignore(Order.CustomerIdProperty.Name, ConfigurationSource.Explicit);

            Assert.Null(entityBuilder.PrimaryKey(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.DataAnnotation));
        }

        [Fact]
        public void PrimaryKey_returns_null_for_ignored_property_names()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            entityBuilder.Ignore(Order.CustomerIdProperty.Name, ConfigurationSource.DataAnnotation);

            Assert.Null(entityBuilder.PrimaryKey(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, ConfigurationSource.Convention));
        }

        [Fact]
        public void Can_only_override_lower_or_equal_source_primary_key()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var entityType = entityBuilder.Metadata;

            var compositeKeyBuilder = entityBuilder.PrimaryKey(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.Convention);
            var simpleKeyBuilder = entityBuilder.PrimaryKey(new[] { Order.IdProperty }, ConfigurationSource.Convention);

            Assert.NotNull(simpleKeyBuilder);
            Assert.NotEqual(compositeKeyBuilder, simpleKeyBuilder);
            Assert.Equal(Order.IdProperty.Name, entityType.GetKeys().Single().Properties.Single().Name);

            var simpleKeyBuilder2 = entityBuilder.HasKey(new[] { Order.IdProperty }, ConfigurationSource.Explicit);
            Assert.Same(simpleKeyBuilder, simpleKeyBuilder2);

            var compositeKeyBuilder2 = entityBuilder.PrimaryKey(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.Convention);
            Assert.NotNull(compositeKeyBuilder2);
            Assert.NotEqual(compositeKeyBuilder, compositeKeyBuilder2);
            Assert.Same(compositeKeyBuilder2.Metadata, entityBuilder.Metadata.GetPrimaryKey());
            Assert.Equal(2, entityType.GetKeys().Count());

            Assert.NotNull(entityBuilder.PrimaryKey(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.Explicit));

            Assert.Null(entityBuilder.PrimaryKey(new[] { Order.IdProperty }, ConfigurationSource.DataAnnotation));
            Assert.Same(compositeKeyBuilder2.Metadata, entityBuilder.Metadata.GetPrimaryKey());
            Assert.Equal(2, entityType.GetKeys().Count());
        }

        [Fact]
        public void Can_only_override_existing_primary_key_explicitly()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var entityType = entityBuilder.Metadata;
            entityType.SetPrimaryKey(new[] { entityType.GetOrAddProperty(Order.IdProperty), entityType.GetOrAddProperty(Order.CustomerIdProperty) });

            Assert.Null(entityBuilder.PrimaryKey(new[] { Order.IdProperty }, ConfigurationSource.DataAnnotation));

            Assert.Equal(new[] { Order.IdProperty.Name, Order.CustomerIdProperty.Name }, entityType.GetPrimaryKey().Properties.Select(p => p.Name).ToArray());

            Assert.NotNull(entityBuilder.PrimaryKey(new[] { Order.IdProperty }, ConfigurationSource.Explicit));

            Assert.Equal(Order.IdProperty.Name, entityType.GetPrimaryKey().Properties.Single().Name);
        }

        [Fact]
        public void Changing_primary_key_removes_previously_referenced_primary_key_of_lower_or_equal_source()
        {
            var modelBuilder = CreateModelBuilder();
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var keyBuilder = principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Convention);
            var fkProperty1 = dependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Convention).Metadata;
            var fkProperty2 = dependentEntityBuilder.Property(Order.CustomerUniqueProperty, ConfigurationSource.Convention).Metadata;

            dependentEntityBuilder.Relationship(principalEntityBuilder, ConfigurationSource.DataAnnotation)
                .HasPrincipalKey(keyBuilder.Metadata.Properties, ConfigurationSource.DataAnnotation)
                .HasForeignKey(new[] { fkProperty1, fkProperty2 }, ConfigurationSource.Explicit);

            keyBuilder = principalEntityBuilder.PrimaryKey(new[] { Order.IdProperty }, ConfigurationSource.DataAnnotation);

            Assert.Same(keyBuilder.Metadata, principalEntityBuilder.Metadata.GetPrimaryKey());
            var fk = dependentEntityBuilder.Metadata.GetForeignKeys().Single();
            Assert.Equal(new List<Property> { fkProperty1, fkProperty2 }, fk.Properties);
        }

        [Fact]
        public void Changing_primary_key_removes_previously_referenced_key_of_lower_or_equal_source()
        {
            var modelBuilder = CreateModelBuilder();
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var keyBuilder = principalEntityBuilder.HasKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Convention);
            dependentEntityBuilder.Relationship(principalEntityBuilder, ConfigurationSource.DataAnnotation)
                .HasPrincipalKey(keyBuilder.Metadata.Properties, ConfigurationSource.DataAnnotation);

            keyBuilder = principalEntityBuilder.PrimaryKey(new[] { Order.IdProperty }, ConfigurationSource.DataAnnotation);

            Assert.Same(keyBuilder.Metadata, dependentEntityBuilder.Metadata.GetForeignKeys().Single().PrincipalKey);
            Assert.Same(keyBuilder.Metadata, principalEntityBuilder.Metadata.GetKeys().Single());
        }

        [Fact]
        public void Changing_primary_key_does_not_remove_previously_explicitly_referenced_key()
        {
            var modelBuilder = CreateModelBuilder();
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var existingKeyBuilder = principalEntityBuilder.HasKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Convention);
            dependentEntityBuilder.Relationship(principalEntityBuilder, ConfigurationSource.Explicit)
                .HasPrincipalKey(existingKeyBuilder.Metadata.Properties, ConfigurationSource.Explicit);

            var keyBuilder = principalEntityBuilder.PrimaryKey(new[] { Order.IdProperty }, ConfigurationSource.Explicit);

            Assert.Same(existingKeyBuilder.Metadata, dependentEntityBuilder.Metadata.GetForeignKeys().Single().PrincipalKey);
            Assert.Equal(2, principalEntityBuilder.Metadata.GetKeys().Count());
            Assert.Contains(keyBuilder.Metadata, principalEntityBuilder.Metadata.GetKeys());
        }

        [Fact]
        public void Property_returns_same_instance_for_clr_properties()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var propertyBuilder = entityBuilder.Property(Order.IdProperty, ConfigurationSource.Explicit);

            Assert.NotNull(propertyBuilder);
            Assert.Same(propertyBuilder, entityBuilder.Property(Order.IdProperty.Name, typeof(Order), ConfigurationSource.Explicit));
        }

        [Fact]
        public void Property_returns_same_instance_for_property_names()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var propertyBuilder = entityBuilder.Property(Order.IdProperty.Name, typeof(Order), ConfigurationSource.DataAnnotation);

            Assert.NotNull(propertyBuilder);
            Assert.Same(propertyBuilder, entityBuilder.Property(Order.IdProperty, ConfigurationSource.DataAnnotation));
        }

        [Fact]
        public void Can_promote_property_to_base()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var derivedEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            derivedEntityBuilder.HasBaseType(entityBuilder.Metadata, ConfigurationSource.Convention);
            var derivedProperty = derivedEntityBuilder.Property(Order.IdProperty.Name, typeof(string), ConfigurationSource.DataAnnotation);
            derivedProperty.IsConcurrencyToken(true, ConfigurationSource.Convention);
            derivedProperty.HasMaxLength(1, ConfigurationSource.DataAnnotation);
            var derivedEntityBuilder2 = modelBuilder.Entity(typeof(BackOrder), ConfigurationSource.Convention);
            derivedEntityBuilder2.HasBaseType(entityBuilder.Metadata, ConfigurationSource.Convention);
            var derivedProperty2 = derivedEntityBuilder2.Property(Order.IdProperty.Name, typeof(string), ConfigurationSource.Convention);
            derivedProperty2.UseValueGenerator(true, ConfigurationSource.Convention);
            derivedProperty2.HasMaxLength(2, ConfigurationSource.Convention);

            var propertyBuilder = entityBuilder.Property(Order.IdProperty.Name, typeof(int), ConfigurationSource.Convention);
            Assert.Same(propertyBuilder.Metadata, entityBuilder.Metadata.FindProperty(Order.IdProperty.Name));
            Assert.False(entityBuilder.Ignore(Order.IdProperty.Name, ConfigurationSource.Convention));
            Assert.Empty(derivedEntityBuilder.Metadata.GetDeclaredProperties());
            Assert.Empty(derivedEntityBuilder2.Metadata.GetDeclaredProperties());
            Assert.Equal(typeof(int), propertyBuilder.Metadata.ClrType);
            Assert.True(propertyBuilder.Metadata.IsConcurrencyToken);
            Assert.True(propertyBuilder.Metadata.RequiresValueGenerator);
            Assert.Equal(1, propertyBuilder.Metadata.GetMaxLength());
        }

        [Fact]
        public void Can_configure_inherited_property()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            entityBuilder.Property(Order.IdProperty.Name, typeof(int), ConfigurationSource.Explicit);

            var derivedEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            derivedEntityBuilder.HasBaseType(entityBuilder.Metadata, ConfigurationSource.Convention);

            var propertyBuilder = derivedEntityBuilder.Property(Order.IdProperty.Name, typeof(int), ConfigurationSource.DataAnnotation);

            Assert.Same(entityBuilder.Metadata.FindProperty(Order.IdProperty.Name), propertyBuilder.Metadata);
            Assert.True(propertyBuilder.IsConcurrencyToken(true, ConfigurationSource.Convention));
            Assert.True(propertyBuilder.Metadata.IsConcurrencyToken);
            Assert.False(propertyBuilder.ClrType(typeof(string), ConfigurationSource.DataAnnotation));
        }

        [Fact]
        public void Can_ignore_same_or_lower_source_property()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var entityType = entityBuilder.Metadata;

            Assert.True(entityBuilder.Ignore(Order.IdProperty.Name, ConfigurationSource.Explicit));

            Assert.Null(entityType.FindProperty(Order.IdProperty.Name));
            Assert.True(entityBuilder.Ignore(Order.IdProperty.Name, ConfigurationSource.Explicit));
            Assert.Null(entityBuilder.Property(Order.IdProperty.Name, typeof(Order), ConfigurationSource.DataAnnotation));

            Assert.NotNull(entityBuilder.Property(Order.IdProperty.Name, typeof(Order), ConfigurationSource.Explicit));
        }

        [Fact]
        public void Cannot_ignore_higher_source_property()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var entityType = entityBuilder.Metadata;

            Assert.NotNull(entityBuilder.Property(Order.IdProperty.Name, typeof(Order), ConfigurationSource.DataAnnotation));
            Assert.False(entityBuilder.Ignore(Order.IdProperty.Name, ConfigurationSource.Convention));
            Assert.NotNull(entityType.FindProperty(Order.IdProperty.Name));

            Assert.True(entityBuilder.Ignore(Order.IdProperty.Name, ConfigurationSource.DataAnnotation));
            Assert.Null(entityType.FindProperty(Order.IdProperty.Name));

            Assert.NotNull(entityBuilder.Property(Order.IdProperty.Name, typeof(Order), ConfigurationSource.Explicit));
            Assert.False(entityBuilder.Ignore(Order.IdProperty.Name, ConfigurationSource.Convention));
            Assert.False(entityBuilder.Ignore(Order.IdProperty.Name, ConfigurationSource.DataAnnotation));
            Assert.NotNull(entityType.FindProperty(Order.IdProperty.Name));

            Assert.True(entityBuilder.Ignore(Order.IdProperty.Name, ConfigurationSource.Explicit));
            Assert.Null(entityType.FindProperty(Order.IdProperty.Name));
        }

        [Fact]
        public void Can_ignore_existing_property()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var entityType = entityBuilder.Metadata;
            var property = entityType.AddProperty(Order.IdProperty.Name, typeof(int));
            property.IsShadowProperty = false;

            Assert.Same(property, entityBuilder.Property(Order.IdProperty.Name, ConfigurationSource.Convention).Metadata);

            Assert.True(entityBuilder.Ignore(Order.IdProperty.Name, ConfigurationSource.Explicit));
            Assert.Null(entityType.FindProperty(Order.IdProperty.Name));
        }

        [Fact]
        public void Can_ignore_inherited_property()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var derivedEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            derivedEntityBuilder.HasBaseType(typeof(Order), ConfigurationSource.DataAnnotation);

            Assert.NotNull(entityBuilder.Property(Order.IdProperty.Name, typeof(Order), ConfigurationSource.DataAnnotation));
            Assert.False(derivedEntityBuilder.Ignore(Order.IdProperty.Name, ConfigurationSource.Convention));
            Assert.NotNull(entityBuilder.Metadata.FindProperty(Order.IdProperty.Name));

            Assert.True(derivedEntityBuilder.Ignore(Order.IdProperty.Name, ConfigurationSource.DataAnnotation));
            Assert.Null(entityBuilder.Metadata.FindProperty(Order.IdProperty.Name));
        }

        [Fact]
        public void Can_ignore_property_that_is_part_of_lower_source_foreign_key_preserving_the_relationship()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var key = principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var relationshipBuilder =
                dependentEntityBuilder.Relationship(principalEntityBuilder, (string)null, null, ConfigurationSource.DataAnnotation)
                    .HasForeignKey(new[]
                    {
                        dependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Convention).Metadata,
                        dependentEntityBuilder.Property(Order.CustomerUniqueProperty, ConfigurationSource.Convention).Metadata
                    }, ConfigurationSource.DataAnnotation)
                    .HasPrincipalKey(key.Metadata.Properties, ConfigurationSource.DataAnnotation)
                    .IsUnique(true, ConfigurationSource.DataAnnotation)
                    .IsRequired(true, ConfigurationSource.DataAnnotation)
                    .DeleteBehavior(DeleteBehavior.Cascade, ConfigurationSource.DataAnnotation);
            var fk = relationshipBuilder.Metadata;

            Assert.True(dependentEntityBuilder.Ignore(Order.CustomerIdProperty.Name, ConfigurationSource.Explicit));

            Assert.Empty(dependentEntityBuilder.Metadata.Properties.Where(p => p.Name == Order.CustomerIdProperty.Name));
            var newFk = dependentEntityBuilder.Metadata.GetForeignKeys().Single();
            Assert.Same(fk.DeclaringEntityType, newFk.DeclaringEntityType);
            Assert.Same(fk.PrincipalEntityType, newFk.PrincipalEntityType);
            Assert.Null(newFk.PrincipalToDependent);
            Assert.Null(newFk.DependentToPrincipal);
            Assert.NotEqual(fk.Properties, newFk.DeclaringEntityType.Properties);
            Assert.Same(fk.PrincipalKey, newFk.PrincipalKey);
            Assert.True(newFk.IsUnique);
            Assert.True(newFk.IsRequired);
            Assert.Equal(DeleteBehavior.Cascade, newFk.DeleteBehavior);

            relationshipBuilder = dependentEntityBuilder.HasForeignKey(principalEntityBuilder, newFk.Properties, ConfigurationSource.Convention);
            var shadowId = principalEntityBuilder.Property("ShadowId", typeof(int), ConfigurationSource.Convention).Metadata;
            Assert.Null(relationshipBuilder.HasPrincipalKey(new[] { shadowId.Name, Customer.UniqueProperty.Name }, ConfigurationSource.Convention));
            Assert.Null(relationshipBuilder.IsUnique(false, ConfigurationSource.Convention));
            Assert.Null(relationshipBuilder.IsRequired(false, ConfigurationSource.Convention));
            Assert.Null(relationshipBuilder.DependentEntityType(principalEntityBuilder.Metadata, ConfigurationSource.Convention));
            Assert.Null(relationshipBuilder.DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.Convention));
            Assert.Null(relationshipBuilder.PrincipalToDependent(Customer.NotCollectionOrdersProperty.Name, ConfigurationSource.Convention));
            Assert.NotNull(relationshipBuilder.HasForeignKey(
                new[]
                {
                    dependentEntityBuilder.Property("ShadowFk", typeof(int), ConfigurationSource.Convention).Metadata,
                    dependentEntityBuilder.Property(Order.CustomerUniqueProperty, ConfigurationSource.Convention).Metadata
                },
                ConfigurationSource.Convention));
        }

        [Fact]
        public void Cannot_ignore_property_that_is_part_of_higher_source_foreign_key()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            dependentEntityBuilder.HasForeignKey(
                principalEntityBuilder,
                new[]
                {
                    dependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Convention).Metadata,
                    dependentEntityBuilder.Property(Order.CustomerUniqueProperty, ConfigurationSource.Convention).Metadata
                },
                ConfigurationSource.DataAnnotation);

            Assert.False(dependentEntityBuilder.Ignore(Order.CustomerIdProperty.Name, ConfigurationSource.Convention));

            Assert.NotEmpty(dependentEntityBuilder.Metadata.Properties.Where(p => p.Name == Order.CustomerIdProperty.Name));
            Assert.NotEmpty(dependentEntityBuilder.Metadata.GetForeignKeys());
        }

        [Fact]
        public void Can_ignore_property_that_is_part_of_lower_source_index()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            Assert.NotNull(entityBuilder.HasIndex(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.DataAnnotation));

            Assert.True(entityBuilder.Ignore(Order.CustomerIdProperty.Name, ConfigurationSource.Explicit));

            Assert.Empty(entityBuilder.Metadata.Properties.Where(p => p.Name == Order.CustomerIdProperty.Name));
            Assert.Empty(entityBuilder.Metadata.Indexes);
        }

        [Fact]
        public void Cannot_ignore_property_that_is_part_of_same_or_higher_source_index()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            Assert.NotNull(entityBuilder.HasIndex(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.Explicit));

            Assert.False(entityBuilder.Ignore(Order.CustomerIdProperty.Name, ConfigurationSource.DataAnnotation));

            Assert.NotEmpty(entityBuilder.Metadata.Properties.Where(p => p.Name == Order.CustomerIdProperty.Name));
            Assert.NotEmpty(entityBuilder.Metadata.Indexes);
        }

        [Fact]
        public void Can_ignore_property_that_is_part_of_lower_source_key()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            Assert.NotNull(entityBuilder.PrimaryKey(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.DataAnnotation));

            Assert.True(entityBuilder.Ignore(Order.CustomerIdProperty.Name, ConfigurationSource.Explicit));

            Assert.Empty(entityBuilder.Metadata.Properties.Where(p => p.Name == Order.CustomerIdProperty.Name));
            Assert.Empty(entityBuilder.Metadata.GetKeys());
        }

        [Fact]
        public void Can_ignore_property_that_is_part_of_lower_source_principal_key_preserving_the_relationship()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var key = principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Convention);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var principalType = principalEntityBuilder.Metadata;
            var dependentType = dependentEntityBuilder.Metadata;

            var fk = dependentEntityBuilder.Relationship(
                principalEntityBuilder,
                Order.CustomerProperty.Name,
                Customer.OrdersProperty.Name,
                ConfigurationSource.Convention)
                .HasForeignKey(new[]
                {
                    dependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Convention).Metadata,
                    dependentEntityBuilder.Property(Order.CustomerUniqueProperty, ConfigurationSource.Convention).Metadata
                }, ConfigurationSource.Convention)
                .HasPrincipalKey(key.Metadata.Properties, ConfigurationSource.Convention)
                .Metadata;

            Assert.True(principalEntityBuilder.Ignore(Customer.UniqueProperty.Name, ConfigurationSource.DataAnnotation));

            Assert.Empty(principalEntityBuilder.Metadata.Properties.Where(p => p.Name == Customer.UniqueProperty.Name));
            var newFk = dependentEntityBuilder.Metadata.GetForeignKeys().Single();
            Assert.Same(fk.DeclaringEntityType, newFk.DeclaringEntityType);
            Assert.Same(fk.PrincipalEntityType, newFk.PrincipalEntityType);
            Assert.Same(principalType.GetKeys().Single(), newFk.PrincipalKey);
            Assert.Equal(new[] { Order.CustomerIdProperty.Name, newFk.Properties.Single().Name, Order.CustomerUniqueProperty.Name },
                dependentType.Properties.Select(p => p.Name));
            Assert.Equal(new[] { Customer.IdProperty.Name, newFk.PrincipalKey.Properties.Single().Name },
                principalType.Properties.Select(p => p.Name));
            Assert.Equal(Order.CustomerProperty.Name, newFk.DependentToPrincipal.Name);
            Assert.Equal(Customer.OrdersProperty.Name, newFk.PrincipalToDependent.Name);
        }

        [Fact]
        public void Cannot_ignore_property_that_is_part_of_same_or_higher_source_key()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            Assert.NotNull(entityBuilder.PrimaryKey(new[] { Order.IdProperty, Order.CustomerIdProperty }, ConfigurationSource.Explicit));

            Assert.False(entityBuilder.Ignore(Order.CustomerIdProperty.Name, ConfigurationSource.DataAnnotation));

            Assert.NotEmpty(entityBuilder.Metadata.Properties.Where(p => p.Name == Order.CustomerIdProperty.Name));
            Assert.NotEmpty(entityBuilder.Metadata.GetKeys());
        }

        [Fact]
        public void Navigation_returns_same_value()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var foreignKeyBuilder = dependentEntityBuilder.Relationship(principalEntityBuilder, ConfigurationSource.DataAnnotation);

            Assert.True(dependentEntityBuilder.CanAddNavigation(Order.CustomerProperty.Name, ConfigurationSource.Convention));

            foreignKeyBuilder = foreignKeyBuilder.DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.DataAnnotation);

            Assert.False(dependentEntityBuilder.CanAddNavigation(Order.CustomerProperty.Name, ConfigurationSource.Explicit));
            Assert.True(dependentEntityBuilder.CanAddOrReplaceNavigation(Order.CustomerProperty.Name, ConfigurationSource.Explicit));
            Assert.True(principalEntityBuilder.CanAddNavigation(Customer.OrdersProperty.Name, ConfigurationSource.Convention));

            foreignKeyBuilder = foreignKeyBuilder.PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.DataAnnotation);

            Assert.False(principalEntityBuilder.CanAddNavigation(Customer.OrdersProperty.Name, ConfigurationSource.Explicit));
            Assert.True(principalEntityBuilder.CanAddOrReplaceNavigation(Customer.OrdersProperty.Name, ConfigurationSource.Explicit));

            var newForeignKeyBuilder = dependentEntityBuilder.Relationship(principalEntityBuilder, ConfigurationSource.Convention)
                .PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.Convention);
            Assert.Same(foreignKeyBuilder, newForeignKeyBuilder);
            newForeignKeyBuilder = principalEntityBuilder.Relationship(dependentEntityBuilder, ConfigurationSource.Convention)
                .PrincipalToDependent(Order.CustomerProperty.Name, ConfigurationSource.Convention);
            Assert.Same(foreignKeyBuilder, newForeignKeyBuilder);

            Assert.Same(foreignKeyBuilder.Metadata, dependentEntityBuilder.Metadata.GetForeignKeys().Single());
            Assert.Empty(principalEntityBuilder.Metadata.GetForeignKeys());
            Assert.Empty(dependentEntityBuilder.Metadata.GetKeys());
            Assert.Same(foreignKeyBuilder.Metadata.PrincipalKey, principalEntityBuilder.Metadata.GetKeys().Single());
        }

        [Fact]
        public void Can_override_lower_or_equal_source_conflicting_navigation()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var conflictingForeignKeyBuilder = dependentEntityBuilder.HasForeignKey(
                typeof(Customer).FullName, new[] { Order.IdProperty.Name, Order.CustomerUniqueProperty.Name }, ConfigurationSource.DataAnnotation);
            conflictingForeignKeyBuilder = conflictingForeignKeyBuilder.DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.DataAnnotation);
            conflictingForeignKeyBuilder = conflictingForeignKeyBuilder.PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.DataAnnotation);

            var foreignKeyBuilder = dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name, Order.CustomerUniqueProperty.Name }, ConfigurationSource.Convention);

            Assert.Same(conflictingForeignKeyBuilder, foreignKeyBuilder.DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.Convention));
            Assert.Same(conflictingForeignKeyBuilder, foreignKeyBuilder.PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.Convention));

            Assert.Same(conflictingForeignKeyBuilder.Metadata, dependentEntityBuilder.Metadata.FindNavigation(Order.CustomerProperty.Name).ForeignKey);
            Assert.Same(conflictingForeignKeyBuilder.Metadata, principalEntityBuilder.Metadata.FindNavigation(Customer.OrdersProperty.Name).ForeignKey);

            var newForeignKeyBuilder = dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name }, ConfigurationSource.DataAnnotation);
            newForeignKeyBuilder = newForeignKeyBuilder.DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.DataAnnotation);
            newForeignKeyBuilder = newForeignKeyBuilder.PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.DataAnnotation);

            Assert.Equal(Order.CustomerIdProperty.Name, newForeignKeyBuilder.Metadata.Properties.Single().Name);
            Assert.Same(newForeignKeyBuilder.Metadata, dependentEntityBuilder.Metadata.FindNavigation(Order.CustomerProperty.Name).ForeignKey);
            Assert.Same(newForeignKeyBuilder.Metadata, principalEntityBuilder.Metadata.FindNavigation(Customer.OrdersProperty.Name).ForeignKey);
            Assert.Same(newForeignKeyBuilder.Metadata, dependentEntityBuilder.Metadata.GetForeignKeys().Single());
        }

        [Fact]
        public void Can_ignore_lower_or_equal_source_navigation()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var foreignKeyBuilder = dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name, Order.CustomerUniqueProperty.Name }, ConfigurationSource.DataAnnotation);
            foreignKeyBuilder = foreignKeyBuilder.DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.DataAnnotation);
            foreignKeyBuilder = foreignKeyBuilder.PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.DataAnnotation);
            Assert.NotNull(foreignKeyBuilder);

            Assert.True(dependentEntityBuilder.Ignore(Order.CustomerProperty.Name, ConfigurationSource.Explicit));
            Assert.True(principalEntityBuilder.Ignore(Customer.OrdersProperty.Name, ConfigurationSource.Explicit));

            Assert.Null(dependentEntityBuilder.Metadata.FindNavigation(Order.CustomerProperty.Name));
            Assert.Null(principalEntityBuilder.Metadata.FindNavigation(Customer.OrdersProperty.Name));
            Assert.True(dependentEntityBuilder.Ignore(Order.CustomerProperty.Name, ConfigurationSource.Convention));
            Assert.True(principalEntityBuilder.Ignore(Customer.OrdersProperty.Name, ConfigurationSource.Convention));
            Assert.Empty(dependentEntityBuilder.Metadata.GetForeignKeys());

            foreignKeyBuilder = dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name, Order.CustomerUniqueProperty.Name }, ConfigurationSource.DataAnnotation);
            Assert.Null(foreignKeyBuilder.DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.DataAnnotation));
            Assert.Null(foreignKeyBuilder.PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.DataAnnotation));

            Assert.NotNull(foreignKeyBuilder.DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.Explicit));
            Assert.NotNull(foreignKeyBuilder.PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.Explicit));
        }

        [Fact]
        public void Cannot_ignore_higher_source_navigation()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var foreignKeyBuilder = dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name, Order.CustomerUniqueProperty.Name }, ConfigurationSource.Convention);

            Assert.True(dependentEntityBuilder.Ignore(Order.CustomerProperty.Name, ConfigurationSource.DataAnnotation));
            Assert.True(principalEntityBuilder.Ignore(Customer.OrdersProperty.Name, ConfigurationSource.DataAnnotation));
            Assert.False(dependentEntityBuilder.CanAddNavigation(Order.CustomerProperty.Name, ConfigurationSource.DataAnnotation));
            Assert.False(principalEntityBuilder.CanAddNavigation(Customer.OrdersProperty.Name, ConfigurationSource.DataAnnotation));
            Assert.True(dependentEntityBuilder.CanAddNavigation(Order.CustomerProperty.Name, ConfigurationSource.Explicit));
            Assert.True(principalEntityBuilder.CanAddNavigation(Customer.OrdersProperty.Name, ConfigurationSource.Explicit));

            foreignKeyBuilder = foreignKeyBuilder.DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.Explicit);
            foreignKeyBuilder = foreignKeyBuilder.PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.Explicit);

            Assert.False(dependentEntityBuilder.Ignore(Order.CustomerProperty.Name, ConfigurationSource.DataAnnotation));
            Assert.False(principalEntityBuilder.Ignore(Customer.OrdersProperty.Name, ConfigurationSource.DataAnnotation));
            Assert.NotNull(dependentEntityBuilder.Metadata.FindNavigation(Order.CustomerProperty.Name));
            Assert.NotNull(principalEntityBuilder.Metadata.FindNavigation(Customer.OrdersProperty.Name));

            Assert.Same(foreignKeyBuilder, foreignKeyBuilder.PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.Explicit));
            Assert.Null(foreignKeyBuilder.DependentToPrincipal(null, ConfigurationSource.DataAnnotation));
        }

        [Fact]
        public void Can_ignore_existing_navigation()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var property1 = dependentEntityBuilder.Metadata.AddProperty(Order.CustomerIdProperty.Name, typeof(int));
            property1.IsShadowProperty = false;
            var property2 = dependentEntityBuilder.Metadata.AddProperty(Order.CustomerUniqueProperty.Name, typeof(Guid));
            property2.IsShadowProperty = false;
            var foreignKey = dependentEntityBuilder.Metadata.AddForeignKey(
                new[]
                {
                    property1,
                    property2
                },
                principalEntityBuilder.Metadata.GetPrimaryKey(),
                principalEntityBuilder.Metadata);
            var navigationToPrincipal = dependentEntityBuilder.Metadata.AddNavigation(Order.CustomerProperty.Name, foreignKey, pointsToPrincipal: true);
            var navigationToDependent = principalEntityBuilder.Metadata.AddNavigation(Customer.OrdersProperty.Name, foreignKey, pointsToPrincipal: false);

            var relationship = dependentEntityBuilder.HasForeignKey(principalEntityBuilder, foreignKey.Properties, ConfigurationSource.Convention)
                .DependentToPrincipal(navigationToPrincipal.Name, ConfigurationSource.Convention)
                .PrincipalToDependent(navigationToDependent.Name, ConfigurationSource.Convention);
            Assert.Same(foreignKey, relationship.Metadata);

            Assert.True(dependentEntityBuilder.Ignore(Order.CustomerProperty.Name, ConfigurationSource.Explicit));
            Assert.True(principalEntityBuilder.Ignore(Customer.OrdersProperty.Name, ConfigurationSource.Explicit));

            Assert.Null(dependentEntityBuilder.Metadata.FindNavigation(Order.CustomerProperty.Name));
            Assert.Null(principalEntityBuilder.Metadata.FindNavigation(Customer.OrdersProperty.Name));
        }

        [Fact]
        public void Can_ignore_inherited_navigation()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            dependentEntityBuilder.Relationship(
                principalEntityBuilder,
                Order.CustomerProperty.Name,
                Customer.OrdersProperty.Name,
                ConfigurationSource.DataAnnotation);

            var derivedDependentEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            derivedDependentEntityBuilder.HasBaseType(dependentEntityBuilder.Metadata, ConfigurationSource.Convention);

            Assert.False(derivedDependentEntityBuilder.Ignore(Order.CustomerProperty.Name, ConfigurationSource.Convention));
            Assert.Equal(Order.CustomerProperty.Name, dependentEntityBuilder.Metadata.Navigations.Single().Name);

            derivedDependentEntityBuilder.Ignore(Order.CustomerProperty.Name, ConfigurationSource.Explicit);
            Assert.Empty(dependentEntityBuilder.Metadata.Navigations);
        }

        [Fact]
        public void Can_add_navigations_to_higher_source_foreign_key()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var foreignKeyBuilder = dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name, Order.CustomerUniqueProperty.Name }, ConfigurationSource.DataAnnotation);

            var relationship = foreignKeyBuilder.DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.Convention);
            relationship = relationship.PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.Convention);

            Assert.Same(relationship.Metadata, dependentEntityBuilder.Metadata.FindNavigation(Order.CustomerProperty.Name).ForeignKey);
            Assert.Same(relationship.Metadata, principalEntityBuilder.Metadata.FindNavigation(Customer.OrdersProperty.Name).ForeignKey);
        }

        [Fact]
        public void Can_only_override_existing_conflicting_navigations_explicitly()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var existingForeignKey = dependentEntityBuilder.Metadata.AddForeignKey(
                new[]
                {
                    dependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Explicit).Metadata,
                    dependentEntityBuilder.Property(Order.CustomerUniqueProperty, ConfigurationSource.Explicit).Metadata
                },
                principalEntityBuilder.Metadata.GetPrimaryKey(),
                principalEntityBuilder.Metadata);
            existingForeignKey.PrincipalEntityType
                .AddNavigation(Customer.OrdersProperty.Name, existingForeignKey, pointsToPrincipal: false);
            existingForeignKey.DeclaringEntityType
                .AddNavigation(Order.CustomerProperty.Name, existingForeignKey, pointsToPrincipal: true);
            var newForeignKeyBuilder = dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.IdProperty.Name, Order.CustomerUniqueProperty.Name }, ConfigurationSource.Convention);

            newForeignKeyBuilder = newForeignKeyBuilder.DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.Convention);
            Assert.Same(existingForeignKey, newForeignKeyBuilder.Metadata);

            var foreignKeyBuilder = dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name, Order.CustomerUniqueProperty.Name }, ConfigurationSource.Convention);
            Assert.Same(foreignKeyBuilder, foreignKeyBuilder.PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.Explicit));

            Assert.Equal(Customer.OrdersProperty.Name, foreignKeyBuilder.Metadata.PrincipalToDependent.Name);
            Assert.Equal(Order.CustomerProperty.Name, foreignKeyBuilder.Metadata.DependentToPrincipal.Name);
        }

        [Fact]
        public void Navigation_to_principal_does_not_change_uniqueness_for_relationship()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var foreignKeyBuilder = dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name, Order.CustomerUniqueProperty.Name }, ConfigurationSource.Convention);
            foreignKeyBuilder.IsUnique(false, ConfigurationSource.Convention);
            Assert.False(foreignKeyBuilder.Metadata.IsUnique.Value);

            foreignKeyBuilder = foreignKeyBuilder.DependentToPrincipal("Customer", ConfigurationSource.Convention);
            Assert.NotNull(foreignKeyBuilder.Metadata.DependentToPrincipal);
            Assert.False(foreignKeyBuilder.Metadata.IsUnique.Value);
        }

        [Fact]
        public void Navigation_to_dependent_changes_uniqueness_for_relationship_of_lower_or_equal_source()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Explicit);

            var foreignKeyBuilder = dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name, Order.CustomerUniqueProperty.Name }, ConfigurationSource.Convention);
            Assert.Null(foreignKeyBuilder.Metadata.IsUnique);

            foreignKeyBuilder = foreignKeyBuilder.PrincipalToDependent(Customer.NotCollectionOrdersProperty.Name, ConfigurationSource.DataAnnotation);
            Assert.Equal(Customer.NotCollectionOrdersProperty.Name, foreignKeyBuilder.Metadata.PrincipalToDependent.Name);
            Assert.True(foreignKeyBuilder.Metadata.IsUnique);

            foreignKeyBuilder = foreignKeyBuilder.PrincipalToDependent(Customer.AmbiguousOrderProperty.Name, ConfigurationSource.DataAnnotation);
            Assert.Equal(Customer.AmbiguousOrderProperty.Name, foreignKeyBuilder.Metadata.PrincipalToDependent.Name);
            Assert.True(foreignKeyBuilder.Metadata.IsUnique);

            foreignKeyBuilder = foreignKeyBuilder.PrincipalToDependent(Customer.OrdersProperty.Name, ConfigurationSource.DataAnnotation);
            Assert.Equal(Customer.OrdersProperty.Name, foreignKeyBuilder.Metadata.PrincipalToDependent.Name);
            Assert.False(foreignKeyBuilder.Metadata.IsUnique);

            foreignKeyBuilder = foreignKeyBuilder.PrincipalToDependent(Customer.AmbiguousOrderProperty.Name, ConfigurationSource.DataAnnotation);
            Assert.Equal(Customer.AmbiguousOrderProperty.Name, foreignKeyBuilder.Metadata.PrincipalToDependent.Name);
            Assert.False(foreignKeyBuilder.Metadata.IsUnique);
        }

        [Fact]
        public void Navigation_to_dependent_does_not_change_uniqueness_for_relationship_of_higher_source()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var foreignKeyBuilder = dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name, Order.CustomerUniqueProperty.Name }, ConfigurationSource.Convention);
            Assert.Null(foreignKeyBuilder.Metadata.IsUnique);

            foreignKeyBuilder = foreignKeyBuilder.IsUnique(false, ConfigurationSource.DataAnnotation);
            Assert.False(foreignKeyBuilder.Metadata.IsUnique);

            Assert.Null(foreignKeyBuilder.PrincipalToDependent(Customer.NotCollectionOrdersProperty.Name, ConfigurationSource.Convention));
            Assert.False(foreignKeyBuilder.Metadata.IsUnique);

            foreignKeyBuilder = foreignKeyBuilder.PrincipalToDependent("Orders", ConfigurationSource.Convention);
            Assert.Equal("Orders", foreignKeyBuilder.Metadata.PrincipalToDependent.Name);
        }

        [Fact]
        public void Relationship_does_not_return_same_instance_if_no_navigations()
        {
            var modelBuilder = CreateModelBuilder();
            var customerEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var orderEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var relationshipBuilder = orderEntityBuilder.Relationship(customerEntityBuilder, ConfigurationSource.Convention);

            Assert.NotNull(relationshipBuilder);
            Assert.NotSame(relationshipBuilder, orderEntityBuilder.Relationship(customerEntityBuilder, ConfigurationSource.Convention));
            Assert.Equal(2, orderEntityBuilder.Metadata.GetForeignKeys().Count());
        }

        [Fact]
        public void Can_ignore_lower_source_dependent_entity_type()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            Assert.NotNull(dependentEntityBuilder.Relationship(principalEntityBuilder, ConfigurationSource.Convention));

            Assert.True(modelBuilder.Ignore(typeof(Order), ConfigurationSource.Explicit));

            Assert.Equal(typeof(Customer).FullName, modelBuilder.Metadata.EntityTypes.Single().Name);
        }

        [Fact]
        public void Can_ignore_lower_source_principal_entity_type()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Convention);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            Assert.NotNull(dependentEntityBuilder.Relationship(principalEntityBuilder, ConfigurationSource.Convention));

            Assert.True(modelBuilder.Ignore(typeof(Customer), ConfigurationSource.Explicit));

            Assert.Equal(typeof(Order).FullName, modelBuilder.Metadata.EntityTypes.Single().Name);
        }

        [Fact]
        public void Can_ignore_lower_source_navigation_to_dependent()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            Assert.NotNull(dependentEntityBuilder.Relationship(principalEntityBuilder, null, Customer.OrdersProperty.Name, ConfigurationSource.Convention));

            Assert.True(principalEntityBuilder.Ignore(Customer.OrdersProperty.Name, ConfigurationSource.Explicit));

            Assert.Empty(dependentEntityBuilder.Metadata.GetForeignKeys());
            Assert.Empty(principalEntityBuilder.Metadata.GetForeignKeys());
            Assert.True(principalEntityBuilder.Ignore(Customer.OrdersProperty.Name, ConfigurationSource.Convention));
            Assert.Null(dependentEntityBuilder.Relationship(principalEntityBuilder, null, Customer.OrdersProperty.Name, ConfigurationSource.Convention));
        }

        [Fact]
        public void Can_ignore_lower_source_navigation_to_principal()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            Assert.NotNull(dependentEntityBuilder.Relationship(principalEntityBuilder, Order.CustomerProperty, null, ConfigurationSource.Convention));

            Assert.True(dependentEntityBuilder.Ignore(Order.CustomerProperty.Name, ConfigurationSource.Explicit));

            Assert.Empty(dependentEntityBuilder.Metadata.GetForeignKeys());
            Assert.Empty(principalEntityBuilder.Metadata.GetForeignKeys());
            Assert.True(dependentEntityBuilder.Ignore(Order.CustomerProperty.Name, ConfigurationSource.Convention));
            Assert.Null(dependentEntityBuilder.Relationship(principalEntityBuilder, Order.CustomerProperty, null, ConfigurationSource.Convention));
        }

        [Fact]
        public void Cannot_add_navigation_to_principal_if_conflicting_navigation_is_higher_source()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            dependentEntityBuilder.Relationship(principalEntityBuilder, null, Customer.OrdersProperty.Name, ConfigurationSource.Explicit);

            Assert.Null(dependentEntityBuilder.Relationship(principalEntityBuilder, Order.CustomerProperty.Name, Customer.OrdersProperty.Name, ConfigurationSource.Convention));

            Assert.Empty(principalEntityBuilder.Metadata.GetForeignKeys());
            var fk = dependentEntityBuilder.Metadata.GetForeignKeys().Single();
            Assert.Null(fk.DependentToPrincipal);
            Assert.Equal(Customer.OrdersProperty.Name, fk.PrincipalToDependent.Name);
        }

        [Fact]
        public void Cannot_add_navigation_to_dependent_if_conflicting_navigation_is_higher_source()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            dependentEntityBuilder.Relationship(principalEntityBuilder, Order.CustomerProperty.Name, null, ConfigurationSource.Explicit);

            Assert.Null(dependentEntityBuilder.Relationship(principalEntityBuilder, Order.CustomerProperty, Customer.OrdersProperty, ConfigurationSource.Convention));

            Assert.Empty(principalEntityBuilder.Metadata.GetForeignKeys());
            var fk = dependentEntityBuilder.Metadata.GetForeignKeys().Single();
            Assert.Null(fk.PrincipalToDependent);
            Assert.Equal(Order.CustomerProperty.Name, fk.DependentToPrincipal.Name);
        }

        [Fact]
        public void Dependent_conflicting_relationship_is_not_removed_if_principal_conflicting_relationship_cannot_be_removed()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var orderRelationship = dependentEntityBuilder.Relationship(principalEntityBuilder, Order.CustomerProperty.Name, null, ConfigurationSource.DataAnnotation);
            Assert.NotNull(orderRelationship);
            var customerRelationship = dependentEntityBuilder.Relationship(principalEntityBuilder, null, Customer.NotCollectionOrdersProperty.Name, ConfigurationSource.Convention)
                .DependentEntityType(dependentEntityBuilder, ConfigurationSource.Convention);
            Assert.NotNull(customerRelationship);

            Assert.Null(principalEntityBuilder.Relationship(dependentEntityBuilder, Customer.NotCollectionOrdersProperty.Name, Order.CustomerProperty.Name, ConfigurationSource.Convention));

            var orderFk = dependentEntityBuilder.Metadata.Navigations.Single().ForeignKey;
            var customerFk = principalEntityBuilder.Metadata.Navigations.Single().ForeignKey;
            Assert.Null(orderFk.PrincipalToDependent);
            Assert.Equal(Order.CustomerProperty.Name, orderFk.DependentToPrincipal.Name);
            Assert.Equal(Customer.NotCollectionOrdersProperty.Name, customerFk.PrincipalToDependent.Name);
            Assert.Null(customerFk.DependentToPrincipal);
        }

        [Fact]
        public void Principal_conflicting_relationship_is_not_removed_if_dependent_conflicting_relationship_cannot_be_removed()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var orderRelationship = dependentEntityBuilder.Relationship(principalEntityBuilder, Order.CustomerProperty, null, ConfigurationSource.Convention);
            Assert.NotNull(orderRelationship);
            var customerRelationship = dependentEntityBuilder.Relationship(principalEntityBuilder, null, Customer.NotCollectionOrdersProperty, ConfigurationSource.DataAnnotation)
                .PrincipalEntityType(principalEntityBuilder, ConfigurationSource.DataAnnotation);
            Assert.NotNull(customerRelationship);

            Assert.Null(dependentEntityBuilder.Relationship(principalEntityBuilder, Customer.NotCollectionOrdersProperty, Order.CustomerProperty, ConfigurationSource.Convention));

            Assert.Null(orderRelationship.Metadata.PrincipalToDependent);
            Assert.Equal(Order.CustomerProperty.Name, orderRelationship.Metadata.DependentToPrincipal.Name);
            Assert.Equal(Customer.NotCollectionOrdersProperty.Name, customerRelationship.Metadata.PrincipalToDependent.Name);
            Assert.Null(customerRelationship.Metadata.DependentToPrincipal);
        }

        [Fact]
        public void Conflicting_navigations_are_not_removed_if_conflicting_fk_cannot_be_removed()
        {
            var modelBuilder = CreateModelBuilder();
            var customerEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            var orderEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var fkProperty = orderEntityBuilder.Property(Order.IdProperty, ConfigurationSource.Explicit).Metadata;

            var orderRelationship = orderEntityBuilder.Relationship(
                customerEntityBuilder, Order.CustomerProperty.Name, null, ConfigurationSource.Convention);
            Assert.NotNull(orderRelationship);
            var customerRelationship = orderEntityBuilder.Relationship(
                customerEntityBuilder, null, Customer.NotCollectionOrdersProperty.Name, ConfigurationSource.Convention);
            Assert.NotNull(customerRelationship);
            var fkRelationship = orderEntityBuilder.HasForeignKey(
                orderEntityBuilder, new[] { fkProperty }, ConfigurationSource.DataAnnotation);
            Assert.NotNull(fkRelationship);

            Assert.Null(orderEntityBuilder.Relationship(customerEntityBuilder, ConfigurationSource.Convention)
                .DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.Convention)
                .PrincipalToDependent(Customer.NotCollectionOrdersProperty.Name, ConfigurationSource.Convention)
                .HasForeignKey(new[] { fkProperty }, ConfigurationSource.Convention));

            var navigationFk = customerEntityBuilder.Metadata.Navigations.Single().ForeignKey;
            Assert.Same(navigationFk, orderEntityBuilder.Metadata.Navigations.Single().ForeignKey);
            Assert.Same(fkRelationship.Metadata, orderEntityBuilder.Metadata.GetForeignKeys().Single(fk => fk.Properties.Any(p => p == fkProperty)));
            Assert.NotSame(navigationFk, fkRelationship.Metadata);
        }

        [Fact]
        public void Can_only_override_lower_or_equal_source_base_type()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Convention);
            var derivedEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);

            Assert.Same(derivedEntityBuilder,
                derivedEntityBuilder.HasBaseType(entityBuilder.Metadata, ConfigurationSource.DataAnnotation));
            Assert.False(modelBuilder.Ignore(entityBuilder.Metadata.Name, ConfigurationSource.Convention));
            Assert.Same(derivedEntityBuilder,
                derivedEntityBuilder.HasBaseType((Type)null, ConfigurationSource.DataAnnotation));
            Assert.Same(derivedEntityBuilder,
                derivedEntityBuilder.HasBaseType(entityBuilder.Metadata, ConfigurationSource.Explicit));
            Assert.Null(derivedEntityBuilder.HasBaseType((Type)null, ConfigurationSource.Convention));
            Assert.Same(entityBuilder.Metadata, derivedEntityBuilder.Metadata.BaseType);
        }

        [Fact]
        public void Can_only_override_existing_base_type_explicitly()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var derivedEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            derivedEntityBuilder.Metadata.BaseType = entityBuilder.Metadata;

            Assert.Same(derivedEntityBuilder, derivedEntityBuilder.HasBaseType(entityBuilder.Metadata, ConfigurationSource.Convention));
            Assert.Null(derivedEntityBuilder.HasBaseType((EntityType)null, ConfigurationSource.Convention));
            Assert.Same(derivedEntityBuilder, derivedEntityBuilder.HasBaseType((EntityType)null, ConfigurationSource.Explicit));
            Assert.Null(derivedEntityBuilder.Metadata.BaseType);
        }

        [Fact]
        public void Can_set_base_type_if_duplicate_properties_of_higher_source()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            entityBuilder.Property(Order.IdProperty, ConfigurationSource.Convention);
            var derivedEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            derivedEntityBuilder.Property(Order.IdProperty, ConfigurationSource.DataAnnotation);

            Assert.Same(derivedEntityBuilder,
                derivedEntityBuilder.HasBaseType(entityBuilder.Metadata.Name, ConfigurationSource.Convention));
            Assert.Same(entityBuilder.Metadata, derivedEntityBuilder.Metadata.BaseType);
            Assert.Equal(1, entityBuilder.Metadata.GetDeclaredProperties().Count());
            Assert.Equal(0, derivedEntityBuilder.Metadata.GetDeclaredProperties().Count());
        }

        [Fact]
        public void Can_only_set_base_type_if_keys_of_lower_or_equal_source()
        {
            var modelBuilder = CreateModelBuilder();
            var entityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            var derivedEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            derivedEntityBuilder.HasKey(new[] { Order.IdProperty }, ConfigurationSource.DataAnnotation);

            Assert.Null(derivedEntityBuilder.HasBaseType(entityBuilder.Metadata, ConfigurationSource.Convention));
            Assert.Null(derivedEntityBuilder.Metadata.BaseType);
            Assert.Equal(1, derivedEntityBuilder.Metadata.GetDeclaredKeys().Count());

            Assert.Same(derivedEntityBuilder,
                derivedEntityBuilder.HasBaseType(typeof(Order), ConfigurationSource.DataAnnotation));
            Assert.Same(entityBuilder.Metadata, derivedEntityBuilder.Metadata.BaseType);
            Assert.Equal(0, derivedEntityBuilder.Metadata.GetDeclaredKeys().Count());
        }

        [Fact]
        public void Can_only_set_base_type_if_relationship_with_conflicting_navigation_of_lower_or_equal_source()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name, Order.CustomerUniqueProperty.Name }, ConfigurationSource.DataAnnotation)
                .DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.DataAnnotation);

            var derivedDependentEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            var derivedIdProperty = derivedDependentEntityBuilder.Property(Order.IdProperty, ConfigurationSource.Convention).Metadata;

            derivedDependentEntityBuilder.HasForeignKey(
                principalEntityBuilder, new[] { derivedIdProperty }, ConfigurationSource.DataAnnotation)
                .DependentToPrincipal(Order.CustomerProperty.Name,
                    ConfigurationSource.DataAnnotation);

            Assert.Null(derivedDependentEntityBuilder.HasBaseType(dependentEntityBuilder.Metadata, ConfigurationSource.Convention));
            Assert.Null(derivedDependentEntityBuilder.Metadata.BaseType);
            Assert.Equal(1, derivedDependentEntityBuilder.Metadata.GetDeclaredNavigations().Count());

            Assert.Same(derivedDependentEntityBuilder,
                derivedDependentEntityBuilder.HasBaseType(dependentEntityBuilder.Metadata, ConfigurationSource.DataAnnotation));
            Assert.Same(dependentEntityBuilder.Metadata, derivedDependentEntityBuilder.Metadata.BaseType);
            Assert.Equal(1, dependentEntityBuilder.Metadata.GetDeclaredNavigations().Count());
            Assert.Equal(0, derivedDependentEntityBuilder.Metadata.GetDeclaredNavigations().Count());
        }

        [Fact]
        public void Can_only_set_base_type_if_relationship_with_conflicting_navigation_of_lower_or_equal_source_on_base_type()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name, Order.CustomerUniqueProperty.Name }, ConfigurationSource.DataAnnotation)
                .DependentToPrincipal(Order.CustomerProperty.Name, ConfigurationSource.DataAnnotation);

            var derivedDependentEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            var derivedIdProperty = derivedDependentEntityBuilder.Property(Order.IdProperty, ConfigurationSource.Convention).Metadata;

            derivedDependentEntityBuilder.HasForeignKey(
                principalEntityBuilder, new[] { derivedIdProperty }, ConfigurationSource.DataAnnotation)
                .Navigations(Order.CustomerProperty.Name, Customer.SpecialOrdersProperty.Name, ConfigurationSource.DataAnnotation);

            Assert.Null(derivedDependentEntityBuilder.HasBaseType(dependentEntityBuilder.Metadata, ConfigurationSource.Convention));
            Assert.Null(derivedDependentEntityBuilder.Metadata.BaseType);
            Assert.Equal(1, dependentEntityBuilder.Metadata.GetDeclaredNavigations().Count());

            Assert.Same(derivedDependentEntityBuilder,
                derivedDependentEntityBuilder.HasBaseType(dependentEntityBuilder.Metadata, ConfigurationSource.DataAnnotation));
            Assert.Same(dependentEntityBuilder.Metadata, derivedDependentEntityBuilder.Metadata.BaseType);
            Assert.Equal(0, dependentEntityBuilder.Metadata.GetDeclaredNavigations().Count());
            Assert.Equal(1, derivedDependentEntityBuilder.Metadata.GetDeclaredNavigations().Count());
        }

        [Fact]
        public void Can_only_set_base_type_if_relationship_with_conflicting_foreign_key_of_lower_or_equal_source()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name }, ConfigurationSource.DataAnnotation);

            var derivedDependentEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            var derivedIdProperty = derivedDependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Convention).Metadata;

            derivedDependentEntityBuilder.Relationship(
                principalEntityBuilder, Order.CustomerProperty.Name, null, ConfigurationSource.DataAnnotation)
                .HasForeignKey(new[] { derivedIdProperty }, ConfigurationSource.DataAnnotation);

            Assert.Null(derivedDependentEntityBuilder.HasBaseType(dependentEntityBuilder.Metadata, ConfigurationSource.Convention));
            Assert.Null(derivedDependentEntityBuilder.Metadata.BaseType);
            Assert.Equal(1, derivedDependentEntityBuilder.Metadata.GetDeclaredForeignKeys().Count());

            Assert.Same(derivedDependentEntityBuilder,
                derivedDependentEntityBuilder.HasBaseType(dependentEntityBuilder.Metadata, ConfigurationSource.DataAnnotation));
            Assert.Same(dependentEntityBuilder.Metadata, derivedDependentEntityBuilder.Metadata.BaseType);
            Assert.Equal(1, dependentEntityBuilder.Metadata.GetDeclaredForeignKeys().Count());
            Assert.Equal(0, derivedDependentEntityBuilder.Metadata.GetDeclaredForeignKeys().Count());
        }

        [Fact]
        public void Can_only_set_base_type_if_relationship_with_conflicting_foreign_key_of_lower_or_equal_source_on_base_type()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);
            dependentEntityBuilder.HasForeignKey(typeof(Customer).FullName, new[] { Order.CustomerIdProperty.Name }, ConfigurationSource.DataAnnotation);

            var derivedDependentEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            var derivedIdProperty = derivedDependentEntityBuilder.Property(Order.CustomerIdProperty, ConfigurationSource.Convention).Metadata;

            derivedDependentEntityBuilder.Relationship(
                principalEntityBuilder, null, Customer.SpecialOrdersProperty.Name, ConfigurationSource.DataAnnotation)
                .HasForeignKey(new[] { derivedIdProperty }, ConfigurationSource.DataAnnotation);

            Assert.Null(derivedDependentEntityBuilder.HasBaseType(dependentEntityBuilder.Metadata, ConfigurationSource.Convention));
            Assert.Null(derivedDependentEntityBuilder.Metadata.BaseType);
            Assert.Equal(1, derivedDependentEntityBuilder.Metadata.GetDeclaredForeignKeys().Count());

            Assert.Same(derivedDependentEntityBuilder,
                derivedDependentEntityBuilder.HasBaseType(dependentEntityBuilder.Metadata, ConfigurationSource.DataAnnotation));
            Assert.Same(dependentEntityBuilder.Metadata, derivedDependentEntityBuilder.Metadata.BaseType);
            Assert.Equal(0, dependentEntityBuilder.Metadata.GetDeclaredForeignKeys().Count());
            Assert.Equal(1, derivedDependentEntityBuilder.Metadata.GetDeclaredForeignKeys().Count());
        }

        [Fact]
        public void Setting_base_type_preserves_non_conflicting_referencing_relationship()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var derivedPrincipalEntityBuilder = modelBuilder.Entity(typeof(SpecialCustomer), ConfigurationSource.Convention);
            var derivedIdProperty = derivedPrincipalEntityBuilder.Property(Customer.IdProperty, ConfigurationSource.Convention).Metadata;

            dependentEntityBuilder.Relationship(
                derivedPrincipalEntityBuilder,
                Order.CustomerProperty.Name,
                Customer.OrdersProperty.Name,
                ConfigurationSource.Convention)
                .HasPrincipalKey(new[] { derivedIdProperty }, ConfigurationSource.Convention);
            Assert.Equal(1, derivedPrincipalEntityBuilder.Metadata.GetDeclaredKeys().Count());

            Assert.Same(derivedPrincipalEntityBuilder,
                derivedPrincipalEntityBuilder.HasBaseType(principalEntityBuilder.Metadata, ConfigurationSource.Convention));

            Assert.Equal(1, principalEntityBuilder.Metadata.GetDeclaredKeys().Count());
            Assert.Equal(0, derivedPrincipalEntityBuilder.Metadata.GetDeclaredKeys().Count());
            Assert.Equal(0, derivedPrincipalEntityBuilder.Metadata.GetForeignKeys().Count());
            Assert.Equal(0, principalEntityBuilder.Metadata.FindReferencingForeignKeys().Count());
            var fk = derivedPrincipalEntityBuilder.Metadata.FindReferencingForeignKeys().Single();
            Assert.Equal(Order.CustomerProperty.Name, fk.DependentToPrincipal.Name);
            Assert.Equal(Customer.OrdersProperty.Name, fk.PrincipalToDependent.Name);
        }

        [Fact]
        public void Setting_base_type_preserves_non_conflicting_relationship_on_duplicate_foreign_key_properties()
        {
            var modelBuilder = CreateModelBuilder();
            var principalEntityBuilder = modelBuilder.Entity(typeof(Customer), ConfigurationSource.Explicit);
            principalEntityBuilder.PrimaryKey(new[] { Customer.IdProperty, Customer.UniqueProperty }, ConfigurationSource.Explicit);
            var dependentEntityBuilder = modelBuilder.Entity(typeof(Order), ConfigurationSource.Explicit);

            var derivedDependentEntityBuilder = modelBuilder.Entity(typeof(SpecialOrder), ConfigurationSource.Convention);
            var derivedIdProperty = derivedDependentEntityBuilder.Property(Order.IdProperty, ConfigurationSource.Convention).Metadata;

            derivedDependentEntityBuilder.Relationship(
                principalEntityBuilder,
                Order.CustomerProperty.Name,
                Customer.SpecialOrdersProperty.Name,
                ConfigurationSource.DataAnnotation)
                .HasForeignKey(new[] { derivedIdProperty }, ConfigurationSource.DataAnnotation);

            Assert.Same(derivedDependentEntityBuilder,
                derivedDependentEntityBuilder.HasBaseType(dependentEntityBuilder.Metadata, ConfigurationSource.Convention));
            Assert.Equal(0, dependentEntityBuilder.Metadata.GetForeignKeys().Count());
            Assert.Equal(0, dependentEntityBuilder.Metadata.GetDeclaredProperties().Count());
            var fk = derivedDependentEntityBuilder.Metadata.GetForeignKeys().Single();
            Assert.Equal(Order.CustomerProperty.Name, fk.DependentToPrincipal.Name);
            Assert.Equal(Customer.SpecialOrdersProperty.Name, fk.PrincipalToDependent.Name);
            Assert.Equal(Order.IdProperty.Name, fk.Properties.Single().Name);
        }

        private InternalModelBuilder CreateModelBuilder() => new InternalModelBuilder(new Model(), new ConventionSet());

        private class Order
        {
            public static readonly PropertyInfo IdProperty = typeof(Order).GetProperty("Id");
            public static readonly PropertyInfo CustomerIdProperty = typeof(Order).GetProperty("CustomerId");
            public static readonly PropertyInfo CustomerUniqueProperty = typeof(Order).GetProperty("CustomerUnique");
            public static readonly PropertyInfo CustomerProperty = typeof(Order).GetProperty("Customer");

            public int Id { get; set; }
            public int CustomerId { get; set; }
            public Guid? CustomerUnique { get; set; }
            public Customer Customer { get; set; }
        }

        private class SpecialOrder : Order, IEnumerable<Order>
        {
            public IEnumerator<Order> GetEnumerator()
            {
                yield return this;
            }

            IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
        }

        private class BackOrder : Order
        {
        }

        private class Customer
        {
            public static readonly PropertyInfo IdProperty = typeof(Customer).GetProperty("Id");
            public static readonly PropertyInfo UniqueProperty = typeof(Customer).GetProperty("Unique");
            public static readonly PropertyInfo OrdersProperty = typeof(Customer).GetProperty("Orders");
            public static readonly PropertyInfo EnumerableOrdersProperty = typeof(Customer).GetProperty("EnumerableOrders");
            public static readonly PropertyInfo NotCollectionOrdersProperty = typeof(Customer).GetProperty("NotCollectionOrders");
            public static readonly PropertyInfo SpecialOrdersProperty = typeof(Customer).GetProperty("SpecialOrders");
            public static readonly PropertyInfo AmbiguousOrderProperty = typeof(Customer).GetProperty("AmbiguousOrder");

            public int Id { get; set; }
            public Guid Unique { get; set; }
            public ICollection<Order> Orders { get; set; }
            public ICollection<SpecialOrder> SpecialOrders { get; set; }
            public SpecialOrder AmbiguousOrder { get; set; }
            public IEnumerable<Order> EnumerableOrders { get; set; }
            public Order NotCollectionOrders { get; set; }
        }

        private class SpecialCustomer : Customer
        {
        }
    }
}
