require 'active_support/core_ext/string/strip'

module ActiveRecord
  module ConnectionAdapters
    class AbstractAdapter
      class SchemaCreation # :nodoc:
        def initialize(conn)
          @conn = conn
          @cache = {}
        end

        def accept(o)
          m = @cache[o.class] ||= "visit_#{o.class.name.split('::').last}"
          send m, o
        end

        delegate :quote_column_name, :quote_table_name, :quote_default_expression, :type_to_sql, to: :@conn
        private :quote_column_name, :quote_table_name, :quote_default_expression, :type_to_sql

        private

          def visit_AlterTable(o)
            sql = "ALTER TABLE #{quote_table_name(o.name)} "
            sql << o.adds.map { |col| accept col }.join(' ')
            sql << o.foreign_key_adds.map { |fk| visit_AddForeignKey fk }.join(' ')
            sql << o.foreign_key_drops.map { |fk| visit_DropForeignKey fk }.join(' ')
          end

          def visit_ColumnDefinition(o)
            o.sql_type ||= type_to_sql(o.type, o.limit, o.precision, o.scale)
            column_sql = "#{quote_column_name(o.name)} #{o.sql_type}"
            add_column_options!(column_sql, column_options(o)) unless o.type == :primary_key
            column_sql
          end

          def visit_AddColumnDefinition(o)
            "ADD #{accept(o.column)}"
          end

          def visit_TableDefinition(o)
            create_sql = "CREATE#{' TEMPORARY' if o.temporary} TABLE "
            create_sql << "#{quote_table_name(o.name)} "
            create_sql << "(#{o.columns.map { |c| accept c }.join(', ')}) " unless o.as
            create_sql << "#{o.options}"
            create_sql << " AS #{@conn.to_sql(o.as)}" if o.as
            create_sql
          end

          def visit_AddForeignKey(o)
            sql = <<-SQL.strip_heredoc
              ADD CONSTRAINT #{quote_column_name(o.name)}
              FOREIGN KEY (#{quote_column_name(o.column)})
                REFERENCES #{quote_table_name(o.to_table)} (#{quote_column_name(o.primary_key)})
            SQL
            sql << " #{action_sql('DELETE', o.on_delete)}" if o.on_delete
            sql << " #{action_sql('UPDATE', o.on_update)}" if o.on_update
            sql
          end

          def visit_DropForeignKey(name)
            "DROP CONSTRAINT #{quote_column_name(name)}"
          end

          def column_options(o)
            column_options = {}
            column_options[:null] = o.null unless o.null.nil?
            column_options[:default] = o.default unless o.default.nil?
            column_options[:column] = o
            column_options[:first] = o.first
            column_options[:after] = o.after
            column_options[:auto_increment] = o.auto_increment
            column_options[:primary_key] = o.primary_key
            column_options[:collation] = o.collation
            column_options
          end

          def add_column_options!(sql, options)
            sql << " DEFAULT #{quote_default_expression(options[:default], options[:column])}" if options_include_default?(options)
            # must explicitly check for :null to allow change_column to work on migrations
            if options[:null] == false
              sql << " NOT NULL"
            end
            if options[:auto_increment] == true
              sql << " AUTO_INCREMENT"
            end
            if options[:primary_key] == true
              sql << " PRIMARY KEY"
            end
            sql
          end

          def options_include_default?(options)
            options.include?(:default) && !(options[:null] == false && options[:default].nil?)
          end

          def action_sql(action, dependency)
            case dependency
            when :nullify then "ON #{action} SET NULL"
            when :cascade  then "ON #{action} CASCADE"
            when :restrict then "ON #{action} RESTRICT"
            else
              raise ArgumentError, <<-MSG.strip_heredoc
                '#{dependency}' is not supported for :on_update or :on_delete.
                Supported values are: :nullify, :cascade, :restrict
              MSG
            end
          end
      end
    end
  end
end
