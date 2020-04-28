const options = require('../.test.settings.js');
const { delay } = require('../util');

module.exports = async (client, templateId) => {
  await client.setTemplateValidationMode({
    templateKey: `${templateId}`,
    validationMode: 'WARN'
  });

  const title = 'Page with warning';
  const pages = await client.queryAllPages(`space="${options.sampleSpace}" and title="${title}"`);
  if (pages.length > 0) {
    await client.delPage(pages[0].id);
  }

  await page.goto(`${options.baseaddr}/pages/createpage-entervariables.action?templateId=${templateId}&spaceKey=${options.sampleSpace}`);
  await page.waitForSelector('#content-title');
  await page.focus('#content-title');
  await delay(500);
  await page.type('#content-title', title);
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
  const warningTitle = await page.evaluate(() =>
    document.querySelector('#aui-flag-container .aui-message.aui-message-error .title')
    ? document.querySelector('#aui-flag-container .aui-message.aui-message-error .title').textContent
    : null
  );
  expect(warningTitle).toBe('Page validation failed');

  await page.waitForSelector('h1#title-text');

  await delay(2000);
};
