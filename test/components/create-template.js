const { readFileSync } = require('fs');
const { join } = require('path');
const Client = require('./client');

module.exports = async (options) => {
  const client = new Client(options);
  const content = readFileSync(join(__dirname, '..', 'data', 'company.xml'), 'utf8');
  const data = {
    title: 'Demo Company Detail',
    description: 'This is a demo company detail template for testing',
    content,
    labels: ['test-company'],
    spaceKey: options.spaceKey
  };

  const template = await client.createTemplate(data);
  console.debug('Template created: ', template);
  options.template = template;

  await client.setTemplateValidationMode({
    templateKey: `${template.id}`,
    validationMode: 'FAIL',
  });
};
