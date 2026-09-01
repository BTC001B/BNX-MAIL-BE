const fs = require('fs');

console.log("=== APPLYING DEFAULT TEXT STYLE FIX TO FLOATINGCOMPOSE.JSX & COMPOSEPAGE.JSX ===");

// 1. UPDATE FLOATINGCOMPOSE.JSX
const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
if (fs.existsSync(composePath)) {
  let content = fs.readFileSync(composePath, 'utf8');

  // Ensure settingsAPI is imported
  if (!content.includes('settingsAPI')) {
    content = content.replace(
      'import { mailAPI, api, userAPI, signatureAPI, casboxAPI } from "../services/api";',
      'import { mailAPI, api, userAPI, signatureAPI, casboxAPI, settingsAPI } from "../services/api";'
    );
  }

  // Add CSS helper functions if not present
  if (!content.includes('getFontFamilyCss')) {
    const helperFunctions = `
// Helper CSS mappers for Default Text Style
const getFontFamilyCss = (font) => {
  switch (font) {
    case 'Arial': return 'Arial, sans-serif';
    case 'Georgia': return 'Georgia, serif';
    case 'Tahoma': return 'Tahoma, sans-serif';
    case 'Times New Roman': return "'Times New Roman', Times, serif";
    case 'Trebuchet MS': return "'Trebuchet MS', sans-serif";
    case 'Verdana': return 'Verdana, sans-serif';
    case 'Courier New': return "'Courier New', Courier, monospace";
    case 'Calibri': return 'Calibri, sans-serif';
    default: return font ? \`\${font}, sans-serif\` : 'Arial, sans-serif';
  }
};

const getFontSizeCss = (size) => {
  switch (size) {
    case 'Small': return '14px';
    case 'Normal': return '16px';
    case 'Large': return '18px';
    case 'Extra Large':
    case 'Huge': return '24px';
    default: return '16px';
  }
};

const getTextColorCss = (color) => color || '#000000';
`;
    content = content.replace(
      'const FloatingCompose = () => {',
      helperFunctions + '\nconst FloatingCompose = () => {'
    );
  }

  // Add default text style state if not present
  if (!content.includes('defaultFontFamily')) {
    const stateDeclarations = `
  const [defaultFontFamily, setDefaultFontFamily] = useState(() => localStorage.getItem("bnx_setting_fontFamily") || "Arial");
  const [defaultFontSize, setDefaultFontSize] = useState(() => localStorage.getItem("bnx_setting_fontSizeText") || "Normal");
  const [defaultTextColor, setDefaultTextColor] = useState(() => localStorage.getItem("bnx_setting_textColor") || "#000000");
`;
    content = content.replace(
      'const [undoSendDelay, setUndoSendDelay] = useState(0);',
      'const [undoSendDelay, setUndoSendDelay] = useState(0);' + stateDeclarations
    );
  }

  // Update fetchSettings in useEffect
  if (!content.includes('settingsAPI.getTextStyle()')) {
    content = content.replace(
      `const [settingsRes, sigsRes] = await Promise.all([
          userAPI.getSettings(),
          signatureAPI.getSignatures().catch(() => null)
        ]);`,
      `const [settingsRes, sigsRes, textStyleRes] = await Promise.all([
          userAPI.getSettings(),
          signatureAPI.getSignatures().catch(() => null),
          settingsAPI.getTextStyle().catch(() => null)
        ]);

        if (textStyleRes?.data) {
          const ts = textStyleRes.data;
          if (ts.fontFamily) {
            setDefaultFontFamily(ts.fontFamily);
            localStorage.setItem("bnx_setting_fontFamily", ts.fontFamily);
          }
          if (ts.fontSize) {
            setDefaultFontSize(ts.fontSize);
            localStorage.setItem("bnx_setting_fontSizeText", ts.fontSize);
          }
          if (ts.textColor) {
            setDefaultTextColor(ts.textColor);
            localStorage.setItem("bnx_setting_textColor", ts.textColor);
          }
        }`
    );
  }

  // Inject dynamic style tag for .compose-quill .ql-editor
  if (!content.includes('compose-quill .ql-editor')) {
    const styleBlock = `
        <style>{\`
          .compose-quill .ql-editor {
            font-family: \${getFontFamilyCss(defaultFontFamily)} !important;
            font-size: \${getFontSizeCss(defaultFontSize)} !important;
            color: \${getTextColorCss(defaultTextColor)} !important;
          }
          .compose-quill .ql-editor p {
            font-family: inherit;
            font-size: inherit;
            color: inherit;
          }
        \`}</style>
`;
    content = content.replace(
      `{/* HEADER / DRAG HANDLE */}`,
      styleBlock + `\n        {/* HEADER / DRAG HANDLE */}`
    );
  }

  fs.writeFileSync(composePath, content, 'utf8');
  console.log("✓ Updated FloatingCompose.jsx with Default Text Style formatting!");
} else {
  console.error("✗ FloatingCompose.jsx not found!");
}

// 2. UPDATE COMPOSEPAGE.JSX
const composePagePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/ComposePage.jsx';
if (fs.existsSync(composePagePath)) {
  let content = fs.readFileSync(composePagePath, 'utf8');

  if (!content.includes('defaultFontFamily')) {
    // Add imports / helpers / states to ComposePage.jsx
    if (!content.includes('getFontFamilyCss')) {
      const helpers = `
const getFontFamilyCss = (font) => {
  switch (font) {
    case 'Arial': return 'Arial, sans-serif';
    case 'Georgia': return 'Georgia, serif';
    case 'Tahoma': return 'Tahoma, sans-serif';
    case 'Times New Roman': return "'Times New Roman', Times, serif";
    case 'Trebuchet MS': return "'Trebuchet MS', sans-serif";
    case 'Verdana': return 'Verdana, sans-serif';
    case 'Courier New': return "'Courier New', Courier, monospace";
    case 'Calibri': return 'Calibri, sans-serif';
    default: return font ? \`\${font}, sans-serif\` : 'Arial, sans-serif';
  }
};

const getFontSizeCss = (size) => {
  switch (size) {
    case 'Small': return '14px';
    case 'Normal': return '16px';
    case 'Large': return '18px';
    case 'Extra Large':
    case 'Huge': return '24px';
    default: return '16px';
  }
};
`;
      content = content.replace(
        'const ComposePage = () => {',
        helpers + '\nconst ComposePage = () => {'
      );
    }

    content = content.replace(
      'const ComposePage = () => {',
      `const ComposePage = () => {
  const [defaultFontFamily] = useState(() => localStorage.getItem("bnx_setting_fontFamily") || "Arial");
  const [defaultFontSize] = useState(() => localStorage.getItem("bnx_setting_fontSizeText") || "Normal");
  const [defaultTextColor] = useState(() => localStorage.getItem("bnx_setting_textColor") || "#000000");`
    );

    // Apply inline style to textarea
    content = content.replace(
      'placeholder="Type your message…"',
      `placeholder="Type your message…"
                style={{
                  fontFamily: getFontFamilyCss(defaultFontFamily),
                  fontSize: getFontSizeCss(defaultFontSize),
                  color: defaultTextColor || undefined
                }}`
    );

    fs.writeFileSync(composePagePath, content, 'utf8');
    console.log("✓ Updated ComposePage.jsx with Default Text Style formatting!");
  } else {
    console.log("✓ ComposePage.jsx already updated!");
  }
}

console.log("=== COMPLETED SCRIPT EXECUTION ===");
