/* ============================================================
   منصة متابعة منظومة اجتماعات نائب الأمين - سكربتات الواجهة
   ============================================================ */

// تبديل الشريط الجانبي على الأجهزة الصغيرة
function toggleSidebar() {
  document.querySelector('.sidebar')?.classList.toggle('open');
}

// إخفاء رسائل Toast تلقائيًا
document.addEventListener('DOMContentLoaded', function () {
  document.querySelectorAll('.toast').forEach(function (t) {
    setTimeout(function () {
      t.style.transition = 'opacity .4s';
      t.style.opacity = '0';
      setTimeout(() => t.remove(), 400);
    }, 4000);
  });

  // بحث فوري في الجداول
  document.querySelectorAll('[data-table-search]').forEach(function (input) {
    input.addEventListener('input', function () {
      const term = this.value.trim().toLowerCase();
      const table = document.querySelector(this.getAttribute('data-table-search'));
      if (!table) return;
      table.querySelectorAll('tbody tr').forEach(function (tr) {
        tr.style.display = tr.innerText.toLowerCase().includes(term) ? '' : 'none';
      });
    });
  });

  // فرز الأعمدة عند النقر على الترويسة
  document.querySelectorAll('table.data thead th[data-sort]').forEach(function (th, idx) {
    th.style.cursor = 'pointer';
    let asc = true;
    th.addEventListener('click', function () {
      const table = th.closest('table');
      const tbody = table.querySelector('tbody');
      const rows = Array.from(tbody.querySelectorAll('tr'));
      const colIndex = Array.from(th.parentNode.children).indexOf(th);
      rows.sort(function (a, b) {
        const av = a.children[colIndex]?.innerText.trim() || '';
        const bv = b.children[colIndex]?.innerText.trim() || '';
        const an = parseFloat(av.replace('%', '')); const bn = parseFloat(bv.replace('%', ''));
        if (!isNaN(an) && !isNaN(bn)) return asc ? an - bn : bn - an;
        return asc ? av.localeCompare(bv, 'ar') : bv.localeCompare(av, 'ar');
      });
      asc = !asc;
      rows.forEach(r => tbody.appendChild(r));
    });
  });

  // تأكيد الحذف
  document.querySelectorAll('form[data-confirm]').forEach(function (form) {
    form.addEventListener('submit', function (e) {
      if (!confirm(form.getAttribute('data-confirm'))) e.preventDefault();
    });
  });
});

// ============ مساعدات Chart.js ============
const AR_FONT = "Bahij, 'IBM Plex Sans Arabic', sans-serif";
const PALETTE = ['#22744a', '#2980b9', '#c9a227', '#c0392b', '#8a92a6', '#2e9e65',
                 '#0d3b24', '#e67e22', '#7b68a6', '#16a085', '#d35400', '#34495e'];

function makeDoughnut(canvasId, data) {
  const el = document.getElementById(canvasId);
  if (!el || !data || !data.length) return;
  const _ex = Chart.getChart(el); if (_ex) _ex.destroy();
  new Chart(el, {
    type: 'doughnut',
    data: {
      labels: data.map(d => d.label),
      datasets: [{
        data: data.map(d => d.value),
        backgroundColor: data.map((d, i) => d.color || PALETTE[i % PALETTE.length]),
        borderWidth: 2, borderColor: '#fff'
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { position: 'bottom', labels: { font: { family: AR_FONT, size: 12 }, padding: 12 } } }
    }
  });
}

function makeBar(canvasId, data, horizontal, label) {
  const el = document.getElementById(canvasId);
  if (!el || !data || !data.length) return;
  const _ex = Chart.getChart(el); if (_ex) _ex.destroy();
  new Chart(el, {
    type: 'bar',
    data: {
      labels: data.map(d => d.label),
      datasets: [{
        label: label || 'عدد المهام',
        data: data.map(d => d.value),
        backgroundColor: data.map((d, i) => d.color || PALETTE[i % PALETTE.length]),
        borderRadius: 6
      }]
    },
    options: {
      indexAxis: horizontal ? 'y' : 'x',
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { ticks: { font: { family: AR_FONT, size: 11 } }, grid: { display: !horizontal } },
        y: { ticks: { font: { family: AR_FONT, size: 11 } }, grid: { display: horizontal }, beginAtZero: true }
      }
    }
  });
}

function makeLine(canvasId, data, label) {
  const el = document.getElementById(canvasId);
  if (!el || !data || !data.length) return;
  const _ex = Chart.getChart(el); if (_ex) _ex.destroy();
  new Chart(el, {
    type: 'line',
    data: {
      labels: data.map(d => d.label),
      datasets: [{
        label: label || '', data: data.map(d => d.value),
        borderColor: '#22744a', backgroundColor: 'rgba(46,158,101,.15)',
        fill: true, tension: .35, pointBackgroundColor: '#c9a227'
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { ticks: { font: { family: AR_FONT, size: 11 } } },
        y: { beginAtZero: true, max: 100, ticks: { font: { family: AR_FONT, size: 11 } } }
      }
    }
  });
}

// ============ اختيار متعدد للجهات ============
function updateMsDd(dd) {
  if (!dd) return;
  const checked = [...dd.querySelectorAll('.ms-menu input[type=checkbox]:checked')];
  const val = checked.map(c => c.value).join(',');
  const hidden = dd.querySelector('.ms-value');
  if (hidden) hidden.value = val;
  const toggle = dd.querySelector('.ms-toggle-text');
  if (toggle) toggle.textContent = checked.length === 0
    ? (dd.getAttribute('data-placeholder') || '— اختر —')
    : (checked.length + ' ' + (dd.getAttribute('data-unit') || 'جهة') + ' مختارة');
}
document.addEventListener('click', function (e) {
  const toggle = e.target.closest('.ms-toggle');
  if (toggle) {
    const dd = toggle.closest('.ms-dd');
    const wasOpen = dd.classList.contains('open');
    document.querySelectorAll('.ms-dd.open').forEach(d => d.classList.remove('open'));
    if (!wasOpen) dd.classList.add('open');
    e.preventDefault();
    return;
  }
  if (!e.target.closest('.ms-menu')) {
    document.querySelectorAll('.ms-dd.open').forEach(d => d.classList.remove('open'));
  }
});
document.addEventListener('change', function (e) {
  if (e.target.matches('.ms-menu input[type=checkbox]')) {
    updateMsDd(e.target.closest('.ms-dd'));
  }
});
document.addEventListener('DOMContentLoaded', function () {
  document.querySelectorAll('.ms-dd').forEach(updateMsDd);
});

// ============ الوضع الليلي / النهاري ============
function toggleTheme() {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  if (isDark) {
    document.documentElement.removeAttribute('data-theme');
    try { localStorage.setItem('theme', 'light'); } catch (e) {}
  } else {
    document.documentElement.setAttribute('data-theme', 'dark');
    try { localStorage.setItem('theme', 'dark'); } catch (e) {}
  }
  updateThemeLabels();
}
function updateThemeLabels() {
  const dark = document.documentElement.getAttribute('data-theme') === 'dark';
  document.querySelectorAll('[data-theme-icon]').forEach(el => {
    el.className = (dark ? 'fa-solid fa-sun' : 'fa-solid fa-moon');
  });
  document.querySelectorAll('[data-theme-label]').forEach(el => {
    el.innerHTML = dark
      ? '<i class="fa-solid fa-sun"></i> الوضع النهاري'
      : '<i class="fa-solid fa-moon"></i> الوضع الليلي';
  });
}
document.addEventListener('DOMContentLoaded', updateThemeLabels);

// ============ محرّك الفلاتر التفاعلية ============
// يربط نموذج فلاتر بجلب جزء (fragment) وتحديث منطقة النتائج دون إعادة تحميل.
function initInteractiveFilters(opts) {
  const form = document.getElementById(opts.formId);
  if (!form) return;
  const fragmentUrl = opts.fragmentUrl;
  const pageUrl = opts.pageUrl || location.pathname;

  function queryString() {
    return new URLSearchParams(new FormData(form)).toString();
  }
  let reqSeq = 0;
  function update() {
    const qs = queryString();
    const mySeq = ++reqSeq;
    const spinner = document.getElementById(opts.spinnerId);
    if (spinner) spinner.style.display = 'inline-flex';
    const body = document.getElementById(opts.bodyId);
    if (body) body.style.opacity = '0.45';
    fetch(fragmentUrl + '?' + qs, { headers: { 'X-Requested-With': 'fetch' } })
      .then(r => r.text())
      .then(html => {
        if (mySeq !== reqSeq) return; // تجاهل الاستجابات القديمة
        const cur = document.getElementById(opts.bodyId);
        if (cur) cur.outerHTML = html;
        history.replaceState(null, '', pageUrl + (qs ? '?' + qs : ''));
        if (spinner) spinner.style.display = 'none';
        if (typeof opts.onUpdate === 'function') opts.onUpdate(qs);
      })
      .catch(() => {
        if (mySeq !== reqSeq) return;
        const cur = document.getElementById(opts.bodyId);
        if (cur) cur.style.opacity = '1';
        if (spinner) spinner.style.display = 'none';
      });
  }

  form.querySelectorAll('select').forEach(s => s.addEventListener('change', update));
  let timer;
  form.querySelectorAll('input[type=text], input[type=date]').forEach(inp =>
    inp.addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(update, 400); }));
  form.querySelectorAll('.chip[data-target]').forEach(chip => chip.addEventListener('click', function () {
    const input = document.getElementById(this.dataset.target);
    if (input.value === 'true') { input.value = ''; this.classList.remove('active'); }
    else { input.value = 'true'; this.classList.add('active'); }
    update();
  }));
  form.addEventListener('submit', e => { e.preventDefault(); update(); });

  window[opts.resetName || 'resetFilters'] = function () {
    form.querySelectorAll('input[type=text], input[type=date]').forEach(i => i.value = '');
    form.querySelectorAll('select').forEach(s => s.value = '');
    form.querySelectorAll('input[type=hidden]').forEach(h => h.value = '');
    form.querySelectorAll('.chip[data-target]').forEach(c => c.classList.remove('active'));
    update();
  };
  if (opts.exposeName) window[opts.exposeName] = update;
}
