const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
let content = fs.readFileSync(composePath, 'utf8');

const markerStart = "    return (\r\n      <>\r\n        {isReply && (\r\n          <style>{`";
const markerEnd = "        {/* HEADER / DRAG HANDLE */}";

const startIdx = content.indexOf(markerStart);
const endIdx = content.indexOf(markerEnd);

if (startIdx !== -1 && endIdx !== -1) {
  const replacement = `    return (
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
        \`}</style>\n\n        `;

  content = content.substring(0, startIdx) + replacement + content.substring(endIdx);
  fs.writeFileSync(composePath, content, 'utf8');
  console.log("✓ Successfully replaced JSX section in FloatingCompose.jsx!");
} else {
  console.error(`✗ Markers not found! startIdx=${startIdx}, endIdx=${endIdx}`);
}
