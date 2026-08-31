const fs = require('fs');
const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
let content = fs.readFileSync(path, 'utf8');

if (!content.includes('useTranslation')) {
  content = content.replace(
    'import { useAuth } from "../context/AuthContext";',
    'import { useAuth } from "../context/AuthContext";\nimport { useTranslation } from "../context/LanguageContext";'
  );
}

if (!content.includes('const { t } = useTranslation()')) {
  content = content.replace(
    'const FloatingCompose = ({ onClose, initialData = {} }) => {',
    'const FloatingCompose = ({ onClose, initialData = {} }) => {\n  const { t } = useTranslation();'
  );
}

content = content.replace(
  '>New Message</span>',
  '>{t("common.new_message", "New Message")}</span>'
);

content = content.replace(
  'placeholder="To"',
  'placeholder={t("compose.to", "To")}'
);

content = content.replace(
  'placeholder="Subject"',
  'placeholder={t("compose.subject", "Subject")}'
);

content = content.replace(
  'placeholder="Write your email here..."',
  'placeholder={t("compose.body_placeholder", "Write your email here...")}'
);

fs.writeFileSync(path, content, 'utf8');
console.log("FloatingCompose.jsx updated successfully!");
