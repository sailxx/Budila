// Генерирует SVG-картинки README на всех языках из strings.json.
// Запуск: node tools/readme/build.mjs
// Скриншоты для screens-*.svg лежат в tools/readme/screens/ (уменьшены до 372 px — вдвое больше размера показа).
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { renderPngs } from './png.mjs';

const DIR = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.resolve(DIR, '../../assets/readme');
const STR = JSON.parse(fs.readFileSync(path.join(DIR, 'strings.json'), 'utf8'));
// Версия берётся из versionName в app/build.gradle.kts; в strings.json — {version}.
const VERSION = fs.readFileSync(path.resolve(DIR, '../../app/build.gradle.kts'), 'utf8').match(/versionName\s*=\s*"([^"]+)"/)[1];

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
// *слово* — акцентный цвет
const acc = (s, color) => esc(s).replace(/\*([^*]+)\*/g, `<tspan fill="${color}">$1</tspan>`);
const plain = (s) => String(s).replace(/\*/g, '');

// Моноширинный шрифт: ширина считается в знаках. Не влезло — сборка падает с понятной ошибкой.
let lang = '';
const errors = [];
const fit = (s, max) => {
  if (plain(s).length > max) errors.push(`[${lang}] длиннее ${max} знаков: «${plain(s)}»`);
  return s;
};
const wrap = (s, max, lines) => {
  const out = [];
  for (const para of String(s).split('\n')) { // \n — принудительный перенос
    let cur = '';
    for (const w of para.split(' ')) {
      if (cur && (cur + ' ' + w).length > max) { out.push(cur); cur = w; } else cur = cur ? cur + ' ' + w : w;
    }
    if (cur) out.push(cur);
  }
  out.forEach((l) => fit(l, max));
  if (out.length > lines) errors.push(`[${lang}] больше ${lines} строк: «${s}»`);
  return out;
};

const MONO = '.m{font-family:"JetBrains Mono",SFMono-Regular,Consolas,"Liberation Mono",Menlo,monospace}';
const SANS = '.s{font-family:"Google Sans",Roboto,"Segoe UI",Helvetica,Arial,sans-serif}';
const DOTS = '<defs><pattern id="dots" width="16" height="16" patternUnits="userSpaceOnUse"><circle cx="1" cy="1" r="1" fill="#EAE7F0"/></pattern></defs>';

function hero(t) {
  const sub = wrap(t.hero.sub, 60, 2);
  const tiles = t.hero.tiles.map(([a, b]) => [fit(a, 20), wrap(b, 26, 2)]);
  const tile = (x, [a, b]) => `  <rect x="${x}" y="302" width="188" height="98" rx="12" fill="#1B1C22" stroke="#2A2C35"/>
  <text x="${x + 18}" y="332" class="m" font-size="12" font-weight="700" fill="#B8C4FF">${esc(a)}</text>
${b.map((l, i) => `  <text x="${x + 18}" y="${358 + i * 16}" class="m" font-size="10.5" fill="#8F909A">${esc(l)}</text>`).join('\n')}`;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="880" height="440" viewBox="0 0 880 440" role="img" aria-label="${esc(t.hero.alt)}">
<style>
${MONO}
${SANS}
.f{animation:in .7s ease-out both}
.d1{animation-delay:.1s}.d2{animation-delay:.25s}.d3{animation-delay:.4s}.d4{animation-delay:.55s}.d5{animation-delay:.7s}.d6{animation-delay:.85s}.d7{animation-delay:1s}
@keyframes in{from{opacity:0;transform:translateY(6px)}to{opacity:1;transform:none}}
.ring{transform-origin:808px 112px;animation:pulse 1.6s ease-out infinite}
@keyframes pulse{0%{opacity:.7;transform:scale(1)}100%{opacity:0;transform:scale(2.6)}}
.blink{animation:blink 1s steps(1) infinite}
@keyframes blink{50%{opacity:.25}}
</style>
<rect width="880" height="440" rx="18" fill="#121318"/>
<rect x=".5" y=".5" width="879" height="439" rx="17.5" fill="none" stroke="#2A2C35"/>

<g class="f d1">
  <circle cx="44" cy="40" r="6" fill="#B8C4FF"/>
  <text x="58" y="45.5" class="m" font-size="17" font-weight="700" fill="#E4E1E9">budila</text>
  <text x="840" y="45" class="m" font-size="11" fill="#8F909A" text-anchor="end" letter-spacing="1">v${VERSION}</text>
</g>

<text x="40" y="98" class="m f d2" font-size="10" fill="#8F909A" letter-spacing="3">${esc(fit(t.hero.tag, 52))}</text>
<g class="f d3">
  <text x="40" y="142" class="s" font-size="32" font-weight="700" fill="#E4E1E9">${acc(fit(t.hero.h1, 27), '#B8C4FF')}</text>
  <text x="40" y="184" class="s" font-size="32" font-weight="700" fill="#E4E1E9">${acc(fit(t.hero.h2, 27), '#B8C4FF')}</text>
</g>
<g class="f d4 m" font-size="12" fill="#A9AAB5">
${sub.map((l, i) => `  <text x="40" y="${226 + i * 20}">${esc(l)}</text>`).join('\n')}
</g>

<!-- мини-экран звонка -->
<g class="f d5">
  <rect x="560" y="80" width="280" height="190" rx="16" fill="#1B1C22" stroke="#2A2C35"/>
  <circle cx="580" cy="104" r="4" fill="#B8C4FF" class="blink"/>
  <text x="592" y="108" class="m" font-size="10" fill="#8F909A" letter-spacing="2">${esc(fit(t.hero.ring, 24))}</text>
  <circle cx="808" cy="112" r="9" fill="none" stroke="#B8C4FF" stroke-width="2" class="ring"/>
  <circle cx="808" cy="112" r="9" fill="#B8C4FF"/>
  <text x="578" y="178" class="m" font-size="58" font-weight="700" fill="#B8C4FF">07:30</text>
  <text x="580" y="204" class="s" font-size="13" fill="#C7C5D0">${esc(fit(t.hero.alarm, 34))}</text>
  <rect x="578" y="222" width="150" height="32" rx="16" fill="#B8C4FF"/>
  <text x="653" y="242.5" class="s" font-size="12" font-weight="700" fill="#202C61" text-anchor="middle">${esc(fit(t.hero.off, 18))}</text>
  <rect x="738.5" y="222.5" width="84" height="31" rx="15.5" fill="none" stroke="#454857"/>
  <text x="780.5" y="242.5" class="s" font-size="12" fill="#C7C5D0" text-anchor="middle">${esc(fit(t.hero.snooze, 10))}</text>
</g>

<!-- плитки -->
<g class="f d6">
${tile(40, tiles[0])}

${tile(244, tiles[1])}
</g>
<g class="f d7">
${tile(448, tiles[2])}

${tile(652, tiles[3])}
</g>
</svg>
`;
}

function cta(t) {
  const label = t.cta.label, size = t.cta.size;
  const sx = Math.round(58 + label.length * 8.1 + 12);
  const w = Math.round(sx + size.length * 7.2 + 26);
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="48" viewBox="0 0 ${w} 48" role="img" aria-label="${esc(label)}">
<style>${MONO}</style>
<rect x="1" y="1" width="${w - 2}" height="46" rx="23" fill="#4F5B92"/>
<path d="M38 15v13m-6-6 6 6 6-6M31 33h14" fill="none" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
<text x="58" y="29" class="m" font-size="14" font-weight="700" fill="#fff">${esc(label)}</text>
<text x="${sx}" y="29" class="m" font-size="12" fill="#DDE1FF">${esc(size)}</text>
</svg>
`;
}

function features(t) {
  const f = t.features;
  const card = (i, [title, desc]) => {
    const x = [40, 313, 586][i % 3], y = i < 3 ? 110 : 254;
    const lines = wrap(desc, 31, 3);
    return `  <rect x="${x + 0.5}" y="${y + 0.5}" width="253" height="128" rx="14" class="card"/>
  <text x="${x + 20}" y="${y + 30}" class="m n">0${i + 1}</text>
  <text x="${x + 20}" y="${y + 56}" class="s t">${esc(fit(title, 24))}</text>
${lines.map((l, k) => `  <text x="${x + 20}" y="${y + 80 + k * 16}" class="m x">${esc(l)}</text>`).join('\n')}`;
  };
  return `<svg xmlns="http://www.w3.org/2000/svg" width="880" height="420" viewBox="0 0 880 420" role="img" aria-label="${esc(f.label)} — ${esc(plain(f.title))}">
<style>
${MONO}
${SANS}
.f{animation:in .7s ease-out both}
.d1{animation-delay:.1s}.d2{animation-delay:.25s}.d3{animation-delay:.4s}.d4{animation-delay:.55s}
@keyframes in{from{opacity:0;transform:translateY(6px)}to{opacity:1;transform:none}}
.card{fill:#F3EFF7;stroke:#E3E1EC}
.t{font-size:15px;font-weight:700;fill:#1B1B21}
.x{font-size:10.5px;fill:#5F6070}
.n{font-size:11px;font-weight:700;fill:#4F5B92}
</style>
${DOTS}
<rect width="880" height="420" rx="18" fill="#FBF8FF"/>
<rect width="880" height="420" rx="18" fill="url(#dots)"/>
<rect x=".5" y=".5" width="879" height="419" rx="17.5" fill="none" stroke="#E3E1EC"/>

<g class="f d1">
  <rect x="40" y="34" width="8" height="8" rx="1.5" fill="#4F5B92"/>
  <text x="56" y="42" class="m" font-size="10" fill="#5F6070" letter-spacing="3">${esc(fit(f.label, 60))}</text>
  <text x="40" y="82" class="s" font-size="24" font-weight="700" fill="#1B1B21">${acc(fit(f.title, 60), '#4F5B92')}</text>
</g>

<g class="f d2">
${f.items.slice(0, 3).map((it, i) => card(i, it)).join('\n\n')}
</g>
<g class="f d3">
${f.items.slice(3).map((it, i) => card(i + 3, it)).join('\n\n')}
</g>
</svg>
`;
}

function quality(t) {
  const q = t.quality;
  const yes = (i, s) => {
    const y = 167 + i * 28;
    return `    <circle cx="478" cy="${y}" r="7" fill="#B8C4FF"/><path d="M474.5 ${y}l2.5 2.5 4.5-5" fill="none" stroke="#202C61" stroke-width="1.8" stroke-linecap="round"/>
    <text x="494" y="${y + 4}" class="p">${esc(fit(s, 46))}</text>`;
  };
  const no = (i, s) => {
    const cx = i % 2 ? 654 : 478, cy = 301 + Math.floor(i / 2) * 28;
    return `    <circle cx="${cx}" cy="${cy}" r="7" fill="none" stroke="#454857"/><path d="M${cx - 3} ${cy - 3}l6 6m0-6-6 6" stroke="#6B6C76" stroke-width="1.5" stroke-linecap="round"/>
    <text x="${cx + 16}" y="${cy + 4}" class="no">${esc(fit(s, i % 2 ? 22 : 23))}</text>`;
  };
  return `<svg xmlns="http://www.w3.org/2000/svg" width="880" height="470" viewBox="0 0 880 470" role="img" aria-label="${esc(q.label)} — ${esc(plain(q.title))}">
<style>
${MONO}
${SANS}
.f{animation:in .7s ease-out both}
.d1{animation-delay:.1s}.d2{animation-delay:.25s}.d3{animation-delay:.4s}.d4{animation-delay:.55s}
@keyframes in{from{opacity:0;transform:translateY(6px)}to{opacity:1;transform:none}}
.card{fill:#1B1C22;stroke:#2A2C35}
.lbl{font-size:9.5px;fill:#8F909A;letter-spacing:2px}
.big{font-size:40px;font-weight:700}
.sub{font-size:10.5px;fill:#8F909A}
.p{font-size:11.5px;fill:#C7C5D0}
.no{font-size:11.5px;fill:#6B6C76}
</style>
<rect width="880" height="470" rx="18" fill="#121318"/>
<rect x=".5" y=".5" width="879" height="469" rx="17.5" fill="none" stroke="#2A2C35"/>

<g class="f d1">
  <rect x="40" y="34" width="8" height="8" rx="1.5" fill="#B8C4FF"/>
  <text x="56" y="42" class="m" font-size="10" fill="#8F909A" letter-spacing="3">${esc(fit(q.label, 60))}</text>
  <text x="40" y="82" class="s" font-size="24" font-weight="700" fill="#E4E1E9">${acc(fit(q.title, 60), '#B8C4FF')}</text>
</g>

<!-- цифры -->
<g class="f d2">
  <rect x="40.5" y="110.5" width="190" height="130" rx="14" class="card"/>
  <text x="58" y="138" class="m lbl">${esc(fit(q.apk[0], 19))}</text>
  <text x="58" y="190" class="m big" fill="#B8C4FF">${esc(q.apk[1])}<tspan font-size="16"> ${esc(q.apk[2])}</tspan></text>
  <text x="58" y="220" class="m sub">${esc(fit(q.apk[3], 24))}</text>

  <rect x="246.5" y="110.5" width="190" height="130" rx="14" class="card"/>
  <text x="264" y="138" class="m lbl">${esc(fit(q.trackers[0], 19))}</text>
  <text x="264" y="190" class="m big" fill="#E4E1E9">0</text>
  <text x="264" y="220" class="m sub">${esc(fit(q.trackers[1], 24))}</text>

  <rect x="40.5" y="256.5" width="190" height="130" rx="14" class="card"/>
  <text x="58" y="284" class="m lbl">${esc(fit(q.support[0], 19))}</text>
  <text x="58" y="336" class="m big" fill="#E4E1E9">8–16</text>
  <text x="58" y="366" class="m sub">${esc(fit(q.support[1], 24))}</text>

  <rect x="246.5" y="256.5" width="190" height="130" rx="14" class="card"/>
  <text x="264" y="284" class="m lbl">${esc(fit(q.tests[0], 19))}</text>
  <text x="264" y="336" class="m big" fill="#E4E1E9">17/17</text>
  <text x="264" y="366" class="m sub">${acc(fit(q.tests[1], 24), '#B8C4FF')}</text>
</g>

<!-- разрешения -->
<g class="f d3">
  <rect x="452.5" y="110.5" width="387" height="276" rx="14" class="card"/>
  <text x="472" y="138" class="m lbl">${esc(fit(q.perms, 40))}</text>

  <g class="m">
${q.yes.map((s, i) => yes(i, s)).join('\n')}

    <line x1="472" y1="276" x2="820" y2="276" stroke="#2A2C35"/>

${q.no.map((s, i) => no(i, s)).join('\n')}
  </g>
</g>

<text x="40" y="430" class="m f d4" font-size="9.5" fill="#6B6C76">${esc(fit(q.source.replace('{version}', VERSION), 130))}</text>
</svg>
`;
}

function design(t) {
  const d = t.design;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="880" height="400" viewBox="0 0 880 400" role="img" aria-label="${esc(d.label)} — ${esc(plain(d.title))}">
<style>
${MONO}
${SANS}
.f{animation:in .7s ease-out both}
.d1{animation-delay:.1s}.d2{animation-delay:.25s}.d3{animation-delay:.4s}
@keyframes in{from{opacity:0;transform:translateY(6px)}to{opacity:1;transform:none}}
.card{fill:#F3EFF7;stroke:#E3E1EC}
.lbl{font-size:9.5px;fill:#5F6070;letter-spacing:2px}
.nm{font-size:10.5px;font-weight:700;fill:#1B1B21}
.hx{font-size:9.5px;fill:#5F6070}
</style>
${DOTS}
<rect width="880" height="400" rx="18" fill="#FBF8FF"/>
<rect width="880" height="400" rx="18" fill="url(#dots)"/>
<rect x=".5" y=".5" width="879" height="399" rx="17.5" fill="none" stroke="#E3E1EC"/>

<g class="f d1">
  <rect x="40" y="34" width="8" height="8" rx="1.5" fill="#4F5B92"/>
  <text x="56" y="42" class="m" font-size="10" fill="#5F6070" letter-spacing="3">${esc(fit(d.label, 60))}</text>
  <text x="40" y="82" class="s" font-size="24" font-weight="700" fill="#1B1B21">${acc(fit(d.title, 60), '#4F5B92')}</text>
  <text x="40" y="108" class="m" font-size="11" fill="#5F6070">${esc(fit(d.sub, 118))}</text>
</g>

<g class="f d2">
  <rect x="40.5" y="130.5" width="500" height="132" rx="14" class="card"/>
  <text x="58" y="156" class="m lbl">${esc(fit(d.palette, 50))}</text>
  <rect x="58" y="168" width="80" height="44" rx="10" fill="#4F5B92"/>
  <text x="58" y="230" class="m nm">Primary</text><text x="58" y="245" class="m hx">#4F5B92</text>
  <rect x="152.5" y="168.5" width="80" height="44" rx="10" fill="#DDE1FF" stroke="#CFD3F0"/>
  <text x="152" y="230" class="m nm">Container</text><text x="152" y="245" class="m hx">#DDE1FF</text>
  <rect x="246" y="168" width="80" height="44" rx="10" fill="#75546F"/>
  <text x="246" y="230" class="m nm">Tertiary</text><text x="246" y="245" class="m hx">#75546F</text>
  <rect x="340.5" y="168.5" width="80" height="44" rx="10" fill="#FFFFFF" stroke="#E3E1EC"/>
  <text x="340" y="230" class="m nm">Surface</text><text x="340" y="245" class="m hx">#FBF8FF</text>
  <rect x="434" y="168" width="80" height="44" rx="10" fill="#121318"/>
  <text x="434" y="230" class="m nm">Night</text><text x="434" y="245" class="m hx">#121318</text>
</g>

<g class="f d2">
  <rect x="556.5" y="130.5" width="283" height="132" rx="14" class="card"/>
  <text x="574" y="156" class="m lbl">${esc(fit(d.font, 26))}</text>
  <text x="574" y="200" class="s" font-size="34" fill="#1B1B21">07:30</text>
  <text x="690" y="186" class="m nm">Roboto</text>
  <text x="690" y="201" class="m hx">${esc(fit(d.digits, 24))}</text>
  <text x="574" y="244" class="s" font-size="26" font-weight="700" fill="#1B1B21">${esc(fit(d.sample, 5))}</text>
  <text x="690" y="230" class="m nm">Roboto Bold</text>
  <text x="690" y="245" class="m hx">${esc(fit(d.headings, 24))}</text>
</g>

<g class="f d3">
  <rect x="40.5" y="278.5" width="799" height="92" rx="14" class="card"/>
  <text x="58" y="304" class="m lbl">${esc(fit(d.shapes, 60))}</text>
  <rect x="58" y="316" width="150" height="40" rx="14" fill="#FFFFFF" stroke="#E3E1EC"/>
  <text x="74" y="342" class="s" font-size="17" fill="#1B1B21">06:45</text>
  <rect x="172" y="328" width="26" height="16" rx="8" fill="#4F5B92"/><circle cx="190" cy="336" r="5.5" fill="#fff"/>
  <text x="222" y="341" class="m hx">${esc(fit(d.card, 20))}</text>

  <circle cx="356" cy="336" r="16" fill="#4F5B92"/><text x="356" y="340" class="s" font-size="11" font-weight="700" fill="#fff" text-anchor="middle">${esc(fit(d.mon, 3))}</text>
  <circle cx="394" cy="336" r="16" fill="#E3E1EC"/><text x="394" y="340" class="s" font-size="11" font-weight="700" fill="#5F6070" text-anchor="middle">${esc(fit(d.sat, 3))}</text>
  <text x="422" y="341" class="m hx">${esc(fit(d.days, 17))}</text>

  <rect x="524" y="318" width="120" height="36" rx="18" fill="#4F5B92"/>
  <text x="584" y="341" class="s" font-size="12" font-weight="700" fill="#fff" text-anchor="middle">${esc(fit(d.save, 14))}</text>
  <text x="660" y="341" class="m hx">${esc(fit(d.button, 29))}</text>
</g>
</svg>
`;
}

const shots = [0, 1, 2, 3].map((i) => fs.readFileSync(path.join(DIR, `screens/${i}.jpg`)).toString('base64'));
function screens(t) {
  const s = t.screens;
  const phone = (i) => {
    const x = 44 + i * 206;
    return `<g class="f" style="animation-delay:${100 + i * 150}ms">
<clipPath id="c${i}"><rect x="${x}" y="34" width="186" height="400" rx="22"/></clipPath>
<rect x="${x - 7}" y="27" width="200" height="414" rx="29" fill="#1B1B21"/>
<image x="${x}" y="34" width="186" height="400" preserveAspectRatio="xMidYMin slice" clip-path="url(#c${i})" href="data:image/jpeg;base64,${shots[i]}"/>
<rect x="${x + 73}" y="42" width="40" height="10" rx="5" fill="#1B1B21"/>
<text x="${x + 93}" y="478" class="m" font-size="12" font-weight="700" fill="#1B1B21" text-anchor="middle">${esc(fit(s.labels[i], 26))}</text>
</g>`;
  };
  return `<svg xmlns="http://www.w3.org/2000/svg" width="880" height="520" viewBox="0 0 880 520" role="img" aria-label="${esc(s.alt)}">
<style>${MONO}
.f{animation:in .7s ease-out both}@keyframes in{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:none}}</style>
${DOTS}
<rect width="880" height="520" rx="18" fill="#FBF8FF"/><rect width="880" height="520" rx="18" fill="url(#dots)"/>
<rect x=".5" y=".5" width="879" height="519" rx="17.5" fill="none" stroke="#E3E1EC"/>
${[0, 1, 2, 3].map(phone).join('\n')}
</svg>
`;
}

fs.mkdirSync(OUT, { recursive: true });
const parts = { hero, cta, features, quality, design, screens };
for (const [code, t] of Object.entries(STR)) {
  lang = code;
  for (const [name, render] of Object.entries(parts)) fs.writeFileSync(path.join(OUT, `${name}-${code}.svg`), render(t));
}
if (errors.length) { console.error(errors.join('\n')); process.exit(1); }
console.log(`${Object.keys(STR).length} языков × ${Object.keys(parts).length} картинок → assets/readme`);
renderPngs();
