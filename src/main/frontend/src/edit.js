import edit from './js/edit';

edit();

if (module.hot) {
  module.hot.accept('./js/edit', () => {
    require('./js/edit').default();
  });
}
