// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System.Data.Common;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Infrastructure;
using Microsoft.Data.Entity.Infrastructure.Internal;
using Microsoft.Data.Entity.Utilities;
using Microsoft.Data.Sqlite;
using Microsoft.Extensions.Logging;

namespace Microsoft.Data.Entity.Storage.Internal
{
    public class SqliteRelationalConnection : RelationalConnection
    {
        private readonly ISqlCommandBuilder _sqlCommandBuilder;
        private readonly bool _enforceForeignKeys = true;
        private int _openedCount;

        public SqliteRelationalConnection(
            [NotNull] ISqlCommandBuilder sqlCommandBuilder,
            [NotNull] IDbContextOptions options,
            // ReSharper disable once SuggestBaseTypeForParameter
            [NotNull] ILogger<SqliteRelationalConnection> logger)
            : base(options, logger)
        {
            Check.NotNull(sqlCommandBuilder, nameof(sqlCommandBuilder));

            _sqlCommandBuilder = sqlCommandBuilder;

            var optionsExtension = options.Extensions.OfType<SqliteOptionsExtension>().FirstOrDefault();
            if (optionsExtension != null)
            {
                _enforceForeignKeys = optionsExtension.EnforceForeignKeys;
            }
        }

        protected override DbConnection CreateDbConnection() => new SqliteConnection(ConnectionString);

        public override bool IsMultipleActiveResultSetsEnabled => true;

        public override void Open()
        {
            base.Open();

            _openedCount++;

            if (_openedCount == 1)
            {
                EnableForeignKeys();
            }
        }

        public override async Task OpenAsync(CancellationToken cancellationToken = default(CancellationToken))
        {
            await base.OpenAsync(cancellationToken);

            _openedCount++;

            if (_openedCount == 1)
            {
                EnableForeignKeys();
            }
        }

        public override void Close()
        {
            base.Close();

            _openedCount--;
        }

        private void EnableForeignKeys()
        {
            if (!_enforceForeignKeys)
            {
                return;
            }

            _sqlCommandBuilder.Build("PRAGMA foreign_keys=ON;").ExecuteNonQuery(this);
        }
    }
}
