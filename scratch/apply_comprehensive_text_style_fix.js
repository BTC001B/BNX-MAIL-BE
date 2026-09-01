const fs = require('fs');

console.log("=== APPLYING COMPREHENSIVE DEFAULT TEXT STYLE FIX ===");

// 1. UPDATE FLOATINGCOMPOSE.JSX
const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
if (fs.existsSync(composePath)) {
  let content = fs.readFileSync(composePath, 'utf8');

  // Ensure Quill Parchment attributor registration for font & size
  if (!content.includes("Font.whitelist")) {
    const quillRegisterCode = `
// Register Font & Size attributors in Quill
const Font = Quill.import('attributors/style/font');
Font.whitelist = ['Arial', 'Calibri', 'Times New Roman', 'Georgia', 'Verdana', 'Courier New', 'Tahoma', 'Trebuchet MS', 'Roboto'];
Quill.register(Font, true);

const Size = Quill.import('attributors/style/size');
Size.whitelist = ['14px', '16px', '18px', '24px'];
Quill.register(Size, true);
`;
    content = content.replace(
      "window.Quill = Quill;",
      "window.Quill = Quill;\n" + quillRegisterCode
    );
  }

  // Update getFontFamilyCss & getFontSizeCss to be robust
  const fontCssHelper = `
const getFontFamilyCss = (font) => {
  if (!font) return 'Arial, sans-serif';
  const trimmed = font.trim();
  switch (trimmed) {
    case 'Arial': return 'Arial, sans-serif';
    case 'Calibri': return 'Calibri, sans-serif';
    case 'Times New Roman': return "'Times New Roman', Times, serif";
    case 'Georgia': return 'Georgia, serif';
    case 'Verdana': return 'Verdana, sans-serif';
    case 'Courier New': return "'Courier New', Courier, monospace";
    case 'Tahoma': return 'Tahoma, sans-serif';
    case 'Trebuchet MS': return "'Trebuchet MS', sans-serif";
    case 'Roboto': return 'Roboto, sans-serif';
    default: return \`'\${trimmed}', sans-serif\`;
  }
};

const getFontSizeCss = (size) => {
  if (!size) return '16px';
  const trimmed = size.trim();
  switch (trimmed) {
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

  // Replace existing helper functions if present, or add them
  if (content.includes('const getFontFamilyCss =')) {
    const oldHelperRegex = /const getFontFamilyCss = [\s\S]*?const getTextColorCss = \(color\) => color \|\| '#000000';/;
    content = content.replace(oldHelperRegex, fontCssHelper.trim());
  }

  // Add event listener for bnx_text_style_changed
  if (!content.includes('bnx_text_style_changed')) {
    const eventListenerCode = `
  useEffect(() => {
    const handleTextStyleChanged = (e) => {
      if (e.detail) {
        if (e.detail.fontFamily) setDefaultFontFamily(e.detail.fontFamily);
        if (e.detail.fontSize) setDefaultFontSize(e.detail.fontSize);
        if (e.detail.textColor) setDefaultTextColor(e.detail.textColor);
      }
    };
    window.addEventListener('bnx_text_style_changed', handleTextStyleChanged);
    return () => window.removeEventListener('bnx_text_style_changed', handleTextStyleChanged);
  }, []);
`;
    content = content.replace(
      'const FloatingCompose = () => {',
      'const FloatingCompose = () => {' + eventListenerCode
    );
  }

  // Update .compose-quill .ql-editor CSS rules in style tag
  const newQuillStyle = `<style>{\`
          .compose-quill .ql-editor {
            font-family: \${getFontFamilyCss(defaultFontFamily)} !important;
            font-size: \${getFontSizeCss(defaultFontSize)} !important;
            color: \${getTextColorCss(defaultTextColor)} !important;
          }
          .compose-quill .ql-editor p,
          .compose-quill .ql-editor div,
          .compose-quill .ql-editor span:not([style*="font-family"]) {
            font-family: inherit;
          }
          .compose-quill .ql-editor p,
          .compose-quill .ql-editor div,
          .compose-quill .ql-editor span:not([style*="font-size"]) {
            font-size: inherit;
          }
          .compose-quill .ql-editor p,
          .compose-quill .ql-editor div,
          .compose-quill .ql-editor span:not([style*="color"]) {
            color: inherit;
          }
        \`}</style>`;

  const oldStyleRegex = /<style>\{\`[\s\S]*?\.compose-quill \.ql-editor p \{[\s\S]*?\}\s*\`\}<\/style>/;
  if (oldStyleRegex.test(content)) {
    content = content.replace(oldStyleRegex, newQuillStyle);
  }

  fs.writeFileSync(composePath, content, 'utf8');
  console.log("✓ Updated FloatingCompose.jsx!");
}

// 2. UPDATE SETTINGS.JSX TO DISPATCH bnx_text_style_changed EVENT
const settingsPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/Settings.jsx';
if (fs.existsSync(settingsPath)) {
  let content = fs.readFileSync(settingsPath, 'utf8');

  const dispatchCode = `window.dispatchEvent(new CustomEvent('bnx_text_style_changed', {
          detail: {
            fontFamily: defaultFontFamily,
            fontSize: defaultFontSize,
            textColor: defaultTextColor
          }
        }));`;

  if (!content.includes('bnx_text_style_changed')) {
    content = content.replace(
      'localStorage.setItem("bnx_setting_textColor", defaultTextColor);',
      'localStorage.setItem("bnx_setting_textColor", defaultTextColor);\n        ' + dispatchCode
    );
    fs.writeFileSync(settingsPath, content, 'utf8');
    console.log("✓ Updated Settings.jsx with event dispatch!");
  } else {
    console.log("✓ Settings.jsx already has event dispatch!");
  }
}

// 3. UPDATE COMPOSEPAGE.JSX TO LISTEN TO bnx_text_style_changed EVENT
const composePagePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/ComposePage.jsx';
if (fs.existsSync(composePagePath)) {
  let content = fs.readFileSync(composePagePath, 'utf8');

  if (!content.includes('bnx_text_style_changed')) {
    const eventListenerCodePage = `
  useEffect(() => {
    const handleTextStyleChanged = (e) => {
      if (e.detail) {
        if (e.detail.fontFamily) setDefaultFontFamily(e.detail.fontFamily);
        if (e.detail.fontSize) setDefaultFontSize(e.detail.fontSize);
        if (e.detail.textColor) setDefaultTextColor(e.detail.textColor);
      }
    };
    window.addEventListener('bnx_text_style_changed', handleTextStyleChanged);
    return () => window.removeEventListener('bnx_text_style_changed', handleTextStyleChanged);
  }, []);
`;
    content = content.replace(
      'const ComposePage = () => {',
      'const ComposePage = () => {' + eventListenerCodePage
    );

    fs.writeFileSync(composePagePath, content, 'utf8');
    console.log("✓ Updated ComposePage.jsx with event listener!");
  } else {
    console.log("✓ ComposePage.jsx already updated!");
  }
}

console.log("=== COMPLETED ALL SCRIPT UPDATES ===");
