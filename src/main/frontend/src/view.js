import view from './js/view';
view();

if (module.hot) {
  module.hot.accept('./js/view', () => {
    require('./js/view').default();
  });
}
