// Картинки и README на всех языках в стиле Okto Calc: чёрный фон, свечения и оранжевый акцент иконки Budila,
// шрифты Golos Text и JetBrains Mono из app/src/main/res/font (урезаются под каждый язык).
// Тексты картинок — strings.json, тексты README — md.json. Запуск: cd tools/readme && npm install && node build.mjs
// Скриншоты для screens-*.svg лежат в tools/readme/screens/ (372 px — вдвое больше размера показа),
// исходники — screenshots/i2_list_dark, i6_ringing_oled, 10_list_sunset_dark, 12_custom_theme.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createKit, ICONS } from './kit.mjs';
import { renderPngs } from './png.mjs';

const DIR = path.dirname(fileURLToPath(import.meta.url));
const REPO = path.resolve(DIR, '../..');
const OUT = path.join(REPO, 'assets/readme');
const STR = JSON.parse(fs.readFileSync(path.join(DIR, 'strings.json'), 'utf8'));
const MD = JSON.parse(fs.readFileSync(path.join(DIR, 'md.json'), 'utf8'));
// Версия берётся из versionName в app/build.gradle.kts; в strings.json — {version}.
const VERSION = fs.readFileSync(path.join(REPO, 'app/build.gradle.kts'), 'utf8').match(/versionName\s*=\s*"([^"]+)"/)[1];

const O = '#FF7A1A';
const K = createKit({
  golos: path.join(REPO, 'app/src/main/res/font/golos_text.ttf'),
  mono: path.join(REPO, 'app/src/main/res/font/jetbrains_mono.ttf'),
  accent: { hi: O, grad: ['#FFB27A', '#FF6A00'], on: '#1F0B00', glow: '#FF6A00', glow2: '#FF375F', btn: ['#FF9A4D', '#F06400'] },
});
const { P, T, card, chip, sectionHead, badge, lines, wrap, one, fitSize, fitText, delay, svg } = K;
const flat = (s) => String(s).replace(/\n/g, ' ');

// Значок «Сегменты» (буква «Б» точками 5×7) — те же пути, что в ic_launcher_foreground_segments.xml
const logo = (x, y, s) => `<g transform="translate(${x} ${y}) scale(${s / 108})"><rect width="108" height="108" rx="30" fill="#000" stroke="#3a3a3a" stroke-width="3"/>
<path d="M37.2,33a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M44.2,33a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M51.2,33a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M58.2,33a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M65.2,33a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M37.2,40a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M37.2,47a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M37.2,54a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M44.2,54a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M51.2,54a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M58.2,54a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M37.2,61a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M65.2,61a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M37.2,68a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M65.2,68a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M37.2,75a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M44.2,75a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M51.2,75a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M58.2,75a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0" fill="#FF6A00"/>
<path d="M44.2,40a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M51.2,40a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M58.2,40a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M65.2,40a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M44.2,47a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M51.2,47a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M58.2,47a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M65.2,47a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M65.2,54a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M44.2,61a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M51.2,61a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M58.2,61a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M44.2,68a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M51.2,68a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M58.2,68a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0M65.2,75a2.8,2.8 0,1 0,5.6,0a2.8,2.8 0,1 0,-5.6,0" fill="#262626"/></g>`;

const RING = '.ring{transform-box:fill-box;transform-origin:center;animation:ring 1.6s ease-out infinite}@keyframes ring{0%{opacity:.7;transform:scale(1)}100%{opacity:0;transform:scale(2.6)}}@media (prefers-reduced-motion:reduce){.ring{animation:none}}';

// ---------- HERO ----------
function hero(t) {
  const h = t.hero;
  let b = `<g class="r">${logo(40, 36, 36)}<text x="88" y="62" font-size="24" font-weight="800" letter-spacing="-.4">${T('Budila')}</text></g>`;
  const v = chip(184, 39, 'v' + VERSION);
  b += `<g class="r" ${delay(1)}>${v.s}</g>`;
  const ts = fitSize([h.h1, h.h2], 500, 40, 800);
  b += `<g class="r" ${delay(2)}>${lines(40, 146, [one(h.h1), one(h.h2)], { size: ts, weight: 800, lh: 48, spacing: '-1' })}</g>`;
  const sub = wrap(h.sub, 520, 16, 500);
  b += `<g class="r" ${delay(3)}>${lines(40, 232, sub, { size: 16, weight: 500, lh: 24, fill: P.muted })}</g>`;
  const top = 232 + (sub.length - 1) * 24 + 40;
  const tiles = h.tiles.map(([a, d]) => [a, wrap(flat(d), 214, 13, 500)]);
  const th = 46 + Math.max(...tiles.map(([, d]) => d.length)) * 18 + 8;
  tiles.forEach(([a, d], i) => {
    const x = 40 + (i % 2) * 262, y = top + Math.floor(i / 2) * (th + 12);
    b += `<g class="r" ${delay(4 + i)}>${card(x, y, 250, th)}<circle cx="${x + 22}" cy="${y + 27}" r="4" fill="${O}"/>
${fitText(x + 34, y + 32, a, 200, { size: 16, weight: 700 })}
${lines(x + 20, y + 56, d, { size: 13, weight: 500, lh: 18, fill: P.muted })}</g>`;
  });
  const H = top + 2 * th + 12 + 40;
  // экран звонка в дизайне «Инструмент»: утопленное табло с «незажжёнными» восьмёрками
  const cy = H / 2 - 130, cx = 588;
  b += `<g class="r" ${delay(3)}><ellipse cx="${cx + 126}" cy="${cy + 130}" rx="170" ry="170" fill="url(#g1)"/>${card(cx, cy, 252, 260, 22)}
<circle class="blink" cx="${cx + 24}" cy="${cy + 30}" r="4" fill="${O}"/>
${fitText(cx + 36, cy + 34, h.ring, 160, { size: 10, weight: 600, mono: true, spacing: 1.6, fill: P.dim })}
<circle class="ring" cx="${cx + 226}" cy="${cy + 30}" r="8" fill="none" stroke="${O}" stroke-width="2"/><circle cx="${cx + 226}" cy="${cy + 30}" r="8" fill="${O}"/>
<rect x="${cx + 18}" y="${cy + 54}" width="216" height="96" rx="12" fill="#0A0A0A" stroke="#1F1F1F"/>
<text class="m" x="${cx + 126}" y="${cy + 122}" text-anchor="middle" font-size="56" font-weight="700" letter-spacing="-2" fill="#fff" fill-opacity=".05">${T('88:88')}</text>
<text class="m" x="${cx + 126}" y="${cy + 122}" text-anchor="middle" font-size="56" font-weight="700" letter-spacing="-2" fill="url(#tg)">${T('07')}<tspan class="blink">${T(':')}</tspan>${T('30')}</text>
${fitText(cx + 22, cy + 176, h.alarm, 210, { size: 14, weight: 600, fill: '#D6D6D6' })}
<rect x="${cx + 18}" y="${cy + 196}" width="130" height="40" rx="12" fill="${O}"/>${fitText(cx + 83, cy + 221, h.off, 116, { size: 13, weight: 700, fill: '#1F0B00', anchor: 'middle' })}
<rect x="${cx + 156}" y="${cy + 196}" width="78" height="40" rx="12" fill="#161616" stroke="#333"/>${fitText(cx + 195, cy + 221, h.snooze, 66, { size: 13, weight: 600, anchor: 'middle' })}</g>`;
  return svg(880, H, h.alt, b + `<style>${RING}</style>`);
}

// ---------- ВОЗМОЖНОСТИ ----------
function features(t) {
  const f = t.features;
  const head = sectionHead(f.label, f.title);
  let b = head.s;
  const cw = (800 - 2 * 12) / 3;
  const items = f.items.map(([title, d]) => [title, wrap(flat(d), cw - 40, 14, 500)]);
  const ch = 84 + Math.max(...items.map(([, d]) => d.length)) * 20 + 4;
  items.forEach(([title, d], i) => {
    const x = 40 + (i % 3) * (cw + 12), y = head.y + Math.floor(i / 3) * (ch + 12);
    b += `<g class="r" ${delay(2 + i * 0.6)}>${card(x, y, cw, ch)}${badge(x + 36, y + 38, i + 1)}
${fitText(x + 64, y + 44, title, cw - 80, { size: 17, weight: 700 })}
${lines(x + 20, y + 84, d, { size: 14, weight: 500, lh: 20, fill: P.muted })}</g>`;
  });
  return svg(880, head.y + 2 * ch + 12 + 36, `${f.label} — ${f.title}`, b);
}

// ---------- КАЧЕСТВО ----------
function quality(t) {
  const q = t.quality;
  const head = sectionHead(q.label, q.title);
  let b = head.s;
  const y0 = head.y, cw = 196, ch = 130;
  const num = (x, y, label, value, unit, sub, d, hi) => `<g class="r" ${delay(d)}>${card(x, y, cw, ch)}
${fitText(x + 20, y + 30, label, cw - 40, { size: 11, weight: 600, mono: true, spacing: 1.4, fill: P.dim })}
<text class="m" x="${x + 20}" y="${y + 82}" font-size="40" font-weight="700" letter-spacing="-1.5" fill="${hi ? 'url(#tg)' : '#fff'}">${T(value)}<tspan font-size="16" fill="${P.muted}">${T(unit ? ' ' + unit : '')}</tspan></text>
${fitText(x + 20, y + 110, sub, cw - 36, { size: 13, weight: 500, fill: P.muted })}</g>`;
  b += num(40, y0, q.apk[0], q.apk[1], q.apk[2], q.apk[3], 2, true);
  b += num(40 + cw + 12, y0, q.trackers[0], '0', '', q.trackers[1], 2.5);
  b += num(40, y0 + ch + 12, q.support[0], '8–16', '', q.support[1], 3);
  b += num(40 + cw + 12, y0 + ch + 12, q.tests[0], '17/17', '', q.tests[1], 3.5);
  // разрешения
  const px = 40 + 2 * (cw + 12), pw = 840 - px, ph = 2 * ch + 12;
  let p = `${card(px, y0, pw, ph)}${fitText(px + 22, y0 + 30, q.perms, pw - 44, { size: 11, weight: 600, mono: true, spacing: 1.4, fill: P.dim })}`;
  q.yes.forEach((s, i) => {
    const yy = y0 + 62 + i * 30;
    p += `<circle cx="${px + 32}" cy="${yy - 5}" r="9" fill="${O}"/><path d="M${px + 27.5} ${yy - 5}l3 3 5.5-6" fill="none" stroke="#1F0B00" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>`;
    p += fitText(px + 52, yy, s, pw - 74, { size: 14, weight: 500, fill: '#E6E6E6' });
  });
  const ly = y0 + 62 + q.yes.length * 30 - 8;
  p += `<line x1="${px + 22}" y1="${ly}" x2="${px + pw - 22}" y2="${ly}" stroke="#262626"/>`;
  const colW = (pw - 44) / 2;
  q.no.forEach((s, i) => {
    const xx = px + 22 + (i % 2) * colW, yy = ly + 30 + Math.floor(i / 2) * 28;
    p += `<circle cx="${xx + 10}" cy="${yy - 5}" r="8.5" fill="none" stroke="#3a3a3a"/><path d="M${xx + 6.8} ${yy - 8.2}l6.4 6.4m0-6.4-6.4 6.4" stroke="#6E6E6E" stroke-width="1.5" stroke-linecap="round"/>`;
    p += fitText(xx + 28, yy, s, colW - 34, { size: 13, weight: 500, fill: P.dim });
  });
  b += `<g class="r" ${delay(4)}>${p}</g>`;
  const sy = y0 + ph + 34;
  b += `<g class="r" ${delay(5)}>${fitText(40, sy, q.source.replace('{version}', VERSION), 800, { size: 12, weight: 500, mono: true, fill: P.dim })}</g>`;
  return svg(880, sy + 30, `${q.label} — ${q.title}`, b);
}

// ---------- ДИЗАЙН-КОД (тема Material 3 по умолчанию: значения из ui/theme) ----------
function design(t) {
  const d = t.design;
  const head = sectionHead(d.label, d.title);
  let b = head.s;
  let y = head.y + 2;
  b += `<g class="r" ${delay(1.5)}>${fitText(40, y, d.sub, 800, { size: 15, weight: 500, fill: P.muted })}</g>`;
  y += 28;
  const LW = 500, RX = 40 + LW + 12, RW = 840 - RX;
  const label = (x, yy, s, w) => fitText(x, yy, s, w, { size: 11, weight: 600, mono: true, spacing: 1.4, fill: P.dim });
  // палитра
  let pal = card(40, y, LW, 140) + label(60, y + 30, d.palette, LW - 40);
  [['Primary', '#4F5B92'], ['Container', '#DDE1FF'], ['Tertiary', '#75546F'], ['Surface', '#FBF8FF'], ['Night', '#121318']].forEach(([n, hex], i) => {
    const x = 60 + i * 92;
    pal += `<rect x="${x}" y="${y + 44}" width="80" height="44" rx="10" fill="${hex}" stroke="#fff" stroke-opacity=".12"/>
<text x="${x}" y="${y + 108}" font-size="12" font-weight="700">${T(n)}</text><text class="m" x="${x}" y="${y + 125}" font-size="10.5" fill="${P.dim}">${T(hex)}</text>`;
  });
  b += `<g class="r" ${delay(2)}>${pal}</g>`;
  // шрифт
  const ff = 'style="font-family:Roboto,\'Segoe UI\',system-ui,sans-serif"';
  b += `<g class="r" ${delay(2.5)}>${card(RX, y, RW, 140)}${label(RX + 20, y + 30, d.font, RW - 40)}
<text x="${RX + 20}" y="${y + 80}" font-size="34" font-weight="400" ${ff}>07:30</text>
<text x="${RX + 134}" y="${y + 66}" font-size="12" font-weight="700">${T('Roboto')}</text>${fitText(RX + 134, y + 82, d.digits, RW - 150, { size: 11, weight: 500, fill: P.dim })}
<text x="${RX + 20}" y="${y + 124}" font-size="26" font-weight="700" ${ff}>${esc(d.sample)}</text>
<text x="${RX + 134}" y="${y + 110}" font-size="12" font-weight="700">${T('Roboto Bold')}</text>${fitText(RX + 134, y + 126, d.headings, RW - 150, { size: 11, weight: 500, fill: P.dim })}</g>`;
  y += 152;
  // формы
  let sh = card(40, y, 800, 96) + label(60, y + 28, d.shapes, 760);
  const sy = y + 44;
  sh += `<rect x="60" y="${sy}" width="150" height="40" rx="14" fill="#1B1C22" stroke="#2A2C35"/><text x="76" y="${sy + 27}" font-size="17" ${ff}>06:45</text>
<rect x="174" y="${sy + 12}" width="26" height="16" rx="8" fill="#B8C4FF"/><circle cx="192" cy="${sy + 20}" r="5.5" fill="#202C61"/>`;
  sh += fitText(222, sy + 25, d.card, 130, { size: 12, weight: 500, fill: P.muted });
  sh += `<circle cx="376" cy="${sy + 20}" r="16" fill="#B8C4FF"/><text x="376" y="${sy + 24}" text-anchor="middle" font-size="11" font-weight="700" fill="#202C61">${T(d.mon)}</text>
<circle cx="414" cy="${sy + 20}" r="16" fill="#2A2C35"/><text x="414" y="${sy + 24}" text-anchor="middle" font-size="11" font-weight="700" fill="#A9AAB5">${T(d.sat)}</text>`;
  sh += fitText(442, sy + 25, d.days, 110, { size: 12, weight: 500, fill: P.muted });
  sh += `<rect x="568" y="${sy + 2}" width="112" height="36" rx="18" fill="#B8C4FF"/>${fitText(624, sy + 25, d.save, 100, { size: 12, weight: 700, fill: '#202C61', anchor: 'middle' })}`;
  sh += fitText(694, sy + 25, d.button, 136, { size: 12, weight: 500, fill: P.muted });
  b += `<g class="r" ${delay(3)}>${sh}</g>`;
  return svg(880, y + 96 + 36, `${d.label} — ${d.title}`, b);
}
const esc = (s) => T(s);

// ---------- ЭКРАНЫ ----------
const shots = [0, 1, 2, 3].map((i) => fs.readFileSync(path.join(DIR, `screens/${i}.jpg`)).toString('base64'));
const ORDER = [3, 2, 0, 1]; // подписи strings.json: [теги и цвета, своя тема, звонок, тёмная тема]
function screens(t) {
  const s = t.screens;
  let b = '', defs = '';
  [0, 1, 2, 3].forEach((i) => {
    const x = 50 + i * 200;
    defs += `<clipPath id="c${i}"><rect x="${x}" y="40" width="180" height="387" rx="24"/></clipPath>`;
    const lbl = s.labels[ORDER[i]];
    const c = chip(0, 0, lbl, { upper: false });
    b += `<g class="r" ${delay(1 + i)}><rect x="${x - 7}" y="33" width="194" height="401" rx="30" fill="#111" stroke="#2e2e2e"/>
<image x="${x}" y="40" width="180" height="387" preserveAspectRatio="xMidYMin slice" clip-path="url(#c${i})" href="data:image/jpeg;base64,${shots[i]}"/>
<rect x="${x + 70}" y="48" width="40" height="10" rx="5" fill="#000"/>
${chip(x + 90 - c.w / 2, 452, lbl, { upper: false }).s}</g>`;
  });
  return svg(880, 520, s.alt, `<ellipse cx="440" cy="240" rx="420" ry="220" fill="url(#g1)"/>` + b, defs);
}

// ---------- сборка картинок ----------
const langs = {};
for (const [code, t] of Object.entries(STR)) {
  langs[code] = () => ({
    [`hero-${code}.svg`]: hero(t),
    [`cta-${code}.svg`]: K.button(`${t.cta.label} ${t.cta.size}`, ICONS.download, true),
    [`btn-new-${code}.svg`]: K.button(MD[code].whatsNew, ICONS.spark, false),
    [`screens-${code}.svg`]: screens(t),
    [`features-${code}.svg`]: features(t),
    [`quality-${code}.svg`]: quality(t),
  });
}
await K.build(OUT, langs, 'Budilav.:–/');
renderPngs();

// ---------- README на всех языках (тексты — md.json) ----------
const pic = (name, code, alt) => `<picture><source srcset="assets/readme/${name}-${code}.svg"><img src="assets/readme/png/${name}-${code}.png" width="100%" alt="${alt}"></picture>`;
const det = (summary, body) => `<details>\n<summary><b>${summary}</b></summary>\n\n${body}\n\n</details>`;
for (const [code, m] of Object.entries(MD)) {
  const sw = Object.values(MD).map((x) => (x === m ? `**${x.name}**` : `[${x.name}](${x.file})`)).join(' · ');
  const text = `<div align="center">

${sw}

<br>

${pic('hero', code, m.heroAlt)}

<a href="https://github.com/sailxx/Budila/releases/latest/download/Budila.apk"><img src="assets/readme/cta-${code}.svg" height="52" alt="${m.ctaAlt}"></a>&nbsp;&nbsp;<a href="https://github.com/sailxx/Budila/releases/latest"><img src="assets/readme/btn-new-${code}.svg" height="52" alt="${m.whatsNew}"></a>

${m.intro}

</div>

<br>

${pic('screens', code, m.screensAlt)}

<br>

${pic('features', code, m.featuresAlt)}

${det(m.featuresMore, m.features)}

<br>

${pic('quality', code, m.qualityAlt)}

${det(m.install, m.installBody)}

${det(m.privacy, m.privacyBody)}
`;
  fs.writeFileSync(path.join(REPO, m.file), text);
}
console.log('README: ' + Object.values(MD).map((m) => m.file).join(', '));
