// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Data.Common;
using System.Globalization;
using System.Linq;
using JetBrains.Annotations;
using Microsoft.Data.Entity.Infrastructure;
using Microsoft.Data.Entity.Internal;
using Microsoft.Data.Entity.Utilities;
using Microsoft.Extensions.Logging;

// ReSharper disable once CheckNamespace

namespace Microsoft.Data.Entity.Storage
{
    internal static class RelationalLoggerExtensions
    {
        public static void LogCommand([NotNull] this ISensitiveDataLogger logger, [NotNull] DbCommand command)
        {
            Check.NotNull(logger, nameof(logger));
            Check.NotNull(command, nameof(command));

            logger.LogInformation(
                RelationalLoggingEventId.ExecutingCommand,
                () =>
                    {
                        var logParameterValues
                            = command.Parameters.Count > 0
                              && logger.LogSensitiveData;

                        return new DbCommandLogData(
                            command.CommandText.TrimEnd(),
                            command.CommandType,
                            command.CommandTimeout,
                            command.Parameters
                                .Cast<DbParameter>()
                                .ToDictionary(p => p.ParameterName, p => logParameterValues ? p.Value : "?"));
                    },
                state =>
                    RelationalStrings.RelationalLoggerExecutingCommand(
                        state.Parameters
                            .Select(kv => $"{kv.Key}='{Convert.ToString(kv.Value, CultureInfo.InvariantCulture)}'")
                            .Join(),
                        state.CommandType,
                        state.CommandTimeout,
                        Environment.NewLine,
                        state.CommandText));
        }

        private static void LogInformation<TState>(
            this ILogger logger, RelationalLoggingEventId eventId, Func<TState> state, Func<TState, string> formatter)
        {
            if (logger.IsEnabled(LogLevel.Information))
            {
                logger.Log(LogLevel.Information, (int)eventId, state(), null, (s, _) => formatter((TState)s));
            }
        }

        public static void LogVerbose(
            this ILogger logger, RelationalLoggingEventId eventId, Func<string> formatter)
        {
            if (logger.IsEnabled(LogLevel.Verbose))
            {
                logger.Log(LogLevel.Verbose, (int)eventId, null, null, (_, __) => formatter());
            }
        }

        public static void LogVerbose<TState>(
            this ILogger logger, RelationalLoggingEventId eventId, TState state, Func<TState, string> formatter)
        {
            if (logger.IsEnabled(LogLevel.Verbose))
            {
                logger.Log(LogLevel.Verbose, (int)eventId, state, null, (s, __) => formatter((TState)s));
            }
        }
    }
}
