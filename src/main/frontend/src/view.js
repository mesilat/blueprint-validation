//import 'wr-dependency!com.atlassian.auiplugin:ajs';
//import 'wr-dependency!com.atlassian.auiplugin:aui-flag';
//import 'wr-dependency!com.atlassian.auiplugin:aui-select2';
//import 'wr-dependency!com.atlassian.auiplugin:dialog2';

import view from './js/view';
view();

if (module.hot) {
  module.hot.accept('./js/view', () => {
    require('./js/view').default();
  });
}
