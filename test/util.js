const delay = async ms => new Promise(resolve => setTimeout(() => resolve(), ms));

/*
soynode.setOptions({
  outputDir: './temp', allowDynamicRecompile: true
});
*/
const compileTemplates = async () => {
  return new Promise((resolve, reject) => {
    soynode.compileTemplates(join(__dirname, 'templates'), async function (err){
      if (err) {
        reject(err);
      } else {
        resolve(soynode);
      }
    });
  });
}

const expandBodyXML = async body => {
  return body.replace(/<table/, [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<!DOCTYPE page [',
    '<!ENTITY nbsp \"&#160;\">',
    '<!ENTITY laquo \"&#171;\">',
    '<!ENTITY raquo \"&#187;\">',
    '<!ENTITY ndash \"&#8211;\">',
    '<!ENTITY mdash \"&#8212;\">',
    '<!ENTITY ldquo \"&#8220;\">',
    '<!ENTITY rdquo \"&#8221;\">',
    '<!ENTITY sbquo \"&#8218;\">',
    '<!ENTITY eacute \"&#233;\">',
    ']>',
    '<table',
    'xmlns="http://www.w3.org/1999/xhtml"',
    'xmlns:xsl="http://www.w3.org/1999/XSL/Transform"',
    'xmlns:xhtml="http://www.w3.org/1999/xhtml"',
    'xmlns:ac="http://www.atlassian.com/schema/confluence/4/ac/"',
    'xmlns:at="http://www.atlassian.com/schema/confluence/4/at/"',
    'xmlns:ri="http://www.atlassian.com/schema/confluence/4/ri/"',
    'xmlns:acxhtml="http://www.atlassian.com/schema/confluence/4/"',
    ''
  ].join(' '));
}

const collapseBodyXML = async body => {
  return body.replace(/<ac:layout (.+?)>/g, '<ac:layout>');
}

const transform = async (src, dst) => {
  const xslt = xsltproc.transform('templates/company.xslt', src, {
    output: dst
  });
  return new Promise(resolve => {
    xslt.on('exit', function(code) {
      if (code === 0){
        resolve();
      } else {
        throw new Error(`XSL transformer exited with code: ${code}`);
      }
    });
  });
}

module.exports = {
  delay,
  compileTemplates,
  expandBodyXML,
  collapseBodyXML,
  transform
};
