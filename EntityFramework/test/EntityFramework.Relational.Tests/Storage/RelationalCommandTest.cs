﻿// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Common;
using System.Diagnostics.Tracing;
using System.Threading.Tasks;
using Microsoft.Data.Entity.Infrastructure;
using Microsoft.Data.Entity.Internal;
using Microsoft.Data.Entity.Storage.Internal;
using Microsoft.Data.Entity.Tests.TestUtilities;
using Microsoft.Data.Entity.TestUtilities;
using Microsoft.Data.Entity.TestUtilities.FakeProvider;
using Microsoft.Extensions.Logging;
using Xunit;

namespace Microsoft.Data.Entity.Storage
{
    public class RelationalCommandTest
    {
        [Fact]
        public void Configures_DbCommand()
        {
            var fakeConnection = CreateConnection();

            var relationalCommand = new RelationalCommand(
                new FakeSensitiveDataLogger<RelationalCommand>(),
                new TelemetryListener("Fake"),
                "CommandText",
                new RelationalParameter[0]);

            relationalCommand.ExecuteNonQuery(fakeConnection);

            Assert.Equal(1, fakeConnection.DbConnections.Count);
            Assert.Equal(1, fakeConnection.DbConnections[0].DbCommands.Count);

            var command = fakeConnection.DbConnections[0].DbCommands[0];

            Assert.Equal("CommandText", command.CommandText);
            Assert.Null(command.Transaction);
            Assert.Equal(FakeDbCommand.DefaultCommandTimeout, command.CommandTimeout);
        }

        [Fact]
        public void Configures_DbCommand_with_transaction()
        {
            var fakeConnection = CreateConnection();

            var relationalTransaction = fakeConnection.BeginTransaction();

            var relationalCommand = new RelationalCommand(
                new FakeSensitiveDataLogger<RelationalCommand>(),
                new TelemetryListener("Fake"),
                "CommandText",
                new RelationalParameter[0]);

            relationalCommand.ExecuteNonQuery(fakeConnection);

            Assert.Equal(1, fakeConnection.DbConnections.Count);
            Assert.Equal(1, fakeConnection.DbConnections[0].DbCommands.Count);

            var command = fakeConnection.DbConnections[0].DbCommands[0];

            Assert.Same(relationalTransaction.GetService(), command.Transaction);
        }

        [Fact]
        public void Configures_DbCommand_with_timeout()
        {
            var optionsExtension = new FakeRelationalOptionsExtension
            {
                ConnectionString = ConnectionString,
                CommandTimeout = 42
            };

            var fakeConnection = CreateConnection(CreateOptions(optionsExtension));

            var relationalCommand = new RelationalCommand(
                new FakeSensitiveDataLogger<RelationalCommand>(),
                new TelemetryListener("Fake"),
                "CommandText",
                new RelationalParameter[0]);

            relationalCommand.ExecuteNonQuery(fakeConnection);

            Assert.Equal(1, fakeConnection.DbConnections.Count);
            Assert.Equal(1, fakeConnection.DbConnections[0].DbCommands.Count);

            var command = fakeConnection.DbConnections[0].DbCommands[0];

            Assert.Equal(42, command.CommandTimeout);
        }

        [Fact]
        public void Configures_DbCommand_with_parameters()
        {
            var fakeConnection = CreateConnection();

            var relationalCommand = new RelationalCommand(
                new FakeSensitiveDataLogger<RelationalCommand>(),
                new TelemetryListener("Fake"),
                "CommandText",
                new[]
                {
                    new RelationalParameter("FirstParameter", 17, new RelationalTypeMapping("int", typeof(int), DbType.Int32), false),
                    new RelationalParameter("SecondParameter", 18L,  new RelationalTypeMapping("long", typeof(long), DbType.Int64), true),
                    new RelationalParameter("ThirdParameter", null,  RelationalTypeMapping.NullMapping, null)
                });

            relationalCommand.ExecuteNonQuery(fakeConnection);

            Assert.Equal(1, fakeConnection.DbConnections.Count);
            Assert.Equal(1, fakeConnection.DbConnections[0].DbCommands.Count);
            Assert.Equal(3, fakeConnection.DbConnections[0].DbCommands[0].Parameters.Count);

            var parameter = fakeConnection.DbConnections[0].DbCommands[0].Parameters[0];

            Assert.Equal("FirstParameter", parameter.ParameterName);
            Assert.Equal(17, parameter.Value);
            Assert.Equal(ParameterDirection.Input, parameter.Direction);
            Assert.Equal(false, parameter.IsNullable);
            Assert.Equal(DbType.Int32, parameter.DbType);

            parameter = fakeConnection.DbConnections[0].DbCommands[0].Parameters[1];

            Assert.Equal("SecondParameter", parameter.ParameterName);
            Assert.Equal(18L, parameter.Value);
            Assert.Equal(ParameterDirection.Input, parameter.Direction);
            Assert.Equal(true, parameter.IsNullable);
            Assert.Equal(DbType.Int64, parameter.DbType);

            parameter = fakeConnection.DbConnections[0].DbCommands[0].Parameters[2];

            Assert.Equal("ThirdParameter", parameter.ParameterName);
            Assert.Equal(DBNull.Value, parameter.Value);
            Assert.Equal(ParameterDirection.Input, parameter.Direction);
            Assert.Equal(FakeDbParameter.DefaultDbType, parameter.DbType);
        }

        [Fact]
        public void Can_ExecuteNonQuery()
        {
            var executeNonQueryCount = 0;
            var disposeCount = -1;

            var fakeDbConnection = new FakeDbConnection(
                ConnectionString,
                new FakeCommandExecutor(
                    executeNonQuery: c =>
                    {
                        executeNonQueryCount++;
                        disposeCount = c.DisposeCount;
                        return 1;
                    }));

            var optionsExtension = new FakeRelationalOptionsExtension { Connection = fakeDbConnection };

            var options = CreateOptions(optionsExtension);

            var fakeConnection = new FakeRelationalConnection(options);

            var relationalCommand = new RelationalCommand(
                new FakeSensitiveDataLogger<RelationalCommand>(),
                new TelemetryListener("Fake"),
                "ExecuteNonQuery Command",
                new RelationalParameter[0]);

            relationalCommand.ExecuteNonQuery(fakeConnection);

            // Durring command execution
            Assert.Equal(1, executeNonQueryCount);
            Assert.Equal(0, disposeCount);

            // After command execution
            Assert.Equal(1, fakeDbConnection.DbCommands[0].DisposeCount);
        }

        [Fact]
        public virtual async Task Can_ExecuteNonQueryAsync()
        {
            var executeNonQueryCount = 0;
            var disposeCount = -1;

            var fakeDbConnection = new FakeDbConnection(
                ConnectionString,
                new FakeCommandExecutor(
                    executeNonQueryAsync: (c, ct) =>
                    {
                        executeNonQueryCount++;
                        disposeCount = c.DisposeCount;
                        return Task.FromResult(1);
                    }));

            var optionsExtension = new FakeRelationalOptionsExtension { Connection = fakeDbConnection };

            var options = CreateOptions(optionsExtension);

            var fakeConnection = new FakeRelationalConnection(options);

            var relationalCommand = new RelationalCommand(
                new FakeSensitiveDataLogger<RelationalCommand>(),
                new TelemetryListener("Fake"),
                "ExecuteNonQuery Command",
                new RelationalParameter[0]);

            await relationalCommand.ExecuteNonQueryAsync(fakeConnection);

            // Durring command execution
            Assert.Equal(1, executeNonQueryCount);
            Assert.Equal(0, disposeCount);

            // After command execution
            Assert.Equal(1, fakeDbConnection.DbCommands[0].DisposeCount);
        }

        [Fact]
        public void Can_ExecuteScalar()
        {
            var executeScalarCount = 0;
            var disposeCount = -1;

            var fakeDbConnection = new FakeDbConnection(
                ConnectionString,
                new FakeCommandExecutor(
                    executeScalar: c =>
                    {
                        executeScalarCount++;
                        disposeCount = c.DisposeCount;
                        return "ExecuteScalar Result";
                    }));

            var optionsExtension = new FakeRelationalOptionsExtension { Connection = fakeDbConnection };

            var options = CreateOptions(optionsExtension);

            var fakeConnection = new FakeRelationalConnection(options);

            var relationalCommand = new RelationalCommand(
                new FakeSensitiveDataLogger<RelationalCommand>(),
                new TelemetryListener("Fake"),
                "ExecuteScalar Command",
                new RelationalParameter[0]);

            var result = (string)relationalCommand.ExecuteScalar(fakeConnection);

            Assert.Equal("ExecuteScalar Result", result);

            // Durring command execution
            Assert.Equal(1, executeScalarCount);
            Assert.Equal(0, disposeCount);

            // After command execution
            Assert.Equal(1, fakeDbConnection.DbCommands[0].DisposeCount);
        }

        [Fact]
        public async Task Can_ExecuteScalarAsync()
        {
            var executeScalarCount = 0;
            var disposeCount = -1;

            var fakeDbConnection = new FakeDbConnection(
                ConnectionString,
                new FakeCommandExecutor(
                    executeScalarAsync: (c, ct) =>
                    {
                        executeScalarCount++;
                        disposeCount = c.DisposeCount;
                        return Task.FromResult<object>("ExecuteScalar Result");
                    }));

            var optionsExtension = new FakeRelationalOptionsExtension { Connection = fakeDbConnection };

            var options = CreateOptions(optionsExtension);

            var fakeConnection = new FakeRelationalConnection(options);

            var relationalCommand = new RelationalCommand(
                new FakeSensitiveDataLogger<RelationalCommand>(),
                new TelemetryListener("Fake"),
                "ExecuteScalar Command",
                new RelationalParameter[0]);

            var result = (string)await relationalCommand.ExecuteScalarAsync(fakeConnection);

            Assert.Equal("ExecuteScalar Result", result);

            // Durring command execution
            Assert.Equal(1, executeScalarCount);
            Assert.Equal(0, disposeCount);

            // After command execution
            Assert.Equal(1, fakeDbConnection.DbCommands[0].DisposeCount);
        }

        [Fact]
        public void Can_ExecuteReader()
        {
            var executeReaderCount = 0;
            var disposeCount = -1;

            var dbDataReader = new FakeDbDataReader();

            var fakeDbConnection = new FakeDbConnection(
                ConnectionString,
                new FakeCommandExecutor(
                    executeReader: (c, b) =>
                    {
                        executeReaderCount++;
                        disposeCount = c.DisposeCount;
                        return dbDataReader;
                    }));

            var optionsExtension = new FakeRelationalOptionsExtension { Connection = fakeDbConnection };

            var options = CreateOptions(optionsExtension);

            var fakeConnection = new FakeRelationalConnection(options);

            var relationalCommand = new RelationalCommand(
                new FakeSensitiveDataLogger<RelationalCommand>(),
                new TelemetryListener("Fake"),
                "ExecuteReader Command",
                new RelationalParameter[0]);

            var result = relationalCommand.ExecuteReader(fakeConnection);

            Assert.Same(dbDataReader, result.DbDataReader);

            // Durring command execution
            Assert.Equal(1, executeReaderCount);
            Assert.Equal(0, disposeCount);

            // After command execution
            Assert.Equal(0, dbDataReader.DisposeCount);
            Assert.Equal(0, fakeDbConnection.DbCommands[0].DisposeCount);

            // After reader dispose
            result.Dispose();
            Assert.Equal(1, dbDataReader.DisposeCount);
            Assert.Equal(1, fakeDbConnection.DbCommands[0].DisposeCount);
        }

        [Fact]
        public async Task Can_ExecuteReaderAsync()
        {
            var executeReaderCount = 0;
            var disposeCount = -1;

            var dbDataReader = new FakeDbDataReader();

            var fakeDbConnection = new FakeDbConnection(
                ConnectionString,
                new FakeCommandExecutor(
                    executeReaderAsync: (c, b, ct) =>
                    {
                        executeReaderCount++;
                        disposeCount = c.DisposeCount;
                        return Task.FromResult<DbDataReader>(dbDataReader);
                    }));

            var optionsExtension = new FakeRelationalOptionsExtension { Connection = fakeDbConnection };

            var options = CreateOptions(optionsExtension);

            var fakeConnection = new FakeRelationalConnection(options);

            var relationalCommand = new RelationalCommand(
                new FakeSensitiveDataLogger<RelationalCommand>(),
                new TelemetryListener("Fake"),
                "ExecuteReader Command",
                new RelationalParameter[0]);

            var result = await relationalCommand.ExecuteReaderAsync(fakeConnection);

            Assert.Same(dbDataReader, result.DbDataReader);

            // Durring command execution
            Assert.Equal(1, executeReaderCount);
            Assert.Equal(0, disposeCount);

            // After command execution
            Assert.Equal(0, dbDataReader.DisposeCount);
            Assert.Equal(0, fakeDbConnection.DbCommands[0].DisposeCount);

            // After reader dispose
            result.Dispose();
            Assert.Equal(1, dbDataReader.DisposeCount);
            Assert.Equal(1, fakeDbConnection.DbCommands[0].DisposeCount);
        }

        [Fact]
        public void ExecuteReader_disposes_command_on_exception()
        {
            var fakeDbConnection = new FakeDbConnection(
                ConnectionString,
                new FakeCommandExecutor(
                    executeReader: (c, b) =>
                    {
                        throw new DbUpdateException("ExecuteReader Exception", new InvalidOperationException());
                    }));

            var optionsExtension = new FakeRelationalOptionsExtension { Connection = fakeDbConnection };

            var options = CreateOptions(optionsExtension);

            var fakeConnection = new FakeRelationalConnection(options);

            var relationalCommand = new RelationalCommand(
                new FakeSensitiveDataLogger<RelationalCommand>(),
                new TelemetryListener("Fake"),
                "ExecuteReader Command",
                new RelationalParameter[0]);

            Assert.Throws<DbUpdateException>(() => relationalCommand.ExecuteReader(fakeConnection));
            Assert.Equal(1, fakeDbConnection.DbCommands[0].DisposeCount);
        }

        [Fact]
        public async Task ExecuteReaderAsync_disposes_command_on_exception()
        {
            var fakeDbConnection = new FakeDbConnection(
                ConnectionString,
                new FakeCommandExecutor(
                    executeReaderAsync: (c, b, ct) =>
                    {
                        throw new DbUpdateException("ExecuteReader Exception", new InvalidOperationException());
                    }));

            var optionsExtension = new FakeRelationalOptionsExtension { Connection = fakeDbConnection };

            var options = CreateOptions(optionsExtension);

            var fakeConnection = new FakeRelationalConnection(options);

            var relationalCommand = new RelationalCommand(
                new FakeSensitiveDataLogger<RelationalCommand>(),
                new TelemetryListener("Fake"),
                "ExecuteReader Command",
                new RelationalParameter[0]);

            await Assert.ThrowsAsync<DbUpdateException>(() => relationalCommand.ExecuteReaderAsync(fakeConnection));
            Assert.Equal(1, fakeDbConnection.DbCommands[0].DisposeCount);
        }

        public static TheoryData CommandActions
            => new TheoryData<Delegate, string, bool>
                {
                    {
                        new Action<RelationalCommand, IRelationalConnection>( (command, connection) => command.ExecuteNonQuery(connection)),
                        RelationalTelemetry.ExecuteMethod.ExecuteNonQuery,
                        false
                    },
                    {
                        new Action<RelationalCommand, IRelationalConnection>( (command, connection) => command.ExecuteScalar(connection)),
                        RelationalTelemetry.ExecuteMethod.ExecuteScalar,
                        false
                    },
                    {
                        new Action<RelationalCommand, IRelationalConnection>( (command, connection) => command.ExecuteReader(connection)),
                        RelationalTelemetry.ExecuteMethod.ExecuteReader,
                        false
                    },
                    {
                        new Func<RelationalCommand, IRelationalConnection, Task>( (command, connection) => command.ExecuteNonQueryAsync(connection)),
                        RelationalTelemetry.ExecuteMethod.ExecuteNonQuery,
                        true
                    },
                    {
                        new Func<RelationalCommand, IRelationalConnection, Task>( (command, connection) => command.ExecuteScalarAsync(connection)),
                        RelationalTelemetry.ExecuteMethod.ExecuteScalar,
                        true
                    },
                    {
                        new Func<RelationalCommand, IRelationalConnection, Task>( (command, connection) => command.ExecuteReaderAsync(connection)),
                        RelationalTelemetry.ExecuteMethod.ExecuteReader,
                        true
                    }
                };

        [Theory]
        [MemberData(nameof(CommandActions))]
        public async Task Logs_commands_without_parameter_values(
            Delegate commandDelegate,
            string telemetryName,
            bool async)
        {
            var options = CreateOptions();

            var fakeConnection = new FakeRelationalConnection(options);

            var log = new List<Tuple<LogLevel, string>>();

            var relationalCommand = new RelationalCommand(
                new SensitiveDataLogger<RelationalCommand>(
                    new ListLogger<RelationalCommand>(log),
                    options),
                new TelemetryListener("Fake"),
                "Command Text",
                new[]
                {
                    new RelationalParameter("FirstParameter", 17, new RelationalTypeMapping("int", typeof(int), DbType.Int32), false)
                });

            if (async)
            {
                await ((Func<RelationalCommand, IRelationalConnection, Task>)commandDelegate)(relationalCommand, fakeConnection);
            }
            else
            {
                ((Action<RelationalCommand, IRelationalConnection>)commandDelegate)(relationalCommand, fakeConnection);
            }

            Assert.Equal(1, log.Count);
            Assert.Equal(LogLevel.Information, log[0].Item1);
            Assert.Equal(
                @"Executing DbCommand: [Parameters=[FirstParameter='?'], CommandType='0', CommandTimeout='30']

Command Text
",
                log[0].Item2);
        }

        [Theory]
        [MemberData(nameof(CommandActions))]
        public async Task Logs_commands_parameter_values(
            Delegate commandDelegate,
            string telemetryName,
            bool async)
        {
            var optionsExtension = new FakeRelationalOptionsExtension
            {
                ConnectionString = ConnectionString,
                LogSqlParameterValues = true,
                LogSqlParameterValuesWarned = false
            };

            var options = CreateOptions(optionsExtension);

            var fakeConnection = new FakeRelationalConnection(options);

            var log = new List<Tuple<LogLevel, string>>();

            var relationalCommand = new RelationalCommand(
                new SensitiveDataLogger<RelationalCommand>(
                    new ListLogger<RelationalCommand>(log),
                    options),
                new TelemetryListener("Fake"),
                "Command Text",
                new[]
                {
                    new RelationalParameter("FirstParameter", 17, new RelationalTypeMapping("int", typeof(int), DbType.Int32), false)
                });

            if (async)
            {
                await ((Func<RelationalCommand, IRelationalConnection, Task>)commandDelegate)(relationalCommand, fakeConnection);
            }
            else
            {
                ((Action<RelationalCommand, IRelationalConnection>)commandDelegate)(relationalCommand, fakeConnection);
            }

            Assert.Equal(2, log.Count);
            Assert.Equal(LogLevel.Warning, log[0].Item1);
            Assert.Equal(
@"SQL parameter value logging is enabled. As SQL parameter values may include sensitive application data, this mode should only be enabled during development.",
                log[0].Item2);

            Assert.Equal(LogLevel.Information, log[1].Item1);
            Assert.Equal(
                @"Executing DbCommand: [Parameters=[FirstParameter='17'], CommandType='0', CommandTimeout='30']

Command Text
",
                log[1].Item2);
        }

        [Theory]
        [MemberData(nameof(CommandActions))]
        public async Task Logs_commands_parameter_values_and_warnings(
            Delegate commandDelegate,
            string telemetryName,
            bool async)
        {
            var optionsExtension = new FakeRelationalOptionsExtension
            {
                ConnectionString = ConnectionString,
                LogSqlParameterValues = true
            };

            var options = CreateOptions(optionsExtension);

            var fakeConnection = new FakeRelationalConnection(options);

            var log = new List<Tuple<LogLevel, string>>();

            var relationalCommand = new RelationalCommand(
                new SensitiveDataLogger<RelationalCommand>(
                    new ListLogger<RelationalCommand>(log),
                    options),
                new TelemetryListener("Fake"),
                "Command Text",
                new[]
                {
                    new RelationalParameter("FirstParameter", 17, new RelationalTypeMapping("int", typeof(int), DbType.Int32), false)
                });

            if (async)
            {
                await ((Func<RelationalCommand, IRelationalConnection, Task>)commandDelegate)(relationalCommand, fakeConnection);
            }
            else
            {
                ((Action<RelationalCommand, IRelationalConnection>)commandDelegate)(relationalCommand, fakeConnection);
            }

            Assert.Equal(2, log.Count);
            Assert.Equal(LogLevel.Warning, log[0].Item1);
            Assert.Equal(
@"SQL parameter value logging is enabled. As SQL parameter values may include sensitive application data, this mode should only be enabled during development.",
                log[0].Item2);

            Assert.Equal(LogLevel.Information, log[1].Item1);
            Assert.Equal(
                @"Executing DbCommand: [Parameters=[FirstParameter='17'], CommandType='0', CommandTimeout='30']

Command Text
",
                log[1].Item2);
        }

        [Theory]
        [MemberData(nameof(CommandActions))]
        public async Task Reports_command_telemetry(
            Delegate commandDelegate,
            string telemetryName,
            bool async)
        {
            var options = CreateOptions();

            var fakeConnection = new FakeRelationalConnection(options);

            var telemetry = new List<Tuple<string, object>>();

            var relationalCommand = new RelationalCommand(
                new SensitiveDataLogger<RelationalCommand>(
                    new FakeSensitiveDataLogger<RelationalCommand>(),
                    options),
                new ListTelemetrySource(telemetry),
                "Command Text",
                new[]
                {
                    new RelationalParameter("FirstParameter", 17, new RelationalTypeMapping("int", typeof(int), DbType.Int32), false)
                });

            if (async)
            {
                await ((Func<RelationalCommand, IRelationalConnection, Task>)commandDelegate)(relationalCommand, fakeConnection);
            }
            else
            {
                ((Action<RelationalCommand, IRelationalConnection>)commandDelegate)(relationalCommand, fakeConnection);
            }

            Assert.Equal(2, telemetry.Count);
            Assert.Equal(RelationalTelemetry.BeforeExecuteCommand, telemetry[0].Item1);
            Assert.Equal(RelationalTelemetry.AfterExecuteCommand, telemetry[1].Item1);

            dynamic beforeData = telemetry[0].Item2;
            dynamic afterData = telemetry[1].Item2;

            Assert.Equal(fakeConnection.DbConnections[0].DbCommands[0], beforeData.Command);
            Assert.Equal(fakeConnection.DbConnections[0].DbCommands[0], afterData.Command);

            Assert.Equal(telemetryName, beforeData.ExecuteMethod);
            Assert.Equal(telemetryName, afterData.ExecuteMethod);

            Assert.Equal(async, beforeData.IsAsync);
            Assert.Equal(async, afterData.IsAsync);
        }

        [Theory]
        [MemberData(nameof(CommandActions))]
        public async Task Reports_command_telemetry_on_exception(
            Delegate commandDelegate,
            string telemetryName,
            bool async)
        {
            var exception = new InvalidOperationException();

            var fakeDbConnection = new FakeDbConnection(
                ConnectionString,
                new FakeCommandExecutor(
                    (c) => { throw exception; },
                    (c) => { throw exception; },
                    (c, cb) => { throw exception; },
                    (c, ct) => { throw exception; },
                    (c, ct) => { throw exception; },
                    (c, cb, ct) => { throw exception; }));

            var optionsExtension = new FakeRelationalOptionsExtension { Connection = fakeDbConnection };

            var options = CreateOptions(optionsExtension);

            var fakeConnection = new FakeRelationalConnection(options);

            var telemetry = new List<Tuple<string, object>>();

            var relationalCommand = new RelationalCommand(
                new SensitiveDataLogger<RelationalCommand>(
                    new FakeSensitiveDataLogger<RelationalCommand>(),
                    options),
                new ListTelemetrySource(telemetry),
                "Command Text",
                new[]
                {
                    new RelationalParameter("FirstParameter", 17, new RelationalTypeMapping("int", typeof(int), DbType.Int32), false)
                });

            if (async)
            {
                await Assert.ThrowsAsync<InvalidOperationException>(
                    async ()
                        => await ((Func<RelationalCommand, IRelationalConnection, Task>)commandDelegate)(relationalCommand, fakeConnection));
            }
            else
            {
                Assert.Throws<InvalidOperationException>(()
                    => ((Action<RelationalCommand, IRelationalConnection>)commandDelegate)(relationalCommand, fakeConnection));
            }

            Assert.Equal(2, telemetry.Count);
            Assert.Equal(RelationalTelemetry.BeforeExecuteCommand, telemetry[0].Item1);
            Assert.Equal(RelationalTelemetry.CommandExecutionError, telemetry[1].Item1);

            dynamic beforeData = telemetry[0].Item2;
            dynamic afterData = telemetry[1].Item2;

            Assert.Equal(fakeDbConnection.DbCommands[0], beforeData.Command);
            Assert.Equal(fakeDbConnection.DbCommands[0], afterData.Command);

            Assert.Equal(telemetryName, beforeData.ExecuteMethod);
            Assert.Equal(telemetryName, afterData.ExecuteMethod);

            Assert.Equal(async, beforeData.IsAsync);
            Assert.Equal(async, afterData.IsAsync);

            Assert.Equal(exception, afterData.Exception);
        }

        private const string ConnectionString = "Fake Connection String";

        private static FakeRelationalConnection CreateConnection(IDbContextOptions options = null)
            => new FakeRelationalConnection(options ?? CreateOptions());

        public static IDbContextOptions CreateOptions(FakeRelationalOptionsExtension optionsExtension = null)
        {
            var optionsBuilder = new DbContextOptionsBuilder();

            ((IDbContextOptionsBuilderInfrastructure)optionsBuilder)
                .AddOrUpdateExtension(optionsExtension ?? new FakeRelationalOptionsExtension { ConnectionString = ConnectionString });

            return optionsBuilder.Options;
        }
    }
}
