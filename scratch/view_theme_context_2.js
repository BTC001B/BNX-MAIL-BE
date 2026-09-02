const fs = require('fs');

const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/context/ThemeContext.jsx';
if (fs.existsSync(path)) {
    const content = fs.readFileSync(path, 'utf8');
    const lines = content.split('\n');
    lines.forEach((line, index) => {
        if (index >= 179 && index < 260) {
            console.log(`L${index + 1}: ${line}`);
        }
    });
}
