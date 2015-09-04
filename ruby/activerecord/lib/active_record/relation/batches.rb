require "active_record/relation/batches/batch_enumerator"

module ActiveRecord
  module Batches
    # Looping through a collection of records from the database
    # (using the +all+ method, for example) is very inefficient
    # since it will try to instantiate all the objects at once.
    #
    # In that case, batch processing methods allow you to work
    # with the records in batches, thereby greatly reducing memory consumption.
    #
    # The #find_each method uses #find_in_batches with a batch size of 1000 (or as
    # specified by the +:batch_size+ option).
    #
    #   Person.find_each do |person|
    #     person.do_awesome_stuff
    #   end
    #
    #   Person.where("age > 21").find_each do |person|
    #     person.party_all_night!
    #   end
    #
    # If you do not provide a block to #find_each, it will return an Enumerator
    # for chaining with other methods:
    #
    #   Person.find_each.with_index do |person, index|
    #     person.award_trophy(index + 1)
    #   end
    #
    # ==== Options
    # * <tt>:batch_size</tt> - Specifies the size of the batch. Default to 1000.
    # * <tt>:begin_at</tt> - Specifies the primary key value to start from, inclusive of the value.
    # * <tt>:end_at</tt> - Specifies the primary key value to end at, inclusive of the value.
    # This is especially useful if you want multiple workers dealing with
    # the same processing queue. You can make worker 1 handle all the records
    # between id 0 and 10,000 and worker 2 handle from 10,000 and beyond
    # (by setting the +:begin_at+ and +:end_at+ option on each worker).
    #
    #   # Let's process for a batch of 2000 records, skipping the first 2000 rows
    #   Person.find_each(begin_at: 2000, batch_size: 2000) do |person|
    #     person.party_all_night!
    #   end
    #
    # NOTE: It's not possible to set the order. That is automatically set to
    # ascending on the primary key ("id ASC") to make the batch ordering
    # work. This also means that this method only works when the primary key is
    # orderable (e.g. an integer or string).
    #
    # NOTE: You can't set the limit either, that's used to control
    # the batch sizes.
    def find_each(begin_at: nil, end_at: nil, batch_size: 1000, start: nil)
      if start
        begin_at = start
        ActiveSupport::Deprecation.warn(<<-MSG.squish)
            Passing `start` value to find_each is deprecated, and will be removed in Rails 5.1.
            Please pass `begin_at` instead.
        MSG
      end
      if block_given?
        find_in_batches(begin_at: begin_at, end_at: end_at, batch_size: batch_size) do |records|
          records.each { |record| yield record }
        end
      else
        enum_for(:find_each, begin_at: begin_at, end_at: end_at, batch_size: batch_size) do
          relation = self
          apply_limits(relation, begin_at, end_at).size
        end
      end
    end

    # Yields each batch of records that was found by the find options as
    # an array.
    #
    #   Person.where("age > 21").find_in_batches do |group|
    #     sleep(50) # Make sure it doesn't get too crowded in there!
    #     group.each { |person| person.party_all_night! }
    #   end
    #
    # If you do not provide a block to #find_in_batches, it will return an Enumerator
    # for chaining with other methods:
    #
    #   Person.find_in_batches.with_index do |group, batch|
    #     puts "Processing group ##{batch}"
    #     group.each(&:recover_from_last_night!)
    #   end
    #
    # To be yielded each record one by one, use #find_each instead.
    #
    # ==== Options
    # * <tt>:batch_size</tt> - Specifies the size of the batch. Default to 1000.
    # * <tt>:begin_at</tt> - Specifies the primary key value to start from, inclusive of the value.
    # * <tt>:end_at</tt> - Specifies the primary key value to end at, inclusive of the value.
    # This is especially useful if you want multiple workers dealing with
    # the same processing queue. You can make worker 1 handle all the records
    # between id 0 and 10,000 and worker 2 handle from 10,000 and beyond
    # (by setting the +:begin_at+ and +:end_at+ option on each worker).
    #
    #   # Let's process the next 2000 records
    #   Person.find_in_batches(begin_at: 2000, batch_size: 2000) do |group|
    #     group.each { |person| person.party_all_night! }
    #   end
    #
    # NOTE: It's not possible to set the order. That is automatically set to
    # ascending on the primary key ("id ASC") to make the batch ordering
    # work. This also means that this method only works when the primary key is
    # orderable (e.g. an integer or string).
    #
    # NOTE: You can't set the limit either, that's used to control
    # the batch sizes.
    def find_in_batches(begin_at: nil, end_at: nil, batch_size: 1000, start: nil)
      if start
        begin_at = start
        ActiveSupport::Deprecation.warn(<<-MSG.squish)
            Passing `start` value to find_in_batches is deprecated, and will be removed in Rails 5.1.
            Please pass `begin_at` instead.
        MSG
      end

      relation = self
      unless block_given?
        return to_enum(:find_in_batches, begin_at: begin_at, end_at: end_at, batch_size: batch_size) do
          total = apply_limits(relation, begin_at, end_at).size
          (total - 1).div(batch_size) + 1
        end
      end

      in_batches(of: batch_size, begin_at: begin_at, end_at: end_at, load: true) do |batch|
        yield batch.to_a
      end
    end

    # Yields ActiveRecord::Relation objects to work with a batch of records.
    #
    #   Person.where("age > 21").in_batches do |relation|
    #     relation.delete_all
    #     sleep(10) # Throttle the delete queries
    #   end
    #
    # If you do not provide a block to #in_batches, it will return a
    # BatchEnumerator which is enumerable.
    #
    #   Person.in_batches.with_index do |relation, batch_index|
    #     puts "Processing relation ##{batch_index}"
    #     relation.each { |relation| relation.delete_all }
    #   end
    #
    # Examples of calling methods on the returned BatchEnumerator object:
    #
    #   Person.in_batches.delete_all
    #   Person.in_batches.update_all(awesome: true)
    #   Person.in_batches.each_record(&:party_all_night!)
    #
    # ==== Options
    # * <tt>:of</tt> - Specifies the size of the batch. Default to 1000.
    # * <tt>:load</tt> - Specifies if the relation should be loaded. Default to false.
    # * <tt>:begin_at</tt> - Specifies the primary key value to start from, inclusive of the value.
    # * <tt>:end_at</tt> - Specifies the primary key value to end at, inclusive of the value.
    #
    # This is especially useful if you want to work with the
    # ActiveRecord::Relation object instead of the array of records, or if
    # you want multiple workers dealing with the same processing queue. You can
    # make worker 1 handle all the records between id 0 and 10,000 and worker 2
    # handle from 10,000 and beyond (by setting the +:begin_at+ and +:end_at+
    # option on each worker).
    #
    #   # Let's process the next 2000 records
    #   Person.in_batches(of: 2000, begin_at: 2000).update_all(awesome: true)
    #
    # An example of calling where query method on the relation:
    #
    #   Person.in_batches.each do |relation|
    #     relation.update_all('age = age + 1')
    #     relation.where('age > 21').update_all(should_party: true)
    #     relation.where('age <= 21').delete_all
    #   end
    #
    # NOTE: If you are going to iterate through each record, you should call
    # #each_record on the yielded BatchEnumerator:
    #
    #   Person.in_batches.each_record(&:party_all_night!)
    #
    # NOTE: It's not possible to set the order. That is automatically set to
    # ascending on the primary key ("id ASC") to make the batch ordering
    # consistent. Therefore the primary key must be orderable, e.g an integer
    # or a string.
    #
    # NOTE: You can't set the limit either, that's used to control the batch
    # sizes.
    def in_batches(of: 1000, begin_at: nil, end_at: nil, load: false)
      relation = self
      unless block_given?
        return BatchEnumerator.new(of: of, begin_at: begin_at, end_at: end_at, relation: self)
      end

      if logger && (arel.orders.present? || arel.taken.present?)
        logger.warn("Scoped order and limit are ignored, it's forced to be batch order and batch size")
      end

      relation = relation.reorder(batch_order).limit(of)
      relation = apply_limits(relation, begin_at, end_at)
      batch_relation = relation

      loop do
        if load
          records = batch_relation.to_a
          ids = records.map(&:id)
          yielded_relation = self.where(primary_key => ids)
          yielded_relation.load_records(records)
        else
          ids = batch_relation.pluck(primary_key)
          yielded_relation = self.where(primary_key => ids)
        end

        break if ids.empty?

        primary_key_offset = ids.last
        raise ArgumentError.new("Primary key not included in the custom select clause") unless primary_key_offset

        yield yielded_relation

        break if ids.length < of
        batch_relation = relation.where(table[primary_key].gt(primary_key_offset))
      end
    end

    private

    def apply_limits(relation, begin_at, end_at)
      relation = relation.where(table[primary_key].gteq(begin_at)) if begin_at
      relation = relation.where(table[primary_key].lteq(end_at)) if end_at
      relation
    end

    def batch_order
      "#{quoted_table_name}.#{quoted_primary_key} ASC"
    end
  end
end
