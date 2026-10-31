(() => {
  const $ = id => document.getElementById(id);
  const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
  const inr2 = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });
  const MONTHS = ['January','February','March','April','May','June','July','August','September','October','November','December'];

  const state = { year: new Date().getFullYear(), month: new Date().getMonth() + 1,
                  type: 'EXPENSE', categories: [], editingId: null, page: 0, size: 8, total: 0 };
  let pie, bar;

  const pad = n => String(n).padStart(2, '0');
  const monthKey = (y, m) => `${y}-${pad(m)}`;
  const esc = s => (s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

  async function api(path, opts = {}) {
    const res = await fetch('/api' + path, {
      headers: { 'Content-Type': 'application/json' }, ...opts,
      body: opts.body ? JSON.stringify(opts.body) : undefined
    });
    if (!res.ok) {
      let msg = 'Something went wrong. Try again.';
      try { msg = (await res.json()).error || msg; } catch (_) {}
      throw new Error(msg);
    }
    return res.status === 204 || res.headers.get('content-length') === '0' ? null : res.json().catch(() => null);
  }

  let toastTimer;
  function toast(msg) {
    const t = $('toast'); t.textContent = msg; t.classList.add('show');
    clearTimeout(toastTimer); toastTimer = setTimeout(() => t.classList.remove('show'), 2400);
  }

  /* ---------- categories ---------- */
  async function loadCategories() {
    state.categories = await api('/categories');
    fillFormCategories();
    const expenses = state.categories.filter(c => c.type === 'EXPENSE');
    $('bCategory').innerHTML = expenses.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
    $('fltCategory').innerHTML = '<option value="">All</option>' +
      state.categories.map(c => `<option value="${c.id}">${esc(c.name)} (${c.type.toLowerCase()})</option>`).join('');
  }
  function fillFormCategories(selectedId) {
    const list = state.categories.filter(c => c.type === state.type);
    $('fCategory').innerHTML = list.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
    if (selectedId) $('fCategory').value = selectedId;
  }

  /* ---------- report, charts, budgets ---------- */
  async function loadReport() {
    $('monthLabel').textContent = `${MONTHS[state.month - 1]} ${state.year}`;
    const r = await api(`/reports/monthly?year=${state.year}&month=${state.month}`);
    $('sumIncome').textContent = inr.format(r.income);
    $('sumExpense').textContent = inr.format(r.expense);
    const net = $('sumNet');
    net.textContent = inr.format(r.net);
    net.classList.toggle('negative', r.net < 0);
    drawPie(r.byCategory.filter(c => c.type === 'EXPENSE'));
    drawBar(r.trend);
    drawBudgets(r.budgets);
  }

  const palette = ['#24378f','#1b7556','#b9780c','#b8372b','#6b4fa0','#2a8bb0','#8a6d3b','#4d5d6a'];
  function drawPie(rows) {
    $('pieEmpty').hidden = rows.length > 0;
    $('pieChart').parentElement.hidden = rows.length === 0;
    if (pie) pie.destroy();
    if (!rows.length) return;
    pie = new Chart($('pieChart'), {
      type: 'doughnut',
      data: { labels: rows.map(r => r.category),
              datasets: [{ data: rows.map(r => r.total), backgroundColor: palette, borderWidth: 0 }] },
      options: { maintainAspectRatio: false, cutout: '58%',
        plugins: { legend: { position: 'right', labels: { boxWidth: 12 } },
                   tooltip: { callbacks: { label: c => ` ${c.label}: ${inr.format(c.parsed)}` } } } }
    });
  }
  function drawBar(trend) {
    if (bar) bar.destroy();
    bar = new Chart($('barChart'), {
      type: 'bar',
      data: { labels: trend.map(t => { const [y, m] = t.month.split('-'); return MONTHS[m - 1].slice(0, 3) + ' ' + y.slice(2); }),
        datasets: [
          { label: 'Income', data: trend.map(t => t.income), backgroundColor: '#1b7556', borderRadius: 4 },
          { label: 'Expenses', data: trend.map(t => t.expense), backgroundColor: '#24378f', borderRadius: 4 } ] },
      options: { maintainAspectRatio: false,
        scales: { y: { beginAtZero: true, ticks: { callback: v => '₹' + (v / 1000) + 'k' } } },
        plugins: { tooltip: { callbacks: { label: c => ` ${c.dataset.label}: ${inr.format(c.parsed.y)}` } } } }
    });
  }
  function drawBudgets(list) {
    $('budgetEmpty').hidden = list.length > 0;
    $('budgetList').innerHTML = list.map(b => {
      const pct = b.limit > 0 ? (b.spent / b.limit) * 100 : 0;
      const cls = pct >= 100 ? 'red' : pct >= 80 ? 'amber' : '';
      const left = b.limit - b.spent;
      return `<div class="budget">
        <div class="meta"><span>${esc(b.category)}</span><span>${inr.format(b.spent)} of ${inr.format(b.limit)}</span></div>
        <div class="meter" role="progressbar" aria-valuenow="${Math.round(pct)}" aria-valuemin="0" aria-valuemax="100" aria-label="${esc(b.category)} budget used">
          <i class="${cls}" style="width:${Math.min(pct, 100)}%"></i></div>
        <div class="left ${left < 0 ? 'over' : ''}">${left < 0 ? inr.format(-left) + ' over budget' : inr.format(left) + ' left'}</div>
      </div>`;
    }).join('');
  }

  /* ---------- transactions ---------- */
  function filterQuery() {
    const p = new URLSearchParams({ page: state.page, size: state.size });
    for (const [k, id] of [['from','fltFrom'],['to','fltTo'],['type','fltType'],['categoryId','fltCategory']])
      if ($(id).value) p.set(k, $(id).value);
    return p.toString();
  }
  async function loadTransactions() {
    const r = await api('/transactions?' + filterQuery());
    state.total = r.total;
    $('txnEmpty').hidden = r.items.length > 0;
    $('txnBody').innerHTML = r.items.map(t => `<tr>
      <td>${t.date}</td>
      <td>${esc(t.categoryName)}${t.overBudget ? '<span class="flag">over budget</span>' : ''}</td>
      <td>${esc(t.note)}</td>
      <td class="num ${t.type === 'INCOME' ? 'in' : ''}">${t.type === 'INCOME' ? '+' : '−'}${inr2.format(t.amount)}</td>
      <td class="act"><button data-edit="${t.id}">Edit</button><button class="del" data-del="${t.id}">Delete</button></td>
    </tr>`).join('');
    const pages = Math.max(1, Math.ceil(r.total / state.size));
    $('pageInfo').textContent = `Page ${state.page + 1} of ${pages} · ${r.total} transactions`;
    $('prevPage').disabled = state.page === 0;
    $('nextPage').disabled = state.page + 1 >= pages;
    state.rows = r.items;
  }

  function setType(type) {
    state.type = type;
    document.querySelectorAll('.seg button').forEach(b => b.classList.toggle('on', b.dataset.type === type));
    fillFormCategories();
  }
  function resetForm() {
    state.editingId = null;
    $('formTitle').textContent = 'Add transaction';
    $('saveBtn').textContent = 'Save transaction';
    $('cancelEdit').hidden = true;
    $('fAmount').value = ''; $('fNote').value = ''; $('formError').textContent = '';
    $('fDate').value = new Date().toISOString().slice(0, 10);
  }

  async function saveTransaction() {
    $('formError').textContent = '';
    const body = { categoryId: +$('fCategory').value, amount: $('fAmount').value,
                   date: $('fDate').value, note: $('fNote').value.trim() || null };
    try {
      if (state.editingId) await api('/transactions/' + state.editingId, { method: 'PUT', body });
      else await api('/transactions', { method: 'POST', body });
      toast(state.editingId ? 'Transaction updated' : 'Transaction saved');
      resetForm();
      await Promise.all([loadReport(), loadTransactions()]);
    } catch (e) { $('formError').textContent = e.message; }
  }

  $('txnBody').addEventListener('click', async e => {
    const edit = e.target.dataset.edit, del = e.target.dataset.del;
    if (edit) {
      const t = state.rows.find(r => r.id == edit);
      setType(t.type);
      fillFormCategories(t.categoryId);
      $('fAmount').value = t.amount; $('fDate').value = t.date; $('fNote').value = t.note ?? '';
      state.editingId = t.id;
      $('formTitle').textContent = 'Edit transaction';
      $('saveBtn').textContent = 'Save changes';
      $('cancelEdit').hidden = false;
      $('fAmount').focus();
    } else if (del && confirm('Delete this transaction? This can’t be undone.')) {
      try {
        await api('/transactions/' + del, { method: 'DELETE' });
        toast('Transaction deleted');
        if (state.rows.length === 1 && state.page > 0) state.page--;
        await Promise.all([loadReport(), loadTransactions()]);
      } catch (err) { toast(err.message); }
    }
  });

  /* ---------- budgets ---------- */
  async function setBudget() {
    $('budgetError').textContent = '';
    try {
      await api('/budgets', { method: 'POST', body: { categoryId: +$('bCategory').value,
        year: state.year, month: state.month, limit: $('bLimit').value } });
      $('bLimit').value = '';
      toast('Budget saved');
      await Promise.all([loadReport(), loadTransactions()]);
    } catch (e) { $('budgetError').textContent = e.message; }
  }
  async function copyBudgets() {
    const prev = new Date(state.year, state.month - 2, 1);
    const from = monthKey(prev.getFullYear(), prev.getMonth() + 1);
    try {
      const r = await api(`/budgets/copy?from=${from}&to=${monthKey(state.year, state.month)}`, { method: 'POST' });
      toast(r && r.copied ? `Copied ${r.copied} budgets` : 'Nothing new to copy');
      await loadReport();
    } catch (e) { $('budgetError').textContent = e.message; }
  }

  /* ---------- wiring ---------- */
  function shiftMonth(d) {
    const dt = new Date(state.year, state.month - 1 + d, 1);
    state.year = dt.getFullYear(); state.month = dt.getMonth() + 1;
    loadReport().catch(e => toast(e.message));
  }
  document.querySelectorAll('.seg button').forEach(b => b.addEventListener('click', () => setType(b.dataset.type)));
  $('saveBtn').addEventListener('click', saveTransaction);
  $('cancelEdit').addEventListener('click', resetForm);
  $('setBudget').addEventListener('click', setBudget);
  $('copyBudgets').addEventListener('click', copyBudgets);
  $('prevMonth').addEventListener('click', () => shiftMonth(-1));
  $('nextMonth').addEventListener('click', () => shiftMonth(1));
  ['fltFrom','fltTo','fltType','fltCategory'].forEach(id =>
    $(id).addEventListener('change', () => { state.page = 0; loadTransactions(); }));
  $('clearFilters').addEventListener('click', () => {
    ['fltFrom','fltTo','fltType','fltCategory'].forEach(id => $(id).value = '');
    state.page = 0; loadTransactions();
  });
  $('prevPage').addEventListener('click', () => { state.page--; loadTransactions(); });
  $('nextPage').addEventListener('click', () => { state.page++; loadTransactions(); });

  resetForm();
  loadCategories()
    .then(() => Promise.all([loadReport(), loadTransactions()]))
    .catch(e => toast('Could not load data: ' + e.message));
})();
