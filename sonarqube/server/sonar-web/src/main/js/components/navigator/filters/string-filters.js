define([
  'jquery',
  './base-filters',
  '../templates'
], function ($, BaseFilters) {

  var DetailsStringFilterView = BaseFilters.DetailsFilterView.extend({
    template: Templates['string-filter'],


    events: {
      'change input': 'change'
    },


    change: function(e) {
      this.model.set('value', $(e.target).val());
    },


    onShow: function() {
      BaseFilters.DetailsFilterView.prototype.onShow.apply(this, arguments);
      this.$(':input').focus();
    },


    serializeData: function() {
      return _.extend({}, this.model.toJSON(), {
        value: this.model.get('value') || ''
      });
    }

  });



  return BaseFilters.BaseFilterView.extend({

    initialize: function() {
      BaseFilters.BaseFilterView.prototype.initialize.call(this, {
        detailsView: DetailsStringFilterView
      });
    },


    renderValue: function() {
      return this.isDefaultValue() ? '—' : this.model.get('value');
    },


    renderInput: function() {
      $('<input>')
          .prop('name', this.model.get('property'))
          .prop('type', 'hidden')
          .css('display', 'none')
          .val(this.model.get('value') || '')
          .appendTo(this.$el);
    },


    isDefaultValue: function() {
      return !this.model.get('value');
    },


    restore: function(value) {
      this.model.set({
        value: value,
        enabled: true
      });
    },


    clear: function() {
      this.model.unset('value');
      this.detailsView.render();
    }

  });

});
