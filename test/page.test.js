const fs = require('fs');
const { join } = require('path');
const soynode = require('soynode');
const xsltproc = require('xsltproc');
const Client = require('./client');
const options = require('./.test.settings.js');
const { delay, compileTemplates } = require('./util');
const client = new Client(options);
const loginForm = require('./components/login-form');
const templateFailSave = require('./components/template-fail');
const templateWarnSave = require('./components/template-warn');
const draftPageSave = require('./components/draft-page');
const blueprintFailSave = require('./components/blueprint-fail');
const blueprintWarnSave = require('./components/blueprint-warn');

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
  const content = fs.readFileSync(join('test', 'templates', 'company.xml'), 'utf8');
  const title = 'Sample Data Template';
  const template = await client.createTemplate({
    title,
    description: 'Company financial information demo template',
    content,
    spaceKey: space.key,
    labels: [ 'sample-company' ]
  });
  console.debug(`Template ${template.id} (re)created successfully`);

  await client.setTemplateValidationMode({
    templateKey: `${template.id}`,
    templateName: title,
    validationMode: 'FAIL'
  });
  console.debug(`Template ${template.id} validation mode set to "FAIL"`);
  return template;
}
const loadData = async function (space) {
  const data = JSON.parse(fs.readFileSync('./test/data.json', 'utf-8'));
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
  );

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
