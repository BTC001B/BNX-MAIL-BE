const fs = require('fs');

function checkSyntax(filePath) {
  try {
    const code = fs.readFileSync(filePath, 'utf8');
    let openBrace = 0, openParen = 0;
    for (let char of code) {
      if (char === '{') openBrace++;
      if (char === '}') openBrace--;
      if (char === '(') openParen++;
      if (char === ')') openParen--;
    }
    console.log(`Syntax check ${filePath}: Braces balance=${openBrace}, Parens balance=${openParen}`);
  } catch (err) {
    console.error(`Error checking ${filePath}:`, err.message);
  }
}

checkSyntax('C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/Data/constants.jsx');
checkSyntax('C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/services/api.js');
checkSyntax('C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/Settings.jsx');
checkSyntax('C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx');
checkSyntax('C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/ComposePage.jsx');
