import 'wr-dependency!com.atlassian.auiplugin:aui-select';
import 'wr-dependency!com.atlassian.auiplugin:aui-flag';
import 'wr-dependency!com.atlassian.auiplugin:dialog2';
import 'wr-dependency!com.atlassian.auiplugin:aui-select2';
import 'wr-dependency!com.atlassian.auiplugin:aui-spinner';
import 'wr-dependency!com.mesilat.blueprint-validation:validation-modes';

import config from './js/config';
config();

if (module.hot) {
  module.hot.accept('./js/config', () => {
    require('./js/config').default();
  });
}
