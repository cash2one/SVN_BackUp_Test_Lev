// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System.Collections;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Internal;
using Microsoft.Data.Entity.Metadata;

namespace Microsoft.Data.Entity.ChangeTracking.Internal
{
    public class ChangeDetector : IChangeDetector
    {
        private readonly IEntityGraphAttacher _attacher;

        public ChangeDetector([NotNull] IEntityGraphAttacher attacher)
        {
            _attacher = attacher;
        }

        public virtual void PropertyChanged(InternalEntityEntry entry, IPropertyBase propertyBase)
        {
            var snapshot = entry.TryGetSidecar(Sidecar.WellKnownNames.RelationshipsSnapshot);

            var property = propertyBase as IProperty;
            if (property != null)
            {
                entry.SetPropertyModified(property);

                if (snapshot != null)
                {
                    DetectKeyChange(entry, property, snapshot);
                }
            }
            else
            {
                var navigation = propertyBase as INavigation;
                if (navigation != null
                    && snapshot != null)
                {
                    DetectNavigationChange(entry, navigation, snapshot);
                }
            }
        }

        public virtual void PropertyChanging(InternalEntityEntry entry, IPropertyBase propertyBase)
        {
            if (!entry.EntityType.UseEagerSnapshots())
            {
                var property = propertyBase as IProperty;
                if (property != null
                    && property.GetOriginalValueIndex() >= 0)
                {
                    entry.OriginalValues.EnsureSnapshot(property);
                }

                var navigation = propertyBase as INavigation;
                if ((navigation != null && !navigation.IsCollection())
                    || (property != null && (property.IsKey() || property.IsForeignKey(entry.EntityType))))
                {
                    // TODO: Consider making snapshot temporary here since it is no longer required after PropertyChanged is called
                    // See issue #730
                    entry.RelationshipsSnapshot.TakeSnapshot(propertyBase);
                }
            }
        }

        public virtual void DetectChanges(IStateManager stateManager)
        {
            foreach (var entry in stateManager.Entries.ToList())
            {
                DetectChanges(entry);
            }
        }

        public virtual void DetectChanges(InternalEntityEntry entry)
        {
            DetectPropertyChanges(entry);
            DetectRelationshipChanges(entry);
        }

        private void DetectPropertyChanges(InternalEntityEntry entry)
        {
            var entityType = entry.EntityType;

            if (entityType.HasPropertyChangedNotifications())
            {
                return;
            }

            var snapshot = entry.TryGetSidecar(Sidecar.WellKnownNames.OriginalValues);
            if (snapshot == null)
            {
                return;
            }

            foreach (var property in entityType.GetProperties())
            {
                if (property.GetOriginalValueIndex() >= 0
                    && !Equals(entry[property], snapshot[property]))
                {
                    entry.SetPropertyModified(property);
                }
            }
        }

        private void DetectRelationshipChanges(InternalEntityEntry entry)
        {
            var snapshot = entry.TryGetSidecar(Sidecar.WellKnownNames.RelationshipsSnapshot);
            if (snapshot != null)
            {
                DetectKeyChanges(entry, snapshot);
                DetectNavigationChanges(entry, snapshot);
            }
        }

        private void DetectKeyChanges(InternalEntityEntry entry, Sidecar snapshot)
        {
            var entityType = entry.EntityType;

            if (!entityType.HasPropertyChangedNotifications())
            {
                foreach (var property in entityType.GetProperties())
                {
                    DetectKeyChange(entry, property, snapshot);
                }
            }
        }

        private void DetectNavigationChanges(InternalEntityEntry entry, Sidecar snapshot)
        {
            var entityType = entry.EntityType;

            if (!entityType.HasPropertyChangedNotifications()
                || entityType.GetNavigations().Any(n => n.IsNonNotifyingCollection(entry)))
            {
                foreach (var navigation in entityType.GetNavigations())
                {
                    DetectNavigationChange(entry, navigation, snapshot);
                }
            }
        }

        private void DetectKeyChange(InternalEntityEntry entry, IProperty property, Sidecar snapshot)
        {
            if (!snapshot.HasValue(property))
            {
                return;
            }

            var keys = property.FindContainingKeys().ToList();
            var foreignKeys = property.FindContainingForeignKeys(entry.EntityType).ToList();

            if (keys.Count > 0
                || foreignKeys.Count > 0)
            {
                var snapshotValue = snapshot[property];
                var currentValue = entry[property];

                // Note that mutation of a byte[] key is not supported or detected, but two different instances
                // of byte[] with the same content must be detected as equal.
                if (!StructuralComparisons.StructuralEqualityComparer.Equals(currentValue, snapshotValue))
                {
                    var stateManager = entry.StateManager;

                    if (foreignKeys.Count > 0)
                    {
                        stateManager.Notify.ForeignKeyPropertyChanged(entry, property, snapshotValue, currentValue);

                        foreach (var foreignKey in foreignKeys)
                        {
                            stateManager.UpdateDependentMap(entry, snapshot.GetDependentKeyValue(foreignKey), foreignKey);
                        }
                    }

                    if (keys.Count > 0)
                    {
                        foreach (var key in keys)
                        {
                            stateManager.UpdateIdentityMap(entry, snapshot.GetPrincipalKeyValue(key), key);
                        }

                        stateManager.Notify.PrincipalKeyPropertyChanged(entry, property, snapshotValue, currentValue);
                    }

                    snapshot.TakeSnapshot(property);
                }
            }
        }

        private void DetectNavigationChange(InternalEntityEntry entry, INavigation navigation, Sidecar snapshot)
        {
            var snapshotValue = snapshot[navigation];
            var currentValue = entry[navigation];
            var stateManager = entry.StateManager;

            var added = new HashSet<object>(ReferenceEqualityComparer.Instance);

            if (navigation.IsCollection())
            {
                var snapshotCollection = (IEnumerable)snapshotValue;
                var currentCollection = (IEnumerable)currentValue;

                var removed = new HashSet<object>(ReferenceEqualityComparer.Instance);
                if (snapshotCollection != null)
                {
                    foreach (var entity in snapshotCollection)
                    {
                        removed.Add(entity);
                    }
                }

                if (currentCollection != null)
                {
                    foreach (var entity in currentCollection)
                    {
                        if (!removed.Remove(entity))
                        {
                            added.Add(entity);
                        }
                    }
                }

                if (added.Any()
                    || removed.Any())
                {
                    stateManager.Notify.NavigationCollectionChanged(entry, navigation, added, removed);

                    snapshot.TakeSnapshot(navigation);
                }
            }
            else if (!ReferenceEquals(currentValue, snapshotValue))
            {
                stateManager.Notify.NavigationReferenceChanged(entry, navigation, snapshotValue, currentValue);

                if (currentValue != null)
                {
                    added.Add(currentValue);
                }

                snapshot.TakeSnapshot(navigation);
            }

            foreach (var addedEntity in added)
            {
                var addedEntry = stateManager.GetOrCreateEntry(addedEntity);
                if (addedEntry.EntityState == EntityState.Detached)
                {
                    _attacher.AttachGraph(addedEntry, EntityState.Added);
                }
            }
        }
    }
}
