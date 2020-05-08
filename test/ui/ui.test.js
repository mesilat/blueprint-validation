const fs = require('fs');
const { join } = require('path');
const Client = require('../client');
const options = require('../.test.settings.js');
const { delay } = require('./util');
const client = new Client(options);

const loginForm = require('./components/login-form');
const templateFailSave = require('./components/template-fail');
const templateWarnSave = require('./components/template-warn');
const draftPageSave = require('./components/draft-page');
const blueprintFailSave = require('./components/blueprint-fail');
const blueprintWarnSave = require('./components/blueprint-warn');

describe('Validating Blueprints API tests', () => {
  let template;

  beforeAll(async () =>
    new Promise(async (resolve, reject) => {
      try {
        // await deleteSpace();
        // const space = await createSpace();
        // const template = await createTemplate(space);
        // await loadData(space);
        const templates = (await client.listSpaceTemplates(options.sampleSpace)).results;
        template = templates.filter(template => template.name === 'Sample Data Template')[0];
        await page.goto(`${options.baseaddr}`);

        resolve();
      } catch(err) {
        reject(err);
      }
    })
  , 10000);

  beforeEach(async () => {
    jest.setTimeout(60000);
  });

  it('a user can login using form', loginForm);
  it('a template failes to save in strict mode (FAIL)', async () => templateFailSave(client, template.templateId));
  it('a template save results in warning in loose mode (WARN)', async () => templateWarnSave(client, template.templateId));
  // Not working:
  //it('a user receives warning response on save page from draft', async () => draftPageSave(client, template.templateId));
  it('a blueprint fails to save in strict mode (FAIL)', async () => blueprintFailSave(client, template.templateId));
  it('a blueprint save results in warning in loose mode (WARN)', async () => blueprintWarnSave(client, template.templateId));
});
