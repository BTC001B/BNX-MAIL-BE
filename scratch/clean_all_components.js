const fs = require('fs');
const path = require('path');

const gitReset = require('child_process').execSync;
try {
  gitReset('git checkout -- src/', { cwd: 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE' });
  console.log("Reset FE src files to clean state.");
} catch (e) {
  console.error("Git checkout failed, cleaning manually:", e);
}

const feSrc = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src';

// Re-inject useTranslation cleanly into files
function injectTranslationHook(filePath) {
  if (!fs.existsSync(filePath)) return;
  let content = fs.readFileSync(filePath, 'utf8');

  // Ensure import statement exists
  if (!content.includes('useTranslation')) {
    content = `import { useTranslation } from "../context/LanguageContext";\n${content}`;
  }

  // Inject const { t } = useTranslation(); right after the main function declaration
  const funcRegex = /(export default function \w+\(|const \w+ = \([^)]*\) => \{|function \w+\([^)]*\) \{)/;
  if (!content.includes('const { t } = useTranslation();')) {
    // Find first `{` of the main component function body
    const match = content.match(/(const \w+ = \([^{]*\) => \{|function \w+\([^{]*\) \{)/);
    if (match) {
      const idx = content.indexOf(match[0]) + match[0].length;
      content = content.slice(0, idx) + '\n  const { t } = useTranslation();' + content.slice(idx);
    }
  }

  fs.writeFileSync(filePath, content, 'utf8');
  console.log(`Cleanly injected translation into: ${path.basename(filePath)}`);
}

const filesToUpdate = [
  'components/EmailList.jsx',
  'components/BulkActionsToolbar.jsx',
  'components/StorageWidget.jsx',
  'components/StorageCard.jsx',
  'components/AppLauncher.jsx',
  'components/ContactPanel.jsx',
  'components/CalendarPanel.jsx',
  'components/NotesPanel.jsx',
  'components/WeatherPanel.jsx',
  'pages/Starred.jsx',
  'pages/Draft.jsx',
  'pages/Send.jsx',
  'pages/Trash.jsx',
  'pages/Spam.jsx',
  'pages/Snoozed.jsx',
  'pages/Scheduled.jsx',
  'pages/Archive.jsx',
  'pages/AllMail.jsx',
  'pages/Unread.jsx',
  'pages/Templates.jsx',
  'pages/Casbox.jsx',
  'pages/Vault.jsx',
  'pages/Subscriptions.jsx',
  'pages/Notification.jsx',
  'pages/Support.jsx'
];

filesToUpdate.forEach(relP => injectTranslationHook(path.join(feSrc, relP)));
