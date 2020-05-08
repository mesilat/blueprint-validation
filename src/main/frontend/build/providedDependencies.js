const providedDependencies = new Map();

providedDependencies.set('jquery', {
    dependency: 'jira.webresources:jquery',
    import: {
        var: `require('jquery')`,
        amd: 'jquery',
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
