define('com.mesilat:validator-country',
  ['com.mesilat/autocomplete', 'com.mesilat/autocomplete:countries'],
  function(autocomplete, countries){

  function CountryValidator(options) {
    this.id = options.code;
    this.title = options.name;

    autocomplete.ids[options.code] = {
      id: options.code,
      title: options.name,
      getUrl: function(val) {
        if (val) {
            return AJS.contextPath() + '/rest/countries/1.0/find';
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
        countries.updateCountry(autoCompleteControl, link);
      }
    };
  }

  CountryValidator.prototype.focusin = function($td, empty, ed) {
    if (empty) {
      this.showAutocomplete(ed);
    }
  }

  CountryValidator.prototype.focusout = function($td, empty, ed) {
  }

  CountryValidator.prototype.showAutocomplete = function(ed) {
    ed.execCommand("MesilatAutocomplete", false, {
      id: this.id,
      title: this.title
    }, {
      skip_undo: true
    });
  }

  return CountryValidator;
});
