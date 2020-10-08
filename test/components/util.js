const delay = async ms => new Promise(resolve => setTimeout(() => resolve(), ms));
const alarm = () => { const ch = String.fromCharCode(7); console.log(`${ch}${ch}${ch}`);  };

module.exports = { delay, alarm };
