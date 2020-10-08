const { delay } = require('./util');

module.exports = async (options) => {
  await page.goto(`${options.baseaddr}/spaces/createspace-start.action`);
  await delay(1000);

  await page.waitForSelector('input[name="name"]');
  await page.focus('input[name="name"]');
  page.keyboard.type(options.space);
  await delay(100);

  await page.waitForSelector('input[type="submit"]');
  await page.evaluate(() => document.querySelector('input[type="submit"]').click());
  await delay(1000);

  await page.waitForSelector('h1#title-text');
  const title = await page.evaluate(() => document.querySelector('h1#title-text').textContent.trim());
  expect(title).toBe(`${options.space} Home`);
};
