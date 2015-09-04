*   `rails server` will now honour the `PORT` environment variable

    *David Cornu*

*   Plugins generated using `rails plugin new` are now generated with the
    version number set to 0.1.0.

    *Daniel Morris*

*   `I18n.load_path` is now reloaded under development so there's no need to
    restart the server to make new locale files available. Also, I18n will no
    longer raise for deleted locale files.

    *Kir Shatrov*

*   Add `bin/update` script to update development environment automatically.

    *Mehmet Emin İNAÇ*

*   Fix STATS_DIRECTORIES already defined warning when running rake from within
    the top level directory of an engine that has a test app.

    Fixes #20510

    *Ersin Akinci*

*   Make enabling or disabling caching in development mode possible with
    rake dev:cache.

    Running rake dev:cache will create or remove tmp/caching-dev.txt. When this
    file exists config.action_controller.perform_caching will be set to true in
    config/environments/development.rb.

    Additionally, a server can be started with either --dev-caching or
    --no-dev-caching included to toggle caching on startup.

    *Jussi Mertanen*, *Chuck Callebs*

*   Add a `--api` option in order to generate plugins that can be added
    inside an API application.

    *Robin Dupret*

*   Fix `NoMethodError` when generating a scaffold inside a full engine.

    *Yuji Yaginuma*

*   Adding support for passing a block to the `add_source` action of a custom generator

    *Mike Dalton*, *Hirofumi Wakasugi*

*   `assert_file` understands paths with special characters
    (eg. `v0.1.4~alpha+nightly`).

    *Diego Carrion*

*   Remove ContentLength middleware from the defaults.  If you want it, just
    add it as a middleware in your config.

    *Egg McMuffin*

*   Make it possible to customize the executable inside rerun snippets.

    *Yves Senn*

*   Add support for API only apps.
    Middleware stack was slimmed down and it has only the needed
    middleware for API apps & generators generates the right files,
    folders and configurations.

    *Santiago Pastorino & Jorge Bejar*

*   Make generated scaffold functional tests work inside engines.

    *Yuji Yaginuma*

*   Generator a `.keep` file in the `tmp` folder by default as many scripts
    assume the existence of this folder and most would fail if it is absent.

    See #20299.

    *Yoong Kang Lim*, *Sunny Juneja*

*   `config.static_index` configures directory `index.html` filename

    Set `config.static_index` to serve a static directory index file not named
    `index`. E.g. to serve `main.html` instead of `index.html` for directory
    requests, set `config.static_index` to `"main"`.

    *Eliot Sykes*

*   `bin/setup` uses built-in rake tasks (`log:clear`, `tmp:clear`).

    *Mohnish Thallavajhula*

*   Fix mailer previews with attachments by using the mail gem's own API to
    locate the first part of the correct mime type.

    Fixes #14435.

    *Andrew White*

*   Remove sqlite support from `rails dbconsole`.

    *Andrew White*

*   Rename `railties/bin` to `railties/exe` to match the new Bundler executables
    convention.

    *Islam Wazery*

*   Print `bundle install` output in `rails new` as soon as it's available.

    Running `rails new` will now print the output of `bundle install` as
    it is available, instead of waiting until all gems finish installing.

    *Max Holder*

*   Respect `pluralize_table_names` when generating fixture file.

    Fixes #19519.

    *Yuji Yaginuma*

*   Add a new-line to the end of route method generated code.

    We need to add a `\n`, because we cannot have two routes
    in the same line.

    *arthurnn*

*   Add `rake initializers`.

    This task prints out all defined initializers in the order they are invoked
    by Rails. This is helpful for debugging issues related to the initialization
    process.

    *Naoto Kaneko*

*   Created rake restart task. Restarts your Rails app by touching the
    `tmp/restart.txt`.

    Fixes #18876.

    *Hyonjee Joo*

*   Add `config/initializers/active_record_belongs_to_required_by_default.rb`.

    Newly generated Rails apps have a new initializer called
    `active_record_belongs_to_required_by_default.rb` which sets the value of
    the configuration option `config.active_record.belongs_to_required_by_default`
    to `true` when ActiveRecord is not skipped.

    As a result, new Rails apps require `belongs_to` association on model
    to be valid.

    This initializer is *not* added when running `rake rails:update`, so
    old apps ported to Rails 5 will work without any change.

    *Josef Šimánek*

*   `delete` operations in configurations are run last in order to eliminate
    'No such middleware' errors when `insert_before` or `insert_after` are added
    after the `delete` operation for the middleware being deleted.

    Fixes #16433.

    *Guo Xiang Tan*

*   Newly generated applications get a `README.md` in Markdown.

    *Xavier Noria*

*   Remove the documentation tasks `doc:app`, `doc:rails`, and `doc:guides`.

    *Xavier Noria*

*   Force generated routes to be inserted into `config/routes.rb`.

    *Andrew White*

*   Don't remove all line endings from `config/routes.rb` when revoking scaffold.

    Fixes #15913.

    *Andrew White*

*   Rename `--skip-test-unit` option to `--skip-test` in app generator

    *Melanie Gilman*

*   Add the `method_source` gem to the default Gemfile for apps.

    *Sean Griffin*

*   Drop old test locations from `rake stats`:

    - test/functional
    - test/unit

    *Ravil Bayramgalin*

*   Update `rake stats` to  correctly count declarative tests
    as methods in `_test.rb` files.

    *Ravil Bayramgalin*

*   Remove deprecated `test:all` and `test:all:db` tasks.

    *Rafael Mendonça França*

*   Remove deprecated `Rails::Rack::LogTailer`.

    *Rafael Mendonça França*

*   Remove deprecated `RAILS_CACHE` constant.

    *Rafael Mendonça França*

*   Remove deprecated `serve_static_assets` configuration.

    *Rafael Mendonça França*

*   Use local variables in `_form.html.erb` partial generated by scaffold.

    *Andrew Kozlov*

*   Add `config/initializers/callback_terminator.rb`.

    Newly generated Rails apps have a new initializer called
    `callback_terminator.rb` which sets the value of the configuration option
    `config.active_support.halt_callback_chains_on_return_false` to `false`.

    As a result, new Rails apps do not halt callback chains when a callback
    returns `false`; only when they are explicitly halted with `throw(:abort)`.

    The terminator is *not* added when running `rake rails:update`, so returning
    `false` will still work on old apps ported to Rails 5, displaying a
    deprecation warning to prompt users to update their code to the new syntax.

    *claudiob*

*   Generated fixtures won't use the id when generated with references attributes.

    *Pablo Olmos de Aguilera Corradini*

*   Add `--skip-action-mailer` option to the app generator.

    *claudiob*

*   Autoload any second level directories called `app/*/concerns`.

    *Alex Robbin*

Please check [4-2-stable](https://github.com/rails/rails/blob/4-2-stable/railties/CHANGELOG.md) for previous changes.
