// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

using System;
using System.Linq;
using Microsoft.Data.Entity.FunctionalTests.TestModels.ComplexNavigationsModel;
using Xunit;
using System.Collections.Generic;

namespace Microsoft.Data.Entity.FunctionalTests
{
    public abstract class ComplexNavigationsQueryTestBase<TTestStore, TFixture> : IClassFixture<TFixture>, IDisposable
        where TTestStore : TestStore
        where TFixture : ComplexNavigationsQueryFixtureBase<TTestStore>, new()
    {
        protected ComplexNavigationsContext CreateContext()
        {
            return Fixture.CreateContext(TestStore);
        }

        protected ComplexNavigationsQueryTestBase(TFixture fixture)
        {
            Fixture = fixture;

            TestStore = Fixture.CreateTestStore();
        }

        protected TFixture Fixture { get; }

        protected TTestStore TestStore { get; }

        protected virtual void ClearLog()
        {
        }

        public void Dispose()
        {
            TestStore.Dispose();
        }

        [Fact]
        public virtual void Data_reader_is_closed_correct_number_of_times_for_include_queries_on_optional_navigations()
        {
            using (var context = CreateContext())
            {
                // reader for the last include is not opened because there is no data one level below - we should only try to close connection as many times as we tried to open it
                // if we close it too many times, consecutive query will not work
                // see issue #1457 for more details
                context.LevelOne.Include(e => e.OneToMany_Optional).ThenInclude(e => e.OneToMany_Optional).ThenInclude(e => e.OneToMany_Optional_Inverse.OneToMany_Optional).ToList();

                context.LevelOne.ToList();
            }
        }

        [Fact]
        public virtual void Multi_level_include_one_to_many_optional_and_one_to_many_optional_produces_valid_sql()
        {
            using (var context = CreateContext())
            {
                var result = context.LevelOne.Include(e => e.OneToMany_Optional).ThenInclude(e => e.OneToMany_Optional).ToList();

                Assert.Equal(10, result.Count);

                var level1 = result.Single(e => e.Name == "L1 01");

                Assert.Equal(5, level1.OneToMany_Optional.Count);
                Assert.True(level1.OneToMany_Optional.Select(e => e.Name).Contains("L2 02"));
                Assert.True(level1.OneToMany_Optional.Select(e => e.Name).Contains("L2 04"));
                Assert.True(level1.OneToMany_Optional.Select(e => e.Name).Contains("L2 06"));
                Assert.True(level1.OneToMany_Optional.Select(e => e.Name).Contains("L2 08"));
                Assert.True(level1.OneToMany_Optional.Select(e => e.Name).Contains("L2 10"));

                var level2 = level1.OneToMany_Optional.Single(e => e.Name == "L2 02");

                Assert.Equal(2, level2.OneToMany_Optional.Count);
                Assert.True(level2.OneToMany_Optional.Select(e => e.Name).Contains("L3 04"));
                Assert.True(level2.OneToMany_Optional.Select(e => e.Name).Contains("L3 08"));
            }
        }

        [Fact]
        public virtual void Multi_level_include_correct_PK_is_chosen_as_the_join_predicate_for_queries_that_join_same_table_multiple_times()
        {
            using (var context = CreateContext())
            {
                var result = context.LevelOne.Include(e => e.OneToMany_Optional).ThenInclude(e => e.OneToMany_Optional).ThenInclude(e => e.OneToMany_Required_Inverse.OneToMany_Optional).ToList();

                Assert.Equal(10, result.Count);

                var level1 = result.Single(e => e.Name == "L1 01");

                Assert.Equal(5, level1.OneToMany_Optional.Count);
                Assert.True(level1.OneToMany_Optional.Select(e => e.Name).Contains("L2 02"));
                Assert.True(level1.OneToMany_Optional.Select(e => e.Name).Contains("L2 04"));
                Assert.True(level1.OneToMany_Optional.Select(e => e.Name).Contains("L2 06"));
                Assert.True(level1.OneToMany_Optional.Select(e => e.Name).Contains("L2 08"));
                Assert.True(level1.OneToMany_Optional.Select(e => e.Name).Contains("L2 10"));

                var level2 = level1.OneToMany_Optional.Single(e => e.Name == "L2 02");

                Assert.Equal(2, level2.OneToMany_Optional.Count);
                Assert.True(level2.OneToMany_Optional.Select(e => e.Name).Contains("L3 04"));
                Assert.True(level2.OneToMany_Optional.Select(e => e.Name).Contains("L3 08"));

                Assert.True(level2.OneToMany_Optional.Select(e => e.OneToMany_Required_Inverse).All(e => e.Name == "L2 01"));

                var level2Reverse = level2.OneToMany_Optional.Select(e => e.OneToMany_Required_Inverse).First();

                Assert.Equal(3, level2Reverse.OneToMany_Optional.Count);
                Assert.True(level2Reverse.OneToMany_Optional.Select(e => e.Name).Contains("L3 02"));
                Assert.True(level2Reverse.OneToMany_Optional.Select(e => e.Name).Contains("L3 06"));
                Assert.True(level2Reverse.OneToMany_Optional.Select(e => e.Name).Contains("L3 10"));
            }
        }

        [Fact]
        public virtual void Multi_level_include_reads_key_values_from_data_reader_rather_than_incorrect_reader_deep_into_the_stack()
        {
            using (var context = CreateContext())
            {
                // #1433
                context.LevelOne.Include(e => e.OneToMany_Optional).ToList();
                context.LevelOne.Include(e => e.OneToMany_Optional_Self).ToList();

                //# 1478
                context.LevelOne
                    .Include(e => e.OneToMany_Optional)
                    .ThenInclude(e => e.OneToMany_Optional_Inverse.OneToMany_Optional_Self_Inverse.OneToOne_Optional_FK).ToList();

                context.LevelOne
                    .Include(e => e.OneToMany_Optional)
                    .ThenInclude(e => e.OneToMany_Optional_Inverse.OneToMany_Optional_Self_Inverse.OneToOne_Optional_PK).ToList();

                // #1487
                context.LevelOne
                    .Include(e => e.OneToMany_Optional)
                    .ThenInclude(e => e.OneToMany_Optional_Inverse.OneToOne_Optional_PK.OneToOne_Optional_FK).ToList();

                context.LevelOne
                    .Include(e => e.OneToMany_Optional)
                    .ThenInclude(e => e.OneToMany_Optional_Inverse.OneToOne_Optional_PK.OneToOne_Optional_FK_Inverse).ToList();

                // #1488
                context.LevelOne
                    .Include(e => e.OneToMany_Optional)
                    .ThenInclude(e => e.OneToMany_Optional_Inverse.OneToOne_Optional_PK.OneToOne_Required_FK).ToList();

                context.LevelOne
                    .Include(e => e.OneToMany_Optional)
                    .ThenInclude(e => e.OneToMany_Optional_Inverse.OneToOne_Optional_PK.OneToOne_Required_FK_Inverse).ToList();
            }
        }

        [Fact]
        public virtual void Multi_level_include_with_short_circuiting()
        {
            var fieldLabels = new Dictionary<string, string>();
            var fieldPlaceholders = new Dictionary<string, string>();
            var stringGlobalizations = new Dictionary<string, List<string>>();
            var globalizationLanguages = new Dictionary<string, string>();

            using (var context = CreateContext())
            {
                fieldLabels = context.Fields
                    .Include(f => f.Label)
                    .ToDictionary(f => f.Name, f => f.Label?.DefaultText);

                fieldPlaceholders = context.Fields
                    .Include(f => f.Placeholder)
                    .ToDictionary(f => f.Name, f => f.Placeholder?.DefaultText);

                stringGlobalizations = context.MultilingualStrings
                    .Include(s => s.Globalizations)
                    .ToDictionary(
                        s => s.DefaultText,
                        s => s.Globalizations != null
                            ? s.Globalizations.Select(g => g.Text).ToList()
                            : new List<string>());

                globalizationLanguages = context.Globalizations
                    .Include(g => g.Language)
                    .ToDictionary(g => g.Text, g => g.Language?.Name);
            }

            ClearLog();

            using (var context = CreateContext())
            {
                var query = context.Fields
                   .Include(x => x.Label.Globalizations)
                   .ThenInclude(x => x.Language)
                   .Include(x => x.Placeholder.Globalizations)
                   .ThenInclude(x => x.Language);

                var result = query.ToList();

                var expectedFieldCount = 2;
                Assert.Equal(expectedFieldCount, result.Count);
                Assert.Equal("Field1", result[0].Name);
                Assert.Equal("Field2", result[1].Name);

                for (int i = 0; i < expectedFieldCount; i++)
                {
                    Assert.Equal(fieldLabels[result[i]?.Name], result[i].Label?.DefaultText);
                    Assert.Equal(fieldPlaceholders[result[i]?.Name], result[i].Placeholder?.DefaultText);

                    var label = result[i].Label;
                    if (label != null)
                    {
                        Assert.Equal(stringGlobalizations[label.DefaultText].Count, label.Globalizations.Count);
                        foreach (var globalization in label.Globalizations)
                        {
                            Assert.True(stringGlobalizations[label.DefaultText].Contains(globalization.Text));
                            Assert.Equal(globalizationLanguages[globalization.Text], globalization.Language?.Name);
                        }
                    }

                    var placeholder = result[i].Placeholder;
                    if (placeholder != null)
                    {
                        Assert.Equal(stringGlobalizations[placeholder.DefaultText].Count, placeholder.Globalizations.Count);
                        foreach (var globalization in placeholder.Globalizations)
                        {
                            Assert.True(stringGlobalizations[placeholder.DefaultText].Contains(globalization.Text));
                            Assert.Equal(globalizationLanguages[globalization.Text], globalization.Language?.Name);
                        }
                    }
                }
            }
        }

        [Fact]
        public virtual void Join_navigation_translated_to_FK()
        {
            List<Level1> levelOnes;
            List<Level2> levelTwos;
            using (var context = CreateContext())
            {
                levelOnes = context.LevelOne.ToList();
                levelTwos = context.LevelTwo.Include(e => e.OneToOne_Optional_PK_Inverse).ToList();
            }

            ClearLog();

            using (var context = CreateContext())
            {
                var query = from e1 in context.LevelOne
                            join e2 in context.LevelTwo on e1.Id equals e2.OneToOne_Optional_PK_Inverse.Id
                            select new { Id1 = e1.Id, Id2 = e2.Id };

                var result = query.ToList();

                var expected = (from l1 in levelOnes
                                join l2 in levelTwos on l1.Id equals l2.OneToOne_Optional_PK_Inverse?.Id
                                select new { Id1 = l1.Id, Id2 = l2.Id }).ToList();

                Assert.Equal(expected.Count, result.Count);
                foreach (var resultItem in result)
                {
                    Assert.True(expected.Contains(resultItem));
                }
            }
        }

        [Fact]
        public virtual void Join_navigation_in_outer_selector_translated_to_extra_join()
        {
            List<Level1> levelOnes;
            List<Level2> levelTwos;
            using (var context = CreateContext())
            {
                levelOnes = context.LevelOne.Include(e => e.OneToOne_Optional_FK).ToList();
                levelTwos = context.LevelTwo.ToList();
            }

            ClearLog();

            using (var context = CreateContext())
            {
                var query = from e1 in context.LevelOne
                            join e2 in context.LevelTwo on e1.OneToOne_Optional_FK.Id equals e2.Id
                            select new { Id1 = e1.Id, Id2 = e2.Id };

                var result = query.ToList();

                var expected = (from e1 in levelOnes
                                join e2 in levelTwos on e1.OneToOne_Optional_FK?.Id equals e2.Id
                                select new { Id1 = e1.Id, Id2 = e2.Id }).ToList();

                Assert.Equal(expected.Count, result.Count);
                foreach (var resultItem in result)
                {
                    Assert.True(expected.Contains(resultItem));
                }
            }
        }

        [Fact]
        public virtual void Join_navigation_in_outer_selector_translated_to_extra_join_nested()
        {
            List<Level1> levelOnes;
            List<Level3> levelThrees;
            using (var context = CreateContext())
            {
                levelOnes = context.LevelOne.Include(e => e.OneToOne_Required_FK.OneToOne_Optional_FK).ToList();
                levelThrees = context.LevelThree.ToList();
            }

            ClearLog();

            using (var context = CreateContext())
            {
                var query = from e1 in context.LevelOne
                            join e3 in context.LevelThree on e1.OneToOne_Required_FK.OneToOne_Optional_FK.Id equals e3.Id
                            select new { Id1 = e1.Id, Id3 = e3.Id };

                var result = query.ToList();

                var expected = (from e1 in levelOnes
                                join e3 in levelThrees on e1.OneToOne_Required_FK?.OneToOne_Optional_FK?.Id equals e3.Id
                                select new { Id1 = e1.Id, Id3 = e3.Id }).ToList();

                Assert.Equal(expected.Count, result.Count);
                foreach (var resultItem in result)
                {
                    Assert.True(expected.Contains(resultItem));
                }
            }
        }

        [Fact]
        public virtual void Join_navigation_in_inner_selector_translated_to_subquery()
        {
            List<Level1> levelOnes;
            List<Level2> levelTwos;
            using (var context = CreateContext())
            {
                levelOnes = context.LevelOne.Include(e => e.OneToOne_Optional_FK).ToList();
                levelTwos = context.LevelTwo.ToList();
            }

            ClearLog();

            using (var context = CreateContext())
            {
                var query = from e2 in context.LevelTwo
                            join e1 in context.LevelOne on e2.Id equals e1.OneToOne_Optional_FK.Id
                            select new { Id2 = e2.Id, Id1 = e1.Id };

                var result = query.ToList();

                var expected = (from e2 in levelTwos
                                join e1 in levelOnes on e2.Id equals e1.OneToOne_Optional_FK?.Id
                                select new { Id2 = e2.Id, Id1 = e1.Id }).ToList();

                Assert.Equal(expected.Count, result.Count);
                foreach (var resultItem in result)
                {
                    Assert.True(expected.Contains(resultItem));
                }
            }
        }

        [Fact]
        public virtual void Join_navigation_translated_to_subquery_non_key_join()
        {
            List<Level1> levelOnes;
            List<Level2> levelTwos;
            using (var context = CreateContext())
            {
                levelOnes = context.LevelOne.Include(e => e.OneToOne_Optional_FK).ToList();
                levelTwos = context.LevelTwo.ToList();
            }

            ClearLog();

            using (var context = CreateContext())
            {
                var query = from e2 in context.LevelTwo
                            join e1 in context.LevelOne on e2.Name equals e1.OneToOne_Optional_FK.Name
                            select new { Id2 = e2.Id, Name2 = e2.Name, Id1 = e1.Id, Name1 = e1.Name };

                var result = query.ToList();

                var expected = (from e2 in levelTwos
                                join e1 in levelOnes on e2.Name equals e1.OneToOne_Optional_FK?.Name
                                select new { Id2 = e2.Id, Name2 = e2.Name, Id1 = e1.Id, Name1 = e1.Name }).ToList();

                Assert.Equal(expected.Count, result.Count);
                foreach (var resultItem in result)
                {
                    Assert.True(expected.Contains(resultItem));
                }
            }
        }

        [Fact]
        public virtual void Join_navigation_translated_to_subquery_self_ref()
        {
            List<Level1> levelOnes1;
            List<Level1> levelOnes2;
            using (var context = CreateContext())
            {
                levelOnes1 = context.LevelOne.ToList();
                levelOnes2 = context.LevelOne.Include(e => e.OneToMany_Optional_Self_Inverse).ToList();
            }

            ClearLog();

            using (var context = CreateContext())
            {
                var query = from e1 in context.LevelOne
                            join e2 in context.LevelOne on e1.Id equals e2.OneToMany_Optional_Self_Inverse.Id
                            select new { Id1 = e1.Id, Id2 = e2.Id };

                var result = query.ToList();

                var expected = (from e1 in levelOnes1
                                join e2 in levelOnes2 on e1.Id equals e2.OneToMany_Optional_Self_Inverse?.Id
                                select new { Id1 = e1.Id, Id2 = e2.Id }).ToList();

                Assert.Equal(expected.Count, result.Count);
                foreach (var resultItem in result)
                {
                    Assert.True(expected.Contains(resultItem));
                }
            }
        }

        [Fact]
        public virtual void Join_navigation_translated_to_subquery_nested()
        {
            List<Level1> levelOnes;
            List<Level3> levelThrees;
            using (var context = CreateContext())
            {
                levelOnes = context.LevelOne.Include(e => e.OneToOne_Required_FK.OneToOne_Optional_FK).ToList();
                levelThrees = context.LevelThree.ToList();
            }

            ClearLog();

            using (var context = CreateContext())
            {
                var query = from e3 in context.LevelThree
                            join e1 in context.LevelOne on e3.Id equals e1.OneToOne_Required_FK.OneToOne_Optional_FK.Id
                            select new { Id3 = e3.Id, Id1 = e1.Id };

                var result = query.ToList();

                var expected = (from e3 in levelThrees
                                join e1 in levelOnes on e3.Id equals e1?.OneToOne_Required_FK.OneToOne_Optional_FK?.Id
                                select new { Id3 = e3.Id, Id1 = e1.Id }).ToList();

                Assert.Equal(expected.Count, result.Count);
                foreach (var resultItem in result)
                {
                    Assert.True(expected.Contains(resultItem));
                }
            }
        }

        [Fact]
        public virtual void Join_navigation_translated_to_subquery_deeply_nested_non_key_join()
        {
            List<Level1> levelOnes;
            List<Level4> levelFours;
            using (var context = CreateContext())
            {
                levelOnes = context.LevelOne.Include(e => e.OneToOne_Required_FK.OneToOne_Optional_FK.OneToOne_Required_PK).ToList();
                levelFours = context.LevelFour.ToList();
            }

            ClearLog();

            using (var context = CreateContext())
            {
                var query = from e4 in context.LevelFour
                            join e1 in context.LevelOne on e4.Name equals e1.OneToOne_Required_FK.OneToOne_Optional_FK.OneToOne_Required_PK.Name
                            select new { Id4 = e4.Id, Name4 = e4.Name, Id1 = e1.Id, Name1 = e1.Name };

                var result = query.ToList();

                var expected = (from e4 in levelFours
                                join e1 in levelOnes on e4.Name equals e1?.OneToOne_Required_FK.OneToOne_Optional_FK?.OneToOne_Required_PK?.Name
                                select new { Id4 = e4.Id, Name4 = e4.Name, Id1 = e1.Id, Name1 = e1.Name }).ToList();

                Assert.Equal(expected.Count, result.Count);
                foreach (var resultItem in result)
                {
                    Assert.True(expected.Contains(resultItem));
                }
            }
        }
    }
}
