const options = require('../.test.settings.js');
const { delay } = require('../util');

module.exports = async (client, templateId) => {
  await client.setTemplateValidationMode({
    templateKey: `${templateId}`,
    validationMode: 'FAIL'
  });

  await page.goto(`${options.baseaddr}/pages/createpage-entervariables.action?templateId=${templateId}&spaceKey=${options.sampleSpace}`);
  await page.waitForSelector('#content-title');
  await page.type('#content-title', 'Page to fail');
  await page.waitForSelector('#wysiwygTextarea_ifr');

  await page.evaluate(() => {
    document.querySelector('#wysiwygTextarea_ifr')
    .contentDocument.querySelector('td.dsattr-value')
    .parentElement
    .remove()
  });

  await page.evaluate(() => {
    document.querySelector('#rte-button-publish').click()
  });

  await page.waitForSelector('#aui-flag-container .aui-message.aui-message-error');
  const title = await page.evaluate(() => document.querySelector('#aui-flag-container .aui-message.aui-message-error .title').textContent);
  expect(title).toBe('Validation Error');

  await delay(2000);
};