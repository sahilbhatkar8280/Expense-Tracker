const state = {
    token: localStorage.getItem('jwt') || null,
    isLoginMode: true,
    pieChart: null,
    barChart: null,
    historyData: [],
    categories: []
};

// DOM Elements
const authContainer = document.getElementById('auth-container');
const appContainer = document.getElementById('app-container');
const authForm = document.getElementById('auth-form');
const usernameInput = document.getElementById('username');
const passwordInput = document.getElementById('password');
const btnLoginMode = document.getElementById('show-login');
const btnRegisterMode = document.getElementById('show-register');
const authSubmit = document.getElementById('auth-submit');
const authError = document.getElementById('auth-error');

const dashboardView = document.getElementById('dashboard-view');
const historyView = document.getElementById('history-view');
const settingsView = document.getElementById('settings-view');

const btnSettings = document.getElementById('settings-btn');
const btnBackSettings = document.getElementById('back-settings-btn');
const btnViewHistory = document.getElementById('view-history-btn');
const btnBackDashboard = document.getElementById('back-dashboard-btn');
const btnTheme = document.getElementById('theme-btn');

// Theme Management
let isLightMode = localStorage.getItem('theme') === 'light';
if (isLightMode) document.body.classList.add('light-mode');

function getCssVar(name) {
    return getComputedStyle(document.body).getPropertyValue(name).trim();
}

const editModal = document.getElementById('edit-modal');
const editForm = document.getElementById('edit-form');
const filterSearch = document.getElementById('filter-search');
const filterType = document.getElementById('filter-type');
const exportCsvBtn = document.getElementById('export-csv-btn');

// Budget Elements
const budgetMonthLabel = document.getElementById('budget-month-label');
const budgetSpent = document.getElementById('budget-spent');
const budgetLimit = document.getElementById('budget-limit');
const budgetProgressBar = document.getElementById('budget-progress-bar');
const btnSetBudget = document.getElementById('set-budget-btn');
const budgetModal = document.getElementById('budget-modal');
const budgetForm = document.getElementById('budget-form');
const closeBudgetBtn = document.getElementById('close-budget-btn');

// Category Elements
const categoryList = document.getElementById('category-list');
const addCategoryForm = document.getElementById('add-category-form');

// Insights
const insightsTicker = document.getElementById('insights-ticker');
let insightInterval = null;

function navigateTo(url) {
    history.pushState(null, null, url);
    router();
}

async function router() {
    const path = window.location.pathname;

    if (!state.token && path !== '/login') {
        navigateTo('/login');
        return;
    }
    
    if (state.token && (path === '/login' || path === '/')) {
        navigateTo('/dashboard');
        return;
    }

    if (path === '/login') {
        showAuth();
    } else if (path === '/dashboard') {
        await showDashboard();
    } else if (path === '/history') {
        await showHistory();
    } else if (path === '/settings') {
        await showSettings();
    } else {
        navigateTo(state.token ? '/dashboard' : '/login');
    }
}

window.addEventListener('popstate', router);

// Init
function init() {
    router();
}

// UI Toggles
btnLoginMode.addEventListener('click', () => {
    state.isLoginMode = true;
    btnLoginMode.classList.add('active');
    btnRegisterMode.classList.remove('active');
    authSubmit.textContent = 'Sign In';
});

btnRegisterMode.addEventListener('click', () => {
    state.isLoginMode = false;
    btnRegisterMode.classList.add('active');
    btnLoginMode.classList.remove('active');
    authSubmit.textContent = 'Register';
});

document.getElementById('logout-btn').addEventListener('click', () => {
    localStorage.removeItem('jwt');
    state.token = null;
    navigateTo('/login');
});

btnBackDashboard.addEventListener('click', () => {
    navigateTo('/dashboard');
});

btnSettings.addEventListener('click', () => navigateTo('/settings'));
btnBackSettings.addEventListener('click', () => navigateTo('/dashboard'));

btnTheme.addEventListener('click', () => {
    isLightMode = !isLightMode;
    if(isLightMode) {
        document.body.classList.add('light-mode');
        localStorage.setItem('theme', 'light');
    } else {
        document.body.classList.remove('light-mode');
        localStorage.setItem('theme', 'dark');
    }
    // Refresh to redraw charts accurately
    if (!dashboardView.classList.contains('hidden')) {
        loadDashboardData();
    }
});

document.getElementById('close-modal-btn').addEventListener('click', () => {
    editModal.classList.add('hidden');
});

// Authentication
authForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const endpoint = state.isLoginMode ? '/auth/login' : '/auth/register';
    const payload = { username: usernameInput.value, password: passwordInput.value };
    
    try {
        const res = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if(!res.ok) throw new Error("Request failed");
        
        if(state.isLoginMode) {
            const token = await res.text();
            if(token === 'Invalid') throw new Error("Invalid Credentials");
            state.token = token;
            localStorage.setItem('jwt', token);
            navigateTo('/dashboard');
        } else {
            state.isLoginMode = true;
            btnLoginMode.click();
            authError.style.color = '#10b981';
            authError.textContent = "Registration successful! Please login.";
            setTimeout(()=> { authError.style.color = '#ef4444'; authError.textContent =''; }, 3000);
        }
    } catch(err) {
        authError.textContent = err.message;
    }
});

function showAuth() {
    authContainer.classList.remove('hidden');
    appContainer.classList.add('hidden');
}

async function showDashboard() {
    authContainer.classList.add('hidden');
    appContainer.classList.remove('hidden');
    historyView.classList.add('hidden');
    settingsView.classList.add('hidden');
    dashboardView.classList.remove('hidden');
    await loadDashboardData();
}

// Fetch Logic
async function loadDashboardData() {
    try {
        // Only load data since router handles visibility
        const res = await fetch('/api/dashboard', {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if(!res.ok) throw new Error("Unauthorized");
        const data = await res.json();
        
        document.getElementById('total-income').textContent = `₹${data.totalIncome.toFixed(2)}`;
        document.getElementById('total-expense').textContent = `₹${data.totalExpense.toFixed(2)}`;
        document.getElementById('total-balance').textContent = `₹${data.balance.toFixed(2)}`;
        
        let mExpense = data.monthlyExpense || 0;
        let mBudget = data.monthlyBudget || 0;
        budgetMonthLabel.textContent = `(${data.currentMonthStr})`;
        budgetSpent.textContent = `₹${mExpense.toFixed(2)}`;
        budgetLimit.textContent = mBudget > 0 ? `₹${mBudget.toFixed(2)}` : 'Not Set';
        
        let percent = mBudget > 0 ? (mExpense / mBudget) * 100 : 0;
        if(percent > 100) percent = 100;
        budgetProgressBar.style.width = `${percent}%`;
        
        if(percent >= 90) {
            budgetProgressBar.style.background = getCssVar('--expense');
        } else if(percent >= 75) {
            budgetProgressBar.style.background = getCssVar('--text-muted');
        } else {
            budgetProgressBar.style.background = getCssVar('--accent-primary');
        }
        
        renderList(data.recentTransactions, 'transaction-list', false);
        
        const catRes = await fetch('/api/dashboard/categories', {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        const catData = await catRes.json();
        renderChart(catData);
        
        await renderHistoricalChart();
        await fetchAndPopulateCategories();
        await loadInsights();
        await loadSubscriptions();
    } catch(err) {
        console.error(err);
        document.getElementById('logout-btn').click();
    }
}

function renderList(list, elementId, withActions=true) {
    const el = document.getElementById(elementId);
    if (!list || list.length === 0) {
        el.innerHTML = '<li class="t-item" style="justify-content:center;color:var(--text-muted);">No transactions found.</li>';
        return;
    }
    el.innerHTML = list.map(item => `
        <li class="t-item">
            <div class="t-info">
                <h4>${item.description || item.category} ${item.isRecurring ? '🔁' : ''}</h4>
                <p>${item.category} • ${new Date(item.dateTime).toLocaleString([], {year:'numeric',month:'short',day:'numeric',hour:'2-digit',minute:'2-digit'})}</p>
            </div>
            <div style="text-align: right;">
                <div class="t-amount ${item.type === 'INCOME' ? 'plus' : 'minus'}">
                    ${item.type === 'INCOME' ? '+' : '-'}₹${item.amount.toFixed(2)}
                </div>
                ${withActions ? `
                <div style="margin-top: 5px;">
                    <button class="btn-secondary" style="padding: 2px 8px; font-size: 0.8rem;" onclick='openEditModal(${JSON.stringify(item).replace(/'/g, "&#39;")})'>Edit</button>
                    <button class="btn-secondary" style="padding: 2px 8px; font-size: 0.8rem; color: var(--expense); border-color: rgba(239, 68, 68, 0.2);" onclick="deleteExpense(${item.id})">Delete</button>
                </div>
                ` : ''}
                ${item.billPath ? `<div style="margin-top: 5px;"><a href="/uploads/${item.billPath}" target="_blank" style="font-size: 0.8rem; color: #3b82f6; text-decoration: none;">📄 Bill</a></div>` : ''}
            </div>
        </li>
    `).join('');
}

function renderChart(data) {
    const ctx = document.getElementById('expenseChart').getContext('2d');
    if(state.pieChart) state.pieChart.destroy();
    if(data.length === 0) return;
    state.pieChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: data.map(d => d.category),
            datasets: [{
                data: data.map(d => d.total),
                backgroundColor: [
                    '#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#14b8a6', '#f43f5e'
                ],
                borderWidth: 1,
                borderColor: getCssVar('--border-color')
            }]
        },
        options: { plugins: { legend: { labels: { color: getCssVar('--text-main') } } } }
    });
}

async function loadInsights() {
    try {
        const res = await fetch('/api/dashboard/insights', {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if(res.ok) {
            const insights = await res.json();
            if(insights.length > 0) {
                if(insightInterval) clearInterval(insightInterval);
                insightsTicker.innerHTML = insights.map(i => `<div style="height: 1.2rem; line-height: 1.2rem;">${i}</div>`).join('');
                
                let currentIndex = 0;
                if(insights.length > 1) {
                    insightInterval = setInterval(() => {
                        currentIndex = (currentIndex + 1) % insights.length;
                        insightsTicker.style.top = `-${currentIndex * 1.2}rem`;
                    }, 4000); // Rotates every 4 seconds
                } else {
                    insightsTicker.style.top = '0';
                }
            }
        }
    } catch(e) {}
}

async function loadSubscriptions() {
    try {
        const res = await fetch('/api/dashboard/subscriptions', {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if(res.ok) {
            const subs = await res.json();
            const subsList = document.getElementById('subs-list');
            if(subs.length === 0) {
                subsList.innerHTML = '<li style="color: var(--text-muted);">No recurring subscriptions found.</li>';
            } else {
                subsList.innerHTML = subs.map(s => 
                    `<li style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                        <span>${s.name} (Next: ${s.nextCharge})</span>
                        <strong style="color: var(--expense);">₹${s.amount.toFixed(2)}</strong>
                    </li>`
                ).join('');
            }
        }
    } catch(e) {}
}

async function renderHistoricalChart() {
    const res = await fetch('/api/dashboard/historical', {
        headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if(!res.ok) return;
    const data = await res.json();
    
    if (state.barChart) state.barChart.destroy();
    
    const ctx = document.getElementById('historicalChart').getContext('2d');
    state.barChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: data.labels,
            datasets: [
                { label: 'Income', data: data.incomes, backgroundColor: '#10b981' },
                { label: 'Expense', data: data.expenses, backgroundColor: '#ef4444' }
            ]
        },
        options: {
            plugins: { legend: { labels: { color: getCssVar('--text-main') } } },
            scales: {
                x: { ticks: { color: getCssVar('--text-muted') }, grid: { display: false } },
                y: { ticks: { color: getCssVar('--text-muted') }, grid: { color: getCssVar('--border-color') } }
            }
        }
    });
}

// History & Advanced Features
async function showHistory() {
    try {
        authContainer.classList.add('hidden');
        appContainer.classList.remove('hidden');
        dashboardView.classList.add('hidden');
        settingsView.classList.add('hidden');
        historyView.classList.remove('hidden');

        const res = await fetch('/api/expenses', {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if(!res.ok) throw new Error("Unauthorized");
        state.historyData = await res.json();
        
        filterSearch.value = '';
        filterType.value = 'ALL';
        applyFilters();
    } catch(err) {
        console.error("Failed to load history");
    }
}

btnViewHistory.addEventListener('click', () => {
    navigateTo('/history');
});

function applyFilters() {
    const term = filterSearch.value.toLowerCase();
    const type = filterType.value;
    
    const filtered = state.historyData.filter(t => {
        const matchesTerm = (t.description && t.description.toLowerCase().includes(term)) || (t.category && t.category.toLowerCase().includes(term));
        const matchesType = type === 'ALL' || t.type === type;
        return matchesTerm && matchesType;
    });
    renderList(filtered, 'full-transaction-list', true);
}

filterSearch.addEventListener('keyup', applyFilters);
filterType.addEventListener('change', applyFilters);

// CSV Export
exportCsvBtn.addEventListener('click', () => {
    if(state.historyData.length === 0) return;
    let csvContent = "data:text/csv;charset=utf-8,ID,Date,Category,Type,Amount,Description\n";
    state.historyData.forEach(t => {
        const row = [t.id, t.dateTime, t.category, t.type, t.amount, `"${t.description || ''}"`].join(",");
        csvContent += row + "\r\n";
    });
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", "expense_report.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
});

// Category and Settings
async function showSettings() {
    try {
        authContainer.classList.add('hidden');
        appContainer.classList.remove('hidden');
        dashboardView.classList.add('hidden');
        historyView.classList.add('hidden');
        settingsView.classList.remove('hidden');
        await fetchAndPopulateCategories();
    } catch(err) {
        console.error("Failed to load settings");
    }
}

async function fetchAndPopulateCategories() {
    const res = await fetch('/api/categories', { headers: { 'Authorization': `Bearer ${state.token}` }});
    state.categories = await res.json();
    
    categoryList.innerHTML = state.categories.map(c => `
        <li style="display:flex; justify-content:space-between; padding: 10px; background: rgba(255,255,255,0.05); margin-bottom: 5px; border-radius: 5px; align-items: center;">
            <span>${c.name}</span>
            <button onclick="deleteCategory(${c.id})" class="btn-secondary" style="padding: 4px 8px; font-size: 0.8rem; border-color:#ef4444; color:#ef4444;">Del</button>
        </li>
    `).join('');
    
    const opts = `<option value="" disabled selected>Select Category</option>` + 
        state.categories.map(c => `<option value="${c.name}">${c.name}</option>`).join('');
    
    document.getElementById('t-category').innerHTML = opts;
    document.getElementById('edit-category').innerHTML = opts;
}

addCategoryForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = document.getElementById('new-category-name').value;
    await fetch('/api/categories', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${state.token}` },
        body: JSON.stringify({ name })
    });
    document.getElementById('new-category-name').value = '';
    await fetchAndPopulateCategories();
});

window.deleteCategory = async (id) => {
    if(!confirm("Delete this category?")) return;
    await fetch(`/api/categories/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${state.token}` }
    });
    await fetchAndPopulateCategories();
};

// Budget Operations
btnSetBudget.addEventListener('click', () => {
    budgetModal.classList.remove('hidden');
});

closeBudgetBtn.addEventListener('click', () => {
    budgetModal.classList.add('hidden');
});

budgetForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const amount = parseFloat(document.getElementById('budget-amount-input').value);
    const monthStr = budgetMonthLabel.textContent.replace('(', '').replace(')', '');
    
    try {
        await fetch(`/api/budgets/${monthStr}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${state.token}` },
            body: JSON.stringify({ amount })
        });
        budgetModal.classList.add('hidden');
        loadDashboardData();
    } catch(err) { console.error(err); }
});

// Operations
window.deleteTx = async (id) => {
    if(!confirm("Delete this transaction?")) return;
    try {
        await fetch(`/api/expenses/${id}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        state.historyData = state.historyData.filter(t => t.id !== id);
        applyFilters();
    } catch(err) { console.error(err); }
};

window.openEdit = (txn) => {
    document.getElementById('edit-id').value = txn.id;
    document.getElementById('edit-amount').value = txn.amount;
    document.getElementById('edit-category').value = txn.category;
    document.getElementById('edit-type').value = txn.type;
    document.getElementById('edit-desc').value = txn.description || '';
    document.getElementById('edit-recurring').checked = !!txn.isRecurring;
    
    editModal.classList.remove('hidden');
}

editForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
        const id = document.getElementById('edit-id').value;
        const formData = new FormData(editForm);
        formData.set('isRecurring', document.getElementById('edit-recurring').checked);
        
        const res = await fetch(`/api/expenses/${id}`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${state.token}` },
            body: formData
        });
        editModal.classList.add('hidden');
        if (window.location.pathname === '/history') {
            await showHistory(); // reload to get latest sorted lists and dates
        } else {
            await loadDashboardData();
        }
    } catch(err) { console.error(err); }
});

document.getElementById('transaction-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = document.getElementById('transaction-form');
    try {
        const formData = new FormData(form);
        formData.set('isRecurring', document.getElementById('t-recurring').checked);
        const res = await fetch('/api/expenses', {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.token}` },
            body: formData
        });
        form.reset();
        loadDashboardData();
    } catch(err) { console.error("Failed to add transaction"); }
});

// Boot
init();
