const { readFileSync } = require("fs");
const { join } = require("path");
const { parsePage } = require("./client");

describe("Blueprint Validation plugin tests", () => {
  beforeEach(async () => {
    jest.setTimeout(30000);
  });

  it("parse page to JSON", async () => {
    const data = readFileSync(join(__dirname, "data", "page01.xml"), "utf8");
    const resp = await parsePage(data, "TEST"/* spaceKey */);
    expect(resp.data["project-contract-subject"]).toBeNull();
  });

});
