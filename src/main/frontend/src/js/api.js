import $ from "jquery";
import _ from "lodash";
import FileSaver from "file-saver";

const DEFAULT_TIMEOUT = 30000;

function toError(xhr) {
  if (xhr.responseJSON && xhr.responseJSON.message) {
    return new Error(xhr.responseJSON.message);
  } else if (xhr.responseText) {
    return new Error(xhr.responseText.substr(0, 1000));
  } else {
    return new Error("API call failed");
  }
}

export async function get(url, data, options) {
  return new Promise((resolve, reject) => {
    $.ajax(_.extend({
        url: `${AJS.contextPath()}${url}`,
        type: 'GET',
        data,
        dataType: 'json',
        timeout: DEFAULT_TIMEOUT
      }, options)
    ).then(data => resolve(data), xhr => reject(toError(xhr)));
  });
}
export async function post(url, data, options) {
  return new Promise((resolve, reject) => {
    $.ajax(_.extend({
        url: `${AJS.contextPath()}${url}`,
        type: 'POST',
        data: JSON.stringify(data),
        dataType: 'json',
        contentType: 'application/json',
        processData: false,
        timeout: DEFAULT_TIMEOUT
      }, options)
    ).then(data => resolve(data), xhr => reject(toError(xhr)));
  });
}
export async function put(url, data, options) {
  return new Promise((resolve, reject) => {
    $.ajax(_.extend({
        url: `${AJS.contextPath()}${url}`,
        type: 'PUT',
        data: JSON.stringify(data),
        dataType: 'json',
        contentType: 'application/json',
        processData: false,
        timeout: DEFAULT_TIMEOUT
      }, options)
    ).then(data => resolve(data), xhr => reject(toError(xhr)));
  });
}
export async function del(url, data, options) {
  return new Promise((resolve, reject) => {
    $.ajax(_.extend({
        url: `${AJS.contextPath()}${url}`,
        type: 'DELETE',
        data,
        timeout: DEFAULT_TIMEOUT
      }, options)
    ).then(() => resolve(), xhr => reject(toError(xhr)));
  });
}
export async function putXml(url, data, options) {
  return new Promise((resolve, reject) => {
    $.ajax(_.extend({
        url: `${AJS.contextPath()}${url}`,
        type: 'PUT',
        data,
        dataType: 'json',
        contentType: 'text/xml',
        processData: false,
        timeout: DEFAULT_TIMEOUT
      }, options)
    ).then(data => resolve(data), xhr => reject(toError(xhr)));
  });
}
export async function downloadFile(url, fileName) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.responseType = 'blob';
    xhr.onload = function (e) {
      if (this.status === 200) {
        FileSaver.saveAs(e.target.response, fileName);
        resolve(fileName);
      } else {
        const reader = new FileReader();
        reader.onload = () => {
          try {
            const json = JSON.parse(reader.result);
            if (json.message) {
              reject(new Error(json.message));
              return;
            }
          } catch(ignore) {
          }
          reject(new Error(reader.result.substr(0, 1000)));
        };
        reader.readAsText(this.response.valueOf());
      }
    };
    xhr.send();
  });
}
