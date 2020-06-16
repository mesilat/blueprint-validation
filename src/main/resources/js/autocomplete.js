const REST_API_PATH = "/rest/blueprint-validation/1.0";

define('com.mesilat/autocomplete', [], function(){
    return {
        key: String.fromCharCode(1,2), // Make sure the code is unique
        ids: {}
    };
});


define('com.mesilat/autocomplete-util', [
    'ajs',
    'jquery'
], function(
    AJS,
    $
) {
    "use strict";

    var loadData = function (json, query, callback, field, getRestSpecificAdditionLinks) {
        //console.log('Extra Placeholders', json);

        var hasErrors = json.statusMessage;
        var matrix;
        if (hasErrors) {
            matrix = [[{html: json.statusMessage, className: "error"}]];
        } else {
            var restMatrix = query ? AJS.REST.makeRestMatrixFromSearchData(json) : AJS.REST.makeRestMatrixFromData(json, field);
            matrix = AJS.REST.convertFromRest(restMatrix);
        }
        // do conversion
        function restSpecificAdditionLinksCallback(value, additionalLinks) {
            if (getRestSpecificAdditionLinks && typeof getRestSpecificAdditionLinks === "function") {
                getRestSpecificAdditionLinks(matrix, value, additionalLinks);
            }
        }
        callback(matrix, query, restSpecificAdditionLinksCallback);
    };

    return {
        /**
         * Returns the HTML of a AJS.dropdown link with an icon span. The icon span is required in the dropdown if we
         * want to use a sprite background for the link icon.
         * @param text escaped text of the dropdown item
         * @param className class name to be added to the link
         * @param iconClass class name to be added on the icon span
         * @return HTML string for the dropdown link
         */
        // we should remove this once AUI dropdown supports sprite icons
        dropdownLink : function(text, className, iconClass) {
            return "<a href='#' class='" + (className || "" ) + "'><span class='icon " + (iconClass || "") + "'></span><span>" + text + "</span></a>";
        },

        getRestData : function (autoCompleteControl, getUrl, getParams, val, callback, suggestionField, getRestSpecificAdditionLinks, convertJSON) {
            var url = getUrl(val);
            var cacheManager = autoCompleteControl.settings.cacheManager;
            var cachedData = cacheManager.get(val);
            var xhr;

            if (url) {
                xhr = $.ajax({
                    type: "GET",
                    url: url,
                    data: getParams(autoCompleteControl, val),
                    dataType: "json",
                    global: false,
                    timeout: 5000
                });

                //Always update the cache (eventual consistency)
                xhr.done(function(json){
                    cacheManager.put(val, json);
                });

                if (cachedData){
                    //Cached response
                    loadData(convertJSON && typeof convertJSON === 'function'? convertJSON(cachedData):cachedData, val, callback, suggestionField, getRestSpecificAdditionLinks);
                } else {
                    //Async response
                    xhr.done(function(json){
                        loadData(convertJSON && typeof convertJSON === 'function'? convertJSON(json):json, val, callback, suggestionField, getRestSpecificAdditionLinks);
                    });
                    xhr.fail(function (xml, status) { // ajax error handler
                        if (status === "timeout") {
                            loadData({statusMessage: "Timeout", query: val}, val, callback, suggestionField);
                        }
                    });
                }
            } else {
                // If no url, default items may be displayed - run the callback with no data.
                callback([], val);
            }
        }
    };
});


define('com.mesilat/autocomplete-settings',
[
    'jquery',
    'ajs',
    'underscore',
    'confluence/legacy',
    'tinymce',
    'com.mesilat/autocomplete',
    'com.mesilat/autocomplete-util'
],
function($,AJS,_,Confluence,tinymce,autocomplete,util){
    "use strict";

    const re = /^_ALL\((.+)\)_$/;

    function createMatrixFromArray(arr, val){
        var matrix = [];
        if (arr.length <= 13){
            arr.forEach(function(text){
                if (text !== ''){
                    if (text.match(re)){
                        matrix.push({
                            className: 'content-type-text',
                            href: '#',
                            name: text.replace(re, function(){ return arguments[1]; }),
                            restObj: {}
                        });
                    } else {
                        matrix.push({
                            className: 'content-type-text',
                            href: '#',
                            name: text,
                            restObj: {}
                        });
                    }
                }
            });
        } else {
            var lval = val.toLowerCase();
            arr.filter(function(text){
                return _.isUndefined(val) || val === '' || text.toLowerCase().indexOf(lval) >= 0;
            }).forEach(function(text, index){
                if (text.match(re)){
                    matrix.push({
                        className: 'content-type-text',
                        href: '#',
                        name: text.replace(re, function(){ return arguments[1]; }),
                        restObj: {}
                    });
                } else if (index < 12 && text !== ''){
                    matrix.push({
                        className: 'content-type-text',
                        href: '#',
                        name: text,
                        restObj: {}
                    });
                }
            });
        }
        var result = [];
        result[0] = matrix;
        return result;
    }
    function MesilatAutocompleteSettings() {
        Confluence.Editor.Autocompleter.Settings[autocomplete.key] = {
            ch: autocomplete.key,
            cache: false,
            endChars: [],
            dropDownClassName: 'autocomplete-links',
            selectFirstItem: true,
            headerText: 'unknown',

            getHeaderText: function(){
                return this.headerText;
            },
            getAdditionalLinks: function(){
                return [];
            },
            getDataAndRunCallback: function(autoCompleteControl,val,callback){
                if (autoCompleteControl.settings.id === '_DATA_'){
                    var data = autoCompleteControl.settings.data;
                    if (Array.isArray(data)){
                        callback(createMatrixFromArray(data, val), val, function(){});
                    } else if (typeof data === 'function') {
                        callback(data(val), val, function(){});
                    } else {
                        util.getRestData(
                            autoCompleteControl,
                            data.getUrl,
                            data.getParams,
                            val,
                            callback,
                            'content',
                            null,
                            data.convertJSON
                        );
                    }
                } else if (autoCompleteControl.settings.id in autocomplete.ids){
                    var data = autocomplete.ids[autoCompleteControl.settings.id];
                    if ('values' in data){
                        callback(createMatrixFromArray(data.values, val), val, function(){});
                    } else {
                        util.getRestData(
                            autoCompleteControl,
                            data.getUrl,
                            data.getParams,
                            val,
                            callback,
                            'content',
                            null,
                            data.convertJSON
                        );
                    }
                } else {
                    $.ajax({
                        url: AJS.contextPath() + REST_API_PATH + '/refdata/' + autoCompleteControl.settings.id,
                        type: 'GET',
                        cache: false
                    }).done(function(data){
                        switch(data.type){
                            case 0: // ReferenceData.TYPE_LIST_OF_STRINGS
                                autocomplete.ids[data.code] = {
                                    values: data.data.split(/\r?\n/)
                                };
                                Confluence.Editor.Autocompleter.Settings[autocomplete.key].getDataAndRunCallback(autoCompleteControl, val, callback);
                                break;
                            case 1: // ReferenceData.TYPE_JAVASCRIPT
                                autocomplete.ids[data.code] = eval('(function(){return ' + data.data + ';})()');
                                Confluence.Editor.Autocompleter.Settings[autocomplete.key].getDataAndRunCallback(autoCompleteControl, val, callback);
                                break;
                            default:
                                console.error('com.mesilat.lov-placeholder', 'Invalid data type: ' + data.type);
                        }
                    }).fail(function(jqxhr){
                        console.error('com.mesilat.lov-placeholder', jqxhr.responseText);
                    });
                }
            },
            update: function (autoCompleteControl, link){
                function putText(className){
                    var ed = AJS.Rte.getEditor();
                    var $span = $(ed.dom.create('span'), ed.getDoc());
                    $span.text(link.name);
                    if (!_.isUndefined(className)){
                        $span.addClass(className);
                    }
                    tinymce.confluence.NodeUtils.replaceSelection($span);
                }
                function putTextAll(data, className){
                    var ed = AJS.Rte.getEditor();
                    data.forEach(function(e, n){
                        if (n === 0){
                            var $span = $(ed.dom.create('span'), ed.getDoc());
                            $span.text(e);
                            if (!_.isUndefined(className)){
                                $span.addClass(className);
                            }
                            tinymce.confluence.NodeUtils.replaceSelection($span);
                        } else {
                            var $p = $('<p>'),
                                $span = $('<span>');
                            $span.text(e);
                            if (!_.isUndefined(className)){
                                $span.addClass(className);
                            }
                            $p.append($span);

                            ed.execCommand('mceInsertContent', false, ', ');
                            ed.execCommand('mceInsertContent', false, $p.html());
                        }

                    });
                }

                if (autoCompleteControl.settings.id === '_DATA_'){
                    var data = autoCompleteControl.settings.data;
                    if (typeof data === 'function') {
                      var linkObj = link;
                      if (linkObj.restObj && linkObj.restObj.link) {
                        const _link = AJS.$.extend(linkObj.restObj, {
                          title: linkObj.restObj.linkAlias
                        });
                        linkObj = Confluence.Link.fromREST(_link);
                        linkObj.insert();
                      } else {
                        putText(/*autoCompleteControl.settings.className*/);
                      }
                    } else if (Array.isArray(data)){
                        var _data = {};
                        data.forEach(function(e){
                            _data[e] = true;
                        });
                        if (!(link.name in _data) && ('_ALL(' + link.name + ')_' in _data)){
                            delete _data['_ALL(' + link.name + ')_'];
                            putTextAll(_.keys(_data).sort(function(a,b){ return a.toLowerCase().localeCompare(b.toLowerCase()); }));
                        } else {
                            putText(/*autoCompleteControl.settings.className*/);
                        }
                    } else {
                        if (_.isFunction(data.update)){
                            data.update(autoCompleteControl, link);
                        } else {
                            putText(/*autoCompleteControl.settings.className*/);
                        }
                    }
                } else {
                    var data = autocomplete.ids[autoCompleteControl.settings.id];
                    if (_.isFunction(data.update)){
                        data.update(autoCompleteControl, link);
                    } else {
                        putText();
                    }
                }
                if (_.isFunction(autoCompleteControl.settings.onchange)){
                    setTimeout(function(){
                        autoCompleteControl.settings.onchange();
                    });
                }
            }
        };
    }

    return MesilatAutocompleteSettings;
});


define('com.mesilat/autocomplete-plugin',
[
    'confluence/legacy',
    'tinymce',
    'com.mesilat/autocomplete',
    'com.mesilat/autocomplete-settings'
],
function(Confluence,tinymce,autocomplete,settings) {
    "use strict";

    return {
        init: function (ed) {
            ed.addCommand('MesilatAutocomplete', function (fs, params){
                Confluence.Editor.Autocompleter.Settings[autocomplete.key].id = params.id;
                Confluence.Editor.Autocompleter.Settings[autocomplete.key].headerText =
                    ('title' in params && params.title !== '')? params.title: autocomplete.ids[params.id].getTitle();
                if (params.id === '_DATA_'){
                    Confluence.Editor.Autocompleter.Settings[autocomplete.key].data = params.data;
                }
                if (_.isFunction(params.onchange)){
                    Confluence.Editor.Autocompleter.Settings[autocomplete.key].onchange = params.onchange;
                } else {
                    delete Confluence.Editor.Autocompleter.Settings[autocomplete.key].onchange;
                }
                Confluence.Editor.Autocompleter.Manager.shortcutFired(autocomplete.key);
            });
            settings();
        },
        getInfo: function () {
            return {
                longname:  'Extra Autocomplete',
                author:    'Mesilat Limited',
                authorurl: 'http://www.mesilat.com',
                version :  tinymce.majorVersion + '.' + tinymce.minorVersion
            };
        }
    };
});


require('confluence/module-exporter').safeRequire('com.mesilat/autocomplete-plugin', function(MesilatAutocomplete) {
    var tinymce = require('tinymce');
    if (!('MesilatAutocomplete' in tinymce.plugins)){
        tinymce.create('tinymce.plugins.MesilatAutocomplete', MesilatAutocomplete);
        tinymce.PluginManager.add('mesilatautocomplete', tinymce.plugins.MesilatAutocomplete);
        AJS.Editor.Adapter.addTinyMcePluginInit(function (settings) {
            settings.plugins += ',mesilatautocomplete';
        });
    }
});


(function($,AJS){
    AJS.bind('created.property-panel', function(e,o){
        if (o.type === 'textplaceholder'){
            setTimeout(function(elt){
                var $autocompleteButton = $('a.text-placeholder-property-panel-type-com-mesilat-autocomplete');
                if ($autocompleteButton.length === 0){
                    return;
                }
                if ($autocompleteButton.hasClass('com-mesilat-autocomplete-panel-button')){
                    return;
                }

                var dpt = $(elt).attr('data-placeholder-type');
                var code = (typeof dpt !== 'undefined' && dpt.startsWith('com-mesilat-autocomplete-'))?
                    dpt.substr(25): null;

                $autocompleteButton
                .addClass('com-mesilat-autocomplete-panel-button')
                .on('click', function(e){
                    $.ajax({
                        url: AJS.contextPath() + REST_API_PATH + '/refdata',
                        type: 'GET'
                    }).done(function(data){
                        var $dlg = $(Mesilat.Templates.Autocomplete.autocompleteDialog({
                            data: data,
                            code: code
                        }));

                        var $select = $dlg.find('select');
                        function filterOptions(text){
                            $select.empty();
                            data.forEach(function(rec){
                                if (text === '' || rec.name.toUpperCase().indexOf(text.toUpperCase()) >= 0){
                                    var $option = $('<option>')
                                        .attr('value', rec.id)
                                        .text(rec.name)
                                        .appendTo($select);

                                    if (code === rec.id){
                                        $option.attr('selected','selected');
                                    }
                                }
                            });
                        }

                        $dlg.find('.save').on('click', function(e){
                            e.preventDefault();
                            code = $select.val();
                            $autocompleteButton.attr('data-tooltip', $select.text());
                            AJS.dialog2($dlg).remove();
                            if (code !== null){
                                $(elt).attr('data-placeholder-type','com-mesilat-autocomplete-' + code);
                            }
                        });
                        $dlg.find('.cancel').on('click', function(e){
                            e.preventDefault();
                            AJS.dialog2($dlg).remove();
                            if (code !== null){
                                $(elt).attr('data-placeholder-type','com-mesilat-autocomplete-' + code);
                            }
                        });
                        $dlg.find('.com-mesilat-autocomplete-filter').on('keyup', function(e){
                            var $filter = $(e.target);
                            filterOptions($filter.val());
                        });
                        $dlg.find('.aui-dialog2-footer-hint a').attr('href', AJS.contextPath() + '/plugins/lov-placeholder/settings.action');

                        AJS.dialog2($dlg).show();
                    }).fail(function(jqxhr){
                        console.error('com.mesilat.lov-placeholder', jqxhr.responseText);
                    });
                });

                if ($(elt).attr('data-placeholder-type').startsWith('com-mesilat-autocomplete-')){
                    $autocompleteButton.addClass('selected');
                    $.ajax({
                        url: AJS.contextPath() + REST_API_PATH + '/refdata/' + code,
                        type: 'GET'
                    }).done(function(data){
                        $autocompleteButton.attr('data-tooltip', data.name);
                    }).fail(function(jqxhr){
                        console.error('com.mesilat.lov-placeholder', jqxhr.responseText);
                    });
                }

            }, 10, o.anchor);
        }
    });
})(AJS.$||$,AJS);
