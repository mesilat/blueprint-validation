import { SHOW_WARNING } from "../constants";

function PageValidator(options) {
  this.id = options.code;
  this.title = options.name;
  const cql = options.text? ` AND (${options.text})`: "";

  const autocomplete = window.require("com.mesilat/autocomplete");
  autocomplete.ids[options.code] = {
    id: options.code,
    title: options.name,
    getUrl: function() {
      return AJS.contextPath() + '/rest/api/content/search';
    },
    getParams: function(autoCompleteControl, val) {
      return {
        cql: val? `type=page AND title~"${val}"${cql} ORDER BY title`
           : `type=page${cql} ORDER BY title`
      };
    },
    convertJSON: function(json) {
      const result = {
        group: [{
          name: 'page',
          result: json.results
        }],
        result: []
      };
      result.group[0].result.forEach(function(rec){
        rec.link = [{
          href: `${AJS.contextPath()}${rec._links.webui}`,
          rel: 'alternate',
          type: 'text/html'
        }];
      });
      return result;
    },
    update: function(autoCompleteControl, linkObj) {
      //setLink(linkObj);
      if (linkObj.restObj) {
        var link = AJS.$.extend(linkObj.restObj, {
          title: linkObj.restObj.linkAlias
        });
        linkObj = Confluence.Link.fromREST(link);
      }
      linkObj.insert();
    }
  };
}

PageValidator.prototype.focusin = function($td, empty, ed) {
  if (empty) {
    this.showAutocomplete(ed);
  }
}

PageValidator.prototype.focusout = function($td, empty, ed) {
  if (empty)
    return;

  setTimeout(() => {
    if ($td.find("a[data-linked-resource-id]").length === 0) {
      $td.addClass(SHOW_WARNING);
    }
  }, 100);
}

PageValidator.prototype.showAutocomplete = function(ed) {
  ed.execCommand("MesilatAutocomplete", false, {
    id: this.id,
    title: this.title
  }, {
    skip_undo: true
  });
}

export default PageValidator;
