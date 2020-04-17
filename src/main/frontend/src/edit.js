//import 'wr-dependency!com.atlassian.auiplugin:ajs';
//import 'wr-dependency!com.atlassian.auiplugin:aui-flag';
//import 'wr-dependency!com.atlassian.auiplugin:aui-select2';
//import 'wr-dependency!com.atlassian.auiplugin:dialog2';
//import 'wr-dependency!com.atlassian.auiplugin:aui-dropdown2';
import edit from './js/edit';
edit();

if (module.hot) {
  module.hot.accept('./js/edit', () => {
    require('./js/edit').default();
  });
}
