const fs = require('fs');
const path = require('path');

const feDir = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src';

function searchInDir(dir, query) {
    if (!fs.existsSync(dir)) return;
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        if (stat.isDirectory()) {
            searchInDir(fullPath, query);
        } else if (file.endsWith('.js') || file.endsWith('.jsx') || file.endsWith('.ts') || file.endsWith('.tsx')) {
            const content = fs.readFileSync(fullPath, 'utf8');
            const lines = content.split('\n');
            lines.forEach((line, index) => {
                if (line.toLowerCase().includes(query.toLowerCase())) {
                    console.log(`${fullPath} L${index + 1}: ${line.trim()}`);
                }
            });
        }
    }
}

console.log("--- Searching for 'wallpaper' in BNX-MAIL-FE ---");
searchInDir(feDir, 'wallpaper');

console.log("\n--- Searching for 'background' in BNX-MAIL-FE ---");
searchInDir(feDir, 'background');

console.log("\n--- Searching for 'Reset' in BNX-MAIL-FE/src/pages/Settings.jsx or Appearance ---");
searchInDir(feDir, 'reset');
