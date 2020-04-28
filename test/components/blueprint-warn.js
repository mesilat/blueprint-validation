const options = require('../.test.settings.js');
const { delay } = require('../util');

module.exports = async (client, templateId) => {
  await client.setTemplateValidationMode({
    templateKey: 'com.mesilat.blueprints-vcard:contact-template',
    validationMode: 'WARN'
  });

  const title = 'Doe, John';
  const pages = await client.queryAllPages(`space="${options.sampleSpace}" and title="${title}"`);
  if (pages.length > 0) {
    try {
      await client.delPage(pages[0].id);
    } catch(ignore) {}
  }

  await page.waitForSelector('#create-page-button');
  await page.evaluate(() => {
    document.querySelector('#create-page-button').click()
  });

  await page.waitForSelector('li[data-blueprint-module-complete-key="com.mesilat.blueprints-vcard:contact-blueprint"]');
  await page.evaluate(() => {
    document.querySelector('li[data-blueprint-module-complete-key="com.mesilat.blueprints-vcard:contact-blueprint"]').click()
  });

  await page.waitForSelector('button.create-dialog-create-button.aui-button.aui-button-primary');
  await page.evaluate(() => {
    document.querySelector('button.create-dialog-create-button.aui-button.aui-button-primary').click()
  });

  await page.waitForSelector('#content-title');
  await page.focus('#content-title');
  await delay(500);
  await page.type('#content-title', title);
  await page.waitForSelector('#wysiwygTextarea_ifr');

  await page.evaluate(() => {
    document.querySelector('#wysiwygTextarea_ifr')
    .contentDocument.querySelector('td.dsattr-lastname')
    .parentElement
    .remove()
  });

  await page.evaluate(() => {
    document.querySelector('#rte-button-publish').click()
  });

  await page.waitForSelector('#aui-flag-container .aui-message.aui-message-error');
  const errorTitle = await page.evaluate(() => document.querySelector('#aui-flag-container .aui-message.aui-message-error .title').textContent);
  expect(errorTitle).toBe('Page validation failed');

  await delay(2000);
};
