require 'method_source'

module ActiveSupport
  module Testing
    class CompositeFilter # :nodoc:
      def initialize(runnable, filter, patterns)
        @runnable = runnable
        @filters = [ derive_regexp(filter), *derive_line_filters(patterns) ].compact
      end

      def ===(method)
        @filters.any? { |filter| filter === method }
      end

      private
        def derive_regexp(filter)
          filter =~ %r%/(.*)/% ? Regexp.new($1) : filter
        end

        def derive_line_filters(patterns)
          patterns.map do |file_and_line|
            file, line = file_and_line.split(':')
            Filter.new(@runnable, file, line) if file
          end
        end

        class Filter # :nodoc:
          def initialize(runnable, file, line)
            @runnable, @file = runnable, File.expand_path(file)
            @line = line.to_i if line
          end

          def ===(method)
            return unless @runnable.method_defined?(method)

            if @line
              test_file, test_range = definition_for(@runnable.instance_method(method))
              test_file == @file && test_range.include?(@line)
            else
              @runnable.instance_method(method).source_location.first == @file
            end
          end

          private
            def definition_for(method)
              file, start_line = method.source_location
              end_line = method.source.count("\n") + start_line - 1

              return file, start_line..end_line
            end
        end
    end
  end
end
