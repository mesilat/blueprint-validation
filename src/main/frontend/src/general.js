import general from './js/general';
general();

if (module.hot) {
  module.hot.accept('./js/general', () => {
    require('./js/general').default();
  });
}
