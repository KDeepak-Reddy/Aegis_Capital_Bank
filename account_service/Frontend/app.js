const API_BASE_URL = 'http://localhost:5050'; // Account Service Backend
// If your Auth app is a SPA (Vite/React), this might be '/login' or '/' instead of a .html
const AUTH_FRONTEND_URL = 'http://localhost:5173/login.html';

/** Utility: fetch with timeout to avoid infinite "Loading..." */
async function fetchWithTimeout(resource, options = {}, timeoutMs = 10000) {
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), timeoutMs);
    try {
        const resp = await fetch(resource, { ...options, signal: controller.signal });
        return resp;
    } finally {
        clearTimeout(id);
    }
}

/** UI helpers */
function showState(stateId) {
    document.getElementById('loadingState')?.classList.add('hidden');
    document.getElementById('dashboardState')?.classList.add('hidden');
    document.getElementById('createAccountState')?.classList.add('hidden');
    document.getElementById(stateId)?.classList.remove('hidden');
}

function showCreateForm() { showState('createAccountState'); }
function hideCreateForm() { showState('dashboardState'); }

function showMessage(msg, type) {
    const msgBox = document.getElementById('messageBox');
    if (!msgBox) return;
    msgBox.textContent = msg;
    msgBox.className = `msg ${type}`;
    setTimeout(() => { msgBox.className = 'msg hidden'; }, 5000);
}

/** Main load */
document.addEventListener('DOMContentLoaded', () => {
    // 0) Always attach unauth button (works even without a token)
    const redirectBtn = document.getElementById('redirectToLoginBtn');
    if (redirectBtn) {
        redirectBtn.addEventListener('click', () => {
            window.location.href = AUTH_FRONTEND_URL;
        });
    }

    // 1) Capture token via URL for cross-origin SSO
    const urlParams = new URLSearchParams(window.location.search);
    const tokenFromUrl = urlParams.get('token');
    if (tokenFromUrl) {
        localStorage.setItem('jwt', tokenFromUrl);
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    const token = localStorage.getItem('jwt');
    const mainContainer = document.getElementById('mainContainer');
    const unauthState = document.getElementById('unauthState');

    // 2) Attach Logout button early (even before token checks) — safe no-ops without token
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            try {
                localStorage.removeItem('jwt');
                localStorage.removeItem('mfaEmail');
            } finally {
                // Force a full page reload to reset all state/listeners
                window.location.href = window.location.pathname;
            }
        });
    }

    // 2b) Back to Dashboard button
    const backToDashboardBtn = document.getElementById('backToDashboardBtn');
    if (backToDashboardBtn) {
        backToDashboardBtn.addEventListener('click', () => {
            window.location.href = 'http://localhost:5173/dashboard.html';
        });
    }

    // 3) Token missing → Unauth state & return
    if (!token) {
        mainContainer?.classList.add('hidden');
        unauthState?.classList.remove('hidden');
        return;
    }

    // 4) Authenticated UI
    unauthState?.classList.add('hidden');
    mainContainer?.classList.remove('hidden');

    // Decode token email (best-effort)
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        document.getElementById('userEmailDisplay').textContent = payload.sub || 'User';
    } catch {
        document.getElementById('userEmailDisplay').textContent = 'Authenticated User';
    }

    // 5) Attach authenticated-only listeners
    document.getElementById('showCreateFormBtn')?.addEventListener('click', showCreateForm);
    document.getElementById('cancelCreateBtn')?.addEventListener('click', hideCreateForm);
    document.getElementById('createAccountForm')?.addEventListener('submit', handleCreateAccount);

    // 6) Load accounts
    loadUserAccounts();
});

async function loadUserAccounts() {
    const token = localStorage.getItem('jwt');
    const accountsGrid = document.getElementById('accountsGrid');

    showState('loadingState');

    try {
        const response = await fetchWithTimeout(`${API_BASE_URL}/api/accounts`, {
            headers: { 'Authorization': `Bearer ${token}` }
        }, 10000);

        // Handle auth errors
        if (response.status === 401 || response.status === 403) {
            localStorage.removeItem('jwt');
            window.location.href = window.location.pathname; // Reload to unauth state
            return;
        }

        if (!response.ok) {
            const errTxt = await response.text().catch(() => '');
            throw new Error(`Failed to fetch accounts: ${response.status} ${errTxt}`);
        }

        // If server returned empty body by mistake, guard parsing
        let accounts = [];
        const text = await response.text();
        if (text && text.trim().length > 0) {
            accounts = JSON.parse(text);
        }

        // Render
        accountsGrid.innerHTML = '';
        if (!Array.isArray(accounts) || accounts.length === 0) {
            showMessage("Welcome! You don't have any accounts yet. Please create one.", "success");
            showState('createAccountState');
            document.getElementById('cancelCreateBtn')?.classList.add('hidden');
        } else {
            accounts.forEach(acc => {
                const card = document.createElement('div');
                card.className = 'account-card';
                card.innerHTML = `
                    <div class="account-number">Acc: ${acc.accno}</div>
                    <div class="account-details">
                        <strong>Bank:</strong> ${acc.bankname} <br/>
                        <strong>IFSC:</strong> ${acc.ifsccode}
                    </div>
                    <div class="account-balance">₹ ${acc.balance}</div>
                    <div style="display:flex;gap:8px;margin-top:12px;flex-wrap:wrap;">
                        <button class="txn-btn" data-id="${acc.id}" data-accno="${acc.accno}" style="padding:8px 16px;flex:1;min-width:120px;">Transactions</button>
                        <button class="reset-pin-btn secondary" data-id="${acc.id}" data-accno="${acc.accno}" style="padding:8px 16px;flex:1;min-width:120px;">🔒 Reset PIN</button>
                    </div>
                `;
                accountsGrid.appendChild(card);
            });

            // Attach transaction button listeners
            document.querySelectorAll('.txn-btn').forEach(btn => {
                btn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const accId = btn.dataset.id;
                    const accNo = btn.dataset.accno;
                    const jwt = localStorage.getItem('jwt');
                    window.location.href = `http://localhost:5502/index.html?accountId=${accId}&accno=${accNo}&token=${jwt}`;
                });
            });

            // Attach reset PIN button listeners
            document.querySelectorAll('.reset-pin-btn').forEach(btn => {
                btn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    openResetPinModal(btn.dataset.id, btn.dataset.accno);
                });
            });

            showState('dashboardState');
            document.getElementById('cancelCreateBtn')?.classList.remove('hidden');
        }

    } catch (error) {
        console.error('Error in loadUserAccounts:', error);
        showMessage("An error occurred while loading accounts. Please try again.", "error");
        showState('dashboardState'); // Fallback to dashboard so user can at least try again
    }
}

async function handleCreateAccount(e) {
    e.preventDefault();
    const token = localStorage.getItem('jwt');

    const bankName = document.getElementById('bankName').value.trim();
    const ifscCode = document.getElementById('ifscCode').value.trim();
    const initialBalance = document.getElementById('initialBalance').value.trim();
    const pin = document.getElementById('pin').value.trim();

    const submitBtn = document.getElementById('submitCreateBtn');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Creating...';

    // Send as query params (matches your backend)
    const params = new URLSearchParams();
    params.append('bankName', bankName);
    params.append('ifscCode', ifscCode);
    params.append('pin', pin);
    // Make initial deposit optional: only send if non-empty
    if (initialBalance !== '') params.append('initialBalance', initialBalance);

    try {
        const response = await fetchWithTimeout(`${API_BASE_URL}/api/accounts?${params.toString()}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        }, 10000);

        if (response.status === 401 || response.status === 403) {
            localStorage.removeItem('jwt');
            window.location.href = window.location.pathname;
            return;
        }

        if (!response.ok) {
            const err = await response.text().catch(() => response.statusText);
            throw new Error(err || 'Failed to create account');
        }

        showMessage("Account created successfully!", "success");
        document.getElementById('createAccountForm').reset();
        await loadUserAccounts();

    } catch (error) {
        console.error('Error in handleCreateAccount:', error);
        showMessage(`Failed to create account: ${error.message}`, "error");
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Create Account';
    }
}

// ─── Reset PIN Modal ──────────────────────────────────────────────────
let _resetPinAccountId = null;

function openResetPinModal(accountId, accNo) {
    _resetPinAccountId = accountId;
    document.getElementById('resetPinAccNo').textContent = accNo;
    document.getElementById('resetPinForm').reset();
    const msg = document.getElementById('resetPinMsg');
    msg.textContent = ''; msg.className = 'msg';
    const modal = document.getElementById('resetPinModal');
    modal.style.display = 'flex';
}

function closeResetPinModal() {
    document.getElementById('resetPinModal').style.display = 'none';
    _resetPinAccountId = null;
}

// Close on backdrop click
document.getElementById('resetPinModal')?.addEventListener('click', (e) => {
    if (e.target === document.getElementById('resetPinModal')) closeResetPinModal();
});

document.getElementById('resetPinForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const oldPin = document.getElementById('oldPin').value.trim();
    const newPin = document.getElementById('newPinInput').value.trim();
    const confirmPin = document.getElementById('confirmPin').value.trim();
    const msg = document.getElementById('resetPinMsg');

    if (newPin !== confirmPin) {
        msg.textContent = 'New PINs do not match.';
        msg.className = 'msg error';
        return;
    }
    if (!/^\d{4,6}$/.test(newPin)) {
        msg.textContent = 'PIN must be 4–6 digits.';
        msg.className = 'msg error';
        return;
    }

    const btn = document.getElementById('resetPinSubmitBtn');
    btn.disabled = true; btn.textContent = 'Updating...';
    const token = localStorage.getItem('jwt');
    const params = new URLSearchParams({ oldPin, newPin });

    try {
        const res = await fetchWithTimeout(
            `${API_BASE_URL}/api/accounts/${_resetPinAccountId}/reset-pin?${params}`,
            { method: 'PUT', headers: { 'Authorization': `Bearer ${token}` } },
            10000
        );
        const text = await res.text();
        if (res.ok) {
            msg.textContent = '✅ PIN updated successfully!';
            msg.className = 'msg success';
            setTimeout(closeResetPinModal, 1800);
        } else {
            msg.textContent = text || 'Failed to update PIN.';
            msg.className = 'msg error';
        }
    } catch (err) {
        msg.textContent = 'Connection error. Please try again.';
        msg.className = 'msg error';
    } finally {
        btn.disabled = false; btn.textContent = 'Update PIN';
    }
});
