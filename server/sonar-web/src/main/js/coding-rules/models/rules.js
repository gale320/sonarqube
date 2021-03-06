define([
  'coding-rules/models/rule'
], function (Rule) {

  return Backbone.Collection.extend({
    model: Rule,

    parseRules: function (r) {
      return r.rules;
    },

    setIndex: function () {
      this.forEach(function (rule, index) {
        rule.set({ index: index });
      });
    },

    addExtraAttributes: function (repositories) {
      this.models.forEach(function (model) {
        model.addExtraAttributes(repositories);
      });
    }
  });

});
