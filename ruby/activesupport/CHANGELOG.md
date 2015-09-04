*   Updated Unicode version to 8.0.0

    *Anshul Sharma*
    
*   `number_to_currency` and `number_with_delimiter` now accept custom `delimiter_pattern` option
     to handle placement of delimiter, to support currency formats like INR

     Example:

        number_to_currency(1230000, delimiter_pattern: /(\d+?)(?=(\d\d)+(\d)(?!\d))/, unit: '₹', format: "%u %n")
        # => '₹ 12,30,000.00'

    *Vipul A M*

*   Deprecate `:prefix` option of `number_to_human_size` with no replacement.

    *Jean Boussier*

*   Fix `TimeWithZone#eql?` to properly handle `TimeWithZone` created from `DateTime`:
        twz = DateTime.now.in_time_zone
        twz.eql?(twz.dup) => true

    Fixes #14178.

    *Roque Pinel*

*   ActiveSupport::HashWithIndifferentAccess `select` and `reject` will now return
    enumerator if called without block.

    Fixes #20095

    *Bernard Potocki*

*   Removed `ActiveSupport::Concurrency::Latch`, superseded by `Concurrent::CountDownLatch`
    from the concurrent-ruby gem.

    *Jerry D'Antonio*

*   Fix not calling `#default` on `HashWithIndifferentAccess#to_hash` when only
    `default_proc` is set, which could raise.

    *Simon Eskildsen*

*   Fix setting `default_proc` on `HashWithIndifferentAccess#dup`

    *Simon Eskildsen*

*   Fix a range of values for parameters of the Time#change

    *Nikolay Kondratyev*

*   Add `Enumerable#pluck` to get the same values from arrays as from ActiveRecord
    associations.

    Fixes #20339.

    *Kevin Deisz*

*   Add a bang version to `ActiveSupport::OrderedOptions` get methods which will raise
    an `KeyError` if the value is `.blank?`

    Before:

        if (slack_url = Rails.application.secrets.slack_url).present?
          # Do something worthwhile
        else
          # Raise as important secret password is not specified
        end

    After:

        slack_url = Rails.application.secrets.slack_url!

    *Aditya Sanghi*, *Gaurish Sharma*

*   Remove deprecated `Class#superclass_delegating_accessor`.
    Use `Class#class_attribute` instead.

    *Akshay Vishnoi*

*   Patch `Delegator` to work with `#try`.

    Fixes #5790.

    *Nate Smith*

*   Add `Integer#positive?` and `Integer#negative?` query methods
    in the vein of `Fixnum#zero?`.

    This makes it nicer to do things like `bunch_of_numbers.select(&:positive?)`.

    *DHH*

*   Encoding `ActiveSupport::TimeWithZone` to YAML now preserves the timezone information.

    Fixes #9183.

    *Andrew White*

*   Added `ActiveSupport::TimeZone#strptime` to allow parsing times as if
    from a given timezone.

    *Paul A Jungwirth*

*   `ActiveSupport::Callbacks#skip_callback` now raises an `ArgumentError` if
    an unrecognized callback is removed.

    *Iain Beeston*

*   Added `ActiveSupport::ArrayInquirer` and `Array#inquiry`.

    Wrapping an array in an `ArrayInquirer` gives a friendlier way to check its
    contents:

        variants = ActiveSupport::ArrayInquirer.new([:phone, :tablet])

        variants.phone?    # => true
        variants.tablet?   # => true
        variants.desktop?  # => false

        variants.any?(:phone, :tablet)   # => true
        variants.any?(:phone, :desktop)  # => true
        variants.any?(:desktop, :watch)  # => false

    `Array#inquiry` is a shortcut for wrapping the receiving array in an
    `ArrayInquirer`.

    *George Claghorn*

*   Deprecate `alias_method_chain` in favour of `Module#prepend` introduced in
    Ruby 2.0.

    *Kir Shatrov*

*   Added `#without` on `Enumerable` and `Array` to return a copy of an
    enumerable without the specified elements.

    *Todd Bealmear*

*   Fixed a problem where `String#truncate_words` would get stuck with a complex
    string.

    *Henrik Nygren*

*   Fixed a roundtrip problem with `AS::SafeBuffer` where primitive-like strings
    will be dumped as primitives:

    Before:

        YAML.load ActiveSupport::SafeBuffer.new("Hello").to_yaml  # => "Hello"
        YAML.load ActiveSupport::SafeBuffer.new("true").to_yaml   # => true
        YAML.load ActiveSupport::SafeBuffer.new("false").to_yaml  # => false
        YAML.load ActiveSupport::SafeBuffer.new("1").to_yaml      # => 1
        YAML.load ActiveSupport::SafeBuffer.new("1.1").to_yaml    # => 1.1

    After:

        YAML.load ActiveSupport::SafeBuffer.new("Hello").to_yaml  # => "Hello"
        YAML.load ActiveSupport::SafeBuffer.new("true").to_yaml   # => "true"
        YAML.load ActiveSupport::SafeBuffer.new("false").to_yaml  # => "false"
        YAML.load ActiveSupport::SafeBuffer.new("1").to_yaml      # => "1"
        YAML.load ActiveSupport::SafeBuffer.new("1.1").to_yaml    # => "1.1"

    *Godfrey Chan*

*   Enable `number_to_percentage` to keep the number's precision by allowing
    `:precision` to be `nil`.

    *Jack Xu*

*   `config_accessor` became a private method, as with Ruby's `attr_accessor`.

    *Akira Matsuda*

*   `AS::Testing::TimeHelpers#travel_to` now changes `DateTime.now` as well as
    `Time.now` and `Date.today`.

    *Yuki Nishijima*

*   Add `file_fixture` to `ActiveSupport::TestCase`.
    It provides a simple mechanism to access sample files in your test cases.

    By default file fixtures are stored in `test/fixtures/files`. This can be
    configured per test-case using the `file_fixture_path` class attribute.

    *Yves Senn*

*   Return value of yielded block in `File.atomic_write`.

    *Ian Ker-Seymer*

*   Duplicate frozen array when assigning it to a `HashWithIndifferentAccess` so
    that it doesn't raise a `RuntimeError` when calling `map!` on it in `convert_value`.

    Fixes #18550.

    *Aditya Kapoor*

*   Add missing time zone definitions for Russian Federation and sync them
    with `zone.tab` file from tzdata version 2014j (latest).

    *Andrey Novikov*

*   Add `SecureRandom.base58` for generation of random base58 strings.

    *Matthew Draper*, *Guillermo Iguaran*

*   Add `#prev_day` and `#next_day` counterparts to `#yesterday` and
    `#tomorrow` for `Date`, `Time`, and `DateTime`.

    *George Claghorn*

*   Add `same_time` option to `#next_week` and `#prev_week` for `Date`, `Time`,
    and `DateTime`.

    *George Claghorn*

*   Add `#on_weekend?`, `#next_weekday`, `#prev_weekday` methods to `Date`,
    `Time`, and `DateTime`.

    `#on_weekend?` returns `true` if the receiving date/time falls on a Saturday
    or Sunday.

    `#next_weekday` returns a new date/time representing the next day that does
    not fall on a Saturday or Sunday.

    `#prev_weekday` returns a new date/time representing the previous day that
    does not fall on a Saturday or Sunday.

    *George Claghorn*

*   Change the default test order from `:sorted` to `:random`.

    *Rafael Mendonça França*

*   Remove deprecated `ActiveSupport::JSON::Encoding::CircularReferenceError`.

    *Rafael Mendonça França*

*   Remove deprecated methods `ActiveSupport::JSON::Encoding.encode_big_decimal_as_string=`
    and `ActiveSupport::JSON::Encoding.encode_big_decimal_as_string`.

    *Rafael Mendonça França*

*   Remove deprecated `ActiveSupport::SafeBuffer#prepend`.

    *Rafael Mendonça França*

*   Remove deprecated methods at `Kernel`.

    `silence_stderr`, `silence_stream`, `capture` and `quietly`.

    *Rafael Mendonça França*

*   Remove deprecated `active_support/core_ext/big_decimal/yaml_conversions`
    file.

    *Rafael Mendonça França*

*   Remove deprecated methods `ActiveSupport::Cache::Store.instrument` and
    `ActiveSupport::Cache::Store.instrument=`.

    *Rafael Mendonça França*

*   Change the way in which callback chains can be halted.

    The preferred method to halt a callback chain from now on is to explicitly
    `throw(:abort)`.
    In the past, returning `false` in an ActiveSupport callback had the side
    effect of halting the callback chain. This is not recommended anymore and,
    depending on the value of
    `Callbacks::CallbackChain.halt_and_display_warning_on_return_false`, will
    either not work at all or display a deprecation warning.

*   Add `Callbacks::CallbackChain.halt_and_display_warning_on_return_false`

    Setting `Callbacks::CallbackChain.halt_and_display_warning_on_return_false`
    to `true` will let an app support the deprecated way of halting callback
    chains by returning `false`.

    Setting the value to `false` will tell the app to ignore any `false` value
    returned by callbacks, and only halt the chain upon `throw(:abort)`.

    The value can also be set with the Rails configuration option
    `config.active_support.halt_callback_chains_on_return_false`.

    When the configuration option is missing, its value is `true`, so older apps
    ported to Rails 5.0 will not break (but display a deprecation warning).
    For new Rails 5.0 apps, its value is set to `false` in an initializer, so
    these apps will support the new behavior by default.

    *claudiob*

*   Changes arguments and default value of CallbackChain's `:terminator` option

    Chains of callbacks defined without an explicit `:terminator` option will
    now be halted as soon as a `before_` callback throws `:abort`.

    Chains of callbacks defined with a `:terminator` option will maintain their
    existing behavior of halting as soon as a `before_` callback matches the
    terminator's expectation.

    *claudiob*

*   Deprecate `MissingSourceFile` in favor of `LoadError`.

    `MissingSourceFile` was just an alias to `LoadError` and was not being
    raised inside the framework.

    *Rafael Mendonça França*

*   Add support for error dispatcher classes in `ActiveSupport::Rescuable`.
    Now it acts closer to Ruby's rescue.

    Example:

        class BaseController < ApplicationController
          module ErrorDispatcher
            def self.===(other)
              Exception === other && other.respond_to?(:status)
            end
          end

          rescue_from ErrorDispatcher do |error|
            render status: error.status, json: { error: error.to_s }
          end
        end

    *Genadi Samokovarov*

*   Add `#verified` and `#valid_message?` methods to `ActiveSupport::MessageVerifier`

    Previously, the only way to decode a message with `ActiveSupport::MessageVerifier`
    was to use `#verify`, which would raise an exception on invalid messages. Now
    `#verified` can also be used, which returns `nil` on messages that cannot be
    decoded.

    Previously, there was no way to check if a message's format was valid without
    attempting to decode it. `#valid_message?` is a boolean convenience method that
    checks whether the message is valid without actually decoding it.

    *Logan Leger*

Please check [4-2-stable](https://github.com/rails/rails/blob/4-2-stable/activesupport/CHANGELOG.md) for previous changes.
