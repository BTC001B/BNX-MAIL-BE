const fs = require('fs');
const path = require('path');

const feDir = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src';

function searchInFiles(dir) {
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const fullPath = path.join(dir, file);
    const stat = fs.statSync(fullPath);
    if (stat.isDirectory()) {
      searchInFiles(fullPath);
    } else if (file.endsWith('.js') || file.endsWith('.jsx')) {
      const content = fs.readFileSync(fullPath, 'utf8');
      if (content.includes('getTextStyle') || content.includes('updateTextStyle') || content.includes('bnx_setting_fontFamily')) {
        console.log(`File with text-style references: ${fullPath}`);
      }
    }
  }
}

searchInFiles(feDir);
