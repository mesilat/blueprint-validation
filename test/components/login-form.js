const { delay } = require('./util');

module.exports = async (options) => {
  await page.waitForSelector('#os_username');
  delay(100);
  await page.evaluate((username, password) => {
    document.querySelector('#os_username').value = username;
    document.querySelector('#os_password').value = password;
  }, options.username, options.password);
  await page.waitForSelector('#loginButton');
  await page.evaluate(() => {
    document.querySelector('#loginButton').click();
  });
  await page.waitForSelector('.confluence-dashboard');
};
