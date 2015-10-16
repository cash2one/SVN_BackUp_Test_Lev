// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Text;
using Microsoft.Data.Entity.Storage;
using Microsoft.Data.Entity.Update;

namespace Microsoft.Data.Entity.Tests
{
    public class UpdateSqlGeneratorTest : UpdateSqlGeneratorTestBase
    {
        protected override IUpdateSqlGenerator CreateSqlGenerator()
        {
            return new ConcreteSqlGenerator();
        }

        protected override string RowsAffected
        {
            get { return "provider_specific_rowcount()"; }
        }

        protected override string Identity
        {
            get { return "provider_specific_identity()"; }
        }

        private class ConcreteSqlGenerator : UpdateSqlGenerator
        {
            public ConcreteSqlGenerator()
                :base(new RelationalSqlGenerator())
            {
            }

            protected override void AppendIdentityWhereCondition(StringBuilder commandStringBuilder, ColumnModification columnModification)
            {
                commandStringBuilder
                    .Append(SqlGenerator.DelimitIdentifier(columnModification.ColumnName))
                    .Append(" = ")
                    .Append("provider_specific_identity()");
            }

            protected override void AppendSelectAffectedCountCommand(StringBuilder commandStringBuilder, string name, string schema)
            {
                commandStringBuilder
                    .Append("SELECT provider_specific_rowcount();" + Environment.NewLine);
            }

            protected override void AppendRowsAffectedWhereCondition(StringBuilder commandStringBuilder, int expectedRowsAffected)
            {
                commandStringBuilder
                    .Append("provider_specific_rowcount() = " + expectedRowsAffected);
            }
        }
    }
}
