﻿// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Collections.Generic;
using System.Data.Common;
using System.Diagnostics;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using Microsoft.Data.Entity.ChangeTracking.Internal;
using Microsoft.Data.Entity.Internal;
using Microsoft.Data.Entity.Storage;

namespace Microsoft.Data.Entity.Update
{
    /// <summary>
    ///     A <see cref="ReaderModificationCommandBatch" /> for providers which append an SQL query to find out
    ///     how many rows were affected (see <see cref="SqlGenerator.AppendSelectAffectedCountCommand" />).
    /// </summary>
    public abstract class AffectedCountModificationCommandBatch : ReaderModificationCommandBatch
    {
        private readonly List<bool> _resultSetEnd = new List<bool>();

        protected AffectedCountModificationCommandBatch(
            [NotNull] IRelationalCommandBuilderFactory commandBuilderFactory,
            [NotNull] ISqlGenerator sqlGenerator,
            [NotNull] IUpdateSqlGenerator updateSqlGenerator,
            [NotNull] IRelationalValueBufferFactoryFactory valueBufferFactoryFactory)
            : base(commandBuilderFactory, sqlGenerator, updateSqlGenerator, valueBufferFactoryFactory)
        {
        }

        // contains true if the command at the corresponding index is the last command in its result set
        // the last value will not be read
        protected virtual IList<bool> ResultSetEnds => _resultSetEnd;

        public override bool AddCommand(ModificationCommand modificationCommand)
        {
            _resultSetEnd.Add(true);
            var added = base.AddCommand(modificationCommand);
            if (!added)
            {
                _resultSetEnd.RemoveAt(_resultSetEnd.Count - 1);
            }
            return added;
        }

        protected override void Consume(DbDataReader reader)
        {
            Debug.Assert(ResultSetEnds.Count == ModificationCommands.Count);
            var commandIndex = 0;

            try
            {
                var actualResultSetCount = 0;
                do
                {
                    commandIndex = ModificationCommands[commandIndex].RequiresResultPropagation
                        ? ConsumeResultSetWithPropagation(commandIndex, reader)
                        : ConsumeResultSetWithoutPropagation(commandIndex, reader);
                    actualResultSetCount++;
                }
                while (commandIndex < ResultSetEnds.Count
                       && reader.NextResult());

                Debug.Assert(commandIndex == ModificationCommands.Count,
                    "Expected " + ModificationCommands.Count + " results, got " + commandIndex);
#if DEBUG
                var expectedResultSetCount = 1 + ResultSetEnds.Count(e => e);
                expectedResultSetCount += ResultSetEnds[ResultSetEnds.Count - 1] ? -1 : 0;

                Debug.Assert(actualResultSetCount == expectedResultSetCount,
                    "Expected " + expectedResultSetCount + " result sets, got " + actualResultSetCount);
#endif
            }
            catch (DbUpdateException)
            {
                throw;
            }
            catch (Exception ex)
            {
                throw new DbUpdateException(
                    RelationalStrings.UpdateStoreException,
                    ex,
                    ModificationCommands[commandIndex].Entries);
            }
        }

        protected override async Task ConsumeAsync(
            DbDataReader reader,
            CancellationToken cancellationToken = default(CancellationToken))
        {
            Debug.Assert(ResultSetEnds.Count == ModificationCommands.Count);
            var commandIndex = 0;

            try
            {
                var actualResultSetCount = 0;
                do
                {
                    commandIndex = ModificationCommands[commandIndex].RequiresResultPropagation
                        ? await ConsumeResultSetWithPropagationAsync(commandIndex, reader, cancellationToken)
                        : await ConsumeResultSetWithoutPropagationAsync(commandIndex, reader, cancellationToken);
                    actualResultSetCount++;
                }
                while (commandIndex < ResultSetEnds.Count
                       && await reader.NextResultAsync(cancellationToken));

                Debug.Assert(commandIndex == ModificationCommands.Count, "Expected " + ModificationCommands.Count + " results, got " + commandIndex);
#if DEBUG
                var expectedResultSetCount = 1 + ResultSetEnds.Count(e => e);
                expectedResultSetCount += ResultSetEnds[ResultSetEnds.Count - 1] ? -1 : 0;

                Debug.Assert(actualResultSetCount == expectedResultSetCount, "Expected " + expectedResultSetCount + " result sets, got " + actualResultSetCount);
#endif
            }
            catch (DbUpdateException)
            {
                throw;
            }
            catch (Exception ex)
            {
                throw new DbUpdateException(
                    RelationalStrings.UpdateStoreException,
                    ex,
                    ModificationCommands[commandIndex].Entries);
            }
        }

        protected virtual int ConsumeResultSetWithPropagation(int commandIndex, [NotNull] DbDataReader reader)
        {
            var rowsAffected = 0;
            do
            {
                var tableModification = ModificationCommands[commandIndex];
                Debug.Assert(tableModification.RequiresResultPropagation);

                if (!reader.Read())
                {
                    var expectedRowsAffected = rowsAffected + 1;
                    while (++commandIndex < ResultSetEnds.Count
                           && !ResultSetEnds[commandIndex - 1])
                    {
                        expectedRowsAffected++;
                    }

                    ThrowAggregateUpdateConcurrencyException(commandIndex, expectedRowsAffected, rowsAffected);
                }

                var valueBufferFactory = CreateValueBufferFactory(tableModification.ColumnModifications);

                tableModification.PropagateResults(valueBufferFactory.Create(reader));
                rowsAffected++;
            }
            while (++commandIndex < ResultSetEnds.Count
                   && !ResultSetEnds[commandIndex - 1]);

            return commandIndex;
        }

        protected virtual async Task<int> ConsumeResultSetWithPropagationAsync(
            int commandIndex, [NotNull] DbDataReader reader, CancellationToken cancellationToken)
        {
            var rowsAffected = 0;
            do
            {
                var tableModification = ModificationCommands[commandIndex];
                Debug.Assert(tableModification.RequiresResultPropagation);

                if (!await reader.ReadAsync(cancellationToken))
                {
                    var expectedRowsAffected = rowsAffected + 1;
                    while (++commandIndex < ResultSetEnds.Count
                           && !ResultSetEnds[commandIndex - 1])
                    {
                        expectedRowsAffected++;
                    }

                    ThrowAggregateUpdateConcurrencyException(commandIndex, expectedRowsAffected, rowsAffected);
                }

                var valueBufferFactory = CreateValueBufferFactory(tableModification.ColumnModifications);

                tableModification.PropagateResults(valueBufferFactory.Create(reader));
                rowsAffected++;
            }
            while (++commandIndex < ResultSetEnds.Count
                   && !ResultSetEnds[commandIndex - 1]);

            return commandIndex;
        }

        protected virtual int ConsumeResultSetWithoutPropagation(int commandIndex, [NotNull] DbDataReader reader)
        {
            var expectedRowsAffected = 1;
            while (++commandIndex < ResultSetEnds.Count
                   && !ResultSetEnds[commandIndex - 1])
            {
                Debug.Assert(!ModificationCommands[commandIndex].RequiresResultPropagation);

                expectedRowsAffected++;
            }

            if (reader.Read())
            {
                var rowsAffected = reader.GetInt32(0);
                if (rowsAffected != expectedRowsAffected)
                {
                    ThrowAggregateUpdateConcurrencyException(commandIndex, expectedRowsAffected, rowsAffected);
                }
            }
            else
            {
                ThrowAggregateUpdateConcurrencyException(commandIndex, 1, 0);
            }

            return commandIndex;
        }

        protected virtual async Task<int> ConsumeResultSetWithoutPropagationAsync(
            int commandIndex, [NotNull] DbDataReader reader, CancellationToken cancellationToken)
        {
            var expectedRowsAffected = 1;
            while (++commandIndex < ResultSetEnds.Count
                   && !ResultSetEnds[commandIndex - 1])
            {
                Debug.Assert(!ModificationCommands[commandIndex].RequiresResultPropagation);

                expectedRowsAffected++;
            }

            if (await reader.ReadAsync(cancellationToken))
            {
                var rowsAffected = reader.GetInt32(0);
                if (rowsAffected != expectedRowsAffected)
                {
                    ThrowAggregateUpdateConcurrencyException(commandIndex, expectedRowsAffected, rowsAffected);
                }
            }
            else
            {
                ThrowAggregateUpdateConcurrencyException(commandIndex, 1, 0);
            }

            return commandIndex;
        }

        private IReadOnlyList<InternalEntityEntry> AggregateEntries(int endIndex, int commandCount)
        {
            var entries = new List<InternalEntityEntry>();
            for (var i = endIndex - commandCount; i < endIndex; i++)
            {
                entries.AddRange(ModificationCommands[i].Entries);
            }
            return entries;
        }

        protected virtual void ThrowAggregateUpdateConcurrencyException(
            int commandIndex,
            int expectedRowsAffected,
            int rowsAffected)
        {
            throw new DbUpdateConcurrencyException(
                RelationalStrings.UpdateConcurrencyException(expectedRowsAffected, rowsAffected),
                AggregateEntries(commandIndex, expectedRowsAffected));
        }
    }
}
