const fs = require('fs');

const constantsPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/Data/constants.jsx';
let constantsContent = fs.readFileSync(constantsPath, 'utf8');

if (!constantsContent.includes('COMPOSING:')) {
  constantsContent = constantsContent.replace(
    'SETTINGS: {',
    'SETTINGS: {\n    COMPOSING: \'/api/settings/composing\','
  );
  fs.writeFileSync(constantsPath, constantsContent, 'utf8');
  console.log("Updated constants.jsx with SETTINGS.COMPOSING");
} else {
  console.log("constants.jsx already has COMPOSING");
}

const apiPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/services/api.js';
let apiContent = fs.readFileSync(apiPath, 'utf8');

if (!apiContent.includes('getComposing')) {
  apiContent = apiContent.replace(
    'export const settingsAPI = {',
    `export const settingsAPI = {\n    getComposing: () => api.get(API_ENDPOINTS.SETTINGS?.COMPOSING || '/api/settings/composing'),\n    updateComposing: (data) => api.put(API_ENDPOINTS.SETTINGS?.COMPOSING || '/api/settings/composing', data),`
  );
  fs.writeFileSync(apiPath, apiContent, 'utf8');
  console.log("Updated api.js with getComposing and updateComposing");
} else {
  console.log("api.js already has getComposing");
}
