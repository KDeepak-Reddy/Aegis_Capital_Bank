const TRANSACTION_API = 'http://localhost:5005'; // Transaction Service Backend
const ACCOUNT_API     = 'http://localhost:5050'; // Account Service Backend

let currentAccountId = null;
let currentAccNo = null;
let token = null;

// ─── Utility ────────────────────────────────────────────────────
function showMessage(msg, type) {
    const msgBox = document.getElementById('messageBox');
    if (!msgBox) return;
    msgBox.textContent = msg;
    msgBox.className = `msg ${type}`;
    setTimeout(() => { msgBox.className = 'msg'; }, 5000);
}

async function fetchWithTimeout(resource, options = {}, timeoutMs = 10000) {
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), timeoutMs);
    try {
        return await fetch(resource, { ...options, signal: controller.signal });
    } finally {
        clearTimeout(id);
    }
}

// ─── Tab Switching ──────────────────────────────────────────────
function switchTab(tabName) {
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === tabName);
    });
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.toggle('active', content.id === `tab-${tabName}`);
    });
    // Auto-load history when switching to it
    if (tabName === 'history') {
        loadHistory();
    }
}

// ─── Transfer Mode Toggle ──────────────────────────────────────
function switchTransferMode(mode) {
    const ownForm = document.getElementById('transferOwnForm');
    const extForm = document.getElementById('transferExternalForm');
    const ownBtn  = document.getElementById('toggleOwnBtn');
    const extBtn  = document.getElementById('toggleExternalBtn');

    if (mode === 'own') {
        ownForm.classList.remove('hidden');
        extForm.classList.add('hidden');
        ownBtn.classList.add('active');
        extBtn.classList.remove('active');
    } else {
        ownForm.classList.add('hidden');
        extForm.classList.remove('hidden');
        ownBtn.classList.remove('active');
        extBtn.classList.add('active');
    }
}

// ─── Load User Accounts for Transfer Dropdown ───────────────────
async function loadOwnAccounts() {
    if (!token) return;
    try {
        const resp = await fetchWithTimeout(`${ACCOUNT_API}/api/accounts`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!resp.ok) return;
        const accounts = await resp.json();
        const select = document.getElementById('ownAccountSelect');
        select.innerHTML = '<option value="">-- Select an account --</option>';
        accounts.forEach(acc => {
            // Exclude the current account from the dropdown
            if (String(acc.id) !== String(currentAccountId)) {
                const opt = document.createElement('option');
                opt.value = acc.id;
                opt.textContent = `Acc: ${acc.accno} — ${acc.bankname} (₹${acc.balance})`;
                select.appendChild(opt);
            }
        });
    } catch (err) {
        console.error('Failed to load own accounts:', err);
    }
}

// ─── Deposit ────────────────────────────────────────────────────
async function handleDeposit(e) {
    e.preventDefault();
    const amount = parseFloat(document.getElementById('depositAmount').value);
    const pin = document.getElementById('depositPin').value.trim();
    if (!amount || amount <= 0) { showMessage('Please enter a valid amount.', 'error'); return; }
    if (!pin) { showMessage('Please enter your PIN.', 'error'); return; }

    const btn = document.getElementById('depositBtn');
    btn.disabled = true;
    btn.textContent = 'Processing...';

    try {
        const resp = await fetchWithTimeout(`${TRANSACTION_API}/transactions/deposit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ accountId: currentAccountId, amount: amount, pin: pin })
        });

        if (!resp.ok) {
            const err = await resp.text().catch(() => 'Unknown error');
            throw new Error(err);
        }

        const tx = await resp.json();
        if (tx.status !== 'SUCCESS') {
            showMessage(`Deposit failed: ${tx.status}`, 'error');
        } else {
            showMessage(`Successfully deposited ₹${amount}!`, 'success');
            document.getElementById('depositForm').reset();
        }
    } catch (err) {
        console.error(err);
        showMessage(`Deposit failed: ${err.message}`, 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<span class="btn-icon">↓</span> Deposit';
    }
}

// ─── Withdraw ───────────────────────────────────────────────────
async function handleWithdraw(e) {
    e.preventDefault();
    const amount = parseFloat(document.getElementById('withdrawAmount').value);
    const pin = document.getElementById('withdrawPin').value.trim();
    if (!amount || amount <= 0) { showMessage('Please enter a valid amount.', 'error'); return; }
    if (!pin) { showMessage('Please enter your PIN.', 'error'); return; }

    const btn = document.getElementById('withdrawBtn');
    btn.disabled = true;
    btn.textContent = 'Processing...';

    try {
        const resp = await fetchWithTimeout(`${TRANSACTION_API}/transactions/withdraw`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ accountId: currentAccountId, amount: amount, pin: pin })
        });

        if (!resp.ok) {
            const err = await resp.text().catch(() => 'Unknown error');
            throw new Error(err);
        }

        const tx = await resp.json();
        if (tx.status !== 'SUCCESS') {
            showMessage(`Withdrawal failed: ${tx.status}`, 'error');
        } else {
            showMessage(`Successfully withdrew ₹${amount}!`, 'success');
            document.getElementById('withdrawForm').reset();
        }
    } catch (err) {
        console.error(err);
        showMessage(`Withdrawal failed: ${err.message}`, 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<span class="btn-icon">↑</span> Withdraw';
    }
}

// ─── Transfer (Own Account) ─────────────────────────────────────
async function handleTransferOwn(e) {
    e.preventDefault();
    const toAccountId = document.getElementById('ownAccountSelect').value;
    const amount = parseFloat(document.getElementById('transferOwnAmount').value);
    const pin = document.getElementById('transferOwnPin').value.trim();

    if (!toAccountId) { showMessage('Please select a destination account.', 'error'); return; }
    if (!amount || amount <= 0) { showMessage('Please enter a valid amount.', 'error'); return; }
    if (!pin) { showMessage('Please enter your PIN.', 'error'); return; }

    const btn = document.getElementById('transferOwnBtn');
    btn.disabled = true;
    btn.textContent = 'Processing...';

    try {
        const resp = await fetchWithTimeout(`${TRANSACTION_API}/transactions/transfer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                fromAccountId: currentAccountId,
                toAccountId: parseInt(toAccountId),
                amount: amount,
                pin: pin
            })
        });

        if (!resp.ok) {
            const err = await resp.text().catch(() => 'Unknown error');
            throw new Error(err);
        }

        const tx = await resp.json();
        if (tx.status !== 'SUCCESS') {
            showMessage(`Transfer failed: ${tx.status}`, 'error');
        } else {
            showMessage(`Successfully transferred ₹${amount}!`, 'success');
            document.getElementById('transferOwnForm').reset();
            loadOwnAccounts(); // Refresh dropdown balances
        }
    } catch (err) {
        console.error(err);
        showMessage(`Transfer failed: ${err.message}`, 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<span class="btn-icon">⇄</span> Transfer';
    }
}

// ─── Transfer (External Account) ────────────────────────────────
async function handleTransferExternal(e) {
    e.preventDefault();
    const accNo  = document.getElementById('extAccNo').value.trim();
    const amount = parseFloat(document.getElementById('transferExtAmount').value);
    const pin    = document.getElementById('transferExtPin').value.trim();

    if (!accNo) { showMessage('Please enter the account number.', 'error'); return; }
    if (!amount || amount <= 0) { showMessage('Please enter a valid amount.', 'error'); return; }
    if (!pin) { showMessage('Please enter your PIN.', 'error'); return; }

    const btn = document.getElementById('transferExtBtn');
    btn.disabled = true;
    btn.textContent = 'Looking up account...';

    try {
        // 1) Resolve account number to account ID
        const lookupResp = await fetchWithTimeout(
            `${ACCOUNT_API}/internal/accounts/by-accno/${encodeURIComponent(accNo)}`
        );

        if (lookupResp.status === 404) {
            showMessage('Account not found. Please check the account number.', 'error');
            return;
        }
        if (!lookupResp.ok) {
            throw new Error('Failed to look up account');
        }

        const destAccount = await lookupResp.json();

        btn.textContent = 'Transferring...';

        // 2) Perform the transfer
        const resp = await fetchWithTimeout(`${TRANSACTION_API}/transactions/transfer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                fromAccountId: currentAccountId,
                toAccountId: destAccount.id,
                amount: amount,
                pin: pin
            })
        });

        if (!resp.ok) {
            const err = await resp.text().catch(() => 'Unknown error');
            throw new Error(err);
        }

        const tx = await resp.json();
        if (tx.status !== 'SUCCESS') {
            showMessage(`Transfer failed: ${tx.status}`, 'error');
        } else {
            showMessage(`Successfully transferred ₹${amount} to Acc: ${accNo}!`, 'success');
            document.getElementById('transferExternalForm').reset();
        }
    } catch (err) {
        console.error(err);
        showMessage(`Transfer failed: ${err.message}`, 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<span class="btn-icon">⇄</span> Transfer';
    }
}

// ─── History ────────────────────────────────────────────────────
async function loadHistory() {
    const loading  = document.getElementById('historyLoading');
    const empty    = document.getElementById('historyEmpty');
    const table    = document.getElementById('historyTable');
    const tbody    = document.getElementById('historyBody');

    loading.classList.remove('hidden');
    empty.classList.add('hidden');
    table.classList.add('hidden');

    try {
        const resp = await fetchWithTimeout(
            `${TRANSACTION_API}/transactions/history/${currentAccountId}`
        );

        if (!resp.ok) throw new Error('Failed to load history');

        const transactions = await resp.json();
        loading.classList.add('hidden');

        if (!transactions || transactions.length === 0) {
            empty.classList.remove('hidden');
            return;
        }

        tbody.innerHTML = '';
        transactions.reverse().forEach(tx => {
            const tr = document.createElement('tr');

            // Type badge
            const typeClass = tx.type === 'DEPOSIT' ? 'type-deposit' :
                              tx.type === 'WITHDRAW' ? 'type-withdraw' : 'type-transfer';

            // Status badge
            const statusClass = tx.status === 'SUCCESS' ? 'status-success' : 'status-failed';

            // Format date
            const date = tx.createdAt ? new Date(tx.createdAt).toLocaleString() : '-';

            tr.innerHTML = `
                <td>${tx.id}</td>
                <td><span class="type-badge ${typeClass}">${tx.type}</span></td>
                <td style="font-weight:600;">₹${tx.amount}</td>
                <td>${tx.referenceAccountId || '-'}</td>
                <td><span class="status-badge ${statusClass}">${tx.status}</span></td>
                <td style="color:var(--text-muted);font-size:12px;">${date}</td>
            `;
            tbody.appendChild(tr);
        });

        table.classList.remove('hidden');
    } catch (err) {
        console.error(err);
        loading.classList.add('hidden');
        showMessage('Failed to load transaction history.', 'error');
    }
}

// ─── Main Initialization ────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    const mainContainer = document.getElementById('mainContainer');
    const unauthState   = document.getElementById('unauthState');

    // Redirect button for unauth state
    document.getElementById('redirectToAccountsBtn')?.addEventListener('click', () => {
        window.location.href = 'http://localhost:5501/index.html';
    });

    // Parse URL params
    const urlParams = new URLSearchParams(window.location.search);
    const accountIdParam = urlParams.get('accountId');
    const accnoParam     = urlParams.get('accno');
    const tokenParam     = urlParams.get('token');

    // Store token from URL
    if (tokenParam) {
        localStorage.setItem('jwt', tokenParam);
        // Clean URL
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    token = localStorage.getItem('jwt');
    currentAccountId = accountIdParam;
    currentAccNo = accnoParam;

    // Validate we have what we need
    if (!currentAccountId) {
        mainContainer?.classList.add('hidden');
        unauthState?.classList.remove('hidden');
        return;
    }

    // Show authenticated UI
    unauthState?.classList.add('hidden');
    mainContainer?.classList.remove('hidden');

    // Display account badge
    document.getElementById('accountBadge').textContent = `Account: ${currentAccNo || currentAccountId}`;

    // Back button
    document.getElementById('backToAccountsBtn')?.addEventListener('click', () => {
        window.location.href = `http://localhost:5501/index.html?token=${token}`;
    });

    // Tab navigation
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => switchTab(btn.dataset.tab));
    });

    // Transfer mode toggle
    document.getElementById('toggleOwnBtn')?.addEventListener('click', () => switchTransferMode('own'));
    document.getElementById('toggleExternalBtn')?.addEventListener('click', () => switchTransferMode('external'));

    // Form handlers
    document.getElementById('depositForm')?.addEventListener('submit', handleDeposit);
    document.getElementById('withdrawForm')?.addEventListener('submit', handleWithdraw);
    document.getElementById('transferOwnForm')?.addEventListener('submit', handleTransferOwn);
    document.getElementById('transferExternalForm')?.addEventListener('submit', handleTransferExternal);

    // Refresh history button
    document.getElementById('refreshHistoryBtn')?.addEventListener('click', loadHistory);

    // Load own accounts for transfer dropdown
    loadOwnAccounts();
});
