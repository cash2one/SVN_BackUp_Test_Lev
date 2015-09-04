require 'active_support/core_ext/class/subclasses'
require 'active_support/core_ext/hash/keys'

module ActiveJob
  # Provides helper methods for testing Active Job
  module TestHelper
    extend ActiveSupport::Concern

    included do
      def before_setup # :nodoc:
        test_adapter = ActiveJob::QueueAdapters::TestAdapter.new

        @old_queue_adapters = (ActiveJob::Base.subclasses << ActiveJob::Base).select do |klass|
          # only override explicitly set adapters, a quirk of `class_attribute`
          klass.singleton_class.public_instance_methods(false).include?(:_queue_adapter)
        end.map do |klass|
          [klass, klass.queue_adapter].tap do
            klass.queue_adapter = test_adapter
          end
        end

        clear_enqueued_jobs
        clear_performed_jobs
        super
      end

      def after_teardown # :nodoc:
        super
        @old_queue_adapters.each do |(klass, adapter)|
          klass.queue_adapter = adapter
        end
      end

      # Asserts that the number of enqueued jobs matches the given number.
      #
      #   def test_jobs
      #     assert_enqueued_jobs 0
      #     HelloJob.perform_later('david')
      #     assert_enqueued_jobs 1
      #     HelloJob.perform_later('abdelkader')
      #     assert_enqueued_jobs 2
      #   end
      #
      # If a block is passed, that block should cause the specified number of
      # jobs to be enqueued.
      #
      #   def test_jobs_again
      #     assert_enqueued_jobs 1 do
      #       HelloJob.perform_later('cristian')
      #     end
      #
      #     assert_enqueued_jobs 2 do
      #       HelloJob.perform_later('aaron')
      #       HelloJob.perform_later('rafael')
      #     end
      #   end
      #
      # The number of times a specific job is enqueued can be asserted.
      #
      #   def test_logging_job
      #     assert_enqueued_jobs 2, only: LoggingJob do
      #       LoggingJob.perform_later
      #       HelloJob.perform_later('jeremy')
      #     end
      #   end
      def assert_enqueued_jobs(number, only: nil)
        if block_given?
          original_count = enqueued_jobs_size(only: only)
          yield
          new_count = enqueued_jobs_size(only: only)
          assert_equal number, new_count - original_count, "#{number} jobs expected, but #{new_count - original_count} were enqueued"
        else
          actual_count = enqueued_jobs_size(only: only)
          assert_equal number, actual_count, "#{number} jobs expected, but #{actual_count} were enqueued"
        end
      end

      # Asserts that no jobs have been enqueued.
      #
      #   def test_jobs
      #     assert_no_enqueued_jobs
      #     HelloJob.perform_later('jeremy')
      #     assert_enqueued_jobs 1
      #   end
      #
      # If a block is passed, that block should not cause any job to be enqueued.
      #
      #   def test_jobs_again
      #     assert_no_enqueued_jobs do
      #       # No job should be enqueued from this block
      #     end
      #   end
      #
      # It can be asserted that no jobs of a specific kind are enqueued:
      #
      #   def test_no_logging
      #     assert_no_enqueued_jobs only: LoggingJob do
      #       HelloJob.perform_later('jeremy')
      #     end
      #   end
      #
      # Note: This assertion is simply a shortcut for:
      #
      #   assert_enqueued_jobs 0, &block
      def assert_no_enqueued_jobs(only: nil, &block)
        assert_enqueued_jobs 0, only: only, &block
      end

      # Asserts that the number of performed jobs matches the given number.
      # If no block is passed, <tt>perform_enqueued_jobs</tt>
      # must be called around the job call.
      #
      #   def test_jobs
      #     assert_performed_jobs 0
      #
      #     perform_enqueued_jobs do
      #       HelloJob.perform_later('xavier')
      #     end
      #     assert_performed_jobs 1
      #
      #     perform_enqueued_jobs do
      #       HelloJob.perform_later('yves')
      #       assert_performed_jobs 2
      #     end
      #   end
      #
      # If a block is passed, that block should cause the specified number of
      # jobs to be performed.
      #
      #   def test_jobs_again
      #     assert_performed_jobs 1 do
      #       HelloJob.perform_later('robin')
      #     end
      #
      #     assert_performed_jobs 2 do
      #       HelloJob.perform_later('carlos')
      #       HelloJob.perform_later('sean')
      #     end
      #   end
      #
      # The block form supports filtering. If the :only option is specified,
      # then only the listed job(s) will be performed.
      #
      #     def test_hello_job
      #       assert_performed_jobs 1, only: HelloJob do
      #         HelloJob.perform_later('jeremy')
      #         LoggingJob.perform_later
      #       end
      #     end
      #
      # An array may also be specified, to support testing multiple jobs.
      #
      #     def test_hello_and_logging_jobs
      #       assert_nothing_raised do
      #         assert_performed_jobs 2, only: [HelloJob, LoggingJob] do
      #           HelloJob.perform_later('jeremy')
      #           LoggingJob.perform_later('stewie')
      #           RescueJob.perform_later('david')
      #         end
      #       end
      #     end
      def assert_performed_jobs(number, only: nil)
        if block_given?
          original_count = performed_jobs.size
          perform_enqueued_jobs(only: only) { yield }
          new_count = performed_jobs.size
          assert_equal number, new_count - original_count,
                       "#{number} jobs expected, but #{new_count - original_count} were performed"
        else
          performed_jobs_size = performed_jobs.size
          assert_equal number, performed_jobs_size, "#{number} jobs expected, but #{performed_jobs_size} were performed"
        end
      end

      # Asserts that no jobs have been performed.
      #
      #   def test_jobs
      #     assert_no_performed_jobs
      #
      #     perform_enqueued_jobs do
      #       HelloJob.perform_later('matthew')
      #       assert_performed_jobs 1
      #     end
      #   end
      #
      # If a block is passed, that block should not cause any job to be performed.
      #
      #   def test_jobs_again
      #     assert_no_performed_jobs do
      #       # No job should be performed from this block
      #     end
      #   end
      #
      # The block form supports filtering. If the :only option is specified,
      # then only the listed job(s) will be performed.
      #
      #     def test_hello_job
      #       assert_performed_jobs 1, only: HelloJob do
      #         HelloJob.perform_later('jeremy')
      #         LoggingJob.perform_later
      #       end
      #     end
      #
      # An array may also be specified, to support testing multiple jobs.
      #
      #     def test_hello_and_logging_jobs
      #       assert_nothing_raised do
      #         assert_performed_jobs 2, only: [HelloJob, LoggingJob] do
      #           HelloJob.perform_later('jeremy')
      #           LoggingJob.perform_later('stewie')
      #           RescueJob.perform_later('david')
      #         end
      #       end
      #     end
      #
      # Note: This assertion is simply a shortcut for:
      #
      #   assert_performed_jobs 0, &block
      def assert_no_performed_jobs(only: nil, &block)
        assert_performed_jobs 0, only: only, &block
      end

      # Asserts that the job passed in the block has been enqueued with the given arguments.
      #
      #   def test_assert_enqueued_with
      #     assert_enqueued_with(job: MyJob, args: [1,2,3], queue: 'low') do
      #       MyJob.perform_later(1,2,3)
      #     end
      #   end
      def assert_enqueued_with(args = {}, &_block)
        original_enqueued_jobs = enqueued_jobs.dup
        clear_enqueued_jobs
        args.assert_valid_keys(:job, :args, :at, :queue)
        serialized_args = serialize_args_for_assertion(args)
        yield
        matching_job = enqueued_jobs.find do |job|
          serialized_args.all? { |key, value| value == job[key] }
        end
        assert matching_job, "No enqueued job found with #{args}"
        instantiate_job(matching_job)
      ensure
        queue_adapter.enqueued_jobs = original_enqueued_jobs + enqueued_jobs
      end

      # Asserts that the job passed in the block has been performed with the given arguments.
      #
      #   def test_assert_performed_with
      #     assert_performed_with(job: MyJob, args: [1,2,3], queue: 'high') do
      #       MyJob.perform_later(1,2,3)
      #     end
      #   end
      def assert_performed_with(args = {}, &_block)
        original_performed_jobs = performed_jobs.dup
        clear_performed_jobs
        args.assert_valid_keys(:job, :args, :at, :queue)
        serialized_args = serialize_args_for_assertion(args)
        perform_enqueued_jobs { yield }
        matching_job = performed_jobs.find do |job|
          serialized_args.all? { |key, value| value == job[key] }
        end
        assert matching_job, "No performed job found with #{args}"
        instantiate_job(matching_job)
      ensure
        queue_adapter.performed_jobs = original_performed_jobs + performed_jobs
      end

      def perform_enqueued_jobs(only: nil)
        old_perform_enqueued_jobs = queue_adapter.perform_enqueued_jobs
        old_perform_enqueued_at_jobs = queue_adapter.perform_enqueued_at_jobs
        old_filter = queue_adapter.filter

        begin
          queue_adapter.perform_enqueued_jobs = true
          queue_adapter.perform_enqueued_at_jobs = true
          queue_adapter.filter = only
          yield
        ensure
          queue_adapter.perform_enqueued_jobs = old_perform_enqueued_jobs
          queue_adapter.perform_enqueued_at_jobs = old_perform_enqueued_at_jobs
          queue_adapter.filter = old_filter
        end
      end

      def queue_adapter
        ActiveJob::Base.queue_adapter
      end

      delegate :enqueued_jobs, :enqueued_jobs=,
               :performed_jobs, :performed_jobs=,
               to: :queue_adapter

      private
        def clear_enqueued_jobs
          enqueued_jobs.clear
        end

        def clear_performed_jobs
          performed_jobs.clear
        end

        def enqueued_jobs_size(only: nil)
          if only
            enqueued_jobs.select { |job| job.fetch(:job) == only }.size
          else
            enqueued_jobs.size
          end
        end

        def serialize_args_for_assertion(args)
          serialized_args = args.dup
          if job_args = serialized_args.delete(:args)
            serialized_args[:args] = ActiveJob::Arguments.serialize(job_args)
          end
          serialized_args
        end

        def instantiate_job(payload)
          job = payload[:job].new(*payload[:args])
          job.scheduled_at = Time.at(payload[:at]) if payload.key?(:at)
          job.queue_name = payload[:queue]
          job
        end
    end
  end
end
