const providedDependencies = new Map();

providedDependencies.set('jquery', {
    dependency: 'jira.webresources:jquery',
    import: {
        var: `require('jquery')`,
        amd: 'jquery',
    },
});

providedDependencies.set('lodash', {
    dependency: 'com.atlassian.plugin.jslibs:underscore-1.4.4',
    import: {
        var: `require('atlassian/libs/underscore-1.4.4')`,
        amd: 'atlassian/libs/underscore-1.4.4',
    },
});

providedDependencies.set('tinymce', {
    dependency: 'confluence.web.resources:amd',
    import: {
        var: `require('tinymce')`,
        amd: 'tinymce',
    },
});

providedDependencies.set('moment', {
    dependency: 'confluence.web.resources:moment',
    import: {
        var: `require('moment')`,
        amd: 'moment',
    },
});

module.exports = providedDependencies;
