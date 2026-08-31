const fs = require('fs');
const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/EmailDetails.jsx';
let content = fs.readFileSync(path, 'utf8');

if (!content.includes('useTranslation')) {
  content = content.replace(
    'import { useTheme } from "../context/ThemeContext";',
    'import { useTheme } from "../context/ThemeContext";\nimport { useTranslation } from "../context/LanguageContext";'
  );
}

if (!content.includes('const { t } = useTranslation()')) {
  content = content.replace(
    'const EmailDetails = ({ email, onClose, onOpenCompose, onNavigateEmail, totalEmails, currentIndex, isSplitPane = false }) => {',
    'const EmailDetails = ({ email, onClose, onOpenCompose, onNavigateEmail, totalEmails, currentIndex, isSplitPane = false }) => {\n  const { t } = useTranslation();'
  );
}

content = content.replace(
  '>Reply</span>',
  '>{t("common.reply", "Reply")}</span>'
);

content = content.replace(
  '>Forward</span>',
  '>{t("common.forward", "Forward")}</span>'
);

fs.writeFileSync(path, content, 'utf8');
console.log("EmailDetails.jsx updated successfully!");
