require 'active_record/type/helpers'
require 'active_record/type/value'

require 'active_record/type/big_integer'
require 'active_record/type/binary'
require 'active_record/type/boolean'
require 'active_record/type/date'
require 'active_record/type/date_time'
require 'active_record/type/decimal'
require 'active_record/type/decimal_without_scale'
require 'active_record/type/float'
require 'active_record/type/integer'
require 'active_record/type/serialized'
require 'active_record/type/string'
require 'active_record/type/text'
require 'active_record/type/time'
require 'active_record/type/unsigned_integer'

require 'active_record/type/adapter_specific_registry'
require 'active_record/type/type_map'
require 'active_record/type/hash_lookup_type_map'

require 'active_record/type/internal/abstract_json'

module ActiveRecord
  module Type
    @registry = AdapterSpecificRegistry.new

    class << self
      attr_accessor :registry # :nodoc:
      delegate :add_modifier, to: :registry

      # Add a new type to the registry, allowing it to be referenced as a
      # symbol by ActiveRecord::Attributes::ClassMethods#attribute.  If your
      # type is only meant to be used with a specific database adapter, you can
      # do so by passing +adapter: :postgresql+. If your type has the same
      # name as a native type for the current adapter, an exception will be
      # raised unless you specify an +:override+ option. +override: true+ will
      # cause your type to be used instead of the native type. +override:
      # false+ will cause the native type to be used over yours if one exists.
      def register(type_name, klass = nil, **options, &block)
        registry.register(type_name, klass, **options, &block)
      end

      def lookup(*args, adapter: current_adapter_name, **kwargs) # :nodoc:
        registry.lookup(*args, adapter: adapter, **kwargs)
      end

      private

      def current_adapter_name
        ActiveRecord::Base.connection.adapter_name.downcase.to_sym
      end
    end

    register(:big_integer, Type::BigInteger, override: false)
    register(:binary, Type::Binary, override: false)
    register(:boolean, Type::Boolean, override: false)
    register(:date, Type::Date, override: false)
    register(:date_time, Type::DateTime, override: false)
    register(:decimal, Type::Decimal, override: false)
    register(:float, Type::Float, override: false)
    register(:integer, Type::Integer, override: false)
    register(:string, Type::String, override: false)
    register(:text, Type::Text, override: false)
    register(:time, Type::Time, override: false)
  end
end
