# blueprint-validation

Atlassian Confluence is an excellent tool for building knowledge in an organization or user group.
This knowledge may be customer or product details, employee profiles, IT and production resources,
technologies, etc. However, in most cases, the data in Confluence is not well suited for automated
processing. Wiki-based content editor engine provides format that is too loose for most
data-processing software.

The Blueprint Validation plugin addresses the problem described above. It converts Confluence page
storage format into a typed JSON object and validates the resulting data object against applicable
JSON-schema. If a page (or rather data in a page) is not valid than the system will generate a
notification and provide relevant error details. Confluence administrator can prohibit saving
invalid pages or configure Confluence to just show a warning popup and proceed with page save.

For more details please refer to: https://www.mesilat.com/blueprint-validation.html

## Building

```
cd blueprint-validation
yarn install
yarn mvnpackage
```

## Testing

Get a test Confluence instance running and edit the `.test.settings.js` file to
specify Confluence base address, username and password. Then run the test suite:

```
yarn test
```
