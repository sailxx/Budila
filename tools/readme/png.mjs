// PNG-копии больших картинок README для Komi Store: любую ссылку на .svg он считает значком
// и сжимает до 220×40 dp, а PNG показывает во всю ширину. GitHub берёт SVG из <picture><source>.
// Рендерит headless Chrome: SVG встраивается в страницу, анимации доматываются виртуальным временем.
// JetBrains Mono (OFL-1.1) положить в tools/readme/.font/JetBrainsMono-Var.woff2, иначе будет запасной моноширинный.
// Путь к браузеру можно задать переменной CHROME. Запускается из build.mjs.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';

const DIR = path.dirname(fileURLToPath(import.meta.url));
const SRC = path.resolve(DIR, '../../assets/readme');
const OUT = path.join(SRC, 'png');
const FONT = path.join(DIR, '.font/JetBrainsMono-Var.woff2');
const SCALE = 1.5;
const IMAGES = ['hero', 'screens', 'features', 'quality', 'design'];

const chrome = [
  process.env.CHROME,
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  '/usr/bin/google-chrome',
  '/usr/bin/chromium',
].find((p) => p && fs.existsSync(p));

export function renderPngs() {
  if (!chrome) {
    console.warn('PNG для Komi Store не обновлены: не найден Chrome (задайте переменную CHROME)');
    return;
  }
  if (!fs.existsSync(FONT)) console.warn('Нет tools/readme/.font/JetBrainsMono-Var.woff2 — PNG будут с запасным шрифтом');
  const face = fs.existsSync(FONT)
    ? `@font-face{font-family:"JetBrains Mono";src:url(${pathToFileURL(FONT)}) format("woff2");font-weight:100 800}`
    : '';
  fs.mkdirSync(OUT, { recursive: true });
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'budila-readme-'));
  const names = fs.readdirSync(SRC).filter((f) => new RegExp(`^(${IMAGES.join('|')})-[a-z]+\\.svg$`).test(f));
  for (const name of names) {
    const svg = fs.readFileSync(path.join(SRC, name), 'utf8');
    const [, w, h] = svg.match(/viewBox="0 0 (\d+(?:\.\d+)?) (\d+(?:\.\d+)?)"/).map(Number);
    const page = path.join(tmp, 'page.html');
    fs.writeFileSync(
      page,
      `<!doctype html><style>${face}html,body{margin:0;background:transparent}</style>` +
        `<div style="width:${w}px;height:${h}px">${svg.replace(/^<\?xml[^>]*>/, '')}</div>`,
    );
    execFileSync(chrome, [
      '--headless=new',
      '--disable-gpu',
      '--hide-scrollbars',
      '--allow-file-access-from-files',
      `--user-data-dir=${path.join(tmp, 'profile')}`,
      `--force-device-scale-factor=${SCALE}`,
      '--default-background-color=00000000',
      '--virtual-time-budget=4000',
      `--window-size=${Math.ceil(w)},${Math.ceil(h)}`,
      `--screenshot=${path.join(OUT, name.replace(/\.svg$/, '.png'))}`,
      pathToFileURL(page).href,
    ], { stdio: 'ignore' });
  }
  fs.rmSync(tmp, { recursive: true, force: true });
  console.log(`PNG для Komi Store: ${names.length} файлов в assets/readme/png`);
}
