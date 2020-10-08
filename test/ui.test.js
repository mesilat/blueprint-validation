const fs = require('fs');
const { join } = require('path');
const options = require('./.test.settings.js');
const loginForm = require('./components/login-form');
const createSpace = require('./components/create-space');
const deleteSpace = require('./components/delete-space');
const createTemplate = require('./components/create-template');
const createPageFromTemplate = require('./components/create-page-from-template');
const createPagesFromTemplate = require('./components/create-pages-from-template');
const { alarm } = require('./components/util');

options.space = 'Blueprint Validation Plugin Test';
options.spaceKey = 'BVPT';

describe('Validating Blueprints UI tests', () => {
  beforeAll(async () =>
    new Promise(async (resolve, reject) => {
      try {
        await page.goto(`${options.baseaddr}`);
        resolve();
      } catch(err) {
        reject(err);
      }
    })
  , 10000);

  beforeEach(async () => {
    jest.setTimeout(1800000);
  });

  it('Delete space', async () => await deleteSpace(options));

  it('A user can login using form', async () => await loginForm(options));
  it('A user can create a test space', async () => await createSpace(options));
  it('A user can create a test page template', async () => await createTemplate(options));
  // options.template = { id: 1572902 }; // need to specify template id if prev skipped
  it('Create a page set from template', async () => await createPagesFromTemplate(options));
});
