# Using the MesilatAutocomplete

1. Static strings:

```
ed.execCommand('MesilatAutocomplete', false, {
  id: '_DATA_',
  title: 'Please select one:',
  data: ['one', 'two', 'three']
}, {
  skip_undo: true
});
```

2. Select text using function

```
ed.execCommand('MesilatAutocomplete', false, {
  id: '_DATA_',
  title: 'Please select one:',
  data(){
    return [{
      className: 'content-type-text',
      href: '#',
      name: 'one',
      restObj: {}
    },{
      className: 'content-type-text',
      href: '#',
      name: 'two',
      restObj: {}
    },{
      className: 'content-type-text',
      href: '#',
      name: 'three',
      restObj: {}
    }]
  }
}, {
  skip_undo: true
});
```

3. Select page using function

```
ed.execCommand('MesilatAutocomplete', false, {
  id: '_DATA_',
  title: 'Please select page',
  data(){
    return [
      ... ,
      {
        className: 'content-type-page',
        href: '#',
        name: pages[i].title,
        restObj: {
          id: pages[i].id,
          title: pages[i].title,
          link: [{
            href: `${AJS.contextPath()}/pages/viewpage.action?pageId=${pages[0].id}`,
            rel: 'alternate',
            type: 'text/html',
          }],
          type: 'page'
        }
      },
      ...
    ];
  }
}, {
  skip_undo: true
});
```

4. Select page using ajax

```
ed.execCommand('MesilatAutocomplete', false, {
  id: '_DATA_',
  title: 'Please select page',
  data: {
    getUrl: () => `${AJS.contextPath()}/rest/something`,
    getParams: (autoCompleteControl, q) => ({ q }),
    convertJSON: data => {
      data.forEach((rec) => {
        rec.id = rec.pageId;
        rec.type = 'page';
        rec.title = rec.name;
        rec.link = [{
          href: `${AJS.contextPath()}/pages/viewpage.action?pageId=${rec.pageId}`,
          rel: 'alternate',
          type: 'text/html'
        }];
      });
      json.sort((a,b) => a.title.toLowerCase().localeCompare(b.title.toLowerCase()));
      return {
        group: [{
          name: 'page',
          result: json
        }],
        result: []
      };
    },
    update: (autoCompleteControl, linkObj) => {
      if (linkObj.restObj) {
        const link = AJS.$.extend(linkObj.restObj, {
          title: linkObj.restObj.linkAlias
        });
        linkObj = Confluence.Link.fromREST(link);
      }
      linkObj.insert();
    }
  }
}, {
  skip_undo: true
});
```
