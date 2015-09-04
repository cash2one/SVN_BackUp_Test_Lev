# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import codecs
import importlib
import os

from django.apps import apps
from django.core.management import CommandError, call_command
from django.db import DatabaseError, connection, models
from django.db.migrations.recorder import MigrationRecorder
from django.test import ignore_warnings, mock, override_settings
from django.utils import six
from django.utils.deprecation import RemovedInDjango110Warning
from django.utils.encoding import force_text

from .models import UnicodeModel, UnserializableModel
from .test_base import MigrationTestBase


class MigrateTests(MigrationTestBase):
    """
    Tests running the migrate command.
    """

    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations"})
    def test_migrate(self):
        """
        Tests basic usage of the migrate command.
        """
        # Make sure no tables are created
        self.assertTableNotExists("migrations_author")
        self.assertTableNotExists("migrations_tribble")
        self.assertTableNotExists("migrations_book")
        # Run the migrations to 0001 only
        call_command("migrate", "migrations", "0001", verbosity=0)
        # Make sure the right tables exist
        self.assertTableExists("migrations_author")
        self.assertTableExists("migrations_tribble")
        self.assertTableNotExists("migrations_book")
        # Run migrations all the way
        call_command("migrate", verbosity=0)
        # Make sure the right tables exist
        self.assertTableExists("migrations_author")
        self.assertTableNotExists("migrations_tribble")
        self.assertTableExists("migrations_book")
        # Unmigrate everything
        call_command("migrate", "migrations", "zero", verbosity=0)
        # Make sure it's all gone
        self.assertTableNotExists("migrations_author")
        self.assertTableNotExists("migrations_tribble")
        self.assertTableNotExists("migrations_book")

    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations_initial_false"})
    def test_migrate_initial_false(self):
        """
        `Migration.initial = False` skips fake-initial detection.
        """
        # Make sure no tables are created
        self.assertTableNotExists("migrations_author")
        self.assertTableNotExists("migrations_tribble")
        # Run the migrations to 0001 only
        call_command("migrate", "migrations", "0001", verbosity=0)
        # Fake rollback
        call_command("migrate", "migrations", "zero", fake=True, verbosity=0)
        # Make sure fake-initial detection does not run
        with self.assertRaises(DatabaseError):
            call_command("migrate", "migrations", "0001", fake_initial=True, verbosity=0)

        call_command("migrate", "migrations", "0001", fake=True, verbosity=0)
        # Real rollback
        call_command("migrate", "migrations", "zero", verbosity=0)
        # Make sure it's all gone
        self.assertTableNotExists("migrations_author")
        self.assertTableNotExists("migrations_tribble")
        self.assertTableNotExists("migrations_book")

    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations"})
    def test_migrate_fake_initial(self):
        """
        #24184 - Tests that --fake-initial only works if all tables created in
        the initial migration of an app exists
        """
        # Make sure no tables are created
        self.assertTableNotExists("migrations_author")
        self.assertTableNotExists("migrations_tribble")
        # Run the migrations to 0001 only
        call_command("migrate", "migrations", "0001", verbosity=0)
        # Make sure the right tables exist
        self.assertTableExists("migrations_author")
        self.assertTableExists("migrations_tribble")
        # Fake a roll-back
        call_command("migrate", "migrations", "zero", fake=True, verbosity=0)
        # Make sure the tables still exist
        self.assertTableExists("migrations_author")
        self.assertTableExists("migrations_tribble")
        # Try to run initial migration
        with self.assertRaises(DatabaseError):
            call_command("migrate", "migrations", "0001", verbosity=0)
        # Run initial migration with an explicit --fake-initial
        out = six.StringIO()
        with mock.patch('django.core.management.color.supports_color', lambda *args: False):
            call_command("migrate", "migrations", "0001", fake_initial=True, stdout=out, verbosity=1)
        self.assertIn(
            "migrations.0001_initial... faked",
            out.getvalue().lower()
        )
        # Run migrations all the way
        call_command("migrate", verbosity=0)
        # Make sure the right tables exist
        self.assertTableExists("migrations_author")
        self.assertTableNotExists("migrations_tribble")
        self.assertTableExists("migrations_book")
        # Fake a roll-back
        call_command("migrate", "migrations", "zero", fake=True, verbosity=0)
        # Make sure the tables still exist
        self.assertTableExists("migrations_author")
        self.assertTableNotExists("migrations_tribble")
        self.assertTableExists("migrations_book")
        # Try to run initial migration
        with self.assertRaises(DatabaseError):
            call_command("migrate", "migrations", verbosity=0)
        # Run initial migration with an explicit --fake-initial
        with self.assertRaises(DatabaseError):
            # Fails because "migrations_tribble" does not exist but needs to in
            # order to make --fake-initial work.
            call_command("migrate", "migrations", fake_initial=True, verbosity=0)
        # Fake a apply
        call_command("migrate", "migrations", fake=True, verbosity=0)
        # Unmigrate everything
        call_command("migrate", "migrations", "zero", verbosity=0)
        # Make sure it's all gone
        self.assertTableNotExists("migrations_author")
        self.assertTableNotExists("migrations_tribble")
        self.assertTableNotExists("migrations_book")

    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations_fake_split_initial"})
    def test_migrate_fake_split_initial(self):
        """
        Split initial migrations can be faked with --fake-initial.
        """
        call_command("migrate", "migrations", "0002", verbosity=0)
        call_command("migrate", "migrations", "zero", fake=True, verbosity=0)
        out = six.StringIO()
        with mock.patch('django.core.management.color.supports_color', lambda *args: False):
            call_command("migrate", "migrations", "0002", fake_initial=True, stdout=out, verbosity=1)
        value = out.getvalue().lower()
        self.assertIn("migrations.0001_initial... faked", value)
        self.assertIn("migrations.0002_second... faked", value)
        # Fake an apply
        call_command("migrate", "migrations", fake=True, verbosity=0)
        # Unmigrate everything
        call_command("migrate", "migrations", "zero", verbosity=0)

    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations_conflict"})
    def test_migrate_conflict_exit(self):
        """
        Makes sure that migrate exits if it detects a conflict.
        """
        with self.assertRaisesMessage(CommandError, "Conflicting migrations detected"):
            call_command("migrate", "migrations")

    @ignore_warnings(category=RemovedInDjango110Warning)
    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations"})
    def test_migrate_list(self):
        """
        Tests --list output of migrate command
        """
        out = six.StringIO()
        with mock.patch('django.core.management.color.supports_color', lambda *args: True):
            call_command("migrate", list=True, stdout=out, verbosity=0, no_color=False)
        self.assertEqual(
            '\x1b[1mmigrations\n\x1b[0m'
            ' [ ] 0001_initial\n'
            ' [ ] 0002_second\n',
            out.getvalue().lower()
        )

        call_command("migrate", "migrations", "0001", verbosity=0)

        out = six.StringIO()
        # Giving the explicit app_label tests for selective `show_migration_list` in the command
        call_command("migrate", "migrations", list=True, stdout=out, verbosity=0, no_color=True)
        self.assertEqual(
            'migrations\n'
            ' [x] 0001_initial\n'
            ' [ ] 0002_second\n',
            out.getvalue().lower()
        )

        # Cleanup by unmigrating everything
        call_command("migrate", "migrations", "zero", verbosity=0)

    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations"})
    def test_showmigrations_list(self):
        """
        Tests --list output of showmigrations command
        """
        out = six.StringIO()
        with mock.patch('django.core.management.color.supports_color', lambda *args: True):
            call_command("showmigrations", format='list', stdout=out, verbosity=0, no_color=False)
        self.assertEqual(
            '\x1b[1mmigrations\n\x1b[0m'
            ' [ ] 0001_initial\n'
            ' [ ] 0002_second\n',
            out.getvalue().lower()
        )

        call_command("migrate", "migrations", "0001", verbosity=0)

        out = six.StringIO()
        # Giving the explicit app_label tests for selective `show_list` in the command
        call_command("showmigrations", "migrations", format='list', stdout=out, verbosity=0, no_color=True)
        self.assertEqual(
            'migrations\n'
            ' [x] 0001_initial\n'
            ' [ ] 0002_second\n',
            out.getvalue().lower()
        )
        # Cleanup by unmigrating everything
        call_command("migrate", "migrations", "zero", verbosity=0)

    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations_run_before"})
    def test_showmigrations_plan(self):
        """
        Tests --plan output of showmigrations command
        """
        out = six.StringIO()
        call_command("showmigrations", format='plan', stdout=out)
        self.assertIn(
            "[ ]  migrations.0001_initial\n"
            "[ ]  migrations.0003_third\n"
            "[ ]  migrations.0002_second",
            out.getvalue().lower()
        )

        out = six.StringIO()
        call_command("showmigrations", format='plan', stdout=out, verbosity=2)
        self.assertIn(
            "[ ]  migrations.0001_initial\n"
            "[ ]  migrations.0003_third ... (migrations.0001_initial)\n"
            "[ ]  migrations.0002_second ... (migrations.0001_initial)",
            out.getvalue().lower()
        )

        call_command("migrate", "migrations", "0003", verbosity=0)

        out = six.StringIO()
        call_command("showmigrations", format='plan', stdout=out)
        self.assertIn(
            "[x]  migrations.0001_initial\n"
            "[x]  migrations.0003_third\n"
            "[ ]  migrations.0002_second",
            out.getvalue().lower()
        )

        out = six.StringIO()
        call_command("showmigrations", format='plan', stdout=out, verbosity=2)
        self.assertIn(
            "[x]  migrations.0001_initial\n"
            "[x]  migrations.0003_third ... (migrations.0001_initial)\n"
            "[ ]  migrations.0002_second ... (migrations.0001_initial)",
            out.getvalue().lower()
        )

        # Cleanup by unmigrating everything
        call_command("migrate", "migrations", "zero", verbosity=0)

    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations_empty"})
    def test_showmigrations_plan_no_migrations(self):
        """
        Tests --plan output of showmigrations command without migrations
        """
        out = six.StringIO()
        call_command("showmigrations", format='plan', stdout=out)
        self.assertEqual("", out.getvalue().lower())

        out = six.StringIO()
        call_command("showmigrations", format='plan', stdout=out, verbosity=2)
        self.assertEqual("", out.getvalue().lower())

    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations_squashed_complex"})
    def test_showmigrations_plan_squashed(self):
        """
        Tests --plan output of showmigrations command with squashed migrations.
        """
        out = six.StringIO()
        call_command("showmigrations", format='plan', stdout=out)
        self.assertEqual(
            "[ ]  migrations.1_auto\n"
            "[ ]  migrations.2_auto\n"
            "[ ]  migrations.3_squashed_5\n"
            "[ ]  migrations.6_auto\n"
            "[ ]  migrations.7_auto\n",
            out.getvalue().lower()
        )

        out = six.StringIO()
        call_command("showmigrations", format='plan', stdout=out, verbosity=2)
        self.assertEqual(
            "[ ]  migrations.1_auto\n"
            "[ ]  migrations.2_auto ... (migrations.1_auto)\n"
            "[ ]  migrations.3_squashed_5 ... (migrations.2_auto)\n"
            "[ ]  migrations.6_auto ... (migrations.3_squashed_5)\n"
            "[ ]  migrations.7_auto ... (migrations.6_auto)\n",
            out.getvalue().lower()
        )

        call_command("migrate", "migrations", "3_squashed_5", verbosity=0)

        out = six.StringIO()
        call_command("showmigrations", format='plan', stdout=out)
        self.assertEqual(
            "[x]  migrations.1_auto\n"
            "[x]  migrations.2_auto\n"
            "[x]  migrations.3_squashed_5\n"
            "[ ]  migrations.6_auto\n"
            "[ ]  migrations.7_auto\n",
            out.getvalue().lower()
        )

        out = six.StringIO()
        call_command("showmigrations", format='plan', stdout=out, verbosity=2)
        self.assertEqual(
            "[x]  migrations.1_auto\n"
            "[x]  migrations.2_auto ... (migrations.1_auto)\n"
            "[x]  migrations.3_squashed_5 ... (migrations.2_auto)\n"
            "[ ]  migrations.6_auto ... (migrations.3_squashed_5)\n"
            "[ ]  migrations.7_auto ... (migrations.6_auto)\n",
            out.getvalue().lower()
        )

    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations"})
    def test_sqlmigrate_forwards(self):
        """
        Makes sure that sqlmigrate does something.
        """
        out = six.StringIO()
        call_command("sqlmigrate", "migrations", "0001", stdout=out)
        output = out.getvalue().lower()

        index_tx_start = output.find(connection.ops.start_transaction_sql().lower())
        index_op_desc_author = output.find('-- create model author')
        index_create_table = output.find('create table')
        index_op_desc_tribble = output.find('-- create model tribble')
        index_op_desc_unique_together = output.find('-- alter unique_together')
        index_tx_end = output.find(connection.ops.end_transaction_sql().lower())

        self.assertGreater(index_tx_start, -1, "Transaction start not found")
        self.assertGreater(index_op_desc_author, index_tx_start,
            "Operation description (author) not found or found before transaction start")
        self.assertGreater(index_create_table, index_op_desc_author,
            "CREATE TABLE not found or found before operation description (author)")
        self.assertGreater(index_op_desc_tribble, index_create_table,
            "Operation description (tribble) not found or found before CREATE TABLE (author)")
        self.assertGreater(index_op_desc_unique_together, index_op_desc_tribble,
            "Operation description (unique_together) not found or found before operation description (tribble)")
        self.assertGreater(index_tx_end, index_op_desc_unique_together,
            "Transaction end not found or found before operation description (unique_together)")

    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations"})
    def test_sqlmigrate_backwards(self):
        """
        Makes sure that sqlmigrate does something.
        """
        # Cannot generate the reverse SQL unless we've applied the migration.
        call_command("migrate", "migrations", verbosity=0)

        out = six.StringIO()
        call_command("sqlmigrate", "migrations", "0001", stdout=out, backwards=True)
        output = out.getvalue().lower()

        index_tx_start = output.find(connection.ops.start_transaction_sql().lower())
        index_op_desc_unique_together = output.find('-- alter unique_together')
        index_op_desc_tribble = output.find('-- create model tribble')
        index_op_desc_author = output.find('-- create model author')
        index_drop_table = output.rfind('drop table')
        index_tx_end = output.find(connection.ops.end_transaction_sql().lower())

        self.assertGreater(index_tx_start, -1, "Transaction start not found")
        self.assertGreater(index_op_desc_unique_together, index_tx_start,
            "Operation description (unique_together) not found or found before transaction start")
        self.assertGreater(index_op_desc_tribble, index_op_desc_unique_together,
            "Operation description (tribble) not found or found before operation description (unique_together)")
        self.assertGreater(index_op_desc_author, index_op_desc_tribble,
            "Operation description (author) not found or found before operation description (tribble)")

        self.assertGreater(index_drop_table, index_op_desc_author,
            "DROP TABLE not found or found before operation description (author)")
        self.assertGreater(index_tx_end, index_op_desc_unique_together,
            "Transaction end not found or found before DROP TABLE")

        # Cleanup by unmigrating everything
        call_command("migrate", "migrations", "zero", verbosity=0)

    @override_settings(
        INSTALLED_APPS=[
            "migrations.migrations_test_apps.migrated_app",
            "migrations.migrations_test_apps.migrated_unapplied_app",
            "migrations.migrations_test_apps.unmigrated_app"])
    def test_regression_22823_unmigrated_fk_to_migrated_model(self):
        """
        https://code.djangoproject.com/ticket/22823

        Assuming you have 3 apps, `A`, `B`, and `C`, such that:

        * `A` has migrations
        * `B` has a migration we want to apply
        * `C` has no migrations, but has an FK to `A`

        When we try to migrate "B", an exception occurs because the
        "B" was not included in the ProjectState that is used to detect
        soft-applied migrations.
        """
        call_command("migrate", "migrated_unapplied_app", stdout=six.StringIO())

    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations_squashed"})
    def test_migrate_record_replaced(self):
        """
        Running a single squashed migration should record all of the original
        replaced migrations as run.
        """
        recorder = MigrationRecorder(connection)
        out = six.StringIO()
        call_command("migrate", "migrations", verbosity=0)
        call_command("showmigrations", "migrations", stdout=out, no_color=True)
        self.assertEqual(
            'migrations\n'
            ' [x] 0001_squashed_0002 (2 squashed migrations)\n',
            out.getvalue().lower()
        )
        applied_migrations = recorder.applied_migrations()
        self.assertIn(("migrations", "0001_initial"), applied_migrations)
        self.assertIn(("migrations", "0002_second"), applied_migrations)
        self.assertIn(("migrations", "0001_squashed_0002"), applied_migrations)
        # Rollback changes
        call_command("migrate", "migrations", "zero", verbosity=0)

    @override_settings(MIGRATION_MODULES={"migrations": "migrations.test_migrations_squashed"})
    def test_migrate_record_squashed(self):
        """
        Running migrate for a squashed migration should record as run
        if all of the replaced migrations have been run (#25231).
        """
        recorder = MigrationRecorder(connection)
        recorder.record_applied("migrations", "0001_initial")
        recorder.record_applied("migrations", "0002_second")
        out = six.StringIO()
        call_command("migrate", "migrations", verbosity=0)
        call_command("showmigrations", "migrations", stdout=out, no_color=True)
        self.assertEqual(
            'migrations\n'
            ' [x] 0001_squashed_0002 (2 squashed migrations)\n',
            out.getvalue().lower()
        )
        self.assertIn(
            ("migrations", "0001_squashed_0002"),
            recorder.applied_migrations()
        )
        # No changes were actually applied so there is nothing to rollback


class MakeMigrationsTests(MigrationTestBase):
    """
    Tests running the makemigrations command.
    """

    def setUp(self):
        super(MigrationTestBase, self).setUp()
        self._old_models = apps.app_configs['migrations'].models.copy()

    def tearDown(self):
        apps.app_configs['migrations'].models = self._old_models
        apps.all_models['migrations'] = self._old_models
        apps.clear_cache()
        super(MigrationTestBase, self).tearDown()

    def test_files_content(self):
        self.assertTableNotExists("migrations_unicodemodel")
        apps.register_model('migrations', UnicodeModel)
        with self.temporary_migration_module() as migration_dir:
            call_command("makemigrations", "migrations", verbosity=0)

            # Check for empty __init__.py file in migrations folder
            init_file = os.path.join(migration_dir, "__init__.py")
            self.assertTrue(os.path.exists(init_file))

            with open(init_file, 'r') as fp:
                content = force_text(fp.read())
            self.assertEqual(content, '')

            # Check for existing 0001_initial.py file in migration folder
            initial_file = os.path.join(migration_dir, "0001_initial.py")
            self.assertTrue(os.path.exists(initial_file))

            with codecs.open(initial_file, 'r', encoding='utf-8') as fp:
                content = fp.read()
                self.assertIn('# -*- coding: utf-8 -*-', content)
                self.assertIn('migrations.CreateModel', content)
                self.assertIn('initial = True', content)

                if six.PY3:
                    self.assertIn('úñí©óðé µóðéø', content)  # Meta.verbose_name
                    self.assertIn('úñí©óðé µóðéøß', content)  # Meta.verbose_name_plural
                    self.assertIn('ÚÑÍ¢ÓÐÉ', content)  # title.verbose_name
                    self.assertIn('“Ðjáñgó”', content)  # title.default
                else:
                    self.assertIn('\\xfa\\xf1\\xed\\xa9\\xf3\\xf0\\xe9 \\xb5\\xf3\\xf0\\xe9\\xf8', content)  # Meta.verbose_name
                    self.assertIn('\\xfa\\xf1\\xed\\xa9\\xf3\\xf0\\xe9 \\xb5\\xf3\\xf0\\xe9\\xf8\\xdf', content)  # Meta.verbose_name_plural
                    self.assertIn('\\xda\\xd1\\xcd\\xa2\\xd3\\xd0\\xc9', content)  # title.verbose_name
                    self.assertIn('\\u201c\\xd0j\\xe1\\xf1g\\xf3\\u201d', content)  # title.default

    def test_makemigrations_order(self):
        """
        makemigrations should recognize number-only migrations (0001.py).
        """
        module = 'migrations.test_migrations_order'
        with self.temporary_migration_module(module=module) as migration_dir:
            if hasattr(importlib, 'invalidate_caches'):
                # Python 3 importlib caches os.listdir() on some platforms like
                # Mac OS X (#23850).
                importlib.invalidate_caches()
            call_command('makemigrations', 'migrations', '--empty', '-n', 'a', '-v', '0')
            self.assertTrue(os.path.exists(os.path.join(migration_dir, '0002_a.py')))

    def test_failing_migration(self):
        # If a migration fails to serialize, it shouldn't generate an empty file. #21280
        apps.register_model('migrations', UnserializableModel)

        with self.temporary_migration_module() as migration_dir:
            with six.assertRaisesRegex(self, ValueError, r'Cannot serialize'):
                call_command("makemigrations", "migrations", verbosity=0)

            initial_file = os.path.join(migration_dir, "0001_initial.py")
            self.assertFalse(os.path.exists(initial_file))

    def test_makemigrations_conflict_exit(self):
        """
        Makes sure that makemigrations exits if it detects a conflict.
        """
        with self.temporary_migration_module(module="migrations.test_migrations_conflict"):
            with self.assertRaises(CommandError):
                call_command("makemigrations")

    def test_makemigrations_merge_no_conflict(self):
        """
        Makes sure that makemigrations exits if in merge mode with no conflicts.
        """
        out = six.StringIO()
        with self.temporary_migration_module(module="migrations.test_migrations"):
            try:
                call_command("makemigrations", merge=True, stdout=out)
            except CommandError:
                self.fail("Makemigrations errored in merge mode with no conflicts")
        self.assertIn("No conflicts detected to merge.", out.getvalue())

    def test_makemigrations_no_app_sys_exit(self):
        """
        Makes sure that makemigrations exits if a non-existent app is specified.
        """
        err = six.StringIO()
        with self.assertRaises(SystemExit):
            call_command("makemigrations", "this_app_does_not_exist", stderr=err)
        self.assertIn("'this_app_does_not_exist' could not be found.", err.getvalue())

    def test_makemigrations_empty_no_app_specified(self):
        """
        Makes sure that makemigrations exits if no app is specified with 'empty' mode.
        """
        with self.assertRaises(CommandError):
            call_command("makemigrations", empty=True)

    def test_makemigrations_empty_migration(self):
        """
        Makes sure that makemigrations properly constructs an empty migration.
        """
        with self.temporary_migration_module() as migration_dir:
            try:
                call_command("makemigrations", "migrations", empty=True, verbosity=0)
            except CommandError:
                self.fail("Makemigrations errored in creating empty migration for a proper app.")

            # Check for existing 0001_initial.py file in migration folder
            initial_file = os.path.join(migration_dir, "0001_initial.py")
            self.assertTrue(os.path.exists(initial_file))

            with codecs.open(initial_file, 'r', encoding='utf-8') as fp:
                content = fp.read()
                self.assertIn('# -*- coding: utf-8 -*-', content)

                # Remove all whitespace to check for empty dependencies and operations
                content = content.replace(' ', '')
                self.assertIn('dependencies=[\n]', content)
                self.assertIn('operations=[\n]', content)

    def test_makemigrations_no_changes_no_apps(self):
        """
        Makes sure that makemigrations exits when there are no changes and no apps are specified.
        """
        out = six.StringIO()
        call_command("makemigrations", stdout=out)
        self.assertIn("No changes detected", out.getvalue())

    def test_makemigrations_no_changes(self):
        """
        Makes sure that makemigrations exits when there are no changes to an app.
        """
        out = six.StringIO()
        with self.temporary_migration_module(module="migrations.test_migrations_no_changes"):
            call_command("makemigrations", "migrations", stdout=out)
        self.assertIn("No changes detected in app 'migrations'", out.getvalue())

    def test_makemigrations_no_apps_initial(self):
        """
        makemigrations should detect initial is needed on empty migration
        modules if no app provided.
        """
        out = six.StringIO()
        with self.temporary_migration_module(module="migrations.test_migrations_empty"):
            call_command("makemigrations", stdout=out)
        self.assertIn("0001_initial.py", out.getvalue())

    def test_makemigrations_migrations_announce(self):
        """
        Makes sure that makemigrations announces the migration at the default verbosity level.
        """
        out = six.StringIO()
        with self.temporary_migration_module():
            call_command("makemigrations", "migrations", stdout=out)
        self.assertIn("Migrations for 'migrations'", out.getvalue())

    def test_makemigrations_no_common_ancestor(self):
        """
        Makes sure that makemigrations fails to merge migrations with no common ancestor.
        """
        with self.assertRaises(ValueError) as context:
            with self.temporary_migration_module(module="migrations.test_migrations_no_ancestor"):
                call_command("makemigrations", "migrations", merge=True)
        exception_message = str(context.exception)
        self.assertIn("Could not find common ancestor of", exception_message)
        self.assertIn("0002_second", exception_message)
        self.assertIn("0002_conflicting_second", exception_message)

    def test_makemigrations_interactive_reject(self):
        """
        Makes sure that makemigrations enters and exits interactive mode properly.
        """
        # Monkeypatch interactive questioner to auto reject
        with mock.patch('django.db.migrations.questioner.input', mock.Mock(return_value='N')):
            try:
                with self.temporary_migration_module(module="migrations.test_migrations_conflict") as migration_dir:
                    call_command("makemigrations", "migrations", merge=True, interactive=True, verbosity=0)
                    merge_file = os.path.join(migration_dir, '0003_merge.py')
                    self.assertFalse(os.path.exists(merge_file))
            except CommandError:
                self.fail("Makemigrations failed while running interactive questioner")

    def test_makemigrations_interactive_accept(self):
        """
        Makes sure that makemigrations enters interactive mode and merges properly.
        """
        # Monkeypatch interactive questioner to auto accept
        with mock.patch('django.db.migrations.questioner.input', mock.Mock(return_value='y')):
            out = six.StringIO()
            try:
                with self.temporary_migration_module(module="migrations.test_migrations_conflict") as migration_dir:
                    call_command("makemigrations", "migrations", merge=True, interactive=True, stdout=out)
                    merge_file = os.path.join(migration_dir, '0003_merge.py')
                    self.assertTrue(os.path.exists(merge_file))
            except CommandError:
                self.fail("Makemigrations failed while running interactive questioner")
            self.assertIn("Created new merge migration", force_text(out.getvalue()))

    def test_makemigrations_non_interactive_not_null_addition(self):
        """
        Tests that non-interactive makemigrations fails when a default is missing on a new not-null field.
        """
        class SillyModel(models.Model):
            silly_field = models.BooleanField(default=False)
            silly_int = models.IntegerField()

            class Meta:
                app_label = "migrations"

        out = six.StringIO()
        with self.assertRaises(SystemExit):
            with self.temporary_migration_module(module="migrations.test_migrations_no_default"):
                call_command("makemigrations", "migrations", interactive=False, stdout=out)

    def test_makemigrations_non_interactive_not_null_alteration(self):
        """
        Tests that non-interactive makemigrations fails when a default is missing on a field changed to not-null.
        """
        class Author(models.Model):
            name = models.CharField(max_length=255)
            slug = models.SlugField()
            age = models.IntegerField(default=0)

            class Meta:
                app_label = "migrations"

        out = six.StringIO()
        try:
            with self.temporary_migration_module(module="migrations.test_migrations"):
                call_command("makemigrations", "migrations", interactive=False, stdout=out)
        except CommandError:
            self.fail("Makemigrations failed while running non-interactive questioner.")
        self.assertIn("Alter field slug on author", force_text(out.getvalue()))

    def test_makemigrations_non_interactive_no_model_rename(self):
        """
        Makes sure that makemigrations adds and removes a possible model rename in non-interactive mode.
        """
        class RenamedModel(models.Model):
            silly_field = models.BooleanField(default=False)

            class Meta:
                app_label = "migrations"

        out = six.StringIO()
        try:
            with self.temporary_migration_module(module="migrations.test_migrations_no_default"):
                call_command("makemigrations", "migrations", interactive=False, stdout=out)
        except CommandError:
            self.fail("Makemigrations failed while running non-interactive questioner")
        self.assertIn("Delete model SillyModel", force_text(out.getvalue()))
        self.assertIn("Create model RenamedModel", force_text(out.getvalue()))

    def test_makemigrations_non_interactive_no_field_rename(self):
        """
        Makes sure that makemigrations adds and removes a possible field rename in non-interactive mode.
        """
        class SillyModel(models.Model):
            silly_rename = models.BooleanField(default=False)

            class Meta:
                app_label = "migrations"

        out = six.StringIO()
        try:
            with self.temporary_migration_module(module="migrations.test_migrations_no_default"):
                call_command("makemigrations", "migrations", interactive=False, stdout=out)
        except CommandError:
            self.fail("Makemigrations failed while running non-interactive questioner")
        self.assertIn("Remove field silly_field from sillymodel", force_text(out.getvalue()))
        self.assertIn("Add field silly_rename to sillymodel", force_text(out.getvalue()))

    def test_makemigrations_handle_merge(self):
        """
        Makes sure that makemigrations properly merges the conflicting migrations with --noinput.
        """
        out = six.StringIO()
        with self.temporary_migration_module(module="migrations.test_migrations_conflict") as migration_dir:
            call_command("makemigrations", "migrations", merge=True, interactive=False, stdout=out)
            merge_file = os.path.join(migration_dir, '0003_merge.py')
            self.assertTrue(os.path.exists(merge_file))
        output = force_text(out.getvalue())
        self.assertIn("Merging migrations", output)
        self.assertIn("Branch 0002_second", output)
        self.assertIn("Branch 0002_conflicting_second", output)
        self.assertIn("Created new merge migration", output)

    def test_makemigration_merge_dry_run(self):
        """
        Makes sure that makemigrations respects --dry-run option when fixing
        migration conflicts (#24427).
        """
        out = six.StringIO()
        with self.temporary_migration_module(module="migrations.test_migrations_conflict") as migration_dir:
            call_command("makemigrations", "migrations", dry_run=True, merge=True, interactive=False, stdout=out)
            merge_file = os.path.join(migration_dir, '0003_merge.py')
            self.assertFalse(os.path.exists(merge_file))
        output = force_text(out.getvalue())
        self.assertIn("Merging migrations", output)
        self.assertIn("Branch 0002_second", output)
        self.assertIn("Branch 0002_conflicting_second", output)
        self.assertNotIn("Created new merge migration", output)

    def test_makemigration_merge_dry_run_verbosity_3(self):
        """
        Makes sure that `makemigrations --merge --dry-run` writes the merge
        migration file to stdout with `verbosity == 3` (#24427).
        """
        out = six.StringIO()
        with self.temporary_migration_module(module="migrations.test_migrations_conflict") as migration_dir:
            call_command("makemigrations", "migrations", dry_run=True, merge=True, interactive=False,
                         stdout=out, verbosity=3)
            merge_file = os.path.join(migration_dir, '0003_merge.py')
            self.assertFalse(os.path.exists(merge_file))
        output = force_text(out.getvalue())
        self.assertIn("Merging migrations", output)
        self.assertIn("Branch 0002_second", output)
        self.assertIn("Branch 0002_conflicting_second", output)
        self.assertNotIn("Created new merge migration", output)

        # Additional output caused by verbosity 3
        # The complete merge migration file that would be written
        self.assertIn("# -*- coding: utf-8 -*-", output)
        self.assertIn("class Migration(migrations.Migration):", output)
        self.assertIn("dependencies = [", output)
        self.assertIn("('migrations', '0002_second')", output)
        self.assertIn("('migrations', '0002_conflicting_second')", output)
        self.assertIn("operations = [", output)
        self.assertIn("]", output)

    def test_makemigrations_dry_run(self):
        """
        Ticket #22676 -- `makemigrations --dry-run` should not ask for defaults.
        """

        class SillyModel(models.Model):
            silly_field = models.BooleanField(default=False)
            silly_date = models.DateField()  # Added field without a default

            class Meta:
                app_label = "migrations"

        out = six.StringIO()
        with self.temporary_migration_module(module="migrations.test_migrations_no_default"):
            call_command("makemigrations", "migrations", dry_run=True, stdout=out)
        # Output the expected changes directly, without asking for defaults
        self.assertIn("Add field silly_date to sillymodel", out.getvalue())

    def test_makemigrations_dry_run_verbosity_3(self):
        """
        Ticket #22675 -- Allow `makemigrations --dry-run` to output the
        migrations file to stdout (with verbosity == 3).
        """

        class SillyModel(models.Model):
            silly_field = models.BooleanField(default=False)
            silly_char = models.CharField(default="")

            class Meta:
                app_label = "migrations"

        out = six.StringIO()
        with self.temporary_migration_module(module="migrations.test_migrations_no_default"):
            call_command("makemigrations", "migrations", dry_run=True, stdout=out, verbosity=3)

        # Normal --dry-run output
        self.assertIn("- Add field silly_char to sillymodel", out.getvalue())

        # Additional output caused by verbosity 3
        # The complete migrations file that would be written
        self.assertIn("# -*- coding: utf-8 -*-", out.getvalue())
        self.assertIn("class Migration(migrations.Migration):", out.getvalue())
        self.assertIn("dependencies = [", out.getvalue())
        self.assertIn("('migrations', '0001_initial'),", out.getvalue())
        self.assertIn("migrations.AddField(", out.getvalue())
        self.assertIn("model_name='sillymodel',", out.getvalue())
        self.assertIn("name='silly_char',", out.getvalue())

    def test_makemigrations_migrations_modules_path_not_exist(self):
        """
        Ticket #22682 -- Makemigrations fails when specifying custom location
        for migration files (using MIGRATION_MODULES) if the custom path
        doesn't already exist.
        """

        class SillyModel(models.Model):
            silly_field = models.BooleanField(default=False)

            class Meta:
                app_label = "migrations"

        out = six.StringIO()
        with self.temporary_migration_module(module="migrations.test_migrations_path_doesnt_exist.foo.bar") as migration_dir:
            call_command("makemigrations", "migrations", stdout=out)

            # Migrations file is actually created in the expected path.
            initial_file = os.path.join(migration_dir, "0001_initial.py")
            self.assertTrue(os.path.exists(initial_file))

        # Command output indicates the migration is created.
        self.assertIn(" - Create model SillyModel", out.getvalue())

    def test_makemigrations_interactive_by_default(self):
        """
        Makes sure that the user is prompted to merge by default if there are
        conflicts and merge is True. Answer negative to differentiate it from
        behavior when --noinput is specified.
        """
        # Monkeypatch interactive questioner to auto reject
        out = six.StringIO()
        with mock.patch('django.db.migrations.questioner.input', mock.Mock(return_value='N')):
            try:
                with self.temporary_migration_module(module="migrations.test_migrations_conflict") as migration_dir:
                    call_command("makemigrations", "migrations", merge=True, stdout=out)
                    merge_file = os.path.join(migration_dir, '0003_merge.py')
                    # This will fail if interactive is False by default
                    self.assertFalse(os.path.exists(merge_file))
            except CommandError:
                self.fail("Makemigrations failed while running interactive questioner")
            self.assertNotIn("Created new merge migration", out.getvalue())

    @override_settings(
        INSTALLED_APPS=[
            "migrations",
            "migrations.migrations_test_apps.unspecified_app_with_conflict"])
    def test_makemigrations_unspecified_app_with_conflict_no_merge(self):
        """
        Makes sure that makemigrations does not raise a CommandError when an
        unspecified app has conflicting migrations.
        """
        try:
            with self.temporary_migration_module(module="migrations.test_migrations_no_changes"):
                call_command("makemigrations", "migrations", merge=False, verbosity=0)
        except CommandError:
            self.fail("Makemigrations fails resolving conflicts in an unspecified app")

    @override_settings(
        INSTALLED_APPS=[
            "migrations.migrations_test_apps.migrated_app",
            "migrations.migrations_test_apps.unspecified_app_with_conflict"])
    def test_makemigrations_unspecified_app_with_conflict_merge(self):
        """
        Makes sure that makemigrations does not create a merge for an
        unspecified app even if it has conflicting migrations.
        """
        # Monkeypatch interactive questioner to auto accept
        with mock.patch('django.db.migrations.questioner.input', mock.Mock(return_value='y')):
            out = six.StringIO()
            try:
                with self.temporary_migration_module(app_label="migrated_app") as migration_dir:
                    call_command("makemigrations", "migrated_app", merge=True, interactive=True, stdout=out)
                    merge_file = os.path.join(migration_dir, '0003_merge.py')
                    self.assertFalse(os.path.exists(merge_file))
                self.assertIn("No conflicts detected to merge.", out.getvalue())
            except CommandError:
                self.fail("Makemigrations fails resolving conflicts in an unspecified app")

    @override_settings(
        INSTALLED_APPS=[
            "migrations.migrations_test_apps.migrated_app",
            "migrations.migrations_test_apps.conflicting_app_with_dependencies"])
    def test_makemigrations_merge_dont_output_dependency_operations(self):
        """
        Makes sure that makemigrations --merge does not output any operations
        from apps that don't belong to a given app.
        """
        # Monkeypatch interactive questioner to auto accept
        with mock.patch('django.db.migrations.questioner.input', mock.Mock(return_value='N')):
            out = six.StringIO()
            with mock.patch('django.core.management.color.supports_color', lambda *args: False):
                call_command(
                    "makemigrations", "conflicting_app_with_dependencies",
                    merge=True, interactive=True, stdout=out
                )
            val = out.getvalue().lower()
            self.assertIn('merging conflicting_app_with_dependencies\n', val)
            self.assertIn(
                '  branch 0002_conflicting_second\n'
                '    - create model something\n',
                val
            )
            self.assertIn(
                '  branch 0002_second\n'
                '    - delete model tribble\n'
                '    - remove field silly_field from author\n'
                '    - add field rating to author\n'
                '    - create model book\n',
                val
            )

    def test_makemigrations_with_custom_name(self):
        """
        Makes sure that makemigrations generate a custom migration.
        """
        with self.temporary_migration_module() as migration_dir:

            def cmd(migration_count, migration_name, *args):
                try:
                    call_command("makemigrations", "migrations", "--verbosity", "0", "--name", migration_name, *args)
                except CommandError:
                    self.fail("Makemigrations errored in creating empty migration with custom name for a proper app.")
                migration_file = os.path.join(migration_dir, "%s_%s.py" % (migration_count, migration_name))
                # Check for existing migration file in migration folder
                self.assertTrue(os.path.exists(migration_file))
                with codecs.open(migration_file, "r", encoding="utf-8") as fp:
                    content = fp.read()
                    self.assertIn("# -*- coding: utf-8 -*-", content)
                    content = content.replace(" ", "")
                return content

            # generate an initial migration
            migration_name_0001 = "my_initial_migration"
            content = cmd("0001", migration_name_0001)
            self.assertIn("dependencies=[\n]", content)

            # Python 3 importlib caches os.listdir() on some platforms like
            # Mac OS X (#23850).
            if hasattr(importlib, 'invalidate_caches'):
                importlib.invalidate_caches()

            # generate an empty migration
            migration_name_0002 = "my_custom_migration"
            content = cmd("0002", migration_name_0002, "--empty")
            self.assertIn("dependencies=[\n('migrations','0001_%s'),\n]" % migration_name_0001, content)
            self.assertIn("operations=[\n]", content)

    def test_makemigrations_exit(self):
        """
        makemigrations --exit should exit with sys.exit(1) when there are no
        changes to an app.
        """
        with self.temporary_migration_module():
            call_command("makemigrations", "--exit", "migrations", verbosity=0)

        with self.temporary_migration_module(module="migrations.test_migrations_no_changes"):
            with self.assertRaises(SystemExit):
                call_command("makemigrations", "--exit", "migrations", verbosity=0)


class SquashMigrationsTests(MigrationTestBase):
    """
    Tests running the squashmigrations command.
    """

    def test_squashmigrations_squashes(self):
        """
        Tests that squashmigrations squashes migrations.
        """
        with self.temporary_migration_module(module="migrations.test_migrations") as migration_dir:
            call_command("squashmigrations", "migrations", "0002", interactive=False, verbosity=0)

            squashed_migration_file = os.path.join(migration_dir, "0001_squashed_0002_second.py")
            self.assertTrue(os.path.exists(squashed_migration_file))

    def test_squashmigrations_initial_attribute(self):
        with self.temporary_migration_module(module="migrations.test_migrations") as migration_dir:
            call_command("squashmigrations", "migrations", "0002", interactive=False, verbosity=0)

            squashed_migration_file = os.path.join(migration_dir, "0001_squashed_0002_second.py")
            with codecs.open(squashed_migration_file, "r", encoding="utf-8") as fp:
                content = fp.read()
                self.assertIn("initial = True", content)

    def test_squashmigrations_optimizes(self):
        """
        Tests that squashmigrations optimizes operations.
        """
        out = six.StringIO()
        with self.temporary_migration_module(module="migrations.test_migrations"):
            call_command("squashmigrations", "migrations", "0002", interactive=False, verbosity=1, stdout=out)
        self.assertIn("Optimized from 7 operations to 3 operations.", force_text(out.getvalue()))

    def test_ticket_23799_squashmigrations_no_optimize(self):
        """
        Makes sure that squashmigrations --no-optimize really doesn't optimize operations.
        """
        out = six.StringIO()
        with self.temporary_migration_module(module="migrations.test_migrations"):
            call_command("squashmigrations", "migrations", "0002",
                         interactive=False, verbosity=1, no_optimize=True, stdout=out)
        self.assertIn("Skipping optimization", force_text(out.getvalue()))
