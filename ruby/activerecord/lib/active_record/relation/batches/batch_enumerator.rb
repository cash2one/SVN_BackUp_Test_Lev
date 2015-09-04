module ActiveRecord
  module Batches
    class BatchEnumerator
      include Enumerable

      def initialize(of: 1000, begin_at: nil, end_at: nil, relation:) #:nodoc:
        @of       = of
        @relation = relation
        @begin_at = begin_at
        @end_at   = end_at
      end

      # Looping through a collection of records from the database (using the
      # +all+ method, for example) is very inefficient since it will try to
      # instantiate all the objects at once.
      #
      # In that case, batch processing methods allow you to work with the
      # records in batches, thereby greatly reducing memory consumption.
      #
      #   Person.in_batches.each_record do |person|
      #     person.do_awesome_stuff
      #   end
      #
      #   Person.where("age > 21").in_batches(of: 10).each_record do |person|
      #     person.party_all_night!
      #   end
      #
      # If you do not provide a block to #each_record, it will return an Enumerator
      # for chaining with other methods:
      #
      #   Person.in_batches.each_record.with_index do |person, index|
      #     person.award_trophy(index + 1)
      #   end
      def each_record
        return to_enum(:each_record) unless block_given?

        @relation.to_enum(:in_batches, of: @of, begin_at: @begin_at, end_at: @end_at, load: true).each do |relation|
          relation.to_a.each { |record| yield record }
        end
      end

      # Delegates #delete_all, #update_all, #destroy_all methods to each batch.
      #
      #   People.in_batches.delete_all
      #   People.in_batches.destroy_all('age < 10')
      #   People.in_batches.update_all('age = age + 1')
      [:delete_all, :update_all, :destroy_all].each do |method|
        define_method(method) do |*args, &block|
          @relation.to_enum(:in_batches, of: @of, begin_at: @begin_at, end_at: @end_at, load: false).each do |relation|
            relation.send(method, *args, &block)
          end
        end
      end

      # Yields an ActiveRecord::Relation object for each batch of records.
      #
      #   Person.in_batches.each do |relation|
      #     relation.update_all(awesome: true)
      #   end
      def each
        enum = @relation.to_enum(:in_batches, of: @of, begin_at: @begin_at, end_at: @end_at, load: false)
        return enum.each { |relation| yield relation } if block_given?
        enum
      end
    end
  end
end
