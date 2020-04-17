define('com.mesilat:validator-currency',
  ['com.mesilat/autocomplete', 'com.mesilat/autocomplete:currencies'],
  function(autocomplete, currencies){

  function CurrencyValidator(options) {
    this.id = options.code;
    this.title = options.name;

    autocomplete.ids[options.code] = {
      id: options.code,
      title: options.name,
      getUrl: function(val) {
        if (val) {
            return AJS.contextPath() + '/rest/currencies/1.0/find';
        } else {
            return null;
        }
      },
      getParams: function(autoCompleteControl, val){
          var params = {
              'max-results': 10
          };
          if (val) {
              params.filter = Confluence.unescapeEntities(val);
          }
          return params;
      },
      update: function(autoCompleteControl, link){
        currencies.updateCurrency(autoCompleteControl, link);
      }
    };
  }

  CurrencyValidator.prototype.focusin = function($td, empty, ed) {
    if (empty) {
      this.showAutocomplete(ed);
    }
  }

  CurrencyValidator.prototype.focusout = function($td, empty, ed) {
  }

  CurrencyValidator.prototype.showAutocomplete = function(ed) {
    ed.execCommand("MesilatAutocomplete", false, {
      id: this.id,
      title: this.title
    }, {
      skip_undo: true
    });
  }

  return CurrencyValidator;
});
