const https = require("https");
const axios = require("axios");
const settings = require("../.jest.settings");

const agent = new https.Agent({ rejectUnauthorized: false });

async function get(url, params) {
  return axios.get(`${settings.baseaddr}${url}`, {
    params,
    auth: {
      username: settings.username,
      password: settings.password
    },
    httpsAgent: agent,
    headers: {
      "x-atlassian-token": "no-check"
    }
  });
}
async function post(url, data, params, options) {
  options = options || {};
  if (params) {
    options.params = params;
  }
  options.auth = {
    username: settings.username,
    password: settings.password
  };
  options.httpsAgent = agent;
  options.headers = options.headers || {};
  options.headers["x-atlassian-token"] = "no-check";

  //console.debug("POST", `${options.baseaddr}${url}`, data, options);
  return axios.post(`${settings.baseaddr}${url}`, data, options);
}
async function parsePage(data, spaceKey) {
  const params = { "space-key": spaceKey };
  return post("/rest/blueprint-validation/1.0/parser/parse", data, params, {
    headers: {
      "content-type": "text/xml"
    }
  });
}

module.exports = {
  get, post, parsePage
};
