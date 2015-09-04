require File.expand_path('../../../load_paths', __FILE__)

require 'active_job'
require 'support/job_buffer'

ActiveSupport.halt_callback_chains_on_return_false = false
GlobalID.app = 'aj'

@adapter  = ENV['AJ_ADAPTER'] || 'inline'

if ENV['AJ_INTEGRATION_TESTS']
  require 'support/integration/helper'
else
  require "adapters/#{@adapter}"
end

require 'active_support/testing/autorun'
