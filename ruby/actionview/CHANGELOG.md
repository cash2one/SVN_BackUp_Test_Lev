*   `number_to_currency` and `number_with_delimiter` now accept custom `delimiter_pattern` option 
     to handle placement of delimiter, to support currency formats like INR 
     
     Example: 
        
        number_to_currency(1230000, delimiter_pattern: /(\d+?)(?=(\d\d)+(\d)(?!\d))/, unit: '₹', format: "%u %n")
        # => '₹ 12,30,000.00' 
        
    *Vipul A M*
    
*   Make `disable_with` the default behavior for submit tags. Disables the
    button on submit to prevent double submits.

    *Justin Schiff*

*   Add a break_sequence option to word_wrap so you can specify a custom break.

    * Mauricio Gomez *

*   Add wildcard matching to explicit dependencies.

    Turns:

    ```erb
    <% # Template Dependency: recordings/threads/events/subscribers_changed %>
    <% # Template Dependency: recordings/threads/events/completed %>
    <% # Template Dependency: recordings/threads/events/uncompleted %>
    ```

    Into:

    ```erb
    <% # Template Dependency: recordings/threads/events/* %>
    ```

    *Kasper Timm Hansen*

*   Allow defining explicit collection caching using a `# Template Collection: ...`
    directive inside templates.

    *Dov Murik*

*   Asset helpers raise `ArgumentError` when `nil` is passed as a source.

    *Anton Kolomiychuk*

*   Always attach the template digest to the cache key for collection caching
    even when `virtual_path` is not available from the view context.
    Which could happen if the rendering was done directly in the controller
    and not in a template.

    Fixes #20535

    *Roque Pinel*

*   Improve detection of partial templates eligible for collection caching,
    now allowing multi-line comments at the beginning of the template file.

    *Dov Murik*

*   Raise an ArgumentError when a false value for `include_blank` is passed to a
    required select field (to comply with the HTML5 spec).

    *Grey Baker*

*   Do not put partial name to `local_assigns` when rendering without
    an object or a collection.

    *Henrik Nygren*

*   Remove `:rescue_format` option for `translate` helper since it's no longer
    supported by I18n.

    *Bernard Potocki*

*   `translate` should handle `raise` flag correctly in case of both main and default
    translation is missing.

    Fixes #19967

    *Bernard Potocki*

*   Load the `default_form_builder` from the controller on initialization, which overrides
    the global config if it is present.

    *Kevin McPhillips*

*   Accept lambda as `child_index` option in `fields_for` method.

    *Karol Galanciak*

*   `translate` allows `default: [[]]` again for a default value of `[]`.

    Fixes #19640.

    *Adam Prescott*

*   `translate` should accept nils as members of the `:default`
    parameter without raising a translation missing error.

    Fixes #19419

    *Justin Coyne*

*   `number_to_percentage` does not crash with `Float::NAN` or `Float::INFINITY`
    as input when `precision: 0` is used.

    Fixes #19227.

    *Yves Senn*

*   Fixed the translation helper method to accept different default values types
    besides String.

    *Ulisses Almeida*

*   Collection rendering automatically caches and fetches multiple partials.

    Collections rendered as:

    ```ruby
    <%= render @notifications %>
    <%= render partial: 'notifications/notification', collection: @notifications, as: :notification %>
    ```

    will now read several partials from cache at once, if the template starts with a cache call:

    ```ruby
    # notifications/_notification.html.erb
    <% cache notification do %>
      <%# ... %>
    <% end %>
    ```

    *Kasper Timm Hansen*

*   Fixed a dependency tracker bug that caused template dependencies not
    count layouts as dependencies for partials.

    *Juho Leinonen*

*   Extracted `ActionView::Helpers::RecordTagHelper` to external gem
    (`record_tag_helper`) and added removal notices.

    *Todd Bealmear*

*   Allow to pass a string value to `size` option in `image_tag` and `video_tag`.

    This makes the behavior more consistent with `width` or `height` options.

    *Mehdi Lahmam*

*   Partial template name does no more have to be a valid Ruby identifier.

    There used to be a naming rule that the partial name should start with
    underscore, and should be followed by any combination of letters, numbers
    and underscores.
    But now we can give our partials any name starting with underscore, such as
    _🍔.html.erb.

    *Akira Matsuda*

*   Change the default template handler from `ERB` to `Raw`.

    Files without a template handler in their extension will be rendered using the raw
    handler instead of ERB.

    *Rafael Mendonça França*

*   Remove deprecated `AbstractController::Base::parent_prefixes`.

    *Rafael Mendonça França*

*   Default translations that have a lower precedence than a html safe default,
    but are not themselves safe, should not be marked as html_safe.

    *Justin Coyne*

*   Make possible to use blocks with short version of `render "partial"` helper.

    *Nikolay Shebanov*

*   Add a `hidden_field` on the `file_field` to avoid raise a error when the only
    input on the form is the `file_field`.

    *Mauro George*

*   Add an explicit error message, in `ActionView::PartialRenderer` for partial
    `rendering`, when the value of option `as` has invalid characters.

    *Angelo Capilleri*

*   Allow entries without a link tag in `AtomFeedHelper`.

    *Daniel Gomez de Souza*

Please check [4-2-stable](https://github.com/rails/rails/blob/4-2-stable/actionview/CHANGELOG.md) for previous changes.
