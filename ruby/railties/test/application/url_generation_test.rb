require 'isolation/abstract_unit'

module ApplicationTests
  class UrlGenerationTest < ActiveSupport::TestCase
    include ActiveSupport::Testing::Isolation

    def app
      Rails.application
    end

    test "it works" do
      boot_rails
      require "rails"
      require "action_controller/railtie"
      require "action_view/railtie"

      class MyApp < Rails::Application
        secrets.secret_key_base = "3b7cd727ee24e8444053437c36cc66c4"
        config.session_store :cookie_store, key: "_myapp_session"
        config.active_support.deprecation = :log
        config.eager_load = false
      end

      Rails.application.initialize!

      class ::ApplicationController < ActionController::Base
      end

      class ::OmgController < ::ApplicationController
        def index
          render text: omg_path
        end
      end

      MyApp.routes.draw do
        get "/" => "omg#index", as: :omg
      end

      require 'rack/test'
      extend Rack::Test::Methods

      get "/"
      assert_equal "/", last_response.body
    end

    def test_routes_know_the_relative_root
      boot_rails
      require "rails"
      require "action_controller/railtie"
      require "action_view/railtie"

      relative_url = '/hello'
      ENV["RAILS_RELATIVE_URL_ROOT"] = relative_url
      app = Class.new(Rails::Application)
      assert_equal relative_url, app.routes.relative_url_root
      ENV["RAILS_RELATIVE_URL_ROOT"] = nil
    end
  end
end
