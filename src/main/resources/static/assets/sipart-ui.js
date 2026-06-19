(() => {
  const escapeHtml = (value) => String(value ?? '').replace(/[&<>'"]/g, (char) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  })[char]);

  const roleScope = {
    Owner: new Set(['/dashboard', '/produk-kategori', '/stock', '/supplier', '/transaksi-pembelian', '/manajemen-user']),
    'Admin Gudang': new Set(['/dashboard', '/produk-kategori', '/stock', '/supplier', '/transaksi-pembelian']),
    'Admin Kasir': new Set(['/transaksi-penjualan'])
  };
  const pathPermission = {
    '/dashboard': 'dashboard', '/produk-kategori': 'products', '/stock': 'stock',
    '/supplier': 'suppliers', '/transaksi-pembelian': 'purchases',
    '/transaksi-penjualan': 'sales', '/manajemen-user': 'users'
  };

  function initials(name) {
    return String(name || 'SP').split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join('');
  }

  function applySession(user) {
    document.body.dataset.sessionRole = user.role;
    const label = user.role;
    document.querySelectorAll('.brand-role').forEach((node) => { node.textContent = label; });
    document.querySelectorAll('.profile-name').forEach((node) => { node.textContent = user.name; });
    document.querySelectorAll('.profile > .avatar').forEach((node) => {
      node.textContent = initials(user.name);
      node.setAttribute('aria-label', 'Avatar ' + user.name);
    });

    const allowed = roleScope[user.role] || new Set();
    document.querySelectorAll('.nav-item').forEach((link) => {
      const path = new URL(link.href, window.location.origin).pathname;
      const permission = user.permissions?.[pathPermission[path]];
      link.classList.toggle('hidden', !allowed.has(path) || !permission?.page);
    });
    setupGlobalSearch(user);
    return user;
  }

  function setupMobileNavigation() {
    const sidebar = document.querySelector('.sidebar');
    const brand = sidebar?.querySelector('.brand');
    const navigation = sidebar?.querySelector('.nav-list');
    const logout = sidebar?.querySelector('.logout');
    if (!sidebar || !brand || !navigation || !logout || brand.querySelector('[data-mobile-menu]')) return;

    brand.classList.add('flex', 'items-center', 'justify-between', 'gap-3');
    const button = document.createElement('button');
    button.type = 'button';
    button.dataset.mobileMenu = '';
    button.className = 'grid h-10 w-10 place-items-center rounded-lg border border-[#e5c7b8] bg-white text-[#6f625c] transition hover:border-[#b44f00] hover:bg-[#fff3e9] hover:text-[#8f3d00] lg:hidden';
    button.setAttribute('aria-label', 'Buka navigasi');
    button.setAttribute('aria-expanded', 'false');
    button.innerHTML = '<span class="grid gap-1"><span class="block h-0.5 w-5 bg-current"></span><span class="block h-0.5 w-5 bg-current"></span><span class="block h-0.5 w-5 bg-current"></span></span>';
    brand.appendChild(button);
    navigation.classList.add('hidden', 'lg:flex');
    logout.classList.add('hidden', 'lg:flex');

    button.addEventListener('click', () => {
      const open = button.getAttribute('aria-expanded') !== 'true';
      button.setAttribute('aria-expanded', String(open));
      button.setAttribute('aria-label', open ? 'Tutup navigasi' : 'Buka navigasi');
      navigation.classList.toggle('hidden', !open);
      logout.classList.toggle('hidden', !open);
    });
  }

  function setupGlobalSearch(user) {
    const input = document.querySelector('.topbar .search input[type="search"]');
    if (!input || input.dataset.globalSearchReady) return;
    input.dataset.globalSearchReady = 'true';
    const currentPath = window.location.pathname;
    if (currentPath === '/produk-kategori') {
      const query = new URLSearchParams(window.location.search).get('q');
      if (query) input.value = query;
      return;
    }
    if (!user.permissions?.products?.page) return;
    input.addEventListener('keydown', (event) => {
      if (event.key !== 'Enter') return;
      event.preventDefault();
      const query = input.value.trim();
      window.location.href = '/produk-kategori' + (query ? '?q=' + encodeURIComponent(query) : '');
    });
  }

  async function requireSession() {
    const response = await fetch('/api/auth/session', { headers: { Accept: 'application/json' } });
    if (response.status === 401) {
      window.location.replace('/login');
      return null;
    }
    if (!response.ok) throw new Error('Sesi tidak dapat dimuat.');
    return applySession(await response.json());
  }

  function inputMarkup(field) {
    const type = field.type || 'text';
    const common = `name="${escapeHtml(field.name)}" ${field.required === false ? '' : 'required'} class="h-11 w-full rounded-lg border border-[#e7c7b6] bg-[#fffaf7] px-3.5 text-sm text-[#201a17] outline-none transition focus:border-[#ff7800] focus:ring-2 focus:ring-orange-100"`;
    if (type === 'select') {
      const options = (field.options || []).map((option) => {
        const value = typeof option === 'string' ? option : option.value;
        const label = typeof option === 'string' ? option : option.label;
        return `<option value="${escapeHtml(value)}" ${String(value) === String(field.value ?? '') ? 'selected' : ''}>${escapeHtml(label)}</option>`;
      }).join('');
      return `<select ${common}>${options}</select>`;
    }
    if (type === 'textarea') {
      return `<textarea name="${escapeHtml(field.name)}" ${field.required === false ? '' : 'required'} rows="3" class="min-h-24 w-full rounded-lg border border-[#e7c7b6] bg-[#fffaf7] px-3.5 py-3 text-sm text-[#201a17] outline-none transition focus:border-[#ff7800] focus:ring-2 focus:ring-orange-100">${escapeHtml(field.value ?? '')}</textarea>`;
    }
    const min = field.min == null ? '' : `min="${escapeHtml(field.min)}"`;
    return `<input type="${escapeHtml(type)}" ${common} ${min} value="${escapeHtml(field.value ?? '')}" placeholder="${escapeHtml(field.placeholder ?? '')}">`;
  }

  function form({ title, subtitle = '', fields = [], submitLabel = 'Simpan' }) {
    return new Promise((resolve) => {
      const dialog = document.createElement('dialog');
      dialog.className = 'm-auto w-[calc(100%-2rem)] max-w-xl overflow-hidden rounded-lg border border-[#efc8b6] bg-white p-0 text-[#201a17] shadow-2xl backdrop:bg-black/45';
      dialog.innerHTML = `
        <form class="sipart-modal-form" novalidate>
          <header class="flex items-start justify-between gap-4 border-b border-[#ead8cf] px-5 py-4 sm:px-6">
            <div><h2 class="text-lg font-black">${escapeHtml(title)}</h2>${subtitle ? `<p class="mt-1 text-xs text-[#756862]">${escapeHtml(subtitle)}</p>` : ''}</div>
            <button type="button" data-close class="grid h-9 w-9 shrink-0 place-items-center rounded-lg text-xl text-[#756862] hover:bg-stone-100" aria-label="Tutup">&times;</button>
          </header>
          <div class="grid max-h-[65vh] gap-4 overflow-y-auto px-5 py-5 sm:grid-cols-2 sm:px-6">
            ${fields.map((field) => `<label class="grid gap-1.5 text-xs font-bold text-[#67554d] ${field.full ? 'sm:col-span-2' : ''}"><span>${escapeHtml(field.label)}</span>${inputMarkup(field)}</label>`).join('')}
          </div>
          <footer class="flex justify-end gap-3 border-t border-[#ead8cf] bg-[#fffaf7] px-5 py-4 sm:px-6">
            <button type="button" data-close class="min-h-10 rounded-lg border border-[#e4a77f] px-4 font-bold text-[#a34908] hover:bg-orange-50">Batal</button>
            <button type="submit" class="min-h-10 rounded-lg bg-[#ff7800] px-5 font-extrabold text-white shadow-[0_8px_18px_rgba(255,120,0,.2)] hover:bg-[#e56d00]">${escapeHtml(submitLabel)}</button>
          </footer>
        </form>`;
      document.body.appendChild(dialog);
      let settled = false;
      const finish = (value) => {
        if (settled) return;
        settled = true;
        dialog.close();
        dialog.remove();
        resolve(value);
      };
      dialog.querySelectorAll('[data-close]').forEach((button) => button.addEventListener('click', () => finish(null)));
      dialog.addEventListener('cancel', (event) => { event.preventDefault(); finish(null); });
      dialog.addEventListener('click', (event) => { if (event.target === dialog) finish(null); });
      dialog.querySelector('form').addEventListener('submit', (event) => {
        event.preventDefault();
        const formElement = event.currentTarget;
        if (!formElement.reportValidity()) return;
        finish(Object.fromEntries(new FormData(formElement).entries()));
      });
      dialog.showModal();
      dialog.querySelector('input, select, textarea')?.focus();
    });
  }

  function confirmAction({ title, message, confirmLabel = 'Hapus', danger = true }) {
    return form({
      title,
      subtitle: message,
      fields: [],
      submitLabel: confirmLabel,
      danger
    }).then((value) => value !== null);
  }

  setupMobileNavigation();
  const ready = requireSession().catch(() => {
    window.location.replace('/login');
    return null;
  });
  window.SipartSession = { require: requireSession, apply: applySession, ready };
  window.SipartUI = { form, confirm: confirmAction, escapeHtml };
})();
