const fs = require('fs');
const path = require('path');

const srcDir = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src';

function scanDir(dir) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        if (stat.isDirectory()) {
            scanDir(fullPath);
        } else if (file.endsWith('.jsx') || file.endsWith('.js') || file.endsWith('.tsx') || file.endsWith('.ts')) {
            const content = fs.readFileSync(fullPath, 'utf8');
            if (content.toLowerCase().includes('unsubscribe') || content.toLowerCase().includes('block')) {
                console.log("MATCH:", fullPath);
            }
        }
    }
}

console.log("Scanning BNX-MAIL-FE for block/unsubscribe references...");
scanDir(srcDir);
