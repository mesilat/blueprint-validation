const { readFileSync } = require('fs');
const { join } = require('path');
const { delay, alarm } = require('./util');
const Client = require('./client');
const createPageFromTemplate = require('./create-page-from-template');
const soynode = require('soynode');

async function compileTemplates() {
  return new Promise((resolve, reject) => {
    soynode.compileTemplates(join(__dirname, '..', 'templates'), async function (err){
      if (err) {
        reject(err);
      } else {
        resolve(soynode);
      }
    });
  });
}

module.exports = async (options) => {
  const companies = JSON.parse(readFileSync(join(__dirname, '..', 'data', 'companies.json'), 'utf8'));
  const soynode = await compileTemplates();
  const client = new Client(options);

  for (let i = 0; i < companies.length; i++) {
    const pageId = await createPageFromTemplate(options, companies[i].title);

    console.debug(companies[i].title, pageId);
    if (`${pageId}` === '0') {
      alarm();
      await delay(1000000); // Lets take a look at what's gone wrong
    }

    // Make sure that the template key property for the page is valid
    const templateInfo = await client.getBVPageTemplate(pageId);
    expect(templateInfo).toBe(`${options.template.id}`);

    // Update page content
    const body = soynode.render('Templates.company', { data: companies[i] });
    await client.putPageBodyAlt(pageId, body, true);
  }
};
