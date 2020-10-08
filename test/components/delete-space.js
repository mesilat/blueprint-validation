const { delay } = require('./util');
const Client = require('./client');

module.exports = async (options) => {
  const client = new Client(options);
  await client.deleteSpace(options.spaceKey);
  console.debug(`Space ${options.spaceKey} successfully deleted. This task may take some time to complete...`);
  await delay(3000);
};
