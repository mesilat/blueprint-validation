import $ from "jquery";
import { trace, error } from "../util";
import { post } from "../api";

const getData = async params => post(`/rest/blueprint-validation/1.0/data`, params);

// POST --data '{"templateKey":"4784144","path":"[?(@.sales>200)]"}' "${TEST_WIKI_HOME}"

async function init($macro) {
  trace("report::init");

  try {
    const params = {};
    if ($macro.data("templateKey")) {
      params.templateKey = $macro.data("templateKey");
    }
    if ($macro.data("path")) {
      params.path = $macro.data("path");
    }
    const data = await getData(params)||[];
    if ($macro.data("order")) {
      sort(data, $macro.data("order").split("."));
    } else {
      sort(data, "page.title".split("."));
    }

    let columns = [];
    $macro.data("columns").split(";").forEach(_column => {
      const column = _column.trim();
      switch(column) {
        case "page":
        case "template":
        case "valid":
        case "data.*":
          columns.push(column);
          break;
        case "data":
          columns.push("data.*");
          break;
        default:
          if (column.match(/^data\./)) {
            columns.push(column);
          } else {
            columns.push(`data.${column}`);
          }
      }
    });
    const captions = [];
    $macro.data("captions").split(";").forEach(caption => captions.push(caption.trim()));

    const expandedColumns = [];
    columns.forEach(column => {
      if (column === "data.*") {
        pushAll(getDataColumns(data), expandedColumns);
      } else {
        expandedColumns.push(column);
      }
    });
    columns = expandedColumns;

    const $table = $("<table class='wrapped confluenceTable'>");
    const $tbody = $("<tbody>").appendTo($table);
    let $tr = $("<tr>").appendTo($tbody);
    columns.forEach((column,index) => {
      $("<th class='confluenceTh'>")
        .text(index < captions.length? captions[index]: column)
        .appendTo($tr);
    });

    data.forEach(rec => {
      let $tr = $("<tr>").appendTo($tbody);
      columns.forEach((column,index) => {
        const $td = $("<td class='confluenceTd'>").appendTo($tr);
        serialize(column, rec, $td);
      });
    });

    $macro.empty().append($table);

    trace("report::init", data, columns, captions);
  } catch(err) {
    error(err);
  }
}

function pushAll(src, dest) {
  dest.push.apply(dest, src);
}

function getDataColumns(data) {
  const columns = [];
  data.forEach(rec => {
    Object.keys(rec.data).forEach(key => {
      const column = `data.${key}`;
      if (!columns.includes(column)) {
        columns.push(column);
      }
    });
  });
  return columns;
}

function serialize(column, rec, $td) {
  switch (column) {
    case "page":
      $("<a>")
        .attr("href", `${AJS.contextPath()}/pages/viewpage.action?pageId=${rec.page.id}`)
        .text(rec.page.title)
        .appendTo($td);
      break;
    case "template":
      $td.text(rec.validation.template.title);
      break;
    case "valid":
      $td.text(rec.validation.valid);
      break;
    default:
      const val = get(rec, column.split("."));
      $td.text(val);
      if (!isNaN(val)) {
        $td.css("text-align", "right");
      }
  }
}

function get(obj, path) {
  if (path.length === 0)
    return obj;

  if (path[0] in obj) {
    return get(obj[path[0]], path.slice(1));
  } else {
    return null;
  }
}

function sort(data, column) {
  data.sort((a,b) => {
    const akey = get(a, column);
    const bkey = get(b, column);
    if (!isNaN(akey) && !isNaN(bkey)) {
      return akey - bkey;
    } else {
      return `${akey}`.toLowerCase().localeCompare(`${bkey}`.toLowerCase());
    }
  });
}

export default $macro => init($macro);
