const fs = require('fs');
const { join } = require('path');
const Client = require('../client');
const options = require('../.test.settings.js');
const client = new Client(options);
const { delay, compileTemplates } = require('./util');

const validate = async function (templateKey) {
  const pages = await client.listPagesByLabel('sample-company');
  console.debug(`Total pages to validate: ${pages.length}`);
  for (let i = 0; i < pages.length; i++) {
    const page = pages[i];
    await client.validatePage(page.id, templateKey);
    console.debug(`Page validated: ${page.title}`);
  }
}

describe('Validating Blueprints API tests', () => {
  let template = null;
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
  , 60000);

  beforeEach(async () => {
    jest.setTimeout(360000);
  });

  it('load sample data and validate', async () => {
    expect(template).not.toBeNull();

    await validate(template.templateId);
    console.debug('Page validation complete. Starting tests...');

    let data = await client.queryData({
      templateKey: template.templateId,
      path: '[?(@.sales>200)]'
    });
    console.debug(`Total results before page delete: ${data.length}`);
    expect(data.length).toBe(12);

    const pageTitle = 'Sinopec';
    const pages = await client.queryAllPages(`space="${options.sampleSpace}" and title="${pageTitle}"`);
    expect(pages.length).toBe(1);
    const page = pages[0];

    // Test fot GET ${REST_API_PATH}/data/${pageId}
    data = await client.getPageData(page.id);
    // console.debug(data);
    expect(data.page.title).toBe('Sinopec');
    expect(data.page.labels.length).toBe(1);
    expect(data.page.labels[0]).toBe('sample-company');
    expect(data.validation.valid).toBe(true);
    expect(data.data.position).toBe(88);
    expect(data.data.country).toBe('China');

    // Test for POST ${REST_API_PATH}/data
    data = await client.queryData({
      pageId: page.id,
      path: 'country'
    });
    expect(data.data.value).toBe('China');

    await client.delPage(pages[0].id);

    data = await client.queryData({
      templateKey: template.templateId,
      path: '[?(@.sales>200)]'
    });
    console.debug(`Total results after page delete: ${data.length}`);
    expect(data.length).toBe(11);

    data = await client.queryData({ pageId: page.id });
    expect(data.page.deleted).toBe(true);

    await client.restorePage(pages[0].id);

    data = await client.queryData({
      templateKey: template.templateId,
      path: '[?(@.sales>200)]'
    });
    console.debug(`Total results after page restore: ${data.length}`);
    expect(data.length).toBe(12);

    data = await client.queryData({ pageId: page.id });
    expect('deleted' in data.page).toBe(false);
  });
});
