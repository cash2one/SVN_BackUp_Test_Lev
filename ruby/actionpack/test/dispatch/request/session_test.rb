require 'abstract_unit'
require 'action_dispatch/middleware/session/abstract_store'

module ActionDispatch
  class Request
    class SessionTest < ActiveSupport::TestCase
      attr_reader :req

      def setup
        @req = ActionDispatch::Request.new({})
      end

      def test_create_adds_itself_to_env
        s = Session.create(store, req, {})
        assert_equal s, req.env[Rack::RACK_SESSION]
      end

      def test_to_hash
        s = Session.create(store, req, {})
        s['foo'] = 'bar'
        assert_equal 'bar', s['foo']
        assert_equal({'foo' => 'bar'}, s.to_hash)
      end

      def test_create_merges_old
        s = Session.create(store, req, {})
        s['foo'] = 'bar'

        s1 = Session.create(store, req, {})
        assert_not_equal s, s1
        assert_equal 'bar', s1['foo']
      end

      def test_find
        assert_nil Session.find(req)

        s = Session.create(store, req, {})
        assert_equal s, Session.find(req)
      end

      def test_destroy
        s = Session.create(store, req, {})
        s['rails'] = 'ftw'

        s.destroy

        assert_empty s
      end

      def test_keys
        s = Session.create(store, req, {})
        s['rails'] = 'ftw'
        s['adequate'] = 'awesome'
        assert_equal %w[rails adequate], s.keys
      end

      def test_values
        s = Session.create(store, req, {})
        s['rails'] = 'ftw'
        s['adequate'] = 'awesome'
        assert_equal %w[ftw awesome], s.values
      end

      def test_clear
        s = Session.create(store, req, {})
        s['rails'] = 'ftw'
        s['adequate'] = 'awesome'

        s.clear
        assert_empty(s.values)
      end

      def test_update
        s = Session.create(store, req, {})
        s['rails'] = 'ftw'

        s.update(:rails => 'awesome')

        assert_equal(['rails'], s.keys)
        assert_equal('awesome', s['rails'])
      end

      def test_delete
        s = Session.create(store, req, {})
        s['rails'] = 'ftw'

        s.delete('rails')

        assert_empty(s.keys)
      end

      def test_fetch
        session = Session.create(store, req, {})

        session['one'] = '1'
        assert_equal '1', session.fetch(:one)

        assert_equal '2', session.fetch(:two, '2')
        assert_nil session.fetch(:two, nil)

        assert_equal 'three', session.fetch(:three) {|el| el.to_s }

        assert_raise KeyError do
          session.fetch(:three)
        end
      end

      private
      def store
        Class.new {
          def load_session(env); [1, {}]; end
          def session_exists?(env); true; end
          def destroy_session(env, id, options); 123; end
        }.new
      end
    end
  end
end
