const fs = require('fs');

const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/Settings.jsx';
let content = fs.readFileSync(path, 'utf8');

// Replace language options with clean UTF-8 string
const oldSelect = `<select
                      value={language}
                      onChange={e => setLanguage(e.target.value)}
                      className="w-full p-3 text-sm rounded-xl border outline-none cursor-pointer focus:ring-2 focus:border-transparent transition-all"
                      style={{ background: theme.mode === 'dark' ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)', borderColor: theme.border, color: theme.text }}
                    >
                      <option value="en">English</option>
                      <option value="ta">Tamil ( தமிழ் )</option>
                      <option value="hi">Hindi ( हिन्दी )</option>
                      <option value="te">Telugu ( తెలుగు )</option>
                      <option value="ml">Malayalam ( മലയാളം )</option>
                      <option value="kn">Kannada ( ಕನ್ನಡ )</option>
                    </select>`;

content = content.replace(/<select\s+value=\{language\}[\s\S]*?<\/select>/, `<select
                      value={language}
                      onChange={e => setLanguage(e.target.value)}
                      className="w-full p-3 text-sm rounded-xl border outline-none cursor-pointer focus:ring-2 focus:border-transparent transition-all"
                      style={{ background: theme.mode === 'dark' ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)', borderColor: theme.border, color: theme.text }}
                    >
                      <option value="en">English</option>
                      <option value="ta">Tamil (தமிழ்)</option>
                      <option value="hi">Hindi (हिन्दी)</option>
                      <option value="te">Telugu (తెలుగు)</option>
                      <option value="ml">Malayalam (മലയാളം)</option>
                      <option value="kn">Kannada (ಕನ್ನಡ)</option>
                    </select>`);

// Translate ToggleRow labels in Settings.jsx
content = content.replace('label="Enable Spelling Check"', 'label={t("settings.spelling_check", "Enable Spelling Check")}');
content = content.replace('label="Enable Grammar Check"', 'label={t("settings.grammar_check", "Enable Grammar Check")}');
content = content.replace('label="Enable Auto-correct"', 'label={t("settings.auto_correct", "Enable Auto-correct")}');
content = content.replace('label="Enable Writing Suggestions (Smart Compose)"', 'label={t("settings.writing_suggestions", "Enable Writing Suggestions (Smart Compose)")}');

// Translate Save Preferences button
content = content.replace(/Save Preferences/g, '{t("common.save_preferences", "Save Preferences")}');

fs.writeFileSync(path, content, 'utf8');
console.log("Updated Settings.jsx with clean UTF-8 options and t() translations!");
