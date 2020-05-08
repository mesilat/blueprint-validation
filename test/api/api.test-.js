const fs = require('fs');
const { join } = require('path');
const Client = require('../client');
const options = require('../.test.settings.js');
const client = new Client(options);

describe('Validating Blueprints API tests template', () => {
  let template;
  beforeAll(async () =>
    new Promise(async (resolve, reject) => {
      try {
        const templates = (await client.listSpaceTemplates(options.sampleSpace)).results;
        template = templates.filter(template => template.name === 'Sample Data Template')[0];
        resolve();
      } catch(err) {
        reject(err);
      }
    })
  , 10000);

  beforeEach(async () => {
    jest.setTimeout(10000);
  });

  it('My test #1', async () => {
    expect(template).not.toBeNull();

    // Do some tests here...
  });
});
