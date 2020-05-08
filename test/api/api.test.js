const fs = require('fs');
const { join } = require('path');
const Client = require('../client');
const options = require('../.test.settings.js');
const client = new Client(options);
const { delay, compileTemplates } = require('./util');

const deleteSpace = async function() {
  try {
    await client.deleteSpace(options.sampleSpace);
    console.debug(`Space ${options.sampleSpace} successfully deleted. This task may take some time to complete...`);
  } catch(ignore) {}
}
const createSpace = async function() {
  const space = await client.createSpace(options.sampleSpace, 'Sample Data', 'This space contains sample data and can be freely deleted');
  console.debug(`Space ${space.name} (re)created successfully`);
  return space;
}
const createTemplate = async function(space) {
  const content = fs.readFileSync(join(__dirname, 'templates', 'company.xml'), 'utf8');
  const title = 'Sample Data Template';
  const template = await client.createTemplate({
    title,
    description: 'Company financial information demo template',
    content,
    spaceKey: space.key,
    labels: [ 'sample-company' ]
  });
  console.debug(`Template ${template.id} (re)created successfully`);
  return template;
}
const loadData = async function (space) {
  const data = JSON.parse(fs.readFileSync('../../node_modules/confluence-client/test/data.json', 'utf-8'));
  const soynode = await compileTemplates();

  for (let i = 0; i < data.length; i++) {
    const rec = data[i];
    const title = rec.title;
    const body = soynode.render('Templates.company', { data: rec });
    const resp = await client.createPage(space.key, space.homepage.id, title, body);
    const pageId = resp.id;
    await client.postLabel(pageId, { prefix: 'global', name: 'sample-company' });
    console.debug(`Created page ${title}`);
  }
}
const validate = async function (template) {
  const pages = await client.listPagesByLabel('sample-company');
  console.debug(`Total pages to validate: ${pages.length}`);
  for (let i = 0; i < pages.length; i++) {
    const page = pages[i];
    await client.validatePage(page.id, template.id);
    console.debug(`Page validated: ${page.title}`);
  }
}

describe('Validating Blueprints API tests', () => {
  let space = null;
  let template = null;
  beforeAll(async () =>
    new Promise(async (resolve, reject) => {
      try {
        await deleteSpace();
        await delay(10000);
        space = await createSpace();
        template = await createTemplate(space);
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
    expect(space).not.toBeNull();
    expect(template).not.toBeNull();

    await loadData(space);
    console.debug('Page load successful. Wait some time for Confluence to index the pages...');
    await delay(10000); // Give some time to reindex
    await validate(template);
    console.debug('Page validation complete. Starting tests...');

    let data = await client.queryData({
      templateKey: template.id,
      path: '[?(@.sales>200)]'
    });
    console.debug(`Total results before page delete: ${data.length}`);
    expect(data.length).toBe(12);

    const pageTitle = 'Sinopec';
    const pages = await client.queryAllPages(`space="${options.sampleSpace}" and title="${pageTitle}"`);
    expect(pages.length).toBe(1);
    const page = pages[0];

    // Test fot GET /rest/blueprint-validation/1.0/data/${pageId}
    data = await client.getPageData(page.id);
    // console.debug(data);
    expect(data.page.title).toBe('Sinopec');
    expect(data.page.labels.length).toBe(1);
    expect(data.page.labels[0]).toBe('sample-company');
    expect(data.validation.valid).toBe(true);
    expect(data.data.position).toBe(88);
    expect(data.data.country).toBe('China');

    // Test for POST /rest/blueprint-validation/1.0/data
    data = await client.queryData({
      pageId: page.id,
      path: 'country'
    });
    expect(data.data.value).toBe('China');

    await client.delPage(pages[0].id);

    data = await client.queryData({
      templateKey: template.id,
      path: '[?(@.sales>200)]'
    });
    console.debug(`Total results after page delete: ${data.length}`);
    expect(data.length).toBe(11);

    data = await client.queryData({ pageId: page.id });
    expect(data.page.deleted).toBe(true);

    await client.restorePage(pages[0].id);

    data = await client.queryData({
      templateKey: template.id,
      path: '[?(@.sales>200)]'
    });
    console.debug(`Total results after page restore: ${data.length}`);
    expect(data.length).toBe(12);

    data = await client.queryData({ pageId: page.id });
    expect('deleted' in data.page).toBe(false);
  });
});
