require 'que'

Que::Job.class_eval do
  class << self; alias_method :original_enqueue, :enqueue; end
  def self.enqueue(*args)
    if args.last.is_a?(Hash)
      options = args.pop
      options.delete(:run_at)
      args << options unless options.empty?
    end
    self.run(*args)
  end
end
