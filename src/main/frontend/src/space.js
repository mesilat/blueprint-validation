import 'wr-dependency!com.atlassian.auiplugin:aui-select';
import 'wr-dependency!com.atlassian.auiplugin:aui-flag';
import 'wr-dependency!com.mesilat.blueprint-validation:validation-modes';

import space from './js/space';
space();

if (module.hot) {
  module.hot.accept('./js/space', () => {
    require('./js/space').default();
  });
}
