const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
let content = fs.readFileSync(composePath, 'utf8');

const targetSnippet = `    return (
      <>
        {isReply && (
          <style>{\`
          .compose-quill .ql-editor {`;

const fixedSnippet = `    return (
      <>
        {isReply && (
          <style>{\`
            .reply-composer-style .ql-toolbar {
              display: none !important;
            }
            .reply-composer-style .ql-container {
              border: none !important;
            }
            .reply-composer-style .ql-editor {
              padding: 12px 0 !important;
              font-size: 14px !important;
              line-height: 1.6 !important;
              min-height: 150px !important;
            }
          \`}</style>
        )}

        <style>{\`
          .compose-quill .ql-editor {
            font-family: \${getFontFamilyCss(defaultFontFamily)};
            font-size: \${getFontSizeCss(defaultFontSize)};
            color: \${getTextColorCss(defaultTextColor)};
          }
          .compose-quill .ql-editor p,
          .compose-quill .ql-editor div,
          .compose-quill .ql-editor span:not([style*="font-family"]) {
            font-family: inherit;
          }
          .compose-quill .ql-editor p,
          .compose-quill .ql-editor div,
          .compose-quill .ql-editor span:not([style*="color"]) {
            color: inherit;
          }
        \`}</style>`;

if (content.includes(targetSnippet)) {
  const endIdx = content.indexOf('{/* HEADER / DRAG HANDLE */}');
  const startIdx = content.indexOf('return (\n      <>\n        {isReply && (');
  content = content.substring(0, startIdx) + fixedSnippet + '\n\n        ' + content.substring(endIdx);
  fs.writeFileSync(composePath, content, 'utf8');
  console.log("✓ Fixed FloatingCompose.jsx syntax structure!");
} else {
  console.log("Target snippet not found, inspecting return (...");
}
