require 'abstract_unit'
require "active_support/log_subscriber/test_helper"

# common controller actions
module RequestForgeryProtectionActions
  def index
    render :inline => "<%= form_tag('/') {} %>"
  end

  def show_button
    render :inline => "<%= button_to('New', '/') %>"
  end

  def unsafe
    render plain: 'pwn'
  end

  def meta
    render :inline => "<%= csrf_meta_tags %>"
  end

  def form_for_remote
    render :inline => "<%= form_for(:some_resource, :remote => true ) {} %>"
  end

  def form_for_remote_with_token
    render :inline => "<%= form_for(:some_resource, :remote => true, :authenticity_token => true ) {} %>"
  end

  def form_for_with_token
    render :inline => "<%= form_for(:some_resource, :authenticity_token => true ) {} %>"
  end

  def form_for_remote_with_external_token
    render :inline => "<%= form_for(:some_resource, :remote => true, :authenticity_token => 'external_token') {} %>"
  end

  def same_origin_js
    render js: 'foo();'
  end

  def negotiate_same_origin
    respond_to do |format|
      format.js { same_origin_js }
    end
  end

  def cross_origin_js
    same_origin_js
  end

  def negotiate_cross_origin
    negotiate_same_origin
  end

end

# sample controllers
class RequestForgeryProtectionControllerUsingResetSession < ActionController::Base
  include RequestForgeryProtectionActions
  protect_from_forgery :only => %w(index meta same_origin_js negotiate_same_origin), :with => :reset_session
end

class RequestForgeryProtectionControllerUsingException < ActionController::Base
  include RequestForgeryProtectionActions
  protect_from_forgery :only => %w(index meta same_origin_js negotiate_same_origin), :with => :exception
end

class RequestForgeryProtectionControllerUsingNullSession < ActionController::Base
  protect_from_forgery :with => :null_session

  def signed
    cookies.signed[:foo] = 'bar'
    head :ok
  end

  def encrypted
    cookies.encrypted[:foo] = 'bar'
    head :ok
  end

  def try_to_reset_session
    reset_session
    head :ok
  end
end

class PrependProtectForgeryBaseController < ActionController::Base
  before_action :custom_action
  attr_accessor :called_callbacks

  def index
    render inline: 'OK'
  end

  protected

  def add_called_callback(name)
    @called_callbacks ||= []
    @called_callbacks << name
  end


  def custom_action
    add_called_callback("custom_action")
  end

  def verify_authenticity_token
    add_called_callback("verify_authenticity_token")
  end
end

class FreeCookieController < RequestForgeryProtectionControllerUsingResetSession
  self.allow_forgery_protection = false

  def index
    render :inline => "<%= form_tag('/') {} %>"
  end

  def show_button
    render :inline => "<%= button_to('New', '/') %>"
  end
end

class CustomAuthenticityParamController < RequestForgeryProtectionControllerUsingResetSession
  def form_authenticity_param
    'foobar'
  end
end

# common test methods
module RequestForgeryProtectionTests
  def setup
    @token = Base64.strict_encode64('railstestrailstestrailstestrails')
    @old_request_forgery_protection_token = ActionController::Base.request_forgery_protection_token
    ActionController::Base.request_forgery_protection_token = :custom_authenticity_token
  end

  def teardown
    ActionController::Base.request_forgery_protection_token = @old_request_forgery_protection_token
  end

  def test_should_render_form_with_token_tag
    @controller.stub :form_authenticity_token, @token do
      assert_not_blocked do
        get :index
      end
      assert_select 'form>input[name=?][value=?]', 'custom_authenticity_token', @token
    end
  end

  def test_should_render_button_to_with_token_tag
    @controller.stub :form_authenticity_token, @token do
      assert_not_blocked do
        get :show_button
      end
      assert_select 'form>input[name=?][value=?]', 'custom_authenticity_token', @token
    end
  end

  def test_should_render_form_without_token_tag_if_remote
    assert_not_blocked do
      get :form_for_remote
    end
    assert_no_match(/authenticity_token/, response.body)
  end

  def test_should_render_form_with_token_tag_if_remote_and_embedding_token_is_on
    original = ActionView::Helpers::FormTagHelper.embed_authenticity_token_in_remote_forms
    begin
      ActionView::Helpers::FormTagHelper.embed_authenticity_token_in_remote_forms = true
      assert_not_blocked do
        get :form_for_remote
      end
      assert_match(/authenticity_token/, response.body)
    ensure
      ActionView::Helpers::FormTagHelper.embed_authenticity_token_in_remote_forms = original
    end
  end

  def test_should_render_form_with_token_tag_if_remote_and_external_authenticity_token_requested_and_embedding_is_on
    original = ActionView::Helpers::FormTagHelper.embed_authenticity_token_in_remote_forms
    begin
      ActionView::Helpers::FormTagHelper.embed_authenticity_token_in_remote_forms = true
      assert_not_blocked do
        get :form_for_remote_with_external_token
      end
      assert_select 'form>input[name=?][value=?]', 'custom_authenticity_token', 'external_token'
    ensure
      ActionView::Helpers::FormTagHelper.embed_authenticity_token_in_remote_forms = original
    end
  end

  def test_should_render_form_with_token_tag_if_remote_and_external_authenticity_token_requested
    assert_not_blocked do
      get :form_for_remote_with_external_token
    end
    assert_select 'form>input[name=?][value=?]', 'custom_authenticity_token', 'external_token'
  end

  def test_should_render_form_with_token_tag_if_remote_and_authenticity_token_requested
    @controller.stub :form_authenticity_token, @token do
      assert_not_blocked do
        get :form_for_remote_with_token
      end
      assert_select 'form>input[name=?][value=?]', 'custom_authenticity_token', @token
    end
  end

  def test_should_render_form_with_token_tag_with_authenticity_token_requested
    @controller.stub :form_authenticity_token, @token do
      assert_not_blocked do
        get :form_for_with_token
      end
      assert_select 'form>input[name=?][value=?]', 'custom_authenticity_token', @token
    end
  end

  def test_should_allow_get
    assert_not_blocked { get :index }
  end

  def test_should_allow_head
    assert_not_blocked { head :index }
  end

  def test_should_allow_post_without_token_on_unsafe_action
    assert_not_blocked { post :unsafe }
  end

  def test_should_not_allow_post_without_token
    assert_blocked { post :index }
  end

  def test_should_not_allow_post_without_token_irrespective_of_format
    assert_blocked { post :index, format: 'xml' }
  end

  def test_should_not_allow_patch_without_token
    assert_blocked { patch :index }
  end

  def test_should_not_allow_put_without_token
    assert_blocked { put :index }
  end

  def test_should_not_allow_delete_without_token
    assert_blocked { delete :index }
  end

  def test_should_not_allow_xhr_post_without_token
    assert_blocked { post :index, xhr: true }
  end

  def test_should_allow_post_with_token
    session[:_csrf_token] = @token
    @controller.stub :form_authenticity_token, @token do
      assert_not_blocked { post :index, params: { custom_authenticity_token: @token } }
    end
  end

  def test_should_allow_patch_with_token
    session[:_csrf_token] = @token
    @controller.stub :form_authenticity_token, @token do
      assert_not_blocked { patch :index, params: { custom_authenticity_token: @token } }
    end
  end

  def test_should_allow_put_with_token
    session[:_csrf_token] = @token
    @controller.stub :form_authenticity_token, @token do
      assert_not_blocked { put :index, params: { custom_authenticity_token: @token } }
    end
  end

  def test_should_allow_delete_with_token
    session[:_csrf_token] = @token
    @controller.stub :form_authenticity_token, @token do
      assert_not_blocked { delete :index, params: { custom_authenticity_token: @token } }
    end
  end

  def test_should_allow_post_with_token_in_header
    session[:_csrf_token] = @token
    @request.env['HTTP_X_CSRF_TOKEN'] = @token
    assert_not_blocked { post :index }
  end

  def test_should_allow_delete_with_token_in_header
    session[:_csrf_token] = @token
    @request.env['HTTP_X_CSRF_TOKEN'] = @token
    assert_not_blocked { delete :index }
  end

  def test_should_allow_patch_with_token_in_header
    session[:_csrf_token] = @token
    @request.env['HTTP_X_CSRF_TOKEN'] = @token
    assert_not_blocked { patch :index }
  end

  def test_should_allow_put_with_token_in_header
    session[:_csrf_token] = @token
    @request.env['HTTP_X_CSRF_TOKEN'] = @token
    assert_not_blocked { put :index }
  end

  def test_should_warn_on_missing_csrf_token
    old_logger = ActionController::Base.logger
    logger = ActiveSupport::LogSubscriber::TestHelper::MockLogger.new
    ActionController::Base.logger = logger

    begin
      assert_blocked { post :index }

      assert_equal 1, logger.logged(:warn).size
      assert_match(/CSRF token authenticity/, logger.logged(:warn).last)
    ensure
      ActionController::Base.logger = old_logger
    end
  end

  def test_should_not_warn_if_csrf_logging_disabled
    old_logger = ActionController::Base.logger
    logger = ActiveSupport::LogSubscriber::TestHelper::MockLogger.new
    ActionController::Base.logger = logger
    ActionController::Base.log_warning_on_csrf_failure = false

    begin
      assert_blocked { post :index }

      assert_equal 0, logger.logged(:warn).size
    ensure
      ActionController::Base.logger = old_logger
      ActionController::Base.log_warning_on_csrf_failure = true
    end
  end

  def test_should_only_allow_same_origin_js_get_with_xhr_header
    assert_cross_origin_blocked { get :same_origin_js }
    assert_cross_origin_blocked { get :same_origin_js, format: 'js' }
    assert_cross_origin_blocked do
      @request.accept = 'text/javascript'
      get :negotiate_same_origin
    end

    assert_cross_origin_not_blocked { get :same_origin_js, xhr: true }
    assert_cross_origin_not_blocked { get :same_origin_js, xhr: true, format: 'js'}
    assert_cross_origin_not_blocked do
      @request.accept = 'text/javascript'
      get :negotiate_same_origin, xhr: true
    end
  end

  # Allow non-GET requests since GET is all a remote <script> tag can muster.
  def test_should_allow_non_get_js_without_xhr_header
    session[:_csrf_token] = @token
    assert_cross_origin_not_blocked { post :same_origin_js, params: { custom_authenticity_token: @token } }
    assert_cross_origin_not_blocked { post :same_origin_js, params: { format: 'js', custom_authenticity_token: @token } }
    assert_cross_origin_not_blocked do
      @request.accept = 'text/javascript'
      post :negotiate_same_origin, params: { custom_authenticity_token: @token}
    end
  end

  def test_should_only_allow_cross_origin_js_get_without_xhr_header_if_protection_disabled
    assert_cross_origin_not_blocked { get :cross_origin_js }
    assert_cross_origin_not_blocked { get :cross_origin_js, format: 'js' }
    assert_cross_origin_not_blocked do
      @request.accept = 'text/javascript'
      get :negotiate_cross_origin
    end

    assert_cross_origin_not_blocked { get :cross_origin_js, xhr: true }
    assert_cross_origin_not_blocked { get :cross_origin_js, xhr: true, format: 'js' }
    assert_cross_origin_not_blocked do
      @request.accept = 'text/javascript'
      get :negotiate_cross_origin, xhr: true
    end
  end

  def test_should_not_raise_error_if_token_is_not_a_string
    @controller.unstub(:valid_authenticity_token?)
    assert_blocked do
      patch :index, params: { custom_authenticity_token: { foo: 'bar' } }
    end
  end

  def assert_blocked
    session[:something_like_user_id] = 1
    yield
    assert_nil session[:something_like_user_id], "session values are still present"
    assert_response :success
  end

  def assert_not_blocked
    assert_nothing_raised { yield }
    assert_response :success
  end

  def assert_cross_origin_blocked
    assert_raises(ActionController::InvalidCrossOriginRequest) do
      yield
    end
  end

  def assert_cross_origin_not_blocked
    assert_not_blocked { yield }
  end
end

# OK let's get our test on

class RequestForgeryProtectionControllerUsingResetSessionTest < ActionController::TestCase
  include RequestForgeryProtectionTests

  setup do
    @old_request_forgery_protection_token = ActionController::Base.request_forgery_protection_token
    ActionController::Base.request_forgery_protection_token = :custom_authenticity_token
  end

  teardown do
    ActionController::Base.request_forgery_protection_token = @old_request_forgery_protection_token
  end

  test 'should emit a csrf-param meta tag and a csrf-token meta tag' do
    @controller.stub :form_authenticity_token, @token + '<=?' do
      get :meta
      assert_select 'meta[name=?][content=?]', 'csrf-param', 'custom_authenticity_token'
      assert_select 'meta[name=?]', 'csrf-token'
      regexp = "#{@token}&lt;=\?"
      assert_match(/#{regexp}/, @response.body)
    end
  end
end

class RequestForgeryProtectionControllerUsingNullSessionTest < ActionController::TestCase
  class NullSessionDummyKeyGenerator
    def generate_key(secret)
      '03312270731a2ed0d11ed091c2338a06'
    end
  end

  def setup
    @request.env[ActionDispatch::Cookies::GENERATOR_KEY] = NullSessionDummyKeyGenerator.new
  end

  test 'should allow to set signed cookies' do
    post :signed
    assert_response :ok
  end

  test 'should allow to set encrypted cookies' do
    post :encrypted
    assert_response :ok
  end

  test 'should allow reset_session' do
    post :try_to_reset_session
    assert_response :ok
  end
end

class RequestForgeryProtectionControllerUsingExceptionTest < ActionController::TestCase
  include RequestForgeryProtectionTests
  def assert_blocked
    assert_raises(ActionController::InvalidAuthenticityToken) do
      yield
    end
  end
end

class PrependProtectForgeryBaseControllerTest < ActionController::TestCase
  PrependTrueController = Class.new(PrependProtectForgeryBaseController) do
    protect_from_forgery prepend: true
  end

  PrependFalseController = Class.new(PrependProtectForgeryBaseController) do
    protect_from_forgery prepend: false
  end

  PrependDefaultController = Class.new(PrependProtectForgeryBaseController) do
    protect_from_forgery
  end

  def test_verify_authenticity_token_is_prepended
    @controller = PrependTrueController.new
    get :index
    expected_callback_order = ["verify_authenticity_token", "custom_action"]
    assert_equal(expected_callback_order, @controller.called_callbacks)
  end

  def test_verify_authenticity_token_is_not_prepended
    @controller = PrependFalseController.new
    get :index
    expected_callback_order = ["custom_action", "verify_authenticity_token"]
    assert_equal(expected_callback_order, @controller.called_callbacks)
  end

  def test_verify_authenticity_token_is_prepended_by_default
    @controller = PrependDefaultController.new
    get :index
    expected_callback_order = ["verify_authenticity_token", "custom_action"]
    assert_equal(expected_callback_order, @controller.called_callbacks)
  end
end

class FreeCookieControllerTest < ActionController::TestCase
  def setup
    @controller = FreeCookieController.new
    @token      = "cf50faa3fe97702ca1ae"
    super
  end

  def test_should_not_render_form_with_token_tag
    SecureRandom.stub :base64, @token do
      get :index
      assert_select 'form>div>input[name=?][value=?]', 'authenticity_token', @token, false
    end
  end

  def test_should_not_render_button_to_with_token_tag
    SecureRandom.stub :base64, @token do
      get :show_button
      assert_select 'form>div>input[name=?][value=?]', 'authenticity_token', @token, false
    end
  end

  def test_should_allow_all_methods_without_token
    SecureRandom.stub :base64, @token do
      [:post, :patch, :put, :delete].each do |method|
        assert_nothing_raised { send(method, :index)}
      end
    end
  end

  test 'should not emit a csrf-token meta tag' do
    SecureRandom.stub :base64, @token do
      get :meta
      assert @response.body.blank?
    end
  end
end

class CustomAuthenticityParamControllerTest < ActionController::TestCase
  def setup
    super
    @old_logger = ActionController::Base.logger
    @logger = ActiveSupport::LogSubscriber::TestHelper::MockLogger.new
    @token = Base64.strict_encode64(SecureRandom.random_bytes(32))
    @old_request_forgery_protection_token = ActionController::Base.request_forgery_protection_token
    ActionController::Base.request_forgery_protection_token = @token
  end

  def teardown
    ActionController::Base.request_forgery_protection_token = @old_request_forgery_protection_token
    super
  end

  def test_should_not_warn_if_form_authenticity_param_matches_form_authenticity_token
    ActionController::Base.logger = @logger
    begin
      @controller.stub :valid_authenticity_token?, :true do
        post :index, params: { custom_token_name: 'foobar' }
        assert_equal 0, @logger.logged(:warn).size
      end
    ensure
      ActionController::Base.logger = @old_logger
    end
  end

  def test_should_warn_if_form_authenticity_param_does_not_match_form_authenticity_token
    ActionController::Base.logger = @logger

    begin
      post :index, params: { custom_token_name: 'bazqux' }
      assert_equal 1, @logger.logged(:warn).size
    ensure
      ActionController::Base.logger = @old_logger
    end
  end
end
