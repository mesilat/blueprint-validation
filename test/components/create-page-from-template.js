const { delay } = require('./util');

module.exports = async (options, title) => {
  await page.goto(`${options.baseaddr}/display/${options.spaceKey}`);
  await delay(100);

  await page.waitForSelector('#create-page-button');
  await page.click('#create-page-button');
  await delay(1000);

  await page.waitForSelector(`li[data-template-id="${options.template.id}"]`);
  await delay(100);
  await page.click(`li[data-template-id="${options.template.id}"]`);
  await delay(100);

  await page.waitForSelector('button.create-dialog-create-button');
  await page.click('button.create-dialog-create-button');

  await delay(5000);
  await page.waitForSelector('#content-title');
  await delay(100);
  await page.focus('#content-title');
  await delay(100);
  page.keyboard.type(title);

  await page.waitForSelector('#rte-button-publish');
  await delay(100);
  await Promise.all([
    page.click('#rte-button-publish'),
    page.waitForNavigation({ waitUntil: 'networkidle0' })
  ]);
  await delay(1000);

  await page.waitForSelector('meta[name="ajs-page-id"]');
  await delay(200);
  const pageId = await page.evaluate(() => document.querySelector('meta[name="ajs-page-id"]').getAttribute('content'));
  return pageId;
};
