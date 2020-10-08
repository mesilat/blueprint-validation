/*
module.exports = {
  launch: {
    headless: false,
    devtools: true
  }
};
*/
module.exports = {
  launch: {
    headless: false,
    devtools: true,
    executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    args: ['--app-shell-host-window-size=1200x800'],
  }
}
