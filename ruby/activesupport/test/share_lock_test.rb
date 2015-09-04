require 'abstract_unit'
require 'concurrent/atomics'
require 'active_support/concurrency/share_lock'

class ShareLockTest < ActiveSupport::TestCase
  def setup
    @lock = ActiveSupport::Concurrency::ShareLock.new
  end

  def test_reentrancy
    thread = Thread.new do
      @lock.sharing   { @lock.sharing   {} }
      @lock.exclusive { @lock.exclusive {} }
    end
    assert_threads_not_stuck thread
  end

  def test_sharing_doesnt_block
    with_thread_waiting_in_lock_section(:sharing) do |sharing_thread_latch|
      assert_threads_not_stuck(Thread.new {@lock.sharing {} })
    end
  end

  def test_sharing_blocks_exclusive
    with_thread_waiting_in_lock_section(:sharing) do |sharing_thread_release_latch|
      @lock.exclusive(no_wait: true) { flunk } # polling should fail
      exclusive_thread = Thread.new { @lock.exclusive {} }
      assert_threads_stuck_but_releasable_by_latch exclusive_thread, sharing_thread_release_latch
    end
  end

  def test_exclusive_blocks_sharing
    with_thread_waiting_in_lock_section(:exclusive) do |exclusive_thread_release_latch|
      sharing_thread = Thread.new { @lock.sharing {} }
      assert_threads_stuck_but_releasable_by_latch sharing_thread, exclusive_thread_release_latch
    end
  end

  def test_multiple_exlusives_are_able_to_progress
    with_thread_waiting_in_lock_section(:sharing) do |sharing_thread_release_latch|
      exclusive_threads = (1..2).map do
        Thread.new do
          @lock.exclusive {}
        end
      end

      assert_threads_stuck_but_releasable_by_latch exclusive_threads, sharing_thread_release_latch
    end
  end

  def test_sharing_is_upgradeable_to_exclusive
    upgrading_thread = Thread.new do
      @lock.sharing do
        @lock.exclusive {}
      end
    end
    assert_threads_not_stuck upgrading_thread
  end

  def test_exclusive_upgrade_waits_for_other_sharers_to_leave
    with_thread_waiting_in_lock_section(:sharing) do |sharing_thread_release_latch|
      in_sharing = Concurrent::CountDownLatch.new

      upgrading_thread = Thread.new do
        @lock.sharing do
          in_sharing.count_down
          @lock.exclusive {}
        end
      end

      in_sharing.wait
      assert_threads_stuck_but_releasable_by_latch upgrading_thread, sharing_thread_release_latch
    end
  end

  def test_exclusive_matching_purpose
    [true, false].each do |use_upgrading|
      with_thread_waiting_in_lock_section(:sharing) do |sharing_thread_release_latch|
        exclusive_threads = (1..2).map do
          Thread.new do
            @lock.send(use_upgrading ? :sharing : :tap) do
              @lock.exclusive(purpose: :load, compatible: [:load, :unload]) {}
            end
          end
        end

        assert_threads_stuck_but_releasable_by_latch exclusive_threads, sharing_thread_release_latch
      end
    end
  end

  def test_killed_thread_loses_lock
    with_thread_waiting_in_lock_section(:sharing) do |sharing_thread_release_latch|
      thread = Thread.new do
        @lock.sharing do
          @lock.exclusive {}
        end
      end

      assert_threads_stuck thread
      thread.kill

      sharing_thread_release_latch.count_down

      thread = Thread.new do
        @lock.exclusive {}
      end

      assert_threads_not_stuck thread
    end
  end

  def test_exclusive_conflicting_purpose
    [true, false].each do |use_upgrading|
      with_thread_waiting_in_lock_section(:sharing) do |sharing_thread_release_latch|
        begin
          conflicting_exclusive_threads = [
            Thread.new do
              @lock.send(use_upgrading ? :sharing : :tap) do
                @lock.exclusive(purpose: :red, compatible: [:green, :purple]) {}
              end
            end,
            Thread.new do
              @lock.send(use_upgrading ? :sharing : :tap) do
                @lock.exclusive(purpose: :blue, compatible: [:green]) {}
              end
            end
          ]

          assert_threads_stuck conflicting_exclusive_threads # wait for threads to get into their respective `exclusive {}` blocks

          # This thread will be stuck as long as any other thread is in
          # a sharing block. While it's blocked, it holds no lock, so it
          # doesn't interfere with any other attempts.
          no_purpose_thread = Thread.new do
            @lock.exclusive {}
          end
          assert_threads_stuck no_purpose_thread

          # This thread is compatible with both of the "primary"
          # attempts above. It's initially stuck on the outer share
          # lock, but as soon as that's released, it can run --
          # regardless of whether those threads hold share locks.
          compatible_thread = Thread.new do
            @lock.exclusive(purpose: :green, compatible: []) {}
          end
          assert_threads_stuck compatible_thread

          assert_threads_stuck conflicting_exclusive_threads

          sharing_thread_release_latch.count_down

          assert_threads_not_stuck compatible_thread # compatible thread is now able to squeak through

          if use_upgrading
            # The "primary" threads both each hold a share lock, and are
            # mutually incompatible; they're still stuck.
            assert_threads_stuck conflicting_exclusive_threads

            # The thread without a specified purpose is also stuck; it's
            # not compatible with anything.
            assert_threads_stuck no_purpose_thread
          else
            # As the primaries didn't hold a share lock, as soon as the
            # outer one was released, all the exclusive locks are free
            # to be acquired in turn.

            assert_threads_not_stuck conflicting_exclusive_threads
            assert_threads_not_stuck no_purpose_thread
          end
        ensure
          conflicting_exclusive_threads.each(&:kill)
          no_purpose_thread.kill
        end
      end
    end
  end

  def test_exclusive_ordering
    scratch_pad       = []
    scratch_pad_mutex = Mutex.new

    load_params   = [:load,   [:load]]
    unload_params = [:unload, [:unload, :load]]

    [load_params, load_params, unload_params, unload_params].permutation do |thread_params|
      with_thread_waiting_in_lock_section(:sharing) do |sharing_thread_release_latch|
        threads = thread_params.map do |purpose, compatible|
          Thread.new do
            @lock.sharing do
              @lock.exclusive(purpose: purpose, compatible: compatible) do
                scratch_pad_mutex.synchronize { scratch_pad << purpose }
              end
            end
          end
        end

        sleep(0.01)
        scratch_pad_mutex.synchronize { assert_empty scratch_pad }

        sharing_thread_release_latch.count_down

        assert_threads_not_stuck threads
        scratch_pad_mutex.synchronize do
          assert_equal [:load, :load, :unload, :unload], scratch_pad
          scratch_pad.clear
        end
      end
    end
  end

  def test_in_shared_section_incompatible_non_upgrading_threads_cannot_preempt_upgrading_threads
    scratch_pad       = []
    scratch_pad_mutex = Mutex.new

    upgrading_load_params       = [:load,   [:load],          true]
    non_upgrading_unload_params = [:unload, [:load, :unload], false]

    [upgrading_load_params, non_upgrading_unload_params].permutation do |thread_params|
      with_thread_waiting_in_lock_section(:sharing) do |sharing_thread_release_latch|
        threads = thread_params.map do |purpose, compatible, use_upgrading|
          Thread.new do
            @lock.send(use_upgrading ? :sharing : :tap) do
              @lock.exclusive(purpose: purpose, compatible: compatible) do
                scratch_pad_mutex.synchronize { scratch_pad << purpose }
              end
            end
          end
        end

        assert_threads_stuck threads
        scratch_pad_mutex.synchronize { assert_empty scratch_pad }

        sharing_thread_release_latch.count_down

        assert_threads_not_stuck threads
        scratch_pad_mutex.synchronize do
          assert_equal [:load, :unload], scratch_pad
          scratch_pad.clear
        end
      end
    end
  end

  private

  module CustomAssertions
    SUFFICIENT_TIMEOUT = 0.2

    private

    def assert_threads_stuck_but_releasable_by_latch(threads, latch)
      assert_threads_stuck threads
      latch.count_down
      assert_threads_not_stuck threads
    end

    def assert_threads_stuck(threads)
      sleep(SUFFICIENT_TIMEOUT) # give threads time to do their business
      assert(Array(threads).all? { |t| t.join(0.001).nil? })
    end

    def assert_threads_not_stuck(threads)
      assert(Array(threads).all? { |t| t.join(SUFFICIENT_TIMEOUT) })
    end
  end

  class CustomAssertionsTest < ActiveSupport::TestCase
    include CustomAssertions

    def setup
      @latch = Concurrent::CountDownLatch.new
      @thread = Thread.new { @latch.wait }
    end

    def teardown
      @latch.count_down
      @thread.join
    end

    def test_happy_path
      assert_threads_stuck_but_releasable_by_latch @thread, @latch
    end

    def test_detects_stuck_thread
      assert_raises(Minitest::Assertion) do
        assert_threads_not_stuck @thread
      end
    end

    def test_detects_free_thread
      @latch.count_down
      assert_raises(Minitest::Assertion) do
        assert_threads_stuck @thread
      end
    end

    def test_detects_already_released
      @latch.count_down
      assert_raises(Minitest::Assertion) do
        assert_threads_stuck_but_releasable_by_latch @thread, @latch
      end
    end

    def test_detects_remains_latched
      another_latch = Concurrent::CountDownLatch.new
      assert_raises(Minitest::Assertion) do
        assert_threads_stuck_but_releasable_by_latch @thread, another_latch
      end
    end
  end

  include CustomAssertions

  def with_thread_waiting_in_lock_section(lock_section)
    in_section      = Concurrent::CountDownLatch.new
    section_release = Concurrent::CountDownLatch.new

    stuck_thread = Thread.new do
      @lock.send(lock_section) do
        in_section.count_down
        section_release.wait
      end
    end

    in_section.wait

    yield section_release
  ensure
    section_release.count_down
    stuck_thread.join # clean up
  end
end
